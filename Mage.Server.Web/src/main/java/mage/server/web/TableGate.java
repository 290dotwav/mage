package mage.server.web;

import com.google.gson.JsonObject;
import com.sun.management.GcInfo;
import mage.constants.TableState;
import mage.game.Table;
import mage.server.managers.ManagerFactory;
import org.apache.log4j.Logger;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * <b>The one road a new table takes through the door</b>, with {@link ServerLimits} at its
 * gate. Every way the browser has of making a table comes through here: the door's own
 * {@code table create} (TableOps) and the raw {@code roomCreateTable} / {@code roomCreateTournament}
 * calls the wire also carries (WebSession). Their JBoss port (17171) is not published by the
 * container, so the door is the only way in from outside.
 * <p>
 * Nothing here touches a table that exists: the count is read, the memory is read (once every
 * {@link ServerLimits#READING_MS}, kept between), and a new table is created or refused. The
 * check and the creation hold one lock, so two hosts pressing Open at once cannot make the
 * thirteenth table between them.
 */
final class TableGate {

    private static final Logger logger = Logger.getLogger(TableGate.class);

    private final ManagerFactory managerFactory;
    private ServerLimits.Reading reading;
    private long readAt;

    TableGate(ManagerFactory managerFactory) {
        this.managerFactory = managerFactory;
    }

    /** Tables waiting, starting or being played: everything but a finished one. */
    int tablesInProgress() {
        int n = 0;
        for (Table table : managerFactory.tableManager().getTables()) {
            if (table.getState() != TableState.FINISHED) {
                n++;
            }
        }
        return n;
    }

    /** The memory as last read, read again when the last reading is older than {@link ServerLimits#READING_MS}. */
    synchronized ServerLimits.Reading reading() {
        long now = System.currentTimeMillis();
        if (reading == null || now - readAt >= ServerLimits.READING_MS) {
            reading = new ServerLimits.Reading(heapAfterLastGc(), Runtime.getRuntime().maxMemory(), machineAvailable());
            readAt = now;
        }
        return reading;
    }

    ServerLimits.Verdict verdict() {
        return ServerLimits.decide(tablesInProgress(), reading());
    }

    /** Refuse with {@link ServerLimits#FULL} when the machine is full, say nothing otherwise. */
    void refuseIfFull(String who) {
        int tables = tablesInProgress();
        ServerLimits.Reading now = reading();
        ServerLimits.Verdict verdict = ServerLimits.decide(tables, now);
        if (verdict.open) {
            return;
        }
        logger.warn("Web door: a new table for " + who + " refused (" + verdict.why + "): " + describe(tables, now));
        throw new IllegalStateException(ServerLimits.FULL);
    }

    /** Check, then create, under one lock: the creation runs only if the machine is not full. */
    synchronized <T> T create(String who, Callable<T> creation) throws Exception {
        refuseIfFull(who);
        return creation.call();
    }

    /** What the lobby shows: "5/12 tables", and whether a table would be refused now. */
    JsonObject load() {
        int tables = tablesInProgress();
        ServerLimits.Reading now = reading();
        ServerLimits.Verdict verdict = ServerLimits.decide(tables, now);
        JsonObject o = new JsonObject();
        o.addProperty("tables", tables);
        o.addProperty("maxTables", ServerLimits.MAX_TABLES);
        o.addProperty("open", verdict.open);
        if (verdict.why == null) {
            o.add("why", null);
        } else {
            o.addProperty("why", verdict.why);
        }
        o.addProperty("heapAfterGcPct", now.heapShare() < 0 ? -1 : Math.round(now.heapShare() * 100));
        o.addProperty("maxHeapPct", Math.round(ServerLimits.MAX_HEAP_AFTER_GC * 100));
        o.addProperty("availableMb", now.available < 0 ? -1 : now.available / (1024 * 1024));
        o.addProperty("minAvailableMb", ServerLimits.MIN_AVAILABLE_BYTES / (1024 * 1024));
        return o;
    }

    private static String describe(int tables, ServerLimits.Reading r) {
        return tables + "/" + ServerLimits.MAX_TABLES + " tables, heap after GC "
                + (r.heapShare() < 0 ? "unknown" : Math.round(r.heapShare() * 100) + "%") + " of " + (r.heapMax / (1024 * 1024)) + " MB (limit "
                + Math.round(ServerLimits.MAX_HEAP_AFTER_GC * 100) + "%), available RAM "
                + (r.available < 0 ? "unknown" : (r.available / (1024 * 1024)) + " MB") + " (limit " + (ServerLimits.MIN_AVAILABLE_BYTES / (1024 * 1024)) + " MB)";
    }

    /**
     * The heap in use right after the most recent collection, young ones included: the
     * after-usage their GcInfo keeps for every pool, eden left out (a collection that empties
     * it says 0; a concurrent cycle's pause, which does not, would count garbage as load).
     * Before any collection, the heap in use now, which can only say more.
     */
    static long heapAfterLastGc() {
        Set<String> heapPools = new HashSet<>();
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP && !pool.getName().toLowerCase().contains("eden")) {
                heapPools.add(pool.getName());
            }
        }
        long latestEnd = -1;
        long used = -1;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (!(gc instanceof com.sun.management.GarbageCollectorMXBean)) {
                continue;
            }
            GcInfo info;
            try {
                info = ((com.sun.management.GarbageCollectorMXBean) gc).getLastGcInfo();
            } catch (RuntimeException ex) {
                continue;
            }
            if (info == null || info.getEndTime() < latestEnd) {
                continue;
            }
            long sum = 0;
            for (Map.Entry<String, MemoryUsage> pool : info.getMemoryUsageAfterGc().entrySet()) {
                if (heapPools.contains(pool.getKey())) {
                    sum += pool.getValue().getUsed();
                }
            }
            latestEnd = info.getEndTime();
            used = sum;
        }
        return used >= 0 ? used : ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    /** MemAvailable of the machine (/proc/meminfo; see {@link ServerLimits#MIN_AVAILABLE_BYTES}), -1 where there is none. */
    static long machineAvailable() {
        try {
            return ServerLimits.memAvailable(new String(Files.readAllBytes(Paths.get("/proc/meminfo")), StandardCharsets.US_ASCII));
        } catch (Exception ex) {
            return -1;
        }
    }
}

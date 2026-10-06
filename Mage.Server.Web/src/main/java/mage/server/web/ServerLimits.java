package mage.server.web;

/**
 * <b>How much the machine takes before the door refuses a new table.</b> The three numbers
 * live here and nowhere else (the Claude chairs' two are in ClaudeMTG,
 * {@code packages/agent/src/limits.ts}; docs/JOURNAL.md lists all five).
 * <p>
 * The owner, after the load test of 6 October (« Le serveur peut encaisser combien de
 * joueurs/tables simultanément ? »), approved these guards so that the server cannot be
 * blown up by accident. They only ever refuse a NEW table: a table already waiting or
 * playing is never touched, and joining one is never refused here.
 * <p>
 * What was measured (docs/JOURNAL.md, "What the machine holds"): about 70 MB of heap per
 * table, peaks of 140; at 8 bot tables the heap after a collection peaked at 66 % of a
 * 2 GB {@code -Xmx}. The heap is 3 GB since then, inside a 6 GB container.
 */
final class ServerLimits {

    private ServerLimits() {
    }

    /** Tables in progress (waiting, starting or playing; a finished one does not count) at which a new one is refused. */
    static final int MAX_TABLES = 12;

    /** XMage's heap still in use after the last collection, as a share of its maximum ({@code -Xmx}), over which a new table is refused. */
    static final double MAX_HEAP_AFTER_GC = 0.75;

    /**
     * The machine's available RAM ({@code MemAvailable} of /proc/meminfo, 1.5 GB) under which a new
     * table is refused. The door runs inside the XMage container, and Docker does not virtualise
     * /proc/meminfo: what it reads there is the whole machine's, Valheim and the Claude chairs
     * included, which is the figure wanted.
     */
    static final long MIN_AVAILABLE_BYTES = 1536L * 1024 * 1024;

    /** How long one reading of the memory is kept before it is taken again. */
    static final long READING_MS = 10_000;

    /** What a refused table is told; the site shows these words as they are. */
    static final String FULL = "Server full, try again in a moment";

    /** One reading of the memory. A figure that could not be read is -1 and refuses nothing. */
    static final class Reading {
        final long heapAfterGc;
        final long heapMax;
        final long available;

        Reading(long heapAfterGc, long heapMax, long available) {
            this.heapAfterGc = heapAfterGc;
            this.heapMax = heapMax;
            this.available = available;
        }

        /** The heap after the last collection as a share of its maximum, or -1 when either is unknown. */
        double heapShare() {
            return heapAfterGc < 0 || heapMax <= 0 ? -1 : (double) heapAfterGc / heapMax;
        }
    }

    /** Whether a new table may be opened, and if not, which limit says no: "tables", "heap" or "memory". */
    static final class Verdict {
        final boolean open;
        final String why;

        private Verdict(boolean open, String why) {
            this.open = open;
            this.why = why;
        }

        static final Verdict OPEN = new Verdict(true, null);

        static Verdict refused(String why) {
            return new Verdict(false, why);
        }
    }

    /** The decision, and only the decision: the table count first, then the heap, then the machine. */
    static Verdict decide(int tablesInProgress, Reading reading) {
        if (tablesInProgress >= MAX_TABLES) {
            return Verdict.refused("tables");
        }
        if (reading != null && reading.heapShare() > MAX_HEAP_AFTER_GC) {
            return Verdict.refused("heap");
        }
        if (reading != null && reading.available >= 0 && reading.available < MIN_AVAILABLE_BYTES) {
            return Verdict.refused("memory");
        }
        return Verdict.OPEN;
    }

    /** {@code MemAvailable} out of the text of /proc/meminfo, in bytes, or -1 when it is not there. */
    static long memAvailable(String meminfo) {
        if (meminfo == null) {
            return -1;
        }
        for (String line : meminfo.split("\n")) {
            if (!line.startsWith("MemAvailable:")) {
                continue;
            }
            String[] parts = line.substring("MemAvailable:".length()).trim().split("\\s+");
            try {
                long value = Long.parseLong(parts[0]);
                String unit = parts.length > 1 ? parts[1] : "";
                return "kB".equalsIgnoreCase(unit) ? value * 1024 : value;
            } catch (NumberFormatException ex) {
                return -1;
            }
        }
        return -1;
    }
}

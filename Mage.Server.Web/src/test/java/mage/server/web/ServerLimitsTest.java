package mage.server.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The door's limits on a new table (ServerLimits): the count, the heap after a collection,
 * the machine's available RAM, and a figure that could not be read refusing nothing.
 */
class ServerLimitsTest {

    private static final long MB = 1024L * 1024;
    private static final long GB = 1024L * MB;

    /** A machine at rest: 300 MB of a 3 GB heap after GC, 4 GB available. */
    private static ServerLimits.Reading calm() {
        return new ServerLimits.Reading(300 * MB, 3 * GB, 4 * GB);
    }

    @Test
    void theNumbersAreTheOnesTheOwnerApproved() {
        assertEquals(12, ServerLimits.MAX_TABLES);
        assertEquals(0.75, ServerLimits.MAX_HEAP_AFTER_GC);
        assertEquals(1536 * MB, ServerLimits.MIN_AVAILABLE_BYTES);
        assertEquals(10_000, ServerLimits.READING_MS);
        assertEquals("Server full, try again in a moment", ServerLimits.FULL);
    }

    @Test
    void elevenTablesLeaveRoomForATwelfth() {
        ServerLimits.Verdict verdict = ServerLimits.decide(11, calm());
        assertTrue(verdict.open);
        assertNull(verdict.why);
    }

    @Test
    void theThirteenthTableIsRefused() {
        ServerLimits.Verdict verdict = ServerLimits.decide(12, calm());
        assertFalse(verdict.open);
        assertEquals("tables", verdict.why);
        assertFalse(ServerLimits.decide(40, calm()).open);
    }

    @Test
    void aHeapOverThreeQuartersAfterGcRefusesWhateverTheCount() {
        ServerLimits.Reading full = new ServerLimits.Reading((long) (0.76 * 3 * GB), 3 * GB, 4 * GB);
        ServerLimits.Verdict verdict = ServerLimits.decide(0, full);
        assertFalse(verdict.open);
        assertEquals("heap", verdict.why);
        // exactly three quarters is not over it
        assertTrue(ServerLimits.decide(0, new ServerLimits.Reading(3 * GB / 4, 3 * GB, 4 * GB)).open);
    }

    @Test
    void lessThanOneAndAHalfGbAvailableRefuses() {
        ServerLimits.Verdict verdict = ServerLimits.decide(2, new ServerLimits.Reading(300 * MB, 3 * GB, 1536 * MB - 1));
        assertFalse(verdict.open);
        assertEquals("memory", verdict.why);
        assertTrue(ServerLimits.decide(2, new ServerLimits.Reading(300 * MB, 3 * GB, 1536 * MB)).open);
    }

    @Test
    void theCountIsSaidFirstWhenSeveralLimitsAreReached() {
        ServerLimits.Reading everything = new ServerLimits.Reading(3 * GB, 3 * GB, 0);
        assertEquals("tables", ServerLimits.decide(12, everything).why);
        assertEquals("heap", ServerLimits.decide(1, everything).why);
    }

    @Test
    void aFigureThatCouldNotBeReadRefusesNothing() {
        assertTrue(ServerLimits.decide(3, new ServerLimits.Reading(-1, 3 * GB, -1)).open);
        assertTrue(ServerLimits.decide(3, new ServerLimits.Reading(2 * GB, 0, -1)).open);
        assertTrue(ServerLimits.decide(3, null).open);
        assertFalse(ServerLimits.decide(12, null).open);
    }

    @Test
    void memAvailableIsReadOffProcMeminfo() {
        String meminfo = "MemTotal:       11960520 kB\nMemFree:          512000 kB\nMemAvailable:    3355443 kB\nBuffers:           40000 kB\n";
        assertEquals(3355443L * 1024, ServerLimits.memAvailable(meminfo));
        assertEquals(-1, ServerLimits.memAvailable("MemTotal: 100 kB\n"));
        assertEquals(-1, ServerLimits.memAvailable(null));
        assertEquals(-1, ServerLimits.memAvailable("MemAvailable: lots kB\n"));
    }

    @Test
    void theHeapAfterGcIsReadFromThisJvm() {
        System.gc();
        long used = TableGate.heapAfterLastGc();
        assertTrue(used > 0, "some heap is in use after a collection");
        assertTrue(used <= Runtime.getRuntime().maxMemory(), "never more than the heap's maximum");
    }
}

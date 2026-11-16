package mage.server.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A name that connects on a new socket leaves its older socket behind (« défaut 7 », 16 November: two tabs of one
 * browser, two devices): the door tells that one and closes it (TableOps.movedElsewhere, WebSession.movedElsewhere).
 */
class MovedElsewhereTest {

    @Test
    void aNewSocketForANameAlreadyConnectedLeavesTheOldOneBehind() {
        assertTrue(TableOps.movedElsewhere("web-old", "web-new"));
    }

    @Test
    void aFirstConnectionOrTheSameSocketLeavesNothing() {
        assertFalse(TableOps.movedElsewhere("", "web-new"), "no user of that name yet");
        assertFalse(TableOps.movedElsewhere(null, "web-new"));
        assertFalse(TableOps.movedElsewhere("web-new", "web-new"));
    }

    @Test
    void thePageIsToldInAFrameAndAClosingCodeOfItsOwn() {
        assertEquals("{\"kind\":\"elsewhere\"}", Frames.elsewhere());
        assertEquals(4000, Frames.ELSEWHERE_CODE);
    }
}

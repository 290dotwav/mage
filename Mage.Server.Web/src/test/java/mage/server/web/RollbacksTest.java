package mage.server.web;

import com.google.gson.JsonObject;
import mage.game.RollbackPoints;
import mage.view.ChatMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The lines a going back took back reach the screen as runs of line numbers on every view, and
 * every line of the log reaches it with its number, so the screen can drop the gone future's lines
 * from its log (« Quand on revient en arrière, faut que les logs aussi reviennent en arrière »).
 */
class RollbacksTest {

    @Test
    void eachGoingBackIsARunOfLineNumbers() {
        RollbackPoints points = new RollbackPoints();
        assertEquals("[]", Rollbacks.lines(points).toString());
        points.tookBack(412, 437);
        points.tookBack(9, 8); // nothing said since the point: no run
        points.tookBack(500, 500);
        assertEquals("[[412,437],[500,500]]", Rollbacks.lines(points).toString());
    }

    @Test
    void aChatLineOfTheLogCarriesItsNumberAndTalkDoesNot() {
        JsonObject line = Frames.GSON.toJsonTree(new ChatMessage("", "Bob draws a card", null, null, ChatMessage.MessageColor.BLACK, ChatMessage.MessageType.GAME, null, 41)).getAsJsonObject();
        assertEquals(41, line.get("gameLine").getAsInt());
        JsonObject talk = Frames.GSON.toJsonTree(new ChatMessage("Ann", "gg", null, null, ChatMessage.MessageColor.BLUE, ChatMessage.MessageType.TALK, null)).getAsJsonObject();
        assertTrue(talk.get("gameLine").isJsonNull());
    }
}

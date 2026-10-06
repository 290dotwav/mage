package mage.server.web;

import mage.players.PlayerType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A socket the site vouches for plays under its account's name and no other (DoorIdentity):
 * the header the site's Worker sets, the watcher's name, the bots' seats, the raw calls.
 */
class DoorIdentityTest {

    @Test
    void noHeaderIsAnyName() {
        assertSame(DoorIdentity.ANYONE, DoorIdentity.fromHeader(null));
        assertSame(DoorIdentity.ANYONE, DoorIdentity.fromHeader(""));
        assertSame(DoorIdentity.ANYONE, DoorIdentity.fromHeader("   "));
        assertFalse(DoorIdentity.ANYONE.restricted());
        assertTrue(DoorIdentity.ANYONE.allows("Claude"));
        assertDoesNotThrow(() -> DoorIdentity.ANYONE.check("Anybody", "table create"));
    }

    @Test
    void theHeaderIsTheAccountUriEncoded() {
        DoorIdentity fg = DoorIdentity.fromHeader("fgmuller");
        assertTrue(fg.restricted());
        assertEquals("fgmuller", fg.player);
        // encodeURIComponent("Élodie 2"), as the Worker sends a name outside ASCII
        assertEquals("Élodie 2", DoorIdentity.fromHeader("%C3%89lodie%202").player);
    }

    @Test
    void onlyItsOwnNameAndItsWatcherName() {
        DoorIdentity fg = DoorIdentity.fromHeader("fgmuller");
        assertTrue(fg.allows("fgmuller"));
        assertTrue(fg.allows("fgmuller 👀"));
        assertFalse(fg.allows("Fgmuller"));
        assertFalse(fg.allows("Altr"));
        assertFalse(fg.allows("Altr 👀"));
        assertFalse(fg.allows(null));
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, () -> fg.check("Altr", "table join"));
        assertTrue(refused.getMessage().contains("plays as 'fgmuller'"), refused.getMessage());
    }

    @Test
    void theWatcherNameIsCutLikeTheSiteCutsIt() {
        // spectatorName: 14 characters at most, the mark takes three (a space and a surrogate pair)
        assertEquals("grossomoddo 👀", DoorIdentity.watcherName("grossomoddo"));
        assertEquals("abcdefghijk 👀", DoorIdentity.watcherName("abcdefghijklmn"));
        assertEquals(14, DoorIdentity.watcherName("abcdefghijklmn").length());
        assertTrue(DoorIdentity.fromHeader("abcdefghijklmn").allows("abcdefghijk 👀"));
    }

    @Test
    void rawCallsThatNameAPersonAreChecked() {
        DoorIdentity fg = DoorIdentity.fromHeader("fgmuller");
        String[] chat = {"chatId", "userName", "message"};
        assertDoesNotThrow(() -> fg.checkCall("chatSendMessage", chat, new Object[]{null, "fgmuller", "hi"}));
        assertThrows(IllegalArgumentException.class, () -> fg.checkCall("chatSendMessage", chat, new Object[]{null, "Altr", "hi"}));
        String[] connect = {"userName", "password", "sessionId", "restoreSessionId", "version", "userIdStr"};
        assertThrows(IllegalArgumentException.class, () -> fg.checkCall("connectUser", connect, new Object[]{"Altr", "", "s", "", null, ""}));
        assertDoesNotThrow(() -> fg.checkCall("connectUser", connect, new Object[]{"fgmuller", "", "s", "", null, ""}));
        // calls that name nobody pass
        assertDoesNotThrow(() -> fg.checkCall("roomGetAllTables", new String[]{"roomId"}, new Object[]{null}));
    }

    @Test
    void aBotSeatKeepsItsNameAPersonSeatDoesNot() {
        DoorIdentity fg = DoorIdentity.fromHeader("fgmuller");
        String[] join = {"sessionId", "roomId", "tableId", "name", "playerType", "skill", "deckList", "password"};
        assertDoesNotThrow(() -> fg.checkCall("roomJoinTable", join, new Object[]{"s", null, null, "Bot1", PlayerType.COMPUTER_MAD, 2, null, ""}));
        assertThrows(IllegalArgumentException.class, () -> fg.checkCall("roomJoinTable", join, new Object[]{"s", null, null, "Altr", PlayerType.HUMAN, 2, null, ""}));
        assertDoesNotThrow(() -> fg.checkCall("roomJoinTable", join, new Object[]{"s", null, null, "fgmuller", PlayerType.HUMAN, 2, null, ""}));
    }

    @Test
    void anUnrestrictedSocketChecksNothing() {
        String[] chat = {"chatId", "userName", "message"};
        assertDoesNotThrow(() -> DoorIdentity.ANYONE.checkCall("chatSendMessage", chat, new Object[]{null, "Claude", "hi"}));
    }
}

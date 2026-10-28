package mage.server.web;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * DeckText's reading of a deck without a table (the "check" op): the names their importer
 * cannot find, taken from its messages, and the ellipsis names given the database's spelling.
 * The card database itself is not opened here: the names it knows are a set.
 */
class DeckTextTest {

    /** The importer's messages for a deck with two unknown cards, one on two lines, and one card replaced by name. */
    private static final String MESSAGES = String.join("\n",
            "Line 3: can't find card by number, will try to replace: 1 [???:3] Sol Ring",
            "Line 3: replaced to [C21:263] Sol Ring",
            "Line 59: ERROR, can't find card [Conquest Mission]",
            "Line 60: ERROR, can't find card [Võx, Child of the Collective]",
            "Line 61: ERROR, can't find card [Conquest Mission]",
            "Line 62: ERROR, you can't use night card in deck [Some Night Face]",
            "");

    @Test
    void theUnknownNamesAreReadOffTheImportersErrorsOnceEachInOrder() {
        assertEquals(Arrays.asList("Conquest Mission", "Võx, Child of the Collective"), DeckText.unknownNames(MESSAGES));
        assertEquals("The Borg Queen", DeckText.unknownIn("Line 1: ERROR, can't find card [The Borg Queen]"));
        assertNull(DeckText.unknownIn("Line 62: ERROR, you can't use night card in deck [Some Night Face]"));
        assertNull(DeckText.unknownIn("Line 3: replaced to [C21:263] Sol Ring"));
        assertEquals(0, DeckText.unknownNames("").size());
    }

    @Test
    void aSpacedEllipsisTakesTheSpellingTheDatabaseKnows() {
        Set<String> db = new HashSet<>(Arrays.asList("Set Phasers to...", "I'm a Doctor, Not a ...", "Sol Ring"));
        String in = String.join("\n",
                "NAME:Borg",
                "1 [TRK:34] Set Phasers to . . .",
                "1 [???:2] I'm a Doctor, Not a . . .",
                "1 [???:3] Sol Ring",
                "1 [???:4] All Good Things . . .",
                "SB: 1 [???:5] Set Phasers to...",
                "");
        String out = String.join("\n",
                "NAME:Borg",
                "1 [TRK:34] Set Phasers to...",
                "1 [???:2] I'm a Doctor, Not a ...",
                "1 [???:3] Sol Ring",
                "1 [???:4] All Good Things . . .",
                "SB: 1 [???:5] Set Phasers to...",
                "");
        assertEquals(out, DeckText.repairNames(in, db::contains));
    }

    @Test
    void aListWithoutEllipsisIsLeftExactlyAsItCame() {
        String in = "1 [C21:263] Sol Ring\nSB: 1 [???:2] The Borg Queen";
        assertEquals(in, DeckText.repairNames(in, name -> {
            throw new AssertionError("no lookup for a name without dots: " + name);
        }));
    }
}

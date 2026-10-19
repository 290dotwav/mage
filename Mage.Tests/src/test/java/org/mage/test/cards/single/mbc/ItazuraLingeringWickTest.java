package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class ItazuraLingeringWickTest extends CardTestPlayerBase {

    // the opponent names 2: takes 2, casts one card for free; the controller takes another into hand
    @Test
    public void test_Upkeep() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, "Itazura, Lingering Wick");
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Island", 3);
        addCard(Zone.LIBRARY, playerA, "Grizzly Bears");
        addCard(Zone.LIBRARY, playerA, "Hill Giant");
        addCard(Zone.LIBRARY, playerA, "Swamp");

        setChoiceAmount(playerB, 2); // the secret number
        setChoice(playerB, "Hill Giant"); // which spell to cast for free
        setChoice(playerB, true);
        setChoice(playerA, "Swamp"); // into hand

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertLife(playerB, 18);
        assertPermanentCount(playerB, "Hill Giant", 1);
        assertHandCount(playerA, "Swamp", 1);
        assertExileCount(playerA, "Grizzly Bears", 1);
    }
}

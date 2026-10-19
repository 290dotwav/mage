package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class TheWeaverKingTest extends CardTestPlayerBase {

    // three combat damage: mill three, and a creature card milled this turn comes over
    @Test
    public void test_MillAndSteal() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, "The Weaver King");
        addCard(Zone.GRAVEYARD, playerB, "Hill Giant"); // not from the library this turn
        removeAllCardsFromLibrary(playerB);
        addCard(Zone.LIBRARY, playerB, "Island", 2);
        addCard(Zone.LIBRARY, playerB, "Grizzly Bears");
        addCard(Zone.LIBRARY, playerB, "Island");

        attack(1, playerA, "The Weaver King");
        setChoice(playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 17);
        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertGraveyardCount(playerB, "Hill Giant", 1);
        assertGraveyardCount(playerB, "Island", 2);
        assertLibraryCount(playerB, 1);
    }

    // a creature card milled earlier this turn by something else counts; one already there before doesn't
    @Test
    public void test_EarlierMillCounts() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, "The Weaver King");
        addCard(Zone.BATTLEFIELD, playerA, "Island");
        addCard(Zone.HAND, playerA, "Thought Scour"); // target player mills two, you draw a card
        addCard(Zone.GRAVEYARD, playerB, "Grizzly Bears");
        removeAllCardsFromLibrary(playerB);
        addCard(Zone.LIBRARY, playerB, "Island", 4);
        addCard(Zone.LIBRARY, playerB, "Hill Giant");
        addCard(Zone.LIBRARY, playerB, "Island");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Thought Scour", playerB);
        attack(1, playerA, "The Weaver King");
        setChoice(playerA, "Hill Giant");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Hill Giant", 1);
        assertGraveyardCount(playerB, "Grizzly Bears", 1);
        assertGraveyardCount(playerB, "Island", 4);
    }
}

package org.mage.test.cards.single.frc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class TheUrSphinxTest extends CardTestPlayerBase {

    private static final String sphinx = "The Ur-Sphinx";

    // one Sphinx attacks: each player mills one, and one card from each can be cast for free
    @Test
    public void test_MillAndCast() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, sphinx);
        removeAllCardsFromLibrary(playerA);
        removeAllCardsFromLibrary(playerB);
        addCard(Zone.LIBRARY, playerA, "Island", 2);
        addCard(Zone.LIBRARY, playerA, "Lightning Bolt");
        addCard(Zone.LIBRARY, playerB, "Island", 2);
        addCard(Zone.LIBRARY, playerB, "Grizzly Bears");

        attack(1, playerA, sphinx);
        setChoice(playerA, true); // cast Lightning Bolt
        addTarget(playerA, playerB);
        setChoice(playerA, true); // cast Grizzly Bears

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, "Lightning Bolt", 1);
        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertLife(playerB, 20 - 3 - 10);
    }

    // eminence: other Sphinx spells cost {1} less while it is on the battlefield
    @Test
    public void test_EminenceOnBattlefield() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, sphinx);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 5);
        addCard(Zone.HAND, playerA, "Sphinx of Jwar Isle"); // {4}{U}{U}

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Sphinx of Jwar Isle");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Sphinx of Jwar Isle", 1);
    }
}

package org.mage.test.cards.single.c16;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * Tymna the Weaver: "At the beginning of your postcombat main phase, you may pay X life, where X is the number of
 * opponents that were dealt combat damage this turn. If you do, draw X cards."
 */
public class TymnaTheWeaverTest extends CardTestPlayerBase {

    private static final String tymna = "Tymna the Weaver";

    @Test
    public void testNobodyHitNothingAsked() {
        addCard(Zone.BATTLEFIELD, playerA, tymna);

        // strict mode: a "may" asked here would fail for want of an answer
        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20);
        assertHandCount(playerA, 0);
    }

    @Test
    public void testOneOpponentHitPaysOneDrawsOne() {
        addCard(Zone.BATTLEFIELD, playerA, tymna);
        addCard(Zone.LIBRARY, playerA, "Island", 3);

        attack(3, playerA, tymna, playerB);
        setChoice(playerA, true); // Pay 1 life?

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 2);
        assertLife(playerA, 20 + 2 - 1); // lifelink, then 1 paid
        assertHandCount(playerA, 1 + 1); // the draw of turn 3, then one more
    }

    @Test
    public void testOneOpponentHitMayDecline() {
        addCard(Zone.BATTLEFIELD, playerA, tymna);
        addCard(Zone.LIBRARY, playerA, "Island", 3);

        attack(3, playerA, tymna, playerB);
        setChoice(playerA, false);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20 + 2);
        assertHandCount(playerA, 1);
    }
}

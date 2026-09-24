package org.mage.test.cards.single.hob;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class GollumRiddleMasterTest extends CardTestPlayerBase {

    private static final String gollum = "Gollum, Riddle Master"; // 3/1

    @Test
    public void test_OddSpellsEachModeOnce() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 2);
        addCard(Zone.HAND, playerA, gollum);
        addCard(Zone.BATTLEFIELD, playerB, "Island", 2);
        addCard(Zone.BATTLEFIELD, playerB, "Swamp", 5);
        addCard(Zone.HAND, playerB, "Darkness", 4); // MV 1, odd
        addCard(Zone.HAND, playerB, "Think Twice"); // MV 2, even: no trigger

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, gollum);
        setChoice(playerA, "odd");

        // opponent casts odd spells
        castSpell(2, PhaseStep.UPKEEP, playerB, "Darkness");
        setModeChoice(playerA, "1"); // +1/+1 counter
        waitStackResolved(2, PhaseStep.UPKEEP);
        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Think Twice"); // even: nothing
        waitStackResolved(2, PhaseStep.PRECOMBAT_MAIN);
        castSpell(2, PhaseStep.POSTCOMBAT_MAIN, playerB, "Darkness");
        setModeChoice(playerA, "1"); // drain 2 (numbered among the modes still available)
        waitStackResolved(2, PhaseStep.POSTCOMBAT_MAIN);
        castSpell(2, PhaseStep.END_TURN, playerB, "Darkness");
        setModeChoice(playerA, "1"); // draw, the only one left
        waitStackResolved(2, PhaseStep.END_TURN);
        // fourth one: all modes used, nothing happens
        castSpell(3, PhaseStep.UPKEEP, playerB, "Darkness");

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertCounterCount(playerA, gollum, mage.counters.CounterType.P1P1, 1);
        assertLife(playerA, 22);
        assertLife(playerB, 18);
        assertHandCount(playerA, 2); // Gollum draw + turn 3 draw (turn 1 draw is skipped)
    }
}

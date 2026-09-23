package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class PantherHabitTest extends CardTestPlayerBase {

    // If equipped creature would be dealt damage, prevent that damage and put that many +1/+1 counters on it.
    // Equip {2}
    private static final String HABIT = "Panther Habit";

    @Test
    public void test_PreventAndCounters() {
        addCard(Zone.BATTLEFIELD, playerA, HABIT);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Silvercoat Lion");
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 2);
        addCard(Zone.HAND, playerB, "Lightning Bolt", 2);
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 2);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Equip", "Grizzly Bears");
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerB, "Lightning Bolt", "Grizzly Bears");
        waitStackResolved(1, PhaseStep.POSTCOMBAT_MAIN);
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerB, "Lightning Bolt", "Silvercoat Lion");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertCounterCount(playerA, "Grizzly Bears", CounterType.P1P1, 3);
        assertPowerToughness(playerA, "Grizzly Bears", 5, 5);
        assertDamageReceived(playerA, "Grizzly Bears", 0);
        assertGraveyardCount(playerA, "Silvercoat Lion", 1);
    }
}

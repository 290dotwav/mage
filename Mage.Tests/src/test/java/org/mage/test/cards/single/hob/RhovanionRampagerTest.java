package org.mage.test.cards.single.hob;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class RhovanionRampagerTest extends CardTestPlayerBase {

    private static final String rampager = "Rhovanion Rampager"; // 3/2
    private static final String bears = "Grizzly Bears"; // 2/2

    @Test
    public void test_SacrificeOnAttack_ThenDiesAmass() {
        addCard(Zone.BATTLEFIELD, playerA, rampager);
        addCard(Zone.BATTLEFIELD, playerA, bears);
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 2);
        addCard(Zone.HAND, playerB, "Lightning Bolt", 2);

        attack(1, playerA, rampager);
        setChoice(playerA, true); // sacrifice
        setChoice(playerA, bears);

        // after combat damage: 5/4, kill it with two bolts
        castSpell(1, PhaseStep.END_COMBAT, playerB, "Lightning Bolt", rampager);
        castSpell(1, PhaseStep.END_COMBAT, playerB, "Lightning Bolt", rampager);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertGraveyardCount(playerA, bears, 1);
        assertLife(playerB, 20 - 5);
        assertGraveyardCount(playerA, rampager, 1);
        // amass Goblins X where X is its last known power (5)
        assertPermanentCount(playerA, "Goblin Army Token", 1);
        assertCounterCount(playerA, "Goblin Army Token", CounterType.P1P1, 5);
    }

    @Test
    public void test_DeclineSacrifice() {
        addCard(Zone.BATTLEFIELD, playerA, rampager);
        addCard(Zone.BATTLEFIELD, playerA, bears);

        attack(1, playerA, rampager);
        setChoice(playerA, false);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertPermanentCount(playerA, bears, 1);
        assertPowerToughness(playerA, rampager, 3, 2);
        assertLife(playerB, 20 - 3);
    }
}

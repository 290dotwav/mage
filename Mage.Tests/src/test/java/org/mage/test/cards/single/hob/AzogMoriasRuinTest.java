package org.mage.test.cards.single.hob;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class AzogMoriasRuinTest extends CardTestPlayerBase {

    private static final String azog = "Azog, Moria's Ruin";

    @Test
    public void test_OpponentsCreature() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 3);
        addCard(Zone.HAND, playerA, azog);
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant"); // 3/3

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, azog);
        addTarget(playerA, "Hill Giant");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerB, "Hill Giant", 1);
        assertPermanentCount(playerB, "Goblin Army Token", 1);
        assertCounterCount(playerB, "Goblin Army Token", CounterType.P1P1, 3);
        assertPermanentCount(playerA, "Goblin Army Token", 0);
        assertHandCount(playerA, 0);
    }

    @Test
    public void test_OwnCreatureDraws() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 3);
        addCard(Zone.HAND, playerA, azog);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, azog);
        addTarget(playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, "Grizzly Bears", 1);
        assertCounterCount(playerA, "Goblin Army Token", CounterType.P1P1, 2);
        assertHandCount(playerA, 1);
    }
}

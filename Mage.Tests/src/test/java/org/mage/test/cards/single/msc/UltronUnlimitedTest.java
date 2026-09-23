package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class UltronUnlimitedTest extends CardTestPlayerBase {

    private static final String ultron = "Ultron, Unlimited";

    @Test
    public void test_AttackConnive_PayForRobot() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, ultron);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 1);
        addCard(Zone.LIBRARY, playerA, "Lightning Bolt");

        attack(1, playerA, ultron);
        // connive: the only card in hand (Lightning Bolt, nonland) is discarded -> counter
        setChoice(playerA, true); // pay {1}

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertCounterCount(playerA, ultron, CounterType.P1P1, 1);
        assertGraveyardCount(playerA, "Lightning Bolt", 1);
        assertPermanentCount(playerA, "Robot Villain Token", 1);
        assertLife(playerB, 20 - 3);
    }

    @Test
    public void test_OtherCreatureConnives_DeclinePay() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, ultron);
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 2);
        addCard(Zone.HAND, playerA, "Raffine's Informant"); // When this creature enters, it connives.
        addCard(Zone.LIBRARY, playerA, "Island");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Raffine's Informant");
        // connive: the only card in hand (Island) is discarded -> no counter
        setChoice(playerA, false); // don't pay {1}

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertCounterCount(playerA, "Raffine's Informant", CounterType.P1P1, 0);
        assertPermanentCount(playerA, "Robot Villain Token", 0);
    }
}

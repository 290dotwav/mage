package org.mage.test.cards.single.frc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class TamiyoUpriserCrownedTestFork extends CardTestPlayerBase {

    private static final String tamiyo = "Tamiyo, Upriser Crowned";

    // monarch on entering; the creature that hits the monarch is tapped and stunned
    @Test
    public void test_MonarchStunsAttacker() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 3);
        addCard(Zone.HAND, playerA, tamiyo);
        addCard(Zone.BATTLEFIELD, playerB, "Serra Angel"); // vigilance

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, tamiyo);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        runCode("monarch", 1, PhaseStep.PRECOMBAT_MAIN, playerA,
                (info, player, game) -> Assert.assertEquals(playerA.getId(), game.getMonarchId()));

        attack(2, playerB, "Serra Angel", playerA);
        setChoice(playerA, "Whenever one or more creatures deal combat damage to you while"); // order with the monarch's trigger

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 16);
        assertTapped("Serra Angel", true);
        assertCounterCount(playerB, "Serra Angel", CounterType.STUN, 1);
    }

    // not the monarch: nothing
    @Test
    public void test_NotMonarchNothing() {
        addCard(Zone.BATTLEFIELD, playerA, tamiyo);
        addCard(Zone.BATTLEFIELD, playerB, "Serra Angel");

        attack(2, playerB, "Serra Angel", playerA);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 16);
        assertTapped("Serra Angel", false);
        assertCounterCount(playerB, "Serra Angel", CounterType.STUN, 0);
    }
}

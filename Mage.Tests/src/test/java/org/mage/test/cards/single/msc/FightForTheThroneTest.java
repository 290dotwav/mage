package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestCommander4Players;

public class FightForTheThroneTest extends CardTestCommander4Players {

    private static final String fight = "Fight for the Throne";

    @Test
    public void test_KillWithCommanderOut_BecomeMonarch() {
        addCard(Zone.COMMAND, playerA, "Centaur Courser"); // commander, {2}{G} 3/3
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 5);
        addCard(Zone.HAND, playerA, fight);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Centaur Courser");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, fight);
        addTarget(playerA, "Centaur Courser");
        addTarget(playerA, "Grizzly Bears");

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertCounterCount(playerA, "Centaur Courser", CounterType.P1P1, 1);
        assertGraveyardCount(playerB, "Grizzly Bears", 1);
        Assert.assertEquals(playerA.getId(), currentGame.getMonarchId());
    }

    @Test
    public void test_NoCommander_NoMonarch() {
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant");
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);
        addCard(Zone.HAND, playerA, fight);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, fight);
        addTarget(playerA, "Hill Giant");
        addTarget(playerA, "Grizzly Bears");

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertGraveyardCount(playerB, "Grizzly Bears", 1);
        Assert.assertNull(currentGame.getMonarchId());
    }
}

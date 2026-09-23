package org.mage.test.cards.single.msh;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.player.TestPlayer;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class PoliticalTriumphTest extends CardTestPlayerBase {

    private static final String triumph = "Political Triumph";

    @Test
    public void test_CreatureEnters_ScryAndPlanCounter() {
        addCard(Zone.BATTLEFIELD, playerA, triumph);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);
        addCard(Zone.HAND, playerA, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");
        addTarget(playerA, TestPlayer.TARGET_SKIP); // scry 1: keep on top

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertCounterCount(playerA, triumph, CounterType.PLAN, 1);
    }

    @Test
    public void test_FourthCounter_SacrificeDrawAndCounters() {
        addCard(Zone.BATTLEFIELD, playerA, triumph);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant");
        addCard(Zone.HAND, playerA, "Grizzly Bears");

        addCounters(1, PhaseStep.PRECOMBAT_MAIN, playerA, triumph, CounterType.PLAN, 3);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");
        addTarget(playerA, TestPlayer.TARGET_SKIP); // scry 1: keep on top

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertGraveyardCount(playerA, triumph, 1);
        assertHandCount(playerA, 1);
        assertCounterCount(playerA, "Hill Giant", CounterType.P1P1, 1);
        assertCounterCount(playerA, "Grizzly Bears", CounterType.P1P1, 1);
    }
}

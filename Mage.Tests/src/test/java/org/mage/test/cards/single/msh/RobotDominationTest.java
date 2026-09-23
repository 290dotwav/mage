package org.mage.test.cards.single.msh;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class RobotDominationTest extends CardTestPlayerBase {

    private static final String domination = "Robot Domination";

    @Test
    public void test_OneOrMoreCreatureCards_TriggersOnce() {
        addCard(Zone.BATTLEFIELD, playerA, domination);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);
        addCard(Zone.HAND, playerA, "Pyroclasm");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Pyroclasm");

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertGraveyardCount(playerA, "Grizzly Bears", 2);
        assertCounterCount(playerA, domination, CounterType.PLAN, 1);
        assertLife(playerA, 19);
        assertHandCount(playerA, 1);
    }

    @Test
    public void test_ThirdCounter_SacrificeAndRobots() {
        addCard(Zone.BATTLEFIELD, playerA, domination);
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 3);
        addCard(Zone.HAND, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, "Mind Rot"); // target player discards two cards

        addCounters(1, PhaseStep.PRECOMBAT_MAIN, playerA, domination, CounterType.PLAN, 2);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Mind Rot", playerA);

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertGraveyardCount(playerA, domination, 1);
        assertPermanentCount(playerA, "Robot Villain Token", 3);
        assertLife(playerA, 19);
    }

    @Test
    public void test_NotOpponentsCreatureCards() {
        addCard(Zone.BATTLEFIELD, playerA, domination);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);
        addCard(Zone.HAND, playerA, "Shock");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", "Grizzly Bears");

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertCounterCount(playerA, domination, CounterType.PLAN, 0);
        assertLife(playerA, 20);
    }
}

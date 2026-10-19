package org.mage.test.cards.single.fra;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class JacesMachinationsTest extends CardTestPlayerBase {

    private static final String machinations = "Jace's Machinations";

    // cast on the opponent's turn: a Jace token with 8 loyalty, and its loyalty ability can be activated that turn
    @Test
    public void test_EmpowerAndActivateOnOpponentsTurn() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.HAND, playerA, machinations);

        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerA, machinations);
        waitStackResolved(2, PhaseStep.PRECOMBAT_MAIN, playerA);
        checkPermanentCounters("empowered", 2, PhaseStep.PRECOMBAT_MAIN, playerA, "Jace", CounterType.LOYALTY, 8);
        activateAbility(2, PhaseStep.POSTCOMBAT_MAIN, playerA, "-3: Draw a card");

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertCounterCount(playerA, "Jace", CounterType.LOYALTY, 5);
        assertHandCount(playerA, 1);
        assertGraveyardCount(playerA, machinations, 1);
    }

    // the permission ends with the turn
    @Test
    public void test_PermissionEndsWithTheTurn() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.HAND, playerA, machinations);

        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerA, machinations);
        checkPlayableAbility("this turn", 2, PhaseStep.POSTCOMBAT_MAIN, playerA, "-3: Draw a card", true);
        checkPlayableAbility("not on the next opponent's turn", 4, PhaseStep.PRECOMBAT_MAIN, playerA, "-3: Draw a card", false);

        setStrictChooseMode(true);
        setStopAt(4, PhaseStep.END_TURN);
        execute();

        assertCounterCount(playerA, "Jace", CounterType.LOYALTY, 8);
    }
}

package org.mage.test.cards.single.fra;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class SanctumLurkerTest extends CardTestPlayerBase {

    private static final String lurker = "Sanctum Lurker";

    // empower Jace 1; the token at 0 loyalty stays, then uses the granted +2
    @Test
    public void test_ZeroLoyaltyStaysAndPlusTwo() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 3);
        addCard(Zone.HAND, playerA, lurker);
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 1);
        addCard(Zone.HAND, playerB, "Shock");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, lurker);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPermanentCounters("empowered", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Jace", CounterType.LOYALTY, 1);

        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Shock", "Jace");
        waitStackResolved(2, PhaseStep.PRECOMBAT_MAIN);
        checkPermanentCount("still there at 0", 2, PhaseStep.PRECOMBAT_MAIN, playerA, "Jace", 1);

        activateAbility(3, PhaseStep.PRECOMBAT_MAIN, playerA, "+2: This planeswalker deals 1 damage");

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertCounterCount(playerA, "Jace", CounterType.LOYALTY, 2);
        assertLife(playerB, 19);
        assertLife(playerA, 21);
    }

    // without the Lurker, 0 loyalty is the graveyard as usual
    @Test
    public void test_WithoutLurkerZeroLoyaltyDies() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 3);
        addCard(Zone.HAND, playerA, lurker);
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 2);
        addCard(Zone.HAND, playerB, "Lightning Bolt");
        addCard(Zone.HAND, playerB, "Shock");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, lurker);

        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Lightning Bolt", lurker);
        waitStackResolved(2, PhaseStep.PRECOMBAT_MAIN);
        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Shock", "Jace");

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, lurker, 1);
        assertPermanentCount(playerA, "Jace", 0);
    }
}

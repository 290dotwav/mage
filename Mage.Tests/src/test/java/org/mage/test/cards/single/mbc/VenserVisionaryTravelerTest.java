package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class VenserVisionaryTravelerTest extends CardTestPlayerBase {

    private static final String venser = "Venser, Visionary Traveler";

    // +1: the flickered creature wasn't cast from the hand, so it comes back with two counters
    @Test
    public void test_FlickerGetsCounters() {
        addCard(Zone.BATTLEFIELD, playerA, venser);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "+1: Exile up to one");
        addTarget(playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.UPKEEP);
        execute();

        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertCounterCount(playerA, "Grizzly Bears", CounterType.P1P1, 2);
        assertCounterCount(playerA, venser, CounterType.LOYALTY, 5);
    }

    // a creature cast from the hand gets nothing
    @Test
    public void test_CastFromHandNoCounters() {
        addCard(Zone.BATTLEFIELD, playerA, venser);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);
        addCard(Zone.HAND, playerA, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertCounterCount(playerA, "Grizzly Bears", CounterType.P1P1, 0);
    }

    // -2: for each opponent, a nonland permanent back to hand
    @Test
    public void test_MinusTwo() {
        addCard(Zone.BATTLEFIELD, playerA, venser);
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "-2: For each opponent");
        addTarget(playerA, "Hill Giant");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerB, "Hill Giant", 1);
        assertCounterCount(playerA, venser, CounterType.LOYALTY, 2);
    }
}

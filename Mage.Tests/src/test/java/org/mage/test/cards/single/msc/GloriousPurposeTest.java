package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.player.TestPlayer;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class GloriousPurposeTest extends CardTestPlayerBase {

    private static final String purpose = "Glorious Purpose";
    private static final String informant = "Raffine's Informant"; // When this creature enters, it connives.

    @Test
    public void test_Connive_CounterAndPlan() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, purpose);
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 2);
        addCard(Zone.HAND, playerA, informant);
        addCard(Zone.LIBRARY, playerA, "Island");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, informant);

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertCounterCount(playerA, informant, CounterType.P1P1, 1);
        assertCounterCount(playerA, purpose, CounterType.PLAN, 1);
    }

    @Test
    public void test_SixthCounter_CastFreeRestToHand() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, purpose);
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 2);
        addCard(Zone.HAND, playerA, informant);
        addCard(Zone.LIBRARY, playerA, "Island");
        addCard(Zone.LIBRARY, playerA, "Forest");
        addCard(Zone.LIBRARY, playerA, "Grizzly Bears");
        addCard(Zone.LIBRARY, playerA, "Lightning Bolt");
        addCard(Zone.LIBRARY, playerA, "Plains"); // top: drawn by connive, then discarded

        addCounters(1, PhaseStep.PRECOMBAT_MAIN, playerA, purpose, CounterType.PLAN, 5);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, informant);
        setChoice(playerA, "Lightning Bolt"); // cast free
        setChoice(playerA, true);
        addTarget(playerA, playerB);
        // Grizzly Bears is the only castable card left, it is offered directly
        setChoice(playerA, true);

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertGraveyardCount(playerA, purpose, 1);
        assertLife(playerB, 20 - 3);
        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertHandCount(playerA, "Forest", 1);
        assertHandCount(playerA, "Island", 1);
        assertCounterCount(playerA, informant, CounterType.P1P1, 1);
    }
}

package org.mage.test.cards.single.trc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class SevenOfNineTertiaryAdjunctTest extends CardTestPlayerBase {

    private static final String seven = "Seven of Nine, Tertiary Adjunct";
    private static final String thopter = "Ornithopter";

    // a nonland card discarded: two charge counters on up to one target artifact
    @Test
    public void test_DiscardNonland() {
        addCard(Zone.BATTLEFIELD, playerA, seven);
        addCard(Zone.BATTLEFIELD, playerA, thopter);
        addCard(Zone.HAND, playerA, "Lightning Bolt");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "<i>That Is Irrelevant</i> &mdash; {T}: Draw a card");
        setChoice(playerA, "Lightning Bolt"); // discard
        addTarget(playerA, thopter);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, "Lightning Bolt", 1);
        assertCounterCount(playerA, thopter, CounterType.CHARGE, 2);
        assertTapped(seven, true);
    }

    // a land discarded: nothing more
    @Test
    public void test_DiscardLand() {
        addCard(Zone.BATTLEFIELD, playerA, seven);
        addCard(Zone.BATTLEFIELD, playerA, thopter);
        addCard(Zone.HAND, playerA, "Swamp");
        addCard(Zone.LIBRARY, playerA, "Swamp");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "<i>That Is Irrelevant</i> &mdash; {T}: Draw a card");
        setChoice(playerA, "Swamp"); // discard

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, "Swamp", 1);
        assertCounterCount(playerA, thopter, CounterType.CHARGE, 0);
    }
}

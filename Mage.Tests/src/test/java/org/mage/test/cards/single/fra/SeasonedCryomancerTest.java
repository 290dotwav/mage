package org.mage.test.cards.single.fra;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class SeasonedCryomancerTest extends CardTestPlayerBase {

    private static final String cryomancer = "Seasoned Cryomancer";

    // two nonland cards discarded: up to two creatures tapped and stunned
    @Test
    public void test_TwoNonlandDiscardedStunsTwo() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.HAND, playerA, cryomancer);
        addCard(Zone.HAND, playerA, "Lightning Bolt");
        addCard(Zone.HAND, playerA, "Shock");
        addCard(Zone.LIBRARY, playerA, "Swamp", 2);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, cryomancer);
        setChoice(playerA, "Lightning Bolt"); // discard
        setChoice(playerA, "Shock"); // discard
        addTarget(playerA, "Grizzly Bears^Hill Giant");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, "Lightning Bolt", 1);
        assertGraveyardCount(playerA, "Shock", 1);
        assertHandCount(playerA, "Swamp", 2);
        assertTapped("Grizzly Bears", true);
        assertTapped("Hill Giant", true);
        assertCounterCount(playerB, "Grizzly Bears", CounterType.STUN, 1);
        assertCounterCount(playerB, "Hill Giant", CounterType.STUN, 1);
    }

    // a land and a nonland card discarded: only one creature
    @Test
    public void test_OneNonlandDiscardedStunsOne() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.HAND, playerA, cryomancer);
        addCard(Zone.HAND, playerA, "Lightning Bolt");
        addCard(Zone.HAND, playerA, "Mountain");
        addCard(Zone.LIBRARY, playerA, "Swamp", 2);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, cryomancer);
        setChoice(playerA, "Lightning Bolt"); // discard
        setChoice(playerA, "Mountain"); // discard
        addTarget(playerA, "Hill Giant");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertTapped("Hill Giant", true);
        assertCounterCount(playerB, "Hill Giant", CounterType.STUN, 1);
        assertTapped("Grizzly Bears", false);
        assertCounterCount(playerB, "Grizzly Bears", CounterType.STUN, 0);
    }

    // two lands discarded: no reflexive trigger
    @Test
    public void test_OnlyLandsDiscardedNothing() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.HAND, playerA, cryomancer);
        addCard(Zone.HAND, playerA, "Mountain", 2);
        addCard(Zone.LIBRARY, playerA, "Lightning Bolt", 2);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, cryomancer);
        setChoice(playerA, "Mountain"); // discard
        setChoice(playerA, "Mountain"); // discard

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerA, "Lightning Bolt", 2);
        assertTapped("Grizzly Bears", false);
    }

    // {3}{U}{U}, Exile this card from your graveyard: Draw two cards.
    @Test
    public void test_GraveyardAbilityDraws() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, "Island", 5);
        addCard(Zone.GRAVEYARD, playerA, cryomancer);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{3}{U}{U}, Exile");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertExileCount(playerA, cryomancer, 1);
        assertHandCount(playerA, 2);
    }
}

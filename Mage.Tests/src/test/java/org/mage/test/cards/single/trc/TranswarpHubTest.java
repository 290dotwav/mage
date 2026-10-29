package org.mage.test.cards.single.trc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class TranswarpHubTest extends CardTestPlayerBase {

    private static final String hub = "Transwarp Hub";

    // mill five, up to two artifact cards to the hand
    @Test
    public void test_MillTakeTwoArtifacts() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.HAND, playerA, hub);
        addCard(Zone.LIBRARY, playerA, "Ornithopter", 3);
        addCard(Zone.LIBRARY, playerA, "Grizzly Bears", 2);
        skipInitShuffling();

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, hub);
        setChoice(playerA, "Ornithopter^Ornithopter");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerA, "Ornithopter", 2);
        assertGraveyardCount(playerA, "Ornithopter", 1);
        assertGraveyardCount(playerA, "Grizzly Bears", 2);
    }

    // station 3+: {T}: target creature can't be blocked this turn
    @Test
    public void test_Station3Unblockable() {
        addCard(Zone.BATTLEFIELD, playerA, hub);
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant"); // 3/3, stations it
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Wall of Wood"); // 0/3 defender

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Station");
        setChoice(playerA, "Hill Giant");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: Target creature", "Grizzly Bears");

        attack(1, playerA, "Grizzly Bears");
        block(1, playerB, "Wall of Wood", "Grizzly Bears"); // not allowed: it is unblockable
        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertCounterCount(playerA, hub, CounterType.CHARGE, 3);
        assertTapped(hub, true);
        assertLife(playerB, 20 - 2);
    }
}

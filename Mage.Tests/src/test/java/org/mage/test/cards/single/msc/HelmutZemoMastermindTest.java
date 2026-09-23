package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class HelmutZemoMastermindTest extends CardTestPlayerBase {

    private static final String zemo = "Helmut Zemo, Mastermind";

    @Test
    public void test_CastFromGraveyard_PayCost_ExileAndCounter() {
        addCard(Zone.BATTLEFIELD, playerA, zemo);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);
        addCard(Zone.GRAVEYARD, playerA, "Lightning Bolt");

        attack(1, playerA, zemo);
        addTarget(playerA, "Lightning Bolt");
        setChoice(playerA, true); // cast it
        addTarget(playerA, playerB);

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertTapped("Mountain", true); // the cost was paid
        assertExileCount(playerA, "Lightning Bolt", 1);
        assertCounterCount(playerA, zemo, CounterType.P1P1, 1);
        assertLife(playerB, 20 - 3 - 3);
    }

    @Test
    public void test_TooExpensive_NotTargetable() {
        addCard(Zone.BATTLEFIELD, playerA, zemo);
        addCard(Zone.GRAVEYARD, playerA, "Lava Axe"); // mv 5 > power 2

        attack(1, playerA, zemo);

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertGraveyardCount(playerA, "Lava Axe", 1);
        assertCounterCount(playerA, zemo, CounterType.P1P1, 0);
    }
}

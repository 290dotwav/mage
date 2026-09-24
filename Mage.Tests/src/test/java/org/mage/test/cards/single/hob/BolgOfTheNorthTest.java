package org.mage.test.cards.single.hob;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class BolgOfTheNorthTest extends CardTestPlayerBase {

    private static final String bolg = "Bolg of the North";

    @Test
    public void test_SacrificeDamageExcessAmass() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Air Elemental"); // 4/4
        addCard(Zone.HAND, playerA, bolg);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears"); // 2/2

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, bolg);
        setChoice(playerA, true); // sacrifice
        setChoice(playerA, "Air Elemental");
        addTarget(playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, "Air Elemental", 1);
        assertGraveyardCount(playerB, "Grizzly Bears", 1);
        assertPermanentCount(playerA, "Goblin Army Token", 1);
        assertCounterCount(playerA, "Goblin Army Token", CounterType.P1P1, 2);
    }

    @Test
    public void test_NoExcess() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, bolg);
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant"); // 3/3

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, bolg);
        setChoice(playerA, true);
        setChoice(playerA, "Grizzly Bears");
        addTarget(playerA, "Hill Giant");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertDamageReceived(playerB, "Hill Giant", 2);
        assertPermanentCount(playerA, "Goblin Army Token", 0);
    }
}

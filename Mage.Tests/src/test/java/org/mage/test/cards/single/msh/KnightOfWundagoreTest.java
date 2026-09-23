package org.mage.test.cards.single.msh;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class KnightOfWundagoreTest extends CardTestPlayerBase {

    private static final String knight = "Knight of Wundagore";

    @Test
    public void test_TriggersOnlyOnceEachTurn() {
        addCard(Zone.BATTLEFIELD, playerA, knight);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);
        // Put a +1/+1 counter on target creature.
        addCard(Zone.HAND, playerA, "Battlegrowth", 2);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Battlegrowth", "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Battlegrowth", "Grizzly Bears");

        setStopAt(1, PhaseStep.BEGIN_COMBAT);
        setStrictChooseMode(true);
        execute();

        assertCounterCount(playerA, "Grizzly Bears", CounterType.P1P1, 2);
        assertCounterCount(playerA, knight, CounterType.P1P1, 1);
    }
}

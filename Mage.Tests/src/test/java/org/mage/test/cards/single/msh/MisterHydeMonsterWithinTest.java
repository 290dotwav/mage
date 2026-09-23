package org.mage.test.cards.single.msh;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class MisterHydeMonsterWithinTest extends CardTestPlayerBase {

    private static final String hyde = "Mister Hyde, Monster Within";

    @Test
    public void test_ModeCounter() {
        addCard(Zone.BATTLEFIELD, playerA, hyde);

        setModeChoice(playerA, "1"); // turn 1 upkeep

        setStopAt(1, PhaseStep.DRAW);
        setStrictChooseMode(true);
        execute();

        assertCounterCount(playerA, hyde, CounterType.P1P1, 1);
        assertHandCount(playerA, 0);
    }

    @Test
    public void test_ModeRemoveCounterDraw() {
        addCard(Zone.BATTLEFIELD, playerA, hyde);
        addCard(Zone.BATTLEFIELD, playerA, "Forest");
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, "Battlegrowth");

        setModeChoice(playerA, "1"); // turn 1 upkeep: counter on Hyde
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Battlegrowth", "Grizzly Bears");

        setModeChoice(playerA, "2"); // turn 3 upkeep
        setChoice(playerA, "Grizzly Bears"); // creature to remove a counter from

        setStopAt(3, PhaseStep.PRECOMBAT_MAIN);
        setStrictChooseMode(true);
        execute();

        assertCounterCount(playerA, "Grizzly Bears", CounterType.P1P1, 0);
        assertCounterCount(playerA, hyde, CounterType.P1P1, 1);
        // one card from the upkeep trigger, one from the draw step
        assertHandCount(playerA, 2);
    }
}

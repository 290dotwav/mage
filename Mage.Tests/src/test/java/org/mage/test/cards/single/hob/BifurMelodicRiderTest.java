package org.mage.test.cards.single.hob;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class BifurMelodicRiderTest extends CardTestPlayerBase {

    private static final String bifur = "Bifur, Melodic Rider"; // 4/5 legendary Dwarf

    @Test
    public void test_WithEnduringStory_DwarfTriggersTwice() {
        addCard(Zone.BATTLEFIELD, playerA, bifur);
        addCard(Zone.BATTLEFIELD, playerA, "Isamaru, Hound of Konda"); // legendary
        addCard(Zone.BATTLEFIELD, playerA, "Ornithopter"); // artifact
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        attack(1, playerA, bifur);
        setChoice(playerA, "Whenever"); // order the two triggers
        addTarget(playerA, "Grizzly Bears");
        addTarget(playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertCounterCount(playerA, "Grizzly Bears", CounterType.P1P1, 2);
    }

    @Test
    public void test_WithoutEnduringStory() {
        addCard(Zone.BATTLEFIELD, playerA, bifur);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        attack(1, playerA, bifur);
        addTarget(playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertCounterCount(playerA, "Grizzly Bears", CounterType.P1P1, 1);
    }
}

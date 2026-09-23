package org.mage.test.cards.single.ice;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class ChromaticArmorTest extends CardTestPlayerBase {

    private static final String armor = "Chromatic Armor";

    @Test
    public void test_PreventChosenColor_ThenChangeIt() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, armor);
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 3);
        addCard(Zone.HAND, playerB, "Lightning Bolt");
        addCard(Zone.HAND, playerB, "Shock");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, armor, "Grizzly Bears");
        setChoice(playerA, "Chromatic Armor*choose a color"); // order of the enter replacements
        setChoice(playerA, "Red");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPermanentCounters("one sleight", 1, PhaseStep.PRECOMBAT_MAIN, playerA, armor, CounterType.SLEIGHT, 1);

        castSpell(1, PhaseStep.BEGIN_COMBAT, playerB, "Lightning Bolt", "Grizzly Bears");
        waitStackResolved(1, PhaseStep.BEGIN_COMBAT);
        checkPermanentCount("bolt prevented", 1, PhaseStep.BEGIN_COMBAT, playerA, "Grizzly Bears", 1);

        // {X} with X = 1: now green
        activateAbility(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "{X}: Put");
        setChoice(playerA, "Green");
        waitStackResolved(1, PhaseStep.POSTCOMBAT_MAIN);
        checkPermanentCounters("two sleight", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, armor, CounterType.SLEIGHT, 2);
        castSpell(1, PhaseStep.END_TURN, playerB, "Shock", "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.UPKEEP);
        execute();

        assertGraveyardCount(playerA, "Grizzly Bears", 1);
        assertTappedCount("Plains", true, 2);
        assertTappedCount("Island", true, 2);
    }
}

package org.mage.test.cards.single.trk;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class ConquestMissionTest extends CardTestPlayerBase {

    private static final String mission = "Conquest Mission";
    private static final String thopter = "Ornithopter"; // {0} artifact creature

    // each nontoken artifact: a quest counter and one Borg; the sixth and later: two Borgs. The Borg tokens themselves do not count.
    @Test
    public void test_QuestCountersAndBorgs() {
        addCard(Zone.BATTLEFIELD, playerA, mission);
        addCard(Zone.HAND, playerA, thopter, 6);

        for (int i = 0; i < 6; i++) {
            castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, thopter);
            waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        }

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertCounterCount(playerA, mission, CounterType.QUEST, 6);
        assertPermanentCount(playerA, thopter, 6);
        assertPermanentCount(playerA, "Borg Token", 5 + 2);
    }

    // the dotted names are Scryfall's (and the printed card's): « . . . », not « ... »
    @Test
    public void test_DottedNames() {
        addCard(Zone.HAND, playerA, "I'm a Doctor, Not a . . .");
        addCard(Zone.HAND, playerA, "Set Phasers to . . .");

        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerA, "I'm a Doctor, Not a . . .", 1);
        assertHandCount(playerA, "Set Phasers to . . .", 1);
    }
}

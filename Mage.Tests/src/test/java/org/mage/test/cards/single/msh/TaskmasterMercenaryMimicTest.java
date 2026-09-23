package org.mage.test.cards.single.msh;

import mage.abilities.keyword.FlyingAbility;
import mage.constants.PhaseStep;
import mage.constants.SubType;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class TaskmasterMercenaryMimicTest extends CardTestPlayerBase {

    // Photographic Reflexes -- At the beginning of your first main phase, until your next turn, Taskmaster becomes a
    // copy of up to one target creature on the battlefield or creature card in a graveyard, except his name is
    // Taskmaster, Mercenary Mimic and he's a legendary Human Mercenary Villain creature.
    private static final String TASKMASTER = "Taskmaster, Mercenary Mimic";

    @Test
    public void test_CopyGraveyardCardUntilYourNextTurn() {
        addCard(Zone.BATTLEFIELD, playerA, TASKMASTER);
        addCard(Zone.GRAVEYARD, playerB, "Serra Angel");

        addTarget(playerA, "Serra Angel"); // turn 1
        checkPT("copied", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, TASKMASTER, 4, 4);
        checkAbility("flying", 2, PhaseStep.PRECOMBAT_MAIN, playerA, TASKMASTER, FlyingAbility.class, true);
        checkSubType("human", 2, PhaseStep.PRECOMBAT_MAIN, playerA, TASKMASTER, SubType.MERCENARY, true);
        checkSubType("not an angel", 2, PhaseStep.PRECOMBAT_MAIN, playerA, TASKMASTER, SubType.ANGEL, false);

        // the copy ends as turn 3 begins, before the next trigger
        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.UPKEEP);
        execute();

        assertPowerToughness(playerA, TASKMASTER, 3, 5);
    }

    @Test
    public void test_CopyPermanent() {
        addCard(Zone.BATTLEFIELD, playerA, TASKMASTER);
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");

        addTarget(playerA, "Hill Giant");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.BEGIN_COMBAT);
        execute();

        assertPowerToughness(playerA, TASKMASTER, 3, 3);
        assertSubtype(TASKMASTER, SubType.VILLAIN);
    }
}

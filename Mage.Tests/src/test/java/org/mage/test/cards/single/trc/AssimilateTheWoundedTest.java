package org.mage.test.cards.single.trc;

import mage.constants.PhaseStep;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.game.permanent.Permanent;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class AssimilateTheWoundedTest extends CardTestPlayerBase {

    private static final String wounded = "Assimilate the Wounded";

    // a creature that died this turn comes over; one already in the graveyard is no target
    @Test
    public void test_DiedThisTurn() {
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 3);
        addCard(Zone.HAND, playerA, "Lightning Bolt");
        addCard(Zone.HAND, playerA, wounded);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.GRAVEYARD, playerB, "Hill Giant");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, wounded);
        addTarget(playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertCounterCount(playerA, "Grizzly Bears", CounterType.P1P1, 1);
        assertPowerToughness(playerA, "Grizzly Bears", 3, 3);
        BorgAssert.isBorg(getPermanent("Grizzly Bears", playerA), currentGame, SubType.BEAR);
        assertGraveyardCount(playerB, "Hill Giant", 1);
    }


    // Grafdigger's Cage keeps the card in the graveyard: nothing comes over, and nothing is left
    // waiting for it either. When its owner reanimates it later, it is his Bear, not a Borg.
    @Test
    public void test_KeptInGraveyardLeavesNothingBehind() {
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Grafdigger's Cage");
        addCard(Zone.HAND, playerA, "Lightning Bolt");
        addCard(Zone.HAND, playerA, wounded);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 2);
        addCard(Zone.BATTLEFIELD, playerB, "Swamp");
        addCard(Zone.HAND, playerB, "Shatter");
        addCard(Zone.HAND, playerB, "Reanimate");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, wounded);
        addTarget(playerA, "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkGraveyardCount("stayed", 1, PhaseStep.PRECOMBAT_MAIN, playerB, "Grizzly Bears", 1);
        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Shatter", "Grafdigger's Cage");
        waitStackResolved(2, PhaseStep.PRECOMBAT_MAIN);
        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Reanimate", "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerB, "Grizzly Bears", 1);
        Permanent bears = getPermanent("Grizzly Bears", playerB);
        Assert.assertFalse("not a Borg artifact", bears.isArtifact(currentGame));
        Assert.assertTrue("still a Bear", bears.hasSubtype(SubType.BEAR, currentGame));
        Assert.assertFalse("not a Borg", bears.hasSubtype(SubType.BORG, currentGame));
        assertCounterCount(playerB, "Grizzly Bears", CounterType.P1P1, 0);
    }
}

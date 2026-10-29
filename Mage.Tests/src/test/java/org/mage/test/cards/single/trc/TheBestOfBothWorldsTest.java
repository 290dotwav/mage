package org.mage.test.cards.single.trc;

import mage.constants.PhaseStep;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class TheBestOfBothWorldsTest extends CardTestPlayerBase {

    private static final String saga = "The Best of Both Worlds";
    private static final String bears = "Grizzly Bears";

    // I: his Bears assimilated (control, counter, Borg); II: +2/+2; III: exiled and back to its owner, a Bear again
    @Test
    public void test_ThreeChapters() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.HAND, playerA, saga);
        addCard(Zone.BATTLEFIELD, playerB, bears);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, saga);
        addTarget(playerA, bears);

        checkPermanentCount("I: his now", 1, PhaseStep.END_TURN, playerA, bears, 1);
        checkPT("I: 3/3", 1, PhaseStep.END_TURN, playerA, bears, 3, 3);
        checkPT("II: 5/5", 3, PhaseStep.BEGIN_COMBAT, playerA, bears, 5, 5);
        checkPT("II ends", 4, PhaseStep.UPKEEP, playerA, bears, 3, 3);

        setStrictChooseMode(true);
        setStopAt(5, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, bears, 0);
        assertPermanentCount(playerB, bears, 1);
        assertCounterCount(playerB, bears, CounterType.P1P1, 0);
        Assert.assertTrue(getPermanent(bears, playerB).hasSubtype(SubType.BEAR, currentGame));
        Assert.assertFalse(getPermanent(bears, playerB).isArtifact(currentGame));
        assertPermanentCount(playerA, saga, 0);
    }

    @Test
    public void test_ChapterOneIsBorg() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.HAND, playerA, saga);
        addCard(Zone.BATTLEFIELD, playerB, bears);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, saga);
        addTarget(playerA, bears);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.UPKEEP);
        execute();

        BorgAssert.isBorg(getPermanent(bears, playerA), currentGame, SubType.BEAR);
    }
}

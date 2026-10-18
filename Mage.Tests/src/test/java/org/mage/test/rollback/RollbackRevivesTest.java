package org.mage.test.rollback;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.RollbackPoints;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestMultiPlayerBaseWithRangeAll;

/**
 * Going back to before a player's loss brings him back. Their restore keeps wins and losses on purpose, so a
 * player killed after the point stayed dead while his permanents came back, and another player could take
 * one of them (a Commander game, 18 October).
 */
public class RollbackRevivesTest extends CardTestMultiPlayerBaseWithRangeAll {

    private static RollbackPoints.Point castOf(mage.game.Game game, String card) {
        for (RollbackPoints.Point p : game.getRollbackPoints().list()) {
            if (p.getKind() == RollbackPoints.Kind.CAST && p.getLabel().contains(card)) {
                return p;
            }
        }
        Assert.fail("no cast point for " + card);
        return null;
    }

    @Test
    public void test_BackBeforeALoss_ThePlayerIsBackWithHisBoard() {
        setLife(playerD, 3);
        addCard(Zone.BATTLEFIELD, playerD, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");
        addCard(Zone.HAND, playerA, "Lightning Bolt");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerD);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN, playerA);
        runCode("D is out, then back before the bolt", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            Assert.assertTrue("the bolt killed D", game.getPlayer(playerD.getId()).hasLost());
            Assert.assertEquals("his bears left with him", 0, game.getBattlefield().getAllActivePermanents(playerD.getId()).size());
            Assert.assertTrue(game.rollbackToPoint(castOf(game, "Lightning Bolt").getId()));
        });

        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        Assert.assertFalse("alive again", currentGame.getPlayer(playerD.getId()).hasLost());
        Assert.assertFalse(currentGame.getPlayer(playerD.getId()).hasLeft());
        Assert.assertTrue(currentGame.getPlayer(playerD.getId()).isInGame());
        assertLife(playerD, 3);
        assertPermanentCount(playerD, "Grizzly Bears", 1);
        assertHandCount(playerA, "Lightning Bolt", 1);
    }
}

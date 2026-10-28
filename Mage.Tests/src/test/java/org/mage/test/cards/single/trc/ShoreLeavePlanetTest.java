package org.mage.test.cards.single.trc;

import mage.constants.CardType;
import mage.constants.PhaseStep;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.game.permanent.Permanent;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class ShoreLeavePlanetTest extends CardTestPlayerBase {

    private static final String planet = "Shore Leave Planet";
    private static final String isamaru = "Isamaru, Hound of Konda"; // legendary 2/2

    // a copy of his legendary creature, not legendary (no legend rule), a Robot artifact too
    @Test
    public void test_CopyNotLegendaryRobotArtifact() {
        addCard(Zone.BATTLEFIELD, playerA, planet);
        addCard(Zone.BATTLEFIELD, playerA, isamaru);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 5);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{4}{U}: ", isamaru);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertPermanentCount(playerA, isamaru, 2);
        assertPermanentCount(playerA, planet, 0);
        boolean found = false;
        for (Permanent permanent : currentGame.getBattlefield().getAllActivePermanents()) {
            if (permanent.getName().equals(isamaru) && permanent.isArtifact(currentGame)) {
                found = true;
                Assert.assertFalse(permanent.isLegendary(currentGame));
                Assert.assertTrue(permanent.hasSubtype(SubType.ROBOT, currentGame));
                Assert.assertTrue(permanent.isCreature(currentGame));
                Assert.assertFalse(permanent.isLand(currentGame));
            }
        }
        Assert.assertTrue("the land became the copy", found);
    }

    // until end of turn only
    @Test
    public void test_BackToLand() {
        addCard(Zone.BATTLEFIELD, playerA, planet);
        addCard(Zone.BATTLEFIELD, playerA, isamaru);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 5);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{4}{U}: ", isamaru);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.UPKEEP);
        execute();

        assertPermanentCount(playerA, planet, 1);
        assertPermanentCount(playerA, isamaru, 1);
        assertType(planet, CardType.LAND, true);
    }
}

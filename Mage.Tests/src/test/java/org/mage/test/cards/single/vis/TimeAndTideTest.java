package org.mage.test.cards.single.vis;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.permanent.Permanent;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class TimeAndTideTest extends CardTestPlayerBase {

    private Permanent find(String name) {
        return currentGame.getBattlefield().getAllPermanents().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst().orElse(null);
    }

    @Test
    public void test_PhaseInAndOut() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.HAND, playerA, "Slip Out the Back"); // {U}: +1/+1 counter, target creature phases out
        addCard(Zone.HAND, playerA, "Time and Tide");
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Breezekeeper"); // phasing

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Slip Out the Back", "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Time and Tide");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        Assert.assertTrue("Grizzly Bears phased in", find("Grizzly Bears").isPhasedIn());
        Assert.assertFalse("Breezekeeper phased out", find("Breezekeeper").isPhasedIn());
    }
}

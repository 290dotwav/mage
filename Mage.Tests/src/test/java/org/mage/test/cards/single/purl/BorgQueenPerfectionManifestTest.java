package org.mage.test.cards.single.purl;

import mage.constants.CardType;
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
public class BorgQueenPerfectionManifestTest extends CardTestPlayerBase {

    private static final String queen = "Borg Queen, Perfection Manifest";

    // assimilate: an opponent's creature card comes over with a +1/+1 counter, a Borg artifact creature
    @Test
    public void test_Assimilate() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 6);
        addCard(Zone.HAND, playerA, queen);
        addCard(Zone.GRAVEYARD, playerB, "Hill Giant");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, queen);
        addTarget(playerA, "Hill Giant");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertCounterCount(playerA, "Hill Giant", CounterType.P1P1, 1);
        Permanent giant = getPermanent("Hill Giant", playerA);
        Assert.assertTrue(giant.isArtifact(currentGame));
        Assert.assertTrue(giant.hasSubtype(SubType.BORG, currentGame));
        Assert.assertFalse(giant.hasSubtype(SubType.GIANT, currentGame));
        // 3/3, +1/+1 counter, +2/+0 as an artifact creature
        assertPowerToughness(playerA, "Hill Giant", 6, 4);
        assertPowerToughness(playerA, queen, 3, 4);
    }
}

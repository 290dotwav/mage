package org.mage.test.cards.single.trc;

import mage.constants.SubType;
import mage.game.Game;
import mage.game.permanent.Permanent;
import org.junit.Assert;

/**
 * What assimilate leaves: a Borg artifact creature with no other creature type.
 *
 * @author Claude
 */
final class BorgAssert {

    private BorgAssert() {
    }

    static void isBorg(Permanent permanent, Game game, SubType lost) {
        Assert.assertNotNull("assimilated permanent", permanent);
        Assert.assertTrue("artifact", permanent.isArtifact(game));
        Assert.assertTrue("creature", permanent.isCreature(game));
        Assert.assertTrue("Borg", permanent.hasSubtype(SubType.BORG, game));
        Assert.assertFalse("lost " + lost, permanent.hasSubtype(lost, game));
    }
}

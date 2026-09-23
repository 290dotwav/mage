package org.mage.test.cards.single.msc;

import mage.abilities.keyword.TrampleAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class MBakuJabariChieftainTest extends CardTestPlayerBase {

    // At the beginning of your end step, if there is no monarch, target opponent becomes the monarch.
    // Whenever a creature attacks one of your opponents, if that player is the monarch, that creature gets +1/+1 and gains trample until end of turn.
    private static final String MBAKU = "M'Baku, Jabari Chieftain";

    @Test
    public void test_OpponentMonarchThenBoost() {
        addCard(Zone.BATTLEFIELD, playerA, MBAKU);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        addTarget(playerA, playerB); // end step: B becomes the monarch
        checkMonarch("B is the monarch", 2, PhaseStep.PRECOMBAT_MAIN, playerA, playerB);

        attack(3, playerA, "Grizzly Bears", playerB);
        checkPT("boosted", 3, PhaseStep.DECLARE_BLOCKERS, playerA, "Grizzly Bears", 3, 3);
        checkAbility("trample", 3, PhaseStep.DECLARE_BLOCKERS, playerA, "Grizzly Bears", TrampleAbility.class, true);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_COMBAT);
        execute();

        assertLife(playerB, 20 - 3);
        // combat damage to the monarch took the crown
        Assert.assertEquals("monarch", playerA.getId(), currentGame.getMonarchId());
    }
}

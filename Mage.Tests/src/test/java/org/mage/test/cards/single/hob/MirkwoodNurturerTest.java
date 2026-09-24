package org.mage.test.cards.single.hob;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.player.TestPlayer;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class MirkwoodNurturerTest extends CardTestPlayerBase {

    private static final String nurturer = "Mirkwood Nurturer"; // 3/2

    @Test
    public void test_ReturnAndCounter() {
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, nurturer);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, nurturer);
        addTarget(playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerA, "Grizzly Bears", 1);
        assertPowerToughness(playerA, nurturer, 4, 3);
    }

    @Test
    public void test_NoTargetNoCounter() {
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, nurturer);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, nurturer);
        addTarget(playerA, TestPlayer.TARGET_SKIP);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertPowerToughness(playerA, nurturer, 3, 2);
    }
}

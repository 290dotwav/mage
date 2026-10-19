package org.mage.test.cards.single.pf26;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class AuspiciousAquariumTest extends CardTestPlayerBase {

    private static final String aquarium = "Auspicious Aquarium";

    // the listed types get +2/+2, others don't
    @Test
    public void test_Boost() {
        addCard(Zone.BATTLEFIELD, playerA, aquarium);
        addCard(Zone.BATTLEFIELD, playerA, "Merfolk of the Pearl Trident");
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Merfolk of the Pearl Trident");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertPowerToughness(playerA, "Merfolk of the Pearl Trident", 3, 3);
        assertPowerToughness(playerA, "Grizzly Bears", 2, 2);
        assertPowerToughness(playerB, "Merfolk of the Pearl Trident", 1, 1);
    }

    // twelve of them at upkeep: you win
    @Test
    public void test_TwelveWin() {
        addCard(Zone.BATTLEFIELD, playerA, aquarium);
        addCard(Zone.BATTLEFIELD, playerA, "Merfolk of the Pearl Trident", 12);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertWonTheGame(playerA);
    }

    // a Brushwagg spoils it
    @Test
    public void test_BrushwaggNoWin() {
        addCard(Zone.BATTLEFIELD, playerA, aquarium);
        addCard(Zone.BATTLEFIELD, playerA, "Merfolk of the Pearl Trident", 12);
        addCard(Zone.BATTLEFIELD, playerA, "Brushwagg");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertLife(playerB, 20);
        assertPermanentCount(playerA, aquarium, 1);
        org.junit.Assert.assertFalse(currentGame.hasEnded());
    }
}

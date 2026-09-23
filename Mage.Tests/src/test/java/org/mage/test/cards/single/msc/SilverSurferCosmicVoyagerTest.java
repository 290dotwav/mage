package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class SilverSurferCosmicVoyagerTest extends CardTestPlayerBase {

    private static final String surfer = "Silver Surfer, Cosmic Voyager";

    @Test
    public void test_ExileAndReturn_LandTapped() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 6);
        addCard(Zone.BATTLEFIELD, playerA, "Forest");
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, surfer);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, surfer);
        addTarget(playerA, "Forest^Grizzly Bears");
        addTarget(playerA, org.mage.test.player.TestPlayer.TARGET_SKIP);

        checkPermanentCount("bears exiled", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Grizzly Bears", 0);
        checkPermanentCount("forest exiled", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Forest", 0);

        setStopAt(2, PhaseStep.UPKEEP);
        setStrictChooseMode(true);
        execute();

        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertPermanentCount(playerA, "Forest", 1);
        assertTapped("Forest", true);
        assertTapped("Grizzly Bears", false);
    }
}

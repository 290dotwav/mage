package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class ExtractPowerTest extends CardTestPlayerBase {

    @Test
    public void test_ExileTopOfEach_PlayFree() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, "Island", 6);
        addCard(Zone.HAND, playerA, "Extract Power");
        addCard(Zone.LIBRARY, playerA, "Forest");
        addCard(Zone.LIBRARY, playerB, "Hill Giant");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Extract Power");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        // no mana left: both are played for free from exile
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Hill Giant");
        waitStackResolved(1, PhaseStep.POSTCOMBAT_MAIN);
        showAvailableAbilities("avail", 1, PhaseStep.POSTCOMBAT_MAIN, playerA);
        playLand(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Forest");

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertPermanentCount(playerA, "Hill Giant", 1);
        assertPermanentCount(playerA, "Forest", 1);
    }

    @Test
    public void test_StillPlayableLaterTurns() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, "Island", 6);
        addCard(Zone.HAND, playerA, "Extract Power");
        addCard(Zone.LIBRARY, playerB, "Hill Giant");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Extract Power");
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Hill Giant");

        setStopAt(3, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertPermanentCount(playerA, "Hill Giant", 1);
        assertTappedCount("Island", false, 6);
    }
}

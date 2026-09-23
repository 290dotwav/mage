package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestCommander4Players;

public class KangDynastyTest extends CardTestCommander4Players {

    private static final String kang = "Kang Dynasty";

    @Test
    public void test_ChapterI_TapGoad_DrawOnDamage() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 4);
        addCard(Zone.HAND, playerA, kang);
        addCard(Zone.BATTLEFIELD, playerD, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerC, "Hill Giant");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, kang);
        // targets per opponent, in turn order D, C, B
        addTarget(playerA, "Grizzly Bears");
        addTarget(playerA, "Hill Giant");
        addTarget(playerA, org.mage.test.player.TestPlayer.TARGET_SKIP);

        checkPermanentTapped("bears tapped", 1, PhaseStep.POSTCOMBAT_MAIN, playerD, "Grizzly Bears", true, 1);
        // turn 2 (D): goaded bears must attack C or B
        addTarget(playerD, playerB);
        // turn 3 (C): goaded giant must attack D or B
        addTarget(playerC, playerD);

        setStopAt(3, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertLife(playerB, 20 - 2);
        assertLife(playerD, 20 - 3);
        assertHandCount(playerA, 1 + 2); // turn 1 draw (multiplayer) + one card per damaging creature
    }
}

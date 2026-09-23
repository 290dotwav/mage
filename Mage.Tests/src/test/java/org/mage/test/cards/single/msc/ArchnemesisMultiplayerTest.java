package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.permanent.Permanent;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.player.TestPlayer;
import org.mage.test.serverside.base.CardTestCommander4Players;

/**
 * @author Claude
 */
public class ArchnemesisMultiplayerTest extends CardTestCommander4Players {

    // Enchant opponent
    // Whenever you attack enchanted player, that player loses 2 life. You draw a card and gain 2 life.
    // Whenever a player attacks you, you may attach this Aura to that player.
    private static final String ARCH = "Archnemesis";

    private boolean enchants(TestPlayer player) {
        return player.getAttachments().stream()
                .map(currentGame::getPermanent)
                .anyMatch(p -> p != null && p.getName().equals(ARCH));
    }

    @Test
    public void test_AttackerGetsTheAura() {
        // Player order: A -> D -> C -> B
        addCard(Zone.HAND, playerA, ARCH);
        addCard(Zone.BATTLEFIELD, playerA, "Underground Sea", 3);
        addCard(Zone.BATTLEFIELD, playerD, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, ARCH, playerC);
        attack(2, playerD, "Grizzly Bears", playerA);
        setChoice(playerA, true); // move it onto D

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20 - 2);
        Assert.assertFalse("C is not", enchants(playerC));
        Assert.assertTrue("D is enchanted", enchants(playerD));
        assertLife(playerD, 20);
    }

    @Test
    public void test_Decline() {
        addCard(Zone.HAND, playerA, ARCH);
        addCard(Zone.BATTLEFIELD, playerA, "Underground Sea", 3);
        addCard(Zone.BATTLEFIELD, playerD, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, ARCH, playerC);
        attack(2, playerD, "Grizzly Bears", playerA);
        setChoice(playerA, false);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        Assert.assertTrue("C is still enchanted", enchants(playerC));
    }
}

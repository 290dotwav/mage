package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class ArchnemesisTest extends CardTestPlayerBase {

    // Enchant opponent
    // Whenever you attack enchanted player, that player loses 2 life. You draw a card and gain 2 life.
    // Whenever a player attacks you, you may attach this Aura to that player.
    private static final String ARCH = "Archnemesis";

    @Test
    public void test_AttackEnchantedPlayer() {
        addCard(Zone.HAND, playerA, ARCH);
        addCard(Zone.BATTLEFIELD, playerA, "Underground Sea", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, ARCH, playerB);
        attack(1, playerA, "Grizzly Bears", playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertLife(playerB, 20 - 2 - 2);
        assertLife(playerA, 22);
        assertHandCount(playerA, 1);
    }

    @Test
    public void test_OpponentAttacksMayMove() {
        // A's Archnemesis enchants C? Two players only: attach to B when B attacks A (already B -> nothing to move).
        // Here B controls the aura on A and A attacks B: B may move it back onto A.
        addCard(Zone.HAND, playerB, ARCH);
        addCard(Zone.BATTLEFIELD, playerB, "Underground Sea", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, ARCH, playerA);
        attack(3, playerA, "Grizzly Bears", playerB);
        setChoice(playerB, true); // "attach to that player": already attached to A, nothing happens

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_COMBAT);
        execute();

        Assert.assertTrue(playerA.getAttachments().stream()
                .anyMatch(id -> currentGame.getPermanent(id) != null && currentGame.getPermanent(id).getName().equals(ARCH)));
        assertLife(playerB, 20 - 2);
    }
}

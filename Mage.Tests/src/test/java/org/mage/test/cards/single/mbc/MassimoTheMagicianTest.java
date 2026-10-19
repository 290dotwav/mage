package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.player.TestPlayer;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class MassimoTheMagicianTest extends CardTestPlayerBase {

    // a creature enters, a 1-mana instant is exiled; its combat damage casts a copy for free, each time
    @Test
    public void test_ExileThenCopyOnCombatDamage() {
        addCard(Zone.BATTLEFIELD, playerA, "Massimo, the Magician");
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);
        addCard(Zone.HAND, playerA, "Grizzly Bears");
        addCard(Zone.GRAVEYARD, playerA, "Lightning Bolt");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");
        addTarget(playerA, "Lightning Bolt");

        attack(3, playerA, "Grizzly Bears");
        setChoice(playerA, true); // cast the copy
        addTarget(playerA, playerB);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertExileCount(playerA, "Lightning Bolt", 1);
        assertLife(playerB, 20 - 2 - 3);
    }

    // a card with mana value 2 can't be exiled
    @Test
    public void test_OnlyManaValueOne() {
        addCard(Zone.BATTLEFIELD, playerA, "Massimo, the Magician");
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);
        addCard(Zone.HAND, playerA, "Grizzly Bears");
        addCard(Zone.GRAVEYARD, playerA, "Divination");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");
        addTarget(playerA, TestPlayer.TARGET_SKIP);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, "Divination", 1);
    }
}

package org.mage.test.cards.single.msc;

import mage.abilities.keyword.IndestructibleAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class KlawMasterOfSoundTest extends CardTestPlayerBase {

    // Deathtouch
    // Whenever you play a card from exile, Klaw gains indestructible until end of turn.
    // Whenever Klaw deals combat damage to a player, look at the top card of that player's library, then exile it
    // face down. You may play it for as long as it remains exiled. Mana of any type can be spent to cast a spell this way.
    private static final String KLAW = "Klaw, Master of Sound";

    @Test
    public void test_StealAndPlay() {
        skipInitShuffling();
        removeAllCardsFromLibrary(playerB);
        addCard(Zone.LIBRARY, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, KLAW);
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 2);

        attack(1, playerA, KLAW, playerB);
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Grizzly Bears"); // with black mana

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 3);
        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertAbility(playerA, KLAW, IndestructibleAbility.getInstance(), true);
    }

    @Test
    public void test_NoPlayNoIndestructible() {
        addCard(Zone.BATTLEFIELD, playerA, KLAW);

        attack(1, playerA, KLAW, playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertExileCount(playerB, 1);
        assertAbility(playerA, KLAW, IndestructibleAbility.getInstance(), false);
    }
}

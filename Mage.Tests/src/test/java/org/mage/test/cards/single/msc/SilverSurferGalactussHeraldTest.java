package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class SilverSurferGalactussHeraldTest extends CardTestPlayerBase {

    // Flying
    // When Silver Surfer enters, you may search your library for a card named Galactus, Devourer of Worlds, reveal it, put it into your hand, then shuffle.
    // Whenever Silver Surfer deals combat damage to a player, until the end of your next turn, target creature attacks that player each combat if able.
    private static final String SURFER = "Silver Surfer, Galactus's Herald";

    @Test
    public void test_DamagedPlayersOwnCreatureIsNotForced() {
        // the bears can't attack their own controller: the requirement forces nothing
        addCard(Zone.BATTLEFIELD, playerA, SURFER);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        attack(1, playerA, SURFER, playerB);
        addTarget(playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_COMBAT);
        execute();

        assertLife(playerB, 20 - 4);
        assertLife(playerA, 20);
    }

    @Test
    public void test_OwnCreatureAttacksNextTurn() {
        addCard(Zone.BATTLEFIELD, playerA, SURFER);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        attack(1, playerA, SURFER, playerB);
        addTarget(playerA, "Grizzly Bears");
        // turn 3: the bears attack on their own (AI-less test player must be forced by the requirement)
        attack(3, playerA, SURFER, playerB);
        addTarget(playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_COMBAT);
        execute();

        assertLife(playerB, 20 - 4 - 4 - 2);
    }
}

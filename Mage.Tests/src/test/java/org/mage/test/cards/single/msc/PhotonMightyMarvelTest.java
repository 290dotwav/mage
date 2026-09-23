package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class PhotonMightyMarvelTest extends CardTestPlayerBase {

    // Flying
    // Whenever Photon deals combat damage to a player, add that much mana of any one color. Until end of turn, you don't lose this mana as steps and phases end.
    private static final String PHOTON = "Photon, Mighty Marvel";

    @Test
    public void test_ManaKeptUntilEndOfTurn() {
        addCard(Zone.BATTLEFIELD, playerA, PHOTON);
        addCard(Zone.HAND, playerA, "Grizzly Bears");

        attack(1, playerA, PHOTON, playerB);
        setChoice(playerA, "Green");
        // the mana survives into the second main phase
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 2);
        assertPermanentCount(playerA, "Grizzly Bears", 1);
    }
}

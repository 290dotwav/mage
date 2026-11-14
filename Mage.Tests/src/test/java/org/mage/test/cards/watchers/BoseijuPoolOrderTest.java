package org.mage.test.cards.watchers;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * ClaudeMTG fork: Boseiju's {C} pays the spell when another {C} is already floating.
 * The owner, 14 November 2026: "J'ai tap boseiju pour jouer un spell et qu'on ne puisse pas le contrer.
 * Il a utilise d'autres mana que j'avais deja en pool et j'ai pris un counterspell".
 */
public class BoseijuPoolOrderTest extends CardTestPlayerBase {

    @Test
    public void testBoseijuManaPaysBeforeOtherColorlessInThePool() {
        addCard(Zone.BATTLEFIELD, playerA, "Wastes");
        addCard(Zone.BATTLEFIELD, playerA, "Island");
        addCard(Zone.BATTLEFIELD, playerA, "Boseiju, Who Shelters All");
        addCard(Zone.HAND, playerA, "Impulse"); // {1}{U} instant
        addCard(Zone.BATTLEFIELD, playerB, "Island", 2);
        addCard(Zone.HAND, playerB, "Counterspell"); // {U}{U}

        // the Wastes' {C} floats first, then Boseiju's
        activateManaAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: Add {C}");
        activateManaAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}, Pay 2 life");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Impulse");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerB, "Counterspell", "Impulse", "Impulse");

        setStopAt(1, PhaseStep.BEGIN_COMBAT);
        setStrictChooseMode(false);
        execute();

        assertLife(playerA, 18);
        assertGraveyardCount(playerB, "Counterspell", 1);
        assertGraveyardCount(playerA, "Impulse", 1); // resolved, not countered: and so in the graveyard either way
        assertTappedCount("Wastes", true, 1);
        // countered, Impulse would put no card in the hand
        assertHandCount(playerA, 1);
    }
}

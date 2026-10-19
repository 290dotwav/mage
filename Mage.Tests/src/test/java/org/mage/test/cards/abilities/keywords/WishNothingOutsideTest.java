package org.mage.test.cards.abilities.keywords;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * A wish with nothing outside the game (every Commander table here has no sideboard): "Reveal a sorcery card you own
 * from outside the game and put it into your hand?" was asked, and offered nothing whatever the answer.
 */
public class WishNothingOutsideTest extends CardTestPlayerBase {

    @Test
    public void testNothingOutsideNothingAsked() {
        // Burning Wish {1}{R} sorcery: You may reveal a sorcery card you own from outside the game and put it into
        // your hand. Exile Burning Wish.
        addCard(Zone.HAND, playerA, "Burning Wish", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Burning Wish");

        // strict mode: a question asked here would fail for want of an answer
        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.BEGIN_COMBAT);
        execute();

        assertExileCount(playerA, "Burning Wish", 1);
        assertHandCount(playerA, 0);
    }
}

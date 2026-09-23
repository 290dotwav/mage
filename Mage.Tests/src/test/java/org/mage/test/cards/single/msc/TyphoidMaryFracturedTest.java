package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class TyphoidMaryFracturedTest extends CardTestPlayerBase {

    // Whenever Typhoid Mary attacks, choose one at random. If you discarded a card this turn, you choose one instead.
    // * Mary - Create a Treasure token. * Typhoid Mary - Draw a card. * Bloody Mary - Each opponent loses 2 life and you gain 2 life.
    private static final String MARY = "Typhoid Mary, Fractured";

    @Test
    public void test_DiscardedYouChoose() {
        addCard(Zone.BATTLEFIELD, playerA, MARY);
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Island", 2);
        addCard(Zone.HAND, playerA, "Faithless Looting"); // draw two cards, then discard two cards
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Faithless Looting");
        attack(1, playerA, MARY, playerB);
        setModeChoice(playerA, "3");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertLife(playerA, 22);
        assertLife(playerB, 20 - 3 - 2);
        assertPermanentCount(playerA, "Treasure Token", 0);
    }

    @Test
    public void test_NoDiscardIsRandom() {
        addCard(Zone.BATTLEFIELD, playerA, MARY);

        attack(1, playerA, MARY, playerB);
        // no mode choice: it is random

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        int treasure = currentGame.getBattlefield().getAllActivePermanents().stream()
                .filter(p -> p.getName().equals("Treasure Token")).mapToInt(p -> 1).sum();
        int drew = playerA.getHand().size();
        int drain = playerA.getLife() == 22 ? 1 : 0;
        Assert.assertEquals("exactly one mode happened", 1, treasure + drew + drain);
        assertLife(playerB, 20 - 3 - 2 * drain);
    }
}

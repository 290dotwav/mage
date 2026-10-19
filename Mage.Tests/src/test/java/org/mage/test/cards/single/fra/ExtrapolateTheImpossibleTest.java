package org.mage.test.cards.single.fra;

import mage.cards.Card;
import mage.cards.repository.CardRepository;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.player.TestPlayer;
import org.mage.test.serverside.base.CardTestPlayerBase;

import java.util.Collections;

/**
 * @author Claude
 */
public class ExtrapolateTheImpossibleTest extends CardTestPlayerBase {

    private static final String extrapolate = "Extrapolate the Impossible";

    private void addToSideboard(TestPlayer player, String cardName) {
        Card card = CardRepository.instance.findCards(cardName).get(0).createCard();
        currentGame.loadCards(Collections.singleton(card), player.getId());
        player.getSideboard().add(card);
    }

    // two cards with different names revealed, the opponent picks the one that goes to hand
    @Test
    public void test_OpponentChooses() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 2);
        addCard(Zone.HAND, playerA, extrapolate);
        addToSideboard(playerA, "Grizzly Bears");
        addToSideboard(playerA, "Hill Giant");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, extrapolate);
        setChoice(playerA, true); // reveal
        setChoice(playerA, "Grizzly Bears");
        setChoice(playerA, "Hill Giant");
        setChoice(playerB, "Hill Giant");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerA, "Hill Giant", 1);
        assertHandCount(playerA, "Grizzly Bears", 0);
        Assert.assertEquals("the other card stays outside the game", 1, playerA.getSideboard().size());
    }

    // two cards with the same name are not two cards with different names
    @Test
    public void test_SameNamesDoNothing() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 2);
        addCard(Zone.HAND, playerA, extrapolate);
        addToSideboard(playerA, "Grizzly Bears");
        addToSideboard(playerA, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, extrapolate);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerA, 0);
        Assert.assertEquals(2, playerA.getSideboard().size());
    }
}

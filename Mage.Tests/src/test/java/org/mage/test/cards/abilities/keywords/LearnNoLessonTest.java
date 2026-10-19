package org.mage.test.cards.abilities.keywords;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * Learn (701.48a): "You may discard a card. If you do, draw a card. If you didn't discard a card, you may reveal a
 * Lesson card you own from outside the game and put it into your hand." With no Lesson outside the game (a Commander
 * table, a deck without a sideboard), "Reveal a Lesson card ...?" offered nothing, and a "yes" to it skipped the
 * rummage: the player got neither.
 */
public class LearnNoLessonTest extends CardTestPlayerBase {

    // Igneous Inspiration {2}{R} sorcery: 3 damage to target creature or planeswalker. Learn.
    private static final String inspiration = "Igneous Inspiration";

    @Test
    public void testNoLessonOnlyTheRummageIsAsked() {
        addCard(Zone.HAND, playerA, inspiration, 1);
        addCard(Zone.HAND, playerA, "Plains", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 3);
        addCard(Zone.LIBRARY, playerA, "Island", 3);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears", 1);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, inspiration, "Grizzly Bears");
        setChoice(playerA, true); // discard a card to draw a card?
        setChoice(playerA, "Plains");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.BEGIN_COMBAT);
        execute();

        assertGraveyardCount(playerB, "Grizzly Bears", 1);
        assertGraveyardCount(playerA, "Plains", 1);
        assertHandCount(playerA, 1); // the Plains discarded, a card drawn
    }

    @Test
    public void testNoLessonRummageDeclined() {
        addCard(Zone.HAND, playerA, inspiration, 1);
        addCard(Zone.HAND, playerA, "Plains", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 3);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears", 1);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, inspiration, "Grizzly Bears");
        setChoice(playerA, false);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.BEGIN_COMBAT);
        execute();

        assertHandCount(playerA, "Plains", 1);
        assertGraveyardCount(playerA, inspiration, 1);
    }
}

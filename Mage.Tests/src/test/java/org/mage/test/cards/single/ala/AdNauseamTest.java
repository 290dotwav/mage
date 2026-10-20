package org.mage.test.cards.single.ala;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * Ad Nauseam: "Reveal the top card of your library and put that card into
 * your hand. You lose life equal to its mana value. You may repeat this
 * process any number of times." The first reveal is not optional; only the
 * repetitions are asked. The effect used to ask before the first one too, and
 * a No got nothing for the spell.
 */
public class AdNauseamTest extends CardTestPlayerBase {

    private static final String nauseam = "Ad Nauseam";

    private void board() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 5);
        addCard(Zone.HAND, playerA, nauseam);
        skipInitShuffling();
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Hill Giant", 1); // mana value 4, drawn second
        addCard(Zone.LIBRARY, playerA, "Grizzly Bears", 1); // mana value 2, on top
    }

    @Test
    public void testFirstRevealIsNotAsked() {
        board();
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, nauseam);
        setChoice(playerA, false); // no repetition

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerA, "Grizzly Bears", 1);
        assertHandCount(playerA, "Hill Giant", 0);
        assertLife(playerA, 20 - 2);
    }

    @Test
    public void testRepeatedOnce() {
        board();
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, nauseam);
        setChoice(playerA, true); // the second card; the library is then empty, nothing more asked

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerA, "Grizzly Bears", 1);
        assertHandCount(playerA, "Hill Giant", 1);
        assertLife(playerA, 20 - 2 - 4);
    }
}

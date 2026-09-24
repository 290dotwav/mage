package org.mage.test.cards.single.hob;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class UncoverTheMoonLettersTest extends CardTestPlayerBase {

    private static final String uncover = "Uncover the Moon-Letters";

    @Test
    public void test_DrawManaSpentThenDiscardTwo() {
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Island", 10);
        addCard(Zone.BATTLEFIELD, playerA, uncover);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 3);
        addCard(Zone.HAND, playerA, "Lava Axe");
        addCard(Zone.HAND, playerA, "Grizzly Bears");

        // Volcanic Hammer {1}{R}: 2 mana spent
        addCard(Zone.HAND, playerA, "Volcanic Hammer");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Volcanic Hammer", playerB);
        setChoice(playerA, true); // draw 2
        setChoice(playerA, "Lava Axe^Grizzly Bears"); // discard two

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 3);
        assertHandCount(playerA, "Island", 2);
        assertGraveyardCount(playerA, "Lava Axe", 1);
        assertGraveyardCount(playerA, "Grizzly Bears", 1);
    }

    @Test
    public void test_Decline() {
        addCard(Zone.BATTLEFIELD, playerA, uncover);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);
        addCard(Zone.HAND, playerA, "Volcanic Hammer");
        addCard(Zone.HAND, playerA, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Volcanic Hammer", playerB);
        setChoice(playerA, false);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerA, 1);
    }
}

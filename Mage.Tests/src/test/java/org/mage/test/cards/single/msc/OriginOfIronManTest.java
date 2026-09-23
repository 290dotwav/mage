package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class OriginOfIronManTest extends CardTestPlayerBase {

    // I - Tap up to one target creature. It doesn't untap during its controller's untap step for as long as this Saga remains on the battlefield.
    // II - Draw two cards.
    // III - You may put an artifact card with mana value 5 or less from your hand onto the battlefield.
    private static final String ORIGIN = "Origin of Iron Man";

    @Test
    public void test_AllChapters() {
        addCard(Zone.HAND, playerA, ORIGIN);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 5);
        addCard(Zone.HAND, playerA, "Darksteel Colossus"); // mana value 11: not allowed
        addCard(Zone.HAND, playerA, "Ornithopter");
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, ORIGIN);
        addTarget(playerA, "Grizzly Bears");

        checkPermanentTapped("stays tapped on its controller's turn", 2, PhaseStep.PRECOMBAT_MAIN, playerB, "Grizzly Bears", true, 1);
        checkHandCardCount("chapter II drew two", 3, PhaseStep.PRECOMBAT_MAIN, playerA, "Island", 0);
        checkPermanentTapped("still tapped", 4, PhaseStep.PRECOMBAT_MAIN, playerB, "Grizzly Bears", true, 1);

        // chapter III on turn 5
        setChoice(playerA, true);
        setChoice(playerA, "Ornithopter");

        setStrictChooseMode(true);
        setStopAt(6, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertPermanentCount(playerA, "Ornithopter", 1);
        assertGraveyardCount(playerA, ORIGIN, 1);
        assertHandCount(playerA, "Darksteel Colossus", 1);
        // the saga is gone: the bears untapped in turn 6
        assertTapped("Grizzly Bears", false);
    }
}

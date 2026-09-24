package org.mage.test.cards.single.me4;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class PrimitiveJusticeTest extends CardTestPlayerBase {

    private static final String justice = "Primitive Justice";

    @Test
    public void test_OneRedOneGreen() {
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 4);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);
        addCard(Zone.HAND, playerA, justice);
        addCard(Zone.BATTLEFIELD, playerB, "Ornithopter");
        addCard(Zone.BATTLEFIELD, playerB, "Millstone");
        addCard(Zone.BATTLEFIELD, playerB, "Sol Ring");
        addCard(Zone.BATTLEFIELD, playerB, "Mox Pearl");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, justice);
        setChoice(playerA, true); // {1}{R} once
        setChoice(playerA, false);
        setChoice(playerA, true); // {1}{G} once
        setChoice(playerA, false);
        addTarget(playerA, "Ornithopter^Millstone^Sol Ring");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerB, 3);
        assertPermanentCount(playerB, "Mox Pearl", 1);
        assertLife(playerA, 21);
        assertTappedCount("Mountain", true, 4);
        assertTappedCount("Forest", true, 2);
    }

    @Test
    public void test_NoAdditionalCost() {
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);
        addCard(Zone.HAND, playerA, justice);
        addCard(Zone.BATTLEFIELD, playerB, "Ornithopter");
        addCard(Zone.BATTLEFIELD, playerB, "Millstone");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, justice);
        setChoice(playerA, false);
        setChoice(playerA, false);
        addTarget(playerA, "Millstone");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerB, "Millstone", 1);
        assertPermanentCount(playerB, "Ornithopter", 1);
        assertLife(playerA, 20);
    }
}

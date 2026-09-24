package org.mage.test.cards.single.all;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class TasteOfParadiseTest extends CardTestPlayerBase {

    private static final String taste = "Taste of Paradise";

    @Test
    public void test_PayTwice() {
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 8);
        addCard(Zone.HAND, playerA, taste);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, taste);
        setChoice(playerA, true);
        setChoice(playerA, true);
        setChoice(playerA, false);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20 + 9);
        assertTappedCount("Forest", true, 8);
    }

    @Test
    public void test_NoAdditionalCost() {
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 4);
        addCard(Zone.HAND, playerA, taste);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, taste);
        setChoice(playerA, false);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20 + 3);
    }
}

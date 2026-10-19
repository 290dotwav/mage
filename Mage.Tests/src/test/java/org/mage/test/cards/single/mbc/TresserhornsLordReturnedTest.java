package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class TresserhornsLordReturnedTest extends CardTestPlayerBase {

    private static final String lord = "Tresserhorn's Lord, Returned";

    // three creatures sacrificed, 3 life paid, the opponent draws three; each sacrifice draws a card
    @Test
    public void test_PayAndStay() {
        addCard(Zone.BATTLEFIELD, playerA, "Underground River", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Sulfurous Springs", 3);
        addCard(Zone.HAND, playerA, lord);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears", 3);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, lord);
        addTarget(playerA, playerB);
        setChoice(playerA, true);
        setChoice(playerA, "Grizzly Bears", 3);
        setChoice(playerA, "Whenever you sacrifice a creature", 2); // order of the three draw triggers

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, lord, 1);
        assertGraveyardCount(playerA, "Grizzly Bears", 3);
        assertHandCount(playerB, 3);
        assertHandCount(playerA, 3);
    }

    // without three creatures, he is sacrificed
    @Test
    public void test_CantPaySacrificed() {
        addCard(Zone.BATTLEFIELD, playerA, "Underground River", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Sulfurous Springs", 3);
        addCard(Zone.HAND, playerA, lord);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears", 1); // with the Lord, two creatures: not enough

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, lord);
        addTarget(playerA, playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, lord, 1);
        assertHandCount(playerB, 0);
        assertHandCount(playerA, 1); // he is a creature you sacrificed: draw
    }
}

package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class MODOKEvilIntellectTest extends CardTestPlayerBase {

    @Test
    public void test_SecondDraw_OpponentSacrificesNontoken() {
        addCard(Zone.BATTLEFIELD, playerA, "M.O.D.O.K., Evil Intellect");
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.HAND, playerA, "Divination"); // draw two cards
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        // turn 1: no draw step for the starting player; Divination draws the first and second cards
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Divination");
        addTarget(playerA, playerB);
        setChoice(playerB, "Grizzly Bears");

        setStopAt(1, PhaseStep.BEGIN_COMBAT);
        setStrictChooseMode(true);
        execute();

        assertGraveyardCount(playerB, "Grizzly Bears", 1);
    }
}

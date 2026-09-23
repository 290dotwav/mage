package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class WillieLumpkinPostmanTest extends CardTestPlayerBase {

    // Willie Lumpkin can't be blocked.
    // Whenever Willie Lumpkin deals combat damage to an opponent, you draw a card and that player may draw a card.
    // If they do, that player can't attack you or permanents you control during their next turn.
    private static final String WILLIE = "Willie Lumpkin, Postman";

    @Test
    public void test_OpponentDrawsCantAttack() {
        addCard(Zone.BATTLEFIELD, playerA, WILLIE);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        attack(1, playerA, WILLIE, playerB);
        setChoice(playerB, true); // draw
        // B can't declare the bears as an attacker on turn 2 (an attack command here fails: no such command available)

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 1);
        assertLife(playerA, 20);
        assertHandCount(playerA, 1);
        assertTapped("Grizzly Bears", false);
    }

    @Test
    public void test_OpponentDeclines() {
        addCard(Zone.BATTLEFIELD, playerA, WILLIE);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        attack(1, playerA, WILLIE, playerB);
        setChoice(playerB, false); // no draw

        attack(2, playerB, "Grizzly Bears", playerA);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20 - 2);
    }

    @Test
    public void test_OnlyTheirNextTurn() {
        addCard(Zone.BATTLEFIELD, playerA, WILLIE);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        attack(1, playerA, WILLIE, playerB);
        setChoice(playerB, true); // draw
        attack(4, playerB, "Grizzly Bears", playerA);

        setStrictChooseMode(true);
        setStopAt(4, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20 - 2);
    }
}

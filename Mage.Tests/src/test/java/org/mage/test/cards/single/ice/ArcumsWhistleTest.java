package org.mage.test.cards.single.ice;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class ArcumsWhistleTest extends CardTestPlayerBase {

    private static final String whistle = "Arcum's Whistle";

    @Test
    public void test_DontPay_MustAttack() {
        addCard(Zone.BATTLEFIELD, playerA, whistle);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        activateAbility(2, PhaseStep.UPKEEP, playerA, "{3}, {T}: Choose", "Grizzly Bears");
        setChoice(playerB, false); // don't pay {2}

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 18);
        assertPermanentCount(playerB, "Grizzly Bears", 1); // it attacked: not destroyed
    }

    @Test
    public void test_Pay() {
        addCard(Zone.BATTLEFIELD, playerA, whistle);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Forest", 2);

        activateAbility(2, PhaseStep.UPKEEP, playerA, "{3}, {T}: Choose", "Grizzly Bears");
        setChoice(playerB, true); // pay {2}

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.UPKEEP);
        execute();

        assertLife(playerA, 20);
        assertPermanentCount(playerB, "Grizzly Bears", 1);
        assertTappedCount("Forest", true, 2);
    }

    @Test
    public void test_TappedCantAttack_Destroyed() {
        addCard(Zone.BATTLEFIELD, playerA, whistle);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 2);
        addCard(Zone.HAND, playerA, "Pressure Point"); // {1}{W}: tap target creature, draw a card at next upkeep
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        activateAbility(2, PhaseStep.UPKEEP, playerA, "{3}, {T}: Choose", "Grizzly Bears");
        setChoice(playerB, false);
        waitStackResolved(2, PhaseStep.UPKEEP);
        castSpell(2, PhaseStep.DRAW, playerA, "Pressure Point", "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.UPKEEP);
        execute();

        assertLife(playerA, 20);
        assertGraveyardCount(playerB, "Grizzly Bears", 1);
    }
}

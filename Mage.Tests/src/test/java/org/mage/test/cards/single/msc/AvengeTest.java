package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class AvengeTest extends CardTestPlayerBase {

    @Test
    public void test_Reduced_AfterBeingAttacked_GainLife() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 4);
        addCard(Zone.HAND, playerA, "Avenge");
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");
        addCard(Zone.BATTLEFIELD, playerB, "Darksteel Myr"); // indestructible: not destroyed

        attack(2, playerB, "Hill Giant");
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Avenge");

        setStopAt(3, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertGraveyardCount(playerA, "Avenge", 1);
        assertGraveyardCount(playerA, "Grizzly Bears", 1);
        assertGraveyardCount(playerB, "Hill Giant", 1);
        assertPermanentCount(playerB, "Darksteel Myr", 1);
        assertLife(playerA, 20 - 3 + 2);
    }

    @Test
    public void test_NotReduced_WithoutAttack() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 6);
        addCard(Zone.HAND, playerA, "Avenge");
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");

        attack(2, playerB, "Hill Giant", playerA);
        checkPlayableAbility("reduced after the attack", 3, PhaseStep.UPKEEP, playerA, "Cast Avenge", false); // sorcery
        // on turn 5, B's last turn (turn 4) had no attack: full cost
        castSpell(5, PhaseStep.PRECOMBAT_MAIN, playerA, "Avenge");

        setStopAt(5, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertGraveyardCount(playerB, "Hill Giant", 1);
        assertTappedCount("Plains", true, 6);
    }

    @Test
    public void test_NotPlayableWithoutReduction() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 4);
        addCard(Zone.HAND, playerA, "Avenge");

        checkPlayableAbility("can't afford", 3, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Avenge", false);

        setStopAt(3, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();
    }
}

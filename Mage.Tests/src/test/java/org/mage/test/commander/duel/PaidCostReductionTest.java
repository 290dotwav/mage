package org.mage.test.commander.duel;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.stack.PaidCost;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.game.PaidCostTest;
import org.mage.test.serverside.base.CardTestCommanderDuelBase;

/**
 * A Dragon cast with The Ur-Dragon in the command zone costs {1} less (its eminence), and that
 * reduced cost is the one {@link PaidCost} tells: « Pourquoi vigilantpanda paie 7 pour son dragon
 * alors que son commandant fait que ça coûte 1 de moins » (the owner, 23 November). Once paid,
 * {@link PaidCost#toPay} says nothing: {@link PaidCost#of} answers.
 *
 * @author ClaudeMTG
 */
public class PaidCostReductionTest extends CardTestCommanderDuelBase {

    @Test
    public void test_EminenceReductionIsTheCostTold() {
        addCard(Zone.COMMAND, playerA, "The Ur-Dragon", 1);
        // Shivan Dragon {4}{R}{R}
        addCard(Zone.HAND, playerA, "Shivan Dragon", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 5);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shivan Dragon");
        runCode("on the stack", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            PaidCostTest.assertPaid(PaidCostTest.spell(game, "Shivan Dragon"), "{3}{R}{R}", null, "{R}{R}{R}{R}{R}");
            Assert.assertNull("paid already", PaidCost.toPay(PaidCostTest.spell(game, "Shivan Dragon")));
        });

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
        assertPermanentCount(playerA, "Shivan Dragon", 1);
        assertTappedCount("Mountain", true, 5);
    }
}

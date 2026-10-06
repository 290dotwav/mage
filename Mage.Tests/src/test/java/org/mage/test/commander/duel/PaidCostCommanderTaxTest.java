package org.mage.test.commander.duel;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.game.PaidCostTest;
import org.mage.test.serverside.base.CardTestCommanderDuelBase;

/**
 * The cost a commander was paid for carries its tax (903.8): the second cast of a {R}
 * commander was paid {2}{R}, and that is what {@link mage.game.stack.PaidCost} reads.
 *
 * @author ClaudeMTG
 */
public class PaidCostCommanderTaxTest extends CardTestCommanderDuelBase {

    @Test
    public void test_SecondCastCarriesTheTax() {
        addCard(Zone.COMMAND, playerA, "Lightning Bolt", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 4);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);
        runCode("first cast", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            PaidCostTest.assertPaid(PaidCostTest.spell(game, "Lightning Bolt"), "{R}", null, "{R}");
        });
        setChoice(playerA, true); // back to the command zone
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);
        runCode("second cast", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            PaidCostTest.assertPaid(PaidCostTest.spell(game, "Lightning Bolt"), "{2}{R}", null, "{R}{R}{R}");
        });
        setChoice(playerA, true);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
        assertLife(playerB, 40 - 3 - 3);
    }
}

package org.mage.test.cards.single.frc;

import mage.abilities.keyword.HasteAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class VenserFerventForgerTest extends CardTestPlayerBase {

    private static final String venser = "Venser, Fervent Forger";

    // two hasty token copies of an opponent's permanent, sacrificed at the next end step
    @Test
    public void test_TokenCopies() {
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 6);
        addCard(Zone.HAND, playerA, venser);
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, venser);
        setModeChoice(playerA, "2");
        addTarget(playerA, "Hill Giant");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPermanentCount("two copies", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Hill Giant", 2);
        checkAbility("haste", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Hill Giant", HasteAbility.class, true);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.UPKEEP);
        execute();

        assertPermanentCount(playerA, "Hill Giant", 0);
        assertPermanentCount(playerB, "Hill Giant", 1);
    }

    // flash in response: an opponent's Lightning Bolt copied twice
    @Test
    public void test_CopySpellTwice() {
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 6);
        addCard(Zone.HAND, playerA, venser);
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 1);
        addCard(Zone.HAND, playerB, "Lightning Bolt");

        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Lightning Bolt", playerA);
        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerA, venser, null, "Lightning Bolt", StackClause.WHILE_ON_STACK);
        setModeChoice(playerA, "1");
        addTarget(playerA, "Lightning Bolt");
        setChoice(playerA, true); // new target for copy 1
        addTarget(playerA, playerB);
        setChoice(playerA, true); // new target for copy 2
        addTarget(playerA, playerB);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 14);
        assertLife(playerA, 17);
    }
}

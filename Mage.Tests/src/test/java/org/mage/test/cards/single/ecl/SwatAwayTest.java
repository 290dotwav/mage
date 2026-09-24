package org.mage.test.cards.single.ecl;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * Swat Away {2}{U}{U} Instant
 * This spell costs {2} less to cast if a creature is attacking you.
 * The owner of target spell or creature puts it on their choice of the top or bottom of their library.
 */
public class SwatAwayTest extends CardTestPlayerBase {

    private static final String swat = "Swat Away";

    @Test
    public void test_CostReducedWhileAttacked() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2); // only enough with the reduction
        addCard(Zone.HAND, playerA, swat);
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");

        checkPlayableAbility("full cost before the attack", 2, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast " + swat, false);
        attack(2, playerB, "Hill Giant", playerA);
        checkPlayableAbility("reduced while attacked", 2, PhaseStep.DECLARE_ATTACKERS, playerA, "Cast " + swat, true);
        castSpell(2, PhaseStep.DECLARE_ATTACKERS, playerA, swat, "Hill Giant");
        setChoice(playerB, true); // owner puts it on top of the library

        setStopAt(2, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertGraveyardCount(playerA, swat, 1);
        assertPermanentCount(playerB, "Hill Giant", 0);
        assertLife(playerA, 20);
    }

    @Test
    public void test_FullCostWhenNotAttacked() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 4);
        addCard(Zone.HAND, playerA, swat);
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");

        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerA, swat, "Hill Giant");
        setChoice(playerB, true);

        setStopAt(2, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertPermanentCount(playerB, "Hill Giant", 0);
        assertTappedCount("Island", true, 4);
    }
}

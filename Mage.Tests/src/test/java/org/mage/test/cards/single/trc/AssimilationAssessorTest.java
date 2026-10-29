package org.mage.test.cards.single.trc;

import mage.constants.PhaseStep;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class AssimilationAssessorTest extends CardTestPlayerBase {

    private static final String assessor = "Assimilation Assessor";

    // enters: the opponent's graveyard exiled with it; attacks: a creature card from there comes over
    @Test
    public void test_ExileThenAssimilateOnAttack() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 5);
        addCard(Zone.HAND, playerA, assessor);
        addCard(Zone.GRAVEYARD, playerB, "Hill Giant");
        addCard(Zone.GRAVEYARD, playerB, "Lightning Bolt");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, assessor);
        addTarget(playerA, playerB);

        attack(3, playerA, assessor);
        setChoice(playerA, "Hill Giant");

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerB, 0);
        assertExileCount(playerB, "Lightning Bolt", 1);
        assertPermanentCount(playerA, "Hill Giant", 1);
        assertCounterCount(playerA, "Hill Giant", CounterType.P1P1, 1);
        BorgAssert.isBorg(getPermanent("Hill Giant", playerA), currentGame, SubType.GIANT);
        assertLife(playerB, 20 - 5);
    }
}

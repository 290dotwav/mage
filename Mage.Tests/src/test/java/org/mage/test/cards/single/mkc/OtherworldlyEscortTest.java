package org.mage.test.cards.single.mkc;

import mage.constants.PhaseStep;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.game.permanent.Permanent;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class OtherworldlyEscortTest extends CardTestPlayerBase {

    private static final String escort = "Otherworldly Escort"; // 4/3

    @Test
    public void test_ReturnsAsSpiritOnce_ThenDestroys() {
        addCard(Zone.BATTLEFIELD, playerA, escort);
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 2);
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 4);
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");
        addCard(Zone.HAND, playerB, "Lightning Bolt", 2);

        // dies once: comes back as a Spirit Detective with four charge counters
        castSpell(2, PhaseStep.UPKEEP, playerB, "Lightning Bolt", escort);
        waitStackResolved(2, PhaseStep.UPKEEP);
        checkPermanentCounters("charge", 2, PhaseStep.UPKEEP, playerA, escort, CounterType.CHARGE, 4);
        checkSubType("spirit", 2, PhaseStep.UPKEEP, playerA, escort, SubType.SPIRIT, true);
        checkSubType("no human", 2, PhaseStep.UPKEEP, playerA, escort, SubType.HUMAN, false);

        // Hill Giant hits A, then A destroys it (on turn 4: the Escort is no longer summoning sick)
        attack(4, playerB, "Hill Giant", playerA);
        activateAbility(4, PhaseStep.END_COMBAT, playerA, "{1}{W}, {T}, Remove", "Hill Giant");
        waitStackResolved(4, PhaseStep.END_COMBAT);

        // dies again as a Spirit: stays dead
        castSpell(4, PhaseStep.POSTCOMBAT_MAIN, playerB, "Lightning Bolt", escort);

        setStrictChooseMode(true);
        setStopAt(4, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20 - 3);
        assertGraveyardCount(playerB, "Hill Giant", 1);
        assertGraveyardCount(playerA, escort, 1);
    }
}

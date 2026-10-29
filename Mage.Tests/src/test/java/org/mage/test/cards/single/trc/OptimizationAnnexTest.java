package org.mage.test.cards.single.trc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class OptimizationAnnexTest extends CardTestPlayerBase {

    private static final String annex = "Optimization Annex";
    private static final String array = "Gemstone Array"; // {2}: Put a charge counter on Gemstone Array.

    @Test
    public void test_OneMoreChargeCounter() {
        addCard(Zone.BATTLEFIELD, playerA, annex);
        addCard(Zone.BATTLEFIELD, playerA, array);
        addCard(Zone.BATTLEFIELD, playerA, "Wastes", 2);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}: Put a charge counter");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertCounterCount(playerA, array, CounterType.CHARGE, 2);
    }
}

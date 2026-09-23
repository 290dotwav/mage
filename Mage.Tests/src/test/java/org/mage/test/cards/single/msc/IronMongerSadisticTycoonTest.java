package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class IronMongerSadisticTycoonTest extends CardTestPlayerBase {

    // Flying
    // Whenever a creature you control connives, put a +1/+1 counter on each Villain you control.
    private static final String MONGER = "Iron Monger, Sadistic Tycoon";
    // When this creature enters, it connives.
    private static final String INFORMANT = "Raffine's Informant";

    @Test
    public void test_ConniveCountersOnVillains() {
        addCard(Zone.BATTLEFIELD, playerA, MONGER);
        addCard(Zone.BATTLEFIELD, playerA, "The Frightful Four"); // Villain
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears"); // not a Villain
        addCard(Zone.HAND, playerA, INFORMANT);
        addCard(Zone.HAND, playerA, "Island");
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 2);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, INFORMANT);
        setChoice(playerA, "Island"); // discard a land: no counter on the informant

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertCounterCount(playerA, MONGER, CounterType.P1P1, 1);
        assertCounterCount(playerA, "The Frightful Four", CounterType.P1P1, 1);
        assertCounterCount(playerA, "Grizzly Bears", CounterType.P1P1, 0);
        assertCounterCount(playerA, INFORMANT, CounterType.P1P1, 0);
    }
}

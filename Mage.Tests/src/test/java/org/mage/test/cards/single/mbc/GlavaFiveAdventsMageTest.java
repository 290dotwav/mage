package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class GlavaFiveAdventsMageTest extends CardTestPlayerBase {

    // X becomes 5 for the first X permanent spell of the turn, not for the second
    @Test
    public void test_XBecomesFiveOncePerTurn() {
        addCard(Zone.BATTLEFIELD, playerA, "Glava, Five-Advents Mage");
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);
        addCard(Zone.HAND, playerA, "Endless One", 2); // {X}, enters with X +1/+1 counters

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Endless One");
        setChoice(playerA, "X=1");
        setChoice(playerA, true); // X becomes 5
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPermanentCounters("five", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Endless One", CounterType.P1P1, 5);
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Endless One");
        setChoice(playerA, "X=1");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Endless One", 2);
        java.util.List<Integer> sizes = currentGame.getBattlefield().getAllActivePermanents(playerA.getId()).stream()
                .filter(p -> p.getName().equals("Endless One"))
                .map(p -> p.getPower().getValue())
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        org.junit.Assert.assertEquals(java.util.Arrays.asList(1, 5), sizes);
    }
}

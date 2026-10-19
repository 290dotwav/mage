package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class SahirVisitorInDarknessTest extends CardTestPlayerBase {

    private static final String sahir = "Sahir, Visitor in Darkness";

    // eminence pings for 1 (one counter, not two: the life lost to that damage is the same event), Bitterblossom's 1 life is the second: a card
    @Test
    public void test_DamageAndLifeLoss() {
        addCard(Zone.BATTLEFIELD, playerA, sahir);
        addCard(Zone.BATTLEFIELD, playerA, "Bitterblossom");

        setStrictChooseMode(false); // the order of the two upkeep triggers doesn't matter
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertLife(playerA, 18);
        assertCounterCount(playerA, sahir, CounterType.P1P1, 2);
        assertHandCount(playerA, 1);
    }

    // 2 damage from a source you control does nothing
    @Test
    public void test_TwoDamageNothing() {
        addCard(Zone.BATTLEFIELD, playerA, sahir);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");
        addCard(Zone.HAND, playerA, "Shock");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerA);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20 - 1 - 2);
        assertCounterCount(playerA, sahir, CounterType.P1P1, 1);
    }
}

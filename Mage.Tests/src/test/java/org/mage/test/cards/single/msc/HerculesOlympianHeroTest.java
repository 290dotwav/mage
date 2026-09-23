package org.mage.test.cards.single.msc;

import mage.abilities.keyword.IndestructibleAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class HerculesOlympianHeroTest extends CardTestPlayerBase {

    // Whenever Hercules attacks, put a +1/+1 counter on him. He gains indestructible until end of turn.
    // Whenever Hercules is dealt damage for the first time each turn, put that many +1/+1 counters on him.
    private static final String HERCULES = "Hercules, Olympian Hero";

    @Test
    public void test_FirstDamageOnly() {
        addCard(Zone.BATTLEFIELD, playerA, HERCULES);
        addCard(Zone.HAND, playerB, "Shock", 2);
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 2);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerB, "Shock", HERCULES);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerB, "Shock", HERCULES);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        // first Shock: 2 counters (3/3 -> 5/5 with 2 damage); second Shock: no counters, 4 damage on a 5/5
        assertCounterCount(playerA, HERCULES, CounterType.P1P1, 2);
        assertPowerToughness(playerA, HERCULES, 5, 5);
    }

    @Test
    public void test_Attack() {
        addCard(Zone.BATTLEFIELD, playerA, HERCULES);

        attack(1, playerA, HERCULES, playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertCounterCount(playerA, HERCULES, CounterType.P1P1, 1);
        assertAbility(playerA, HERCULES, IndestructibleAbility.getInstance(), true);
        assertLife(playerB, 20 - 4);
    }
}

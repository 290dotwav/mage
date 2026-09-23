package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class HeroicReturnTest extends CardTestPlayerBase {

    private static final String heroicReturn = "Heroic Return";

    @Test
    public void test_Hero_TwoCounters() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 6);
        addCard(Zone.HAND, playerA, heroicReturn);
        addCard(Zone.GRAVEYARD, playerA, "Wolfsbane, Highland Hero"); // a Hero

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, heroicReturn, "Wolfsbane, Highland Hero");

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertCounterCount(playerA, "Wolfsbane, Highland Hero", CounterType.P1P1, 2);
    }

    @Test
    public void test_NonHero_NoCounters_CostReducedWhenAttacked() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 4); // not enough without the reduction
        addCard(Zone.HAND, playerA, heroicReturn);
        addCard(Zone.GRAVEYARD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");

        attack(2, playerB, "Hill Giant");
        checkPlayableAbility("not before combat", 2, PhaseStep.UPKEEP, playerA, "Cast Heroic Return", false);
        checkPlayableAbility("reduced while attacked", 2, PhaseStep.DECLARE_BLOCKERS, playerA, "Cast Heroic Return", true);
        castSpell(2, PhaseStep.DECLARE_BLOCKERS, playerA, heroicReturn, "Grizzly Bears");

        setStopAt(2, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertCounterCount(playerA, "Grizzly Bears", CounterType.P1P1, 0);
    }
}

package org.mage.test.cards.single.msc;

import mage.abilities.keyword.FirstStrikeAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class LoveOnTheBattlefieldTest extends CardTestPlayerBase {

    private static final String love = "Love on the Battlefield";

    @Test
    public void test_ExactlyTwo() {
        addCard(Zone.BATTLEFIELD, playerA, love);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant");
        addCard(Zone.BATTLEFIELD, playerB, "Wall of Stone"); // 0/8

        attack(1, playerA, "Grizzly Bears");
        attack(1, playerA, "Hill Giant");
        block(1, playerB, "Wall of Stone", "Hill Giant");

        checkAbility("first strike", 1, PhaseStep.DECLARE_BLOCKERS, playerA, "Grizzly Bears", FirstStrikeAbility.class, true);

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertHandCount(playerA, 1);
        assertLife(playerB, 20 - 2);
        assertCounterCount(playerA, "Grizzly Bears", CounterType.P1P1, 1);
        assertCounterCount(playerA, "Hill Giant", CounterType.P1P1, 0);
    }

    @Test
    public void test_ThreeAttackers_NoTrigger() {
        addCard(Zone.BATTLEFIELD, playerA, love);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant");
        addCard(Zone.BATTLEFIELD, playerA, "Memnite");

        attack(1, playerA, "Grizzly Bears");
        attack(1, playerA, "Hill Giant");
        attack(1, playerA, "Memnite");

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertHandCount(playerA, 0);
        assertCounterCount(playerA, "Grizzly Bears", CounterType.P1P1, 0);
    }
}

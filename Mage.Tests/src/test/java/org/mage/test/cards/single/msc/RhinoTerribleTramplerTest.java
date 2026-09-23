package org.mage.test.cards.single.msc;

import mage.abilities.keyword.TrampleAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class RhinoTerribleTramplerTest extends CardTestPlayerBase {

    private static final String rhino = "Rhino, Terrible Trampler";

    @Test
    public void test_DestroyAndDistribute() {
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 6);
        addCard(Zone.HAND, playerA, rhino);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant");
        addCard(Zone.BATTLEFIELD, playerB, "Sol Ring");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, rhino);
        addTargetAmount(playerA, "Grizzly Bears", 2);
        addTargetAmount(playerA, "Hill Giant", 1);
        addTarget(playerA, "Sol Ring");

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertGraveyardCount(playerB, "Sol Ring", 1);
        assertCounterCount(playerA, "Grizzly Bears", CounterType.P1P1, 2);
        assertCounterCount(playerA, "Hill Giant", CounterType.P1P1, 1);
        assertAbility(playerA, "Grizzly Bears", TrampleAbility.getInstance(), true);
        assertAbility(playerA, "Hill Giant", TrampleAbility.getInstance(), true);
    }
}

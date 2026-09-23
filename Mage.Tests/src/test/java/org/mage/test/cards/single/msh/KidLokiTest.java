package org.mage.test.cards.single.msh;

import mage.abilities.keyword.HexproofAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class KidLokiTest extends CardTestPlayerBase {

    private static final String loki = "Kid Loki";

    @Test
    public void test_HexproofThisTurnOnly() {
        addCard(Zone.BATTLEFIELD, playerA, loki);
        addCard(Zone.BATTLEFIELD, playerA, "Forest");
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant");
        addCard(Zone.HAND, playerA, "Battlegrowth");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Battlegrowth", "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkAbility("bears hexproof", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears", HexproofAbility.class, true);
        checkAbility("giant not hexproof", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Hill Giant", HexproofAbility.class, false);

        setStopAt(2, PhaseStep.UPKEEP);
        setStrictChooseMode(true);
        execute();

        assertAbility(playerA, "Grizzly Bears", HexproofAbility.getInstance(), false);
    }

    @Test
    public void test_SecondDraw() {
        addCard(Zone.BATTLEFIELD, playerA, loki);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.HAND, playerA, "Divination");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Divination");

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertCounterCount(playerA, loki, CounterType.P1P1, 1);
        assertAbility(playerA, loki, HexproofAbility.getInstance(), true);
    }
}

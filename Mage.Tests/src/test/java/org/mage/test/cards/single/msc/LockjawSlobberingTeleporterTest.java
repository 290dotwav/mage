package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class LockjawSlobberingTeleporterTest extends CardTestPlayerBase {

    private static final String lockjaw = "Lockjaw, Slobbering Teleporter";

    @Test
    public void test_CounterAndUnblockable() {
        addCard(Zone.BATTLEFIELD, playerA, lockjaw);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");
        addCard(Zone.HAND, playerA, "Shock");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerB);
        addTarget(playerA, "Grizzly Bears"); // reflexive: up to one other target creature you control

        setStopAt(1, PhaseStep.DECLARE_ATTACKERS);
        setStrictChooseMode(true);
        execute();

        assertCounterCount(playerA, lockjaw, CounterType.P1P1, 1);
        assertLife(playerB, 18);
    }

    @Test
    public void test_BlockersCantBlock() {
        addCard(Zone.BATTLEFIELD, playerA, lockjaw);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");
        addCard(Zone.HAND, playerA, "Shock");
        addCard(Zone.BATTLEFIELD, playerB, "Wall of Stone", 2); // 0/8 defender

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerB);
        addTarget(playerA, "Grizzly Bears");
        attack(1, playerA, lockjaw);
        attack(1, playerA, "Grizzly Bears");

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        // nothing could be blocked: 2 (shock) + 2 (lockjaw with counter) + 2 (bears)
        assertLife(playerB, 20 - 2 - 2 - 2);
    }

    @Test
    public void test_NoNoncreatureSpell_NoTrigger() {
        addCard(Zone.BATTLEFIELD, playerA, lockjaw);

        setStopAt(1, PhaseStep.DECLARE_ATTACKERS);
        setStrictChooseMode(true);
        execute();

        assertCounterCount(playerA, lockjaw, CounterType.P1P1, 0);
    }
}

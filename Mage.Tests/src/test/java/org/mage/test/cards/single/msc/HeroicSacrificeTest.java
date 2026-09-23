package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class HeroicSacrificeTest extends CardTestPlayerBase {

    @Test
    public void test_RedirectThenMoveCountersAndDraw() {
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant");
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 3);
        addCard(Zone.HAND, playerA, "Battlegrowth");
        addCard(Zone.HAND, playerA, "Heroic Sacrifice");
        addCard(Zone.HAND, playerA, "Shock", 2);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Battlegrowth", "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Heroic Sacrifice", "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerA);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkDamage("bears took redirected damage", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears", 2);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", "Hill Giant");
        addTarget(playerA, "Hill Giant"); // bears died: put its counters on Hill Giant

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertLife(playerA, 20);
        assertGraveyardCount(playerA, "Grizzly Bears", 1);
        assertDamageReceived(playerA, "Hill Giant", 0);
        assertCounterCount(playerA, "Hill Giant", CounterType.P1P1, 1);
        assertHandCount(playerA, 1);
    }

    @Test
    public void test_NotAfterEndOfTurn() {
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);
        addCard(Zone.HAND, playerA, "Heroic Sacrifice");
        addCard(Zone.HAND, playerA, "Shock");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Heroic Sacrifice", "Grizzly Bears");
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerA);

        setStopAt(3, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertLife(playerA, 18);
        assertPermanentCount(playerA, "Grizzly Bears", 1);
    }
}

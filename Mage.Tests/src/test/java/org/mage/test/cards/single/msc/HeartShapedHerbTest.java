package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class HeartShapedHerbTest extends CardTestPlayerBase {

    private static final String herb = "Heart-Shaped Herb";

    @Test
    public void test_PreventOneFromOpponentSources() {
        addCard(Zone.BATTLEFIELD, playerA, herb);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");
        addCard(Zone.HAND, playerA, "Shock");
        addCard(Zone.BATTLEFIELD, playerB, "Mountain");
        addCard(Zone.HAND, playerB, "Shock");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerA); // own source: not prevented
        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Shock", playerA); // opponent's source: 1 prevented

        setStopAt(2, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertLife(playerA, 20 - 2 - 1);
    }

    @Test
    public void test_SacrificeReturnWithCountersAndMonarch() {
        addCard(Zone.BATTLEFIELD, playerA, herb);
        addCard(Zone.BATTLEFIELD, playerA, "Wastes", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}, {T}, Sacrifice");
        setChoice(playerA, true); // sacrifice a creature
        setChoice(playerA, "Grizzly Bears");

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertGraveyardCount(playerA, herb, 1);
        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertCounterCount(playerA, "Grizzly Bears", CounterType.P1P1, 3);
        Assert.assertEquals(playerA.getId(), currentGame.getMonarchId());
    }

    @Test
    public void test_DeclineSacrifice_NoMonarch() {
        addCard(Zone.BATTLEFIELD, playerA, herb);
        addCard(Zone.BATTLEFIELD, playerA, "Wastes", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}, {T}, Sacrifice");
        setChoice(playerA, false);

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertPermanentCount(playerA, "Grizzly Bears", 1);
        Assert.assertNull(currentGame.getMonarchId());
    }
}

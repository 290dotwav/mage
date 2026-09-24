package org.mage.test.cards.single.mkc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.game.permanent.Permanent;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestMultiPlayerBaseWithRangeAll;

/**
 * Player order: A -> D -> C -> B
 *
 * @author Claude
 */
public class ImmortalObligationTest extends CardTestMultiPlayerBaseWithRangeAll {

    private static final String obligation = "Immortal Obligation";

    @Test
    public void test_ReturnedGoadedCantAttackOrBlockYou() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant");
        addCard(Zone.HAND, playerA, obligation);
        addCard(Zone.GRAVEYARD, playerD, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, obligation, "Grizzly Bears");


        // A's next turn: the Bears can't block A's creature
        attack(5, playerA, "Hill Giant", playerD);
        block(5, playerD, "Grizzly Bears", "Hill Giant");

        setStopAt(5, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerD, "Grizzly Bears", 1);
        assertCounterCount(playerD, "Grizzly Bears", CounterType.DUTY, 1);
        Permanent bears = getPermanent("Grizzly Bears", playerD);
        Assert.assertTrue("goaded by A", bears.getGoadingPlayers().contains(playerA.getId()));
        assertLife(playerA, 20);
        assertLife(playerC, 20 - 2); // goaded: it had to attack, and not A
        assertLife(playerD, 20 - 3);
    }
}

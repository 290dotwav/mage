package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class OkoyeMightyAndAdoredTest extends CardTestPlayerBase {

    // When Okoye enters, you become the monarch.
    // At the beginning of combat on your turn, put a +1/+1 counter on target creature. Whenever that creature
    // attacks the monarch this turn, it gains double strike and trample until end of turn.
    private static final String OKOYE = "Okoye, Mighty and Adored";

    @Test
    public void test_EntersMonarch() {
        addCard(Zone.HAND, playerA, OKOYE);
        addCard(Zone.BATTLEFIELD, playerA, "Savannah", 4);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, OKOYE);
        addTarget(playerA, OKOYE); // its beginning of combat trigger

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        Assert.assertEquals("monarch", playerA.getId(), currentGame.getMonarchId());
    }

    @Test
    public void test_AttacksMonarchDoubleStrike() {
        addCard(Zone.BATTLEFIELD, playerA, OKOYE);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        // B's own Okoye makes B the monarch
        addCard(Zone.HAND, playerB, OKOYE);
        addCard(Zone.BATTLEFIELD, playerB, "Savannah", 4);

        addTarget(playerA, "Grizzly Bears"); // turn 1 beginning of combat
        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, OKOYE);
        addTarget(playerB, OKOYE); // B's turn 2 beginning of combat
        addTarget(playerA, "Grizzly Bears"); // turn 3 beginning of combat

        attack(3, playerA, "Grizzly Bears", playerB);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_COMBAT);
        execute();

        assertCounterCount(playerA, "Grizzly Bears", CounterType.P1P1, 2);
        // 4/4 double strike
        assertLife(playerB, 20 - 8);
    }

    @Test
    public void test_AttacksNonMonarchNoDoubleStrike() {
        addCard(Zone.BATTLEFIELD, playerA, OKOYE);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        addTarget(playerA, "Grizzly Bears");
        attack(1, playerA, "Grizzly Bears", playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertLife(playerB, 20 - 3);
    }
}

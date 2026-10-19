package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class RikalaHomaridKingTest extends CardTestPlayerBase {

    private static final String rikala = "Rikala, Homarid King";

    // a tide counter each upkeep, +1/+0 per counter; at three the Islands make an extra {U}
    @Test
    public void test_TideRisesAndIslandsGiveMore() {
        addCard(Zone.BATTLEFIELD, playerA, rikala);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Island", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 1);
        addCard(Zone.HAND, playerA, "Divination"); // {2}{U}

        checkPermanentCounters("one tide", 1, PhaseStep.PRECOMBAT_MAIN, playerA, rikala, CounterType.TIDE, 1);
        checkPT("+1/+0", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears", 3, 2);
        checkPlayableAbility("two lands are not three mana yet", 3, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Divination", false);
        checkPT("+3/+0", 5, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears", 5, 2);
        castSpell(5, PhaseStep.PRECOMBAT_MAIN, playerA, "Divination");

        setStrictChooseMode(true);
        setStopAt(5, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, "Divination", 1);
        assertCounterCount(playerA, rikala, CounterType.TIDE, 3);
    }

    // the fourth counter takes them all away
    @Test
    public void test_FourthCounterRemovesThem() {
        addCard(Zone.BATTLEFIELD, playerA, rikala);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(7, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertCounterCount(playerA, rikala, CounterType.TIDE, 0);
        assertPowerToughness(playerA, "Grizzly Bears", 2, 2);
    }
}

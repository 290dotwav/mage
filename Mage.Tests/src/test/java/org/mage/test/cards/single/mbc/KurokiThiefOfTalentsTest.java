package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class KurokiThiefOfTalentsTest extends CardTestPlayerBase {

    private static final String kuroki = "Kuroki, Thief of Talents";

    // the opponent draws four: a spell from their hand is cast for free
    @Test
    public void test_OpponentDraws() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, kuroki);
        addCard(Zone.HAND, playerB, "Lightning Bolt");
        removeAllCardsFromLibrary(playerB);
        addCard(Zone.LIBRARY, playerB, "Island", 4);

        addTarget(playerA, playerB); // end step trigger
        setChoice(playerB, true); // draw four
        setChoice(playerA, true); // cast Lightning Bolt for free
        addTarget(playerA, playerB);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.UPKEEP);
        execute();

        assertLife(playerB, 17);
        assertHandCount(playerB, "Island", 4);
        assertGraveyardCount(playerB, "Lightning Bolt", 1);
        assertCounterCount(playerA, kuroki, CounterType.P1P1, 0);
    }

    // the opponent refuses: two +1/+1 counters
    @Test
    public void test_OpponentRefuses() {
        addCard(Zone.BATTLEFIELD, playerA, kuroki);

        addTarget(playerA, playerB);
        setChoice(playerB, false);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.UPKEEP);
        execute();

        assertCounterCount(playerA, kuroki, CounterType.P1P1, 2);
        assertPowerToughness(playerA, kuroki, 6, 6);
    }
}

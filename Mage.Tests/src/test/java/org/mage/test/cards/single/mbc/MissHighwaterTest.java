package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class MissHighwaterTest extends CardTestPlayerBase {

    // the contract: a new hand of seven and a contract counter; a player who has one is not offered again
    @Test
    public void test_Contract() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, "Miss Highwater");
        addCard(Zone.HAND, playerB, "Swamp", 2);
        removeAllCardsFromLibrary(playerB);
        addCard(Zone.LIBRARY, playerB, "Island", 10);

        attack(1, playerA, "Miss Highwater");
        setChoice(playerB, true); // sign
        setChoice(playerB, "Island"); // turn 2 cleanup: eight cards, one discarded
        attack(3, playerA, "Miss Highwater");

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 10);
        assertGraveyardCount(playerB, "Swamp", 2);
        Assert.assertEquals(1, playerB.getCountersCount(CounterType.CONTRACT));
        assertHandCount(playerB, 7); // seven, the turn 2 draw, one discarded at cleanup; nothing more on turn 3
    }

    // refusing keeps the hand and no counter
    @Test
    public void test_Refuse() {
        addCard(Zone.BATTLEFIELD, playerA, "Miss Highwater");
        addCard(Zone.HAND, playerB, "Island", 2);

        attack(1, playerA, "Miss Highwater");
        setChoice(playerB, false);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerB, 2);
        Assert.assertEquals(0, playerB.getCountersCount(CounterType.CONTRACT));
    }
}

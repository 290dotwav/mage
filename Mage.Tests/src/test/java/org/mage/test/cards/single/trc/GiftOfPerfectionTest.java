package org.mage.test.cards.single.trc;

import mage.constants.PhaseStep;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class GiftOfPerfectionTest extends CardTestPlayerBase {

    private static final String gift = "Gift of Perfection";

    @Test
    public void test_RevealUntilCreatureAssimilate() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 5);
        addCard(Zone.HAND, playerA, gift);
        addCard(Zone.LIBRARY, playerB, "Hill Giant");
        addCard(Zone.LIBRARY, playerB, "Lightning Bolt");
        skipInitShuffling();

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, gift);
        setChoice(playerA, "Hill Giant");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Hill Giant", 1);
        assertCounterCount(playerA, "Hill Giant", CounterType.P1P1, 1);
        assertPowerToughness(playerA, "Hill Giant", 4, 4);
        BorgAssert.isBorg(getPermanent("Hill Giant", playerA), currentGame, SubType.GIANT);
        assertLibraryCount(playerB, "Lightning Bolt", 1);
    }
}

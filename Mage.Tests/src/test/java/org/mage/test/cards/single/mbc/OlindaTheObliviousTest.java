package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class OlindaTheObliviousTest extends CardTestPlayerBase {

    // the odor counter makes its controller lose 2 at each upkeep, even after Olinda is gone
    @Test
    public void test_OdorLasts() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 4);
        addCard(Zone.HAND, playerA, "Olinda the Oblivious");
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Mountain");
        addCard(Zone.HAND, playerB, "Lightning Bolt");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Olinda the Oblivious");
        addTarget(playerA, "Grizzly Bears");
        castSpell(1, PhaseStep.END_TURN, playerB, "Lightning Bolt", "Olinda the Oblivious");

        setStrictChooseMode(true);
        setStopAt(4, PhaseStep.DRAW);
        execute();

        assertGraveyardCount(playerA, "Olinda the Oblivious", 1);
        assertCounterCount(playerB, "Grizzly Bears", CounterType.ODOR, 1);
        assertLife(playerB, 20 - 2 - 2);
        assertLife(playerA, 20);
    }
}

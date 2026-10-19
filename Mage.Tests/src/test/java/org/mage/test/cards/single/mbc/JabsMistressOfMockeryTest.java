package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class JabsMistressOfMockeryTest extends CardTestPlayerBase {

    // a stolen permanent's trigger happens twice; one's own does not
    @Test
    public void test_StolenTriggersTwice() {
        addCard(Zone.BATTLEFIELD, playerA, "Jabs, Mistress of Mockery");
        addCard(Zone.BATTLEFIELD, playerA, "Island", 6);
        addCard(Zone.HAND, playerA, "Confiscate");
        addCard(Zone.BATTLEFIELD, playerB, "Phyrexian Arena"); // upkeep: draw a card and lose 1 life
        addCard(Zone.BATTLEFIELD, playerA, "Bitterblossom"); // upkeep: lose 1 life, a Faerie token

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Confiscate", "Phyrexian Arena");
        setChoice(playerA, "At the beginning of your upkeep", 2); // turn 3: order of the three triggers

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.DRAW);
        execute();

        // turn 3 upkeep: the own Bitterblossom once, the stolen Arena twice (turn 1 upkeep: Bitterblossom once)
        assertLife(playerA, 20 - 1 - 1 - 2);
        assertPermanentCount(playerA, "Faerie Rogue Token", 2);
        assertHandCount(playerA, 2 + 1); // two from the Arena, one draw step
    }
}

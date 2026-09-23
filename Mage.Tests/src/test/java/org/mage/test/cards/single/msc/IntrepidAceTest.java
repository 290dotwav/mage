package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class IntrepidAceTest extends CardTestPlayerBase {

    // This creature gets +2/+0 as long as it isn't attacking or blocking.
    private static final String ACE = "Intrepid Ace";

    @Test
    public void test_BoostOnlyWhenNotInCombat() {
        addCard(Zone.BATTLEFIELD, playerA, ACE);
        addCard(Zone.BATTLEFIELD, playerB, ACE);

        checkPT("idle", 1, PhaseStep.PRECOMBAT_MAIN, playerA, ACE, 4, 1);
        attack(1, playerA, ACE);
        checkPT("attacking", 1, PhaseStep.DECLARE_BLOCKERS, playerA, ACE, 2, 1);
        checkPT("after combat", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, ACE, 4, 1);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 2);
    }

    @Test
    public void test_Blocking() {
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, ACE);

        attack(1, playerA, "Grizzly Bears");
        block(1, playerB, ACE, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        // blocking 2/1 trades with the 2/2 bears instead of surviving as a 4/1 would not matter; the bears die
        assertGraveyardCount(playerA, "Grizzly Bears", 1);
        assertGraveyardCount(playerB, ACE, 1);
    }
}

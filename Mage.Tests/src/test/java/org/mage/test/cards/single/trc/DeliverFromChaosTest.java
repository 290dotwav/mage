package org.mage.test.cards.single.trc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class DeliverFromChaosTest extends CardTestPlayerBase {

    private static final String deliver = "Deliver from Chaos";

    @Test
    public void test_ExileAndBorgsByManaValue() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 2);
        addCard(Zone.HAND, playerA, deliver);
        addCard(Zone.BATTLEFIELD, playerB, "Serra Angel"); // mana value 5

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, deliver, "Serra Angel");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertExileCount(playerB, "Serra Angel", 1);
        assertPermanentCount(playerA, "Borg Token", 5);
    }
}

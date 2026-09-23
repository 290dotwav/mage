package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class ThorAsgardsAvengerTest extends CardTestPlayerBase {

    private static final String thor = "Thor, Asgard's Avenger";

    @Test
    public void test_OtherSourcesPlusOne_ThorItselfNot() {
        addCard(Zone.BATTLEFIELD, playerA, thor);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);
        addCard(Zone.HAND, playerA, "Shock", 2);
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant"); // 3/3
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerB);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", "Grizzly Bears"); // own creature: 2 damage, not boosted

        attack(3, playerA, thor);

        setStopAt(3, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        // Shock +1 = 3; Thor's own combat damage is not boosted = 4
        assertLife(playerB, 20 - 3 - 4);
        assertGraveyardCount(playerA, "Grizzly Bears", 1);
    }

    @Test
    public void test_PermanentOpponentControls() {
        addCard(Zone.BATTLEFIELD, playerA, thor);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);
        addCard(Zone.HAND, playerA, "Shock");
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant"); // 3/3

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", "Hill Giant");

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertGraveyardCount(playerB, "Hill Giant", 1);
    }
}

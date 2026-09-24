package org.mage.test.cards.single.leg;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class BackdraftTest extends CardTestPlayerBase {

    private static final String backdraft = "Backdraft";

    @Test
    public void test_HalfOfTheSorcerysDamage() {
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);
        addCard(Zone.HAND, playerA, backdraft);
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 5);
        addCard(Zone.HAND, playerB, "Lava Axe"); // 5 damage to target player

        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Lava Axe", playerA);
        waitStackResolved(2, PhaseStep.PRECOMBAT_MAIN);
        castSpell(2, PhaseStep.POSTCOMBAT_MAIN, playerA, backdraft);
        setChoice(playerA, "PlayerB"); // the player who cast a sorcery

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 15);
        assertLife(playerB, 20 - 2);
    }
}

package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestMultiPlayerBase;

/**
 * @author Claude
 */
public class MissHighwaterMultiplayerTest extends CardTestMultiPlayerBase {

    // a player with a contract counter loses: their artifacts and creatures come back as tokens for Miss Highwater's controller
    @Test
    public void test_ContractCollected() {
        // Player order: A -> D -> C -> B
        addCard(Zone.BATTLEFIELD, playerA, "Miss Highwater");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);
        addCard(Zone.HAND, playerA, "Shock");
        addCard(Zone.BATTLEFIELD, playerD, "Hill Giant");
        addCard(Zone.BATTLEFIELD, playerD, "Mind Stone");
        setLife(playerD, 6);

        attack(1, playerA, "Miss Highwater", playerD);
        setChoice(playerD, true); // sign
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Shock", playerD);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLostTheGame(playerD);
        assertTokenCount(playerA, "Hill Giant", 1);
        assertTokenCount(playerA, "Mind Stone", 1);
    }
}

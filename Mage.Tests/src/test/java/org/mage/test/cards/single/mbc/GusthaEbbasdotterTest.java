package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class GusthaEbbasdotterTest extends CardTestPlayerBase {

    // the first spell on an opponent's turn draws three and puts two back; the second spell that turn does nothing
    @Test
    public void test_FirstSpellOnOpponentsTurn() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, "Gustha Ebbasdotter");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);
        addCard(Zone.HAND, playerA, "Lightning Bolt");
        addCard(Zone.HAND, playerA, "Shock");
        addCard(Zone.LIBRARY, playerA, "Island", 3);

        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);
        addTarget(playerA, "Island"); // on top
        addTarget(playerA, "Island"); // on top
        waitStackResolved(2, PhaseStep.PRECOMBAT_MAIN, playerA);
        castSpell(2, PhaseStep.POSTCOMBAT_MAIN, playerA, "Shock", playerB);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 3 - 2);
        assertHandCount(playerA, 1);
        assertHandCount(playerA, "Island", 1);
    }

    // on its controller's own turn nothing happens
    @Test
    public void test_NotOnOwnTurn() {
        addCard(Zone.BATTLEFIELD, playerA, "Gustha Ebbasdotter");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);
        addCard(Zone.HAND, playerA, "Lightning Bolt");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerA, 0);
    }
}

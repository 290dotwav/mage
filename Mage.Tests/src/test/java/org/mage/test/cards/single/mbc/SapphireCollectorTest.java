package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class SapphireCollectorTest extends CardTestPlayerBase {

    private static final String collector = "Sapphire Collector";

    // the second noncreature spell conjures one Mox Sapphire, once per game
    @Test
    public void test_ConjureOnce() {
        addCard(Zone.BATTLEFIELD, playerA, collector);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 5);
        addCard(Zone.HAND, playerA, "Raging Goblin");
        addCard(Zone.HAND, playerA, "Shock", 4);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Raging Goblin");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerB);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkHandCardCount("not yet", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Mox Sapphire", 0);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerB);
        setChoice(playerA, "When you cast your second noncreature spell"); // order with prowess
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkHandCardCount("second noncreature", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Mox Sapphire", 1);
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerB);
        waitStackResolved(3, PhaseStep.PRECOMBAT_MAIN);
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerB);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerA, "Mox Sapphire", 1);
    }

    // {2}{U}: flashback for an instant in the graveyard
    @Test
    public void test_Flashback() {
        addCard(Zone.BATTLEFIELD, playerA, collector);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 3);
        addCard(Zone.GRAVEYARD, playerA, "Lightning Bolt");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}{U}: Target instant", "Lightning Bolt");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Flashback", playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 17);
        assertExileCount(playerA, "Lightning Bolt", 1);
    }
}

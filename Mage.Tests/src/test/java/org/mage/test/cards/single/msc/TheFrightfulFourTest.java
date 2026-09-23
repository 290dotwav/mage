package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class TheFrightfulFourTest extends CardTestPlayerBase {

    // Menace
    // Whenever an opponent casts their first noncreature spell each turn, that player loses life equal to that spell's mana value.
    private static final String FOUR = "The Frightful Four";

    @Test
    public void test_OnlyFirstNoncreatureSpell() {
        addCard(Zone.BATTLEFIELD, playerA, FOUR);
        addCard(Zone.HAND, playerB, "Grizzly Bears"); // creature, {1}{G}: ignored
        addCard(Zone.HAND, playerB, "Divination"); // {2}{U}: first noncreature -> 3 life
        addCard(Zone.HAND, playerB, "Lightning Bolt"); // second noncreature -> nothing
        addCard(Zone.BATTLEFIELD, playerB, "Forest", 5);
        addCard(Zone.BATTLEFIELD, playerB, "Island", 5);
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 5);

        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Grizzly Bears");
        castSpell(2, PhaseStep.POSTCOMBAT_MAIN, playerB, "Divination");
        castSpell(2, PhaseStep.END_TURN, playerB, "Lightning Bolt", playerA);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.CLEANUP);
        execute();

        assertLife(playerB, 20 - 3);
        assertLife(playerA, 20 - 3);
    }

    @Test
    public void test_OwnSpellsIgnored() {
        addCard(Zone.BATTLEFIELD, playerA, FOUR);
        addCard(Zone.HAND, playerA, "Divination");
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Divination");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20);
        assertHandCount(playerA, 2);
    }
}

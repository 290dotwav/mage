package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class TheMadcapJesterTest extends CardTestPlayerBase {

    // the attack exiles the opponent's top card, which can be cast with mana of any type
    @Test
    public void test_AttackExilesAndPlays() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, "The Madcap Jester");
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 1);
        removeAllCardsFromLibrary(playerB);
        addCard(Zone.LIBRARY, playerB, "Lightning Bolt");

        attack(1, playerA, "The Madcap Jester");
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Lightning Bolt", playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 3 - 3);
        assertGraveyardCount(playerB, "Lightning Bolt", 1);
    }

    // another of its controller's abilities exiling from an opponent's library counts too
    @Test
    public void test_OtherEffectCounts() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, "The Madcap Jester");
        addCard(Zone.BATTLEFIELD, playerA, "Circu, Dimir Lobotomist"); // blue spell: exile the top card of target library
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 1);
        addCard(Zone.HAND, playerA, "Storm Crow");
        removeAllCardsFromLibrary(playerB);
        addCard(Zone.LIBRARY, playerB, "Lightning Bolt");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Storm Crow");
        addTarget(playerA, playerB);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Lightning Bolt", playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 17);
    }
}

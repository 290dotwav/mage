package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class DaredevilFearlessFighterTest extends CardTestPlayerBase {

    // Whenever a source you control deals damage to you, Daredevil deals that much damage to target opponent.
    // Whenever Daredevil attacks, exile the top card of your library. Daredevil deals damage to you equal to that card's mana value. You may play it this turn.
    private static final String DAREDEVIL = "Daredevil, Fearless Fighter";

    @Test
    public void test_AttackExileDamageAndPlay() {
        skipInitShuffling();
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Hill Giant"); // {3}{R}, mana value 4
        addCard(Zone.BATTLEFIELD, playerA, DAREDEVIL);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 4);

        attack(1, playerA, DAREDEVIL, playerB);
        addTarget(playerA, playerB); // reflected damage
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Hill Giant");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20 - 4);
        assertLife(playerB, 20 - 4 - 3);
        assertPermanentCount(playerA, "Hill Giant", 1);
    }

    @Test
    public void test_OpponentSourceDoesNotTrigger() {
        addCard(Zone.BATTLEFIELD, playerA, DAREDEVIL);
        addCard(Zone.HAND, playerB, "Lightning Bolt");
        addCard(Zone.BATTLEFIELD, playerB, "Mountain");

        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Lightning Bolt", playerA);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 17);
        assertLife(playerB, 20);
    }
}

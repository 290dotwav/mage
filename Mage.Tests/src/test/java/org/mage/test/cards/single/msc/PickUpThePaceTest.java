package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class PickUpThePaceTest extends CardTestPlayerBase {

    // Whenever a creature you control that entered this turn attacks, exile the top card of your library.
    // You may play cards exiled with this enchantment as long as you attacked this turn.
    private static final String PACE = "Pick Up the Pace";

    @Test
    public void test_ExileAndPlayAfterAttacking() {
        skipInitShuffling();
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Mountain");
        addCard(Zone.BATTLEFIELD, playerA, PACE);
        addCard(Zone.HAND, playerA, "Raging Goblin"); // haste 1/1
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Raging Goblin");
        attack(1, playerA, "Raging Goblin", playerB);
        playLand(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Mountain");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Mountain", 2);
        assertLife(playerB, 19);
    }

    @Test
    public void test_NotWithoutAttacking() {
        skipInitShuffling();
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Island", 2);
        addCard(Zone.LIBRARY, playerA, "Mountain"); // exiled on turn 1
        addCard(Zone.BATTLEFIELD, playerA, PACE);
        addCard(Zone.HAND, playerA, "Raging Goblin");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Raging Goblin");
        attack(1, playerA, "Raging Goblin", playerB);
        // turn 3: no attack yet, so the exiled Mountain can't be played
        checkPlayableAbility("no attack yet", 3, PhaseStep.PRECOMBAT_MAIN, playerA, "Play Mountain", false);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertExileCount(playerA, "Mountain", 1);
    }

    @Test
    public void test_OldCreatureDoesNotExile() {
        addCard(Zone.BATTLEFIELD, playerA, PACE);
        addCard(Zone.BATTLEFIELD, playerA, "Raging Goblin");

        // on turn 1 a test permanent counts as having entered this turn: attack on turn 3
        attack(3, playerA, "Raging Goblin", playerB);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertExileCount(playerA, 0);
    }
}

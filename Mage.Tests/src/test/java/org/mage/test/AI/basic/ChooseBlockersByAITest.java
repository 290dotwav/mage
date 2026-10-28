package org.mage.test.AI.basic;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBaseWithAIHelps;

/**
 * An AI holding a "you choose which creatures block and how" effect
 * (ChooseBlockersEffect: Brutal Hordechief, Master Warcraft, Odric, Master
 * Tactician, Melee, Berserkers' Frenzy).
 * <p>
 * The AI's own blocker declaration raised DECLARING_BLOCKERS again, which the
 * effect replaced by asking its controller, the same AI, to declare the
 * blockers: StackOverflowError, the game stopped (seen on the fork's door,
 * three 4-seat Commander games with "Computer - simple" bots, Master Warcraft
 * in a bot's deck).
 *
 * @author Claude
 */
public class ChooseBlockersByAITest extends CardTestPlayerBaseWithAIHelps {

    @Test
    public void test_AiChoosesHowOpponentsBlock_NoLoop() {
        // Whenever a creature you control attacks, defending player loses 1 life and you gain 1 life.
        // {3}{R/W}{R/W}: Creatures your opponents control block this turn if able, and you choose how those creatures block.
        addCard(Zone.BATTLEFIELD, playerA, "Brutal Hordechief", 1); // 3/3
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 5);
        addCard(Zone.BATTLEFIELD, playerB, "Silvercoat Lion", 1); // 2/2

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{3}{R/W}{R/W}: Creatures your opponents control");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);

        attack(1, playerA, "Brutal Hordechief");
        // the AI declares the blockers it was given to choose (before: endless recursion)
        aiPlayStep(1, PhaseStep.DECLARE_BLOCKERS, playerA);

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        // the lion had to block (block if able) and did
        assertPermanentCount(playerA, "Brutal Hordechief", 1);
        assertGraveyardCount(playerB, "Silvercoat Lion", 1);
        assertLife(playerA, 20 + 1);
        assertLife(playerB, 20 - 1);
    }
}

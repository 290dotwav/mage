package mage.server.scenario;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestCommanderDuelBase;

/**
 * The campaign's sixth lesson, « Ton commandant » (Scenarios m6): a Commander duel, the student at
 * 40 with Arni Brokenbrow in the command zone, Claude at 9 with a Juggernaut that attacked last turn.
 */
public class ScenarioCommanderTest extends CardTestCommanderDuelBase {

    private void lesson() {
        Scenarios.apply(Scenarios.get("m6"), currentGame, gameOptions, playerA.getId());
    }

    @Test
    public void positionAsWritten() {
        lesson();
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertLife(playerA, 40);
        assertLife(playerB, 9);
        assertHandCount(playerA, 1);
        assertHandCount(playerA, "Fling", 1);
        assertPermanentCount(playerA, "Mountain", 7);
        assertTappedCount("Juggernaut", true, 1);
        assertCommandZoneCount(playerA, "Arni Brokenbrow", 1);
        // nothing of the deck it joined with: no other commander, no Ob Nixilis
        assertCommandZoneCount(playerA, "Ob Nixilis of the Black Oath", 0);
        assertCommandZoneCount(playerB, "Ob Nixilis of the Black Oath", 0);
    }

    @Test
    public void flingThenTheCommanderAgainWithTheTax() {
        lesson();
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Arni Brokenbrow");
        attack(1, playerA, "Arni Brokenbrow");
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Fling", playerB);
        setChoice(playerA, "Arni Brokenbrow"); // the sacrifice
        setChoice(playerA, true); // to the command zone
        checkLife("after the first turn", 1, PhaseStep.END_TURN, playerB, 3);
        // Juggernaut attacks by its own text on Claude's turn: 40 takes it
        checkLife("40 takes it", 2, PhaseStep.END_TURN, playerA, 35);
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Arni Brokenbrow"); // {2}{R} and the tax of {2}
        attack(3, playerA, "Arni Brokenbrow");
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 0);
        assertLostTheGame(playerB);
    }

    @Test
    public void twoAttacksAndFling() {
        lesson();
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Arni Brokenbrow");
        attack(1, playerA, "Arni Brokenbrow");
        attack(3, playerA, "Arni Brokenbrow");
        castSpell(3, PhaseStep.POSTCOMBAT_MAIN, playerA, "Fling", playerB);
        setChoice(playerA, "Arni Brokenbrow");
        setChoice(playerA, true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 0);
        assertLostTheGame(playerB);
    }

    @Test
    public void noAttackOnTheFirstTurnIsTooLate() {
        lesson();
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Arni Brokenbrow");
        attack(3, playerA, "Arni Brokenbrow");
        castSpell(3, PhaseStep.POSTCOMBAT_MAIN, playerA, "Fling", playerB);
        setChoice(playerA, "Arni Brokenbrow");
        setChoice(playerA, true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 3);
    }
}

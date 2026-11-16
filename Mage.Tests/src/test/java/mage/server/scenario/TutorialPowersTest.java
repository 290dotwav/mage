package mage.server.scenario;

import mage.constants.PhaseStep;
import mage.game.lesson.RivalScript;
import org.junit.Assert;
import org.junit.Test;

/**
 * The tutorial's fifth lesson, « Les pouvoirs » (Scenarios tuto5, TUTO.md mission 5): the Rival does
 * nothing; the Pyromancer, Sol Ring, Raise the Alarm and Blood Artist on turn 1 (10 to 9), the
 * Bombardment and a Soldier on turn 3 (9 to 7), then the machine to 0, the Artist last.
 */
public class TutorialPowersTest extends TutorialTestBase {

    private static final String PING = "{T}: {this} deals 1 damage";
    private static final String SACRIFICE = "Sacrifice a creature";

    @Test
    public void positionAsWritten() {
        RivalScript script = lesson("tuto5");
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        Assert.assertTrue(script.getLines().isEmpty());
        assertLife(playerB, 10);
        assertLife(playerA, 20);
        assertPermanentCount(playerA, "Prodigal Pyromancer", 1);
        assertPermanentCount(playerA, "Plains", 1);
        assertPermanentCount(playerA, "Swamp", 1);
        assertPermanentCount(playerA, "Mountain", 2);
        assertHandCount(playerA, "Sol Ring", 1);
        assertHandCount(playerA, "Raise the Alarm", 1);
        assertHandCount(playerA, "Blood Artist", 1);
        Assert.assertEquals("Goblin Bombardment", playerA.getLibrary().getFromTop(currentGame).getName());
        assertPermanentCount(playerB, 0);
        Assert.assertEquals("CMR", playerA.getHand().getCards(currentGame).stream().filter(c -> c.getName().equals("Sol Ring")).findFirst().get().getExpansionSetCode());
    }

    private void firstTurn() {
        // 5.5 b the Pyromancer on the Rival: 10 to 9
        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, PING, playerB);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN, playerA);
        // 5.10 Sol Ring, a Mountain pays (the site's payment, imposed)
        activateManaAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: Add {R}");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Sol Ring");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN, playerA);
        // 5.14 Raise the Alarm, the Plains and the Ring (its second mana waits in the pool)
        activateManaAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: Add {C}{C}");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Raise the Alarm");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN, playerA);
        // 5.21 Blood Artist, the Swamp and the Ring's second mana
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Blood Artist");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN, playerA);
    }

    @Test
    public void theFirstTurnAsWritten() {
        lesson("tuto5");
        firstTurn();
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertLife(playerB, 9);
        assertPermanentCount(playerA, "Sol Ring", 1);
        assertPermanentCount(playerA, "Soldier Token", 2);
        assertPermanentCount(playerA, "Blood Artist", 1);
        // the mana the script says: a Mountain for the Ring, the Plains, the Ring, the Swamp; one Mountain left
        assertTappedCount("Mountain", true, 1);
        assertTappedCount("Mountain", false, 1);
        assertTappedCount("Plains", true, 1);
        assertTappedCount("Swamp", true, 1);
        assertTappedCount("Sol Ring", true, 1);
    }

    @Test
    public void theLessonAsWritten() {
        lesson("tuto5");
        firstTurn();
        // 5.26 the Bombardment drawn and cast; 5.27 a Soldier: 1 and the Artist's 1, 9 to 7
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Goblin Bombardment");
        waitStackResolved(3, PhaseStep.PRECOMBAT_MAIN, playerA);
        activateAbility(3, PhaseStep.PRECOMBAT_MAIN, playerA, SACRIFICE, playerB);
        setChoice(playerA, "Soldier Token");
        waitStackResolved(3, PhaseStep.PRECOMBAT_MAIN, playerA);
        // 5.28 the Pyromancer (6), the other Soldier (4), the Pyromancer tapped (2), the Artist last (0)
        activateAbility(3, PhaseStep.PRECOMBAT_MAIN, playerA, PING, playerB);
        waitStackResolved(3, PhaseStep.PRECOMBAT_MAIN, playerA);
        activateAbility(3, PhaseStep.PRECOMBAT_MAIN, playerA, SACRIFICE, playerB);
        setChoice(playerA, "Soldier Token");
        waitStackResolved(3, PhaseStep.PRECOMBAT_MAIN, playerA);
        activateAbility(3, PhaseStep.PRECOMBAT_MAIN, playerA, SACRIFICE, playerB);
        setChoice(playerA, "Prodigal Pyromancer");
        waitStackResolved(3, PhaseStep.PRECOMBAT_MAIN, playerA);
        activateAbility(3, PhaseStep.PRECOMBAT_MAIN, playerA, SACRIFICE, playerB);
        setChoice(playerA, "Blood Artist");
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 0);
        assertLostTheGame(playerB);
        assertLife(playerA, 24); // the Artist: 1 for each of the four deaths
    }

    @Test
    public void theArtistTooEarlyIsShort() {
        lesson("tuto5");
        firstTurn();
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Goblin Bombardment");
        waitStackResolved(3, PhaseStep.PRECOMBAT_MAIN, playerA);
        activateAbility(3, PhaseStep.PRECOMBAT_MAIN, playerA, SACRIFICE, playerB);
        setChoice(playerA, "Soldier Token");
        waitStackResolved(3, PhaseStep.PRECOMBAT_MAIN, playerA);
        // « sans elle, chaque mort ne compte plus que pour 1 »
        activateAbility(3, PhaseStep.PRECOMBAT_MAIN, playerA, SACRIFICE, playerB);
        setChoice(playerA, "Blood Artist");
        waitStackResolved(3, PhaseStep.PRECOMBAT_MAIN, playerA);
        activateAbility(3, PhaseStep.PRECOMBAT_MAIN, playerA, PING, playerB);
        waitStackResolved(3, PhaseStep.PRECOMBAT_MAIN, playerA);
        activateAbility(3, PhaseStep.PRECOMBAT_MAIN, playerA, SACRIFICE, playerB);
        setChoice(playerA, "Soldier Token");
        waitStackResolved(3, PhaseStep.PRECOMBAT_MAIN, playerA);
        activateAbility(3, PhaseStep.PRECOMBAT_MAIN, playerA, SACRIFICE, playerB);
        setChoice(playerA, "Prodigal Pyromancer");
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        // 7, the Artist with her own death 2 (5), the Pyromancer 1 (4), then 1 and 1: 2 left
        assertLife(playerB, 2);
    }

    @Test
    public void theFirstSoldierIsTwoAtOnce() {
        lesson("tuto5");
        firstTurn();
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Goblin Bombardment");
        waitStackResolved(3, PhaseStep.PRECOMBAT_MAIN, playerA);
        activateAbility(3, PhaseStep.PRECOMBAT_MAIN, playerA, SACRIFICE, playerB);
        setChoice(playerA, "Soldier Token");
        setStopAt(3, PhaseStep.BEGIN_COMBAT);
        execute();

        // 5.27 « Deux d'un coup : 1 pour le Bombardement, 1 pour l'Artiste »: 9 to 7
        assertLife(playerB, 7);
        assertPermanentCount(playerA, "Soldier Token", 1);
        assertPermanentCount(playerA, "Goblin Bombardment", 1);
    }
}

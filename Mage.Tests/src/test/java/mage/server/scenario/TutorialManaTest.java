package mage.server.scenario;

import mage.constants.PhaseStep;
import mage.game.lesson.RivalScript;
import mage.game.permanent.Permanent;
import org.junit.Assert;
import org.junit.Test;

/**
 * The tutorial's second lesson, « Le mana » (Scenarios tuto2, TUTO.md mission 2): the position as
 * written, the lesson played as the script says it (2.4 to 2.24) with the Rival's life falling as it
 * says (6, 4, 0), the Rival doing nothing at all, and the engine's own refusals of the script (the
 * Bears without green in 2.11, the Lions that just came in 2.15, one creature only in 2.24).
 */
public class TutorialManaTest extends TutorialTestBase {

    @Test
    public void positionAsWritten() {
        RivalScript script = lesson("tuto2");
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        Assert.assertNotNull("the Rival has a script", script);
        Assert.assertFalse(script.isFree());
        Assert.assertTrue("and no line: he does nothing", script.getLines().isEmpty());
        Assert.assertEquals("the student starts", playerA.getId(), currentGame.getActivePlayerId());
        assertHandCount(playerA, 3);
        assertHandCount(playerA, "Plains", 1);
        assertHandCount(playerA, "Savannah Lions", 1);
        assertHandCount(playerA, "Grizzly Bears", 1);
        assertPermanentCount(playerA, 0);
        assertPermanentCount(playerB, 0);
        assertHandCount(playerB, 0);
        assertLife(playerA, 20);
        assertLife(playerB, 6);
        Assert.assertEquals("Forest", playerA.getLibrary().getFromTop(currentGame).getName());
        Assert.assertEquals("10E", playerA.getHand().getCards(currentGame).stream().filter(c -> c.getName().equals("Grizzly Bears")).findFirst().get().getExpansionSetCode());
    }

    @Test
    public void theLessonAsWritten() {
        lesson("tuto2");
        // 2.4 the Plains, 2.9 the Lions (the Plains pays)
        playLand(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Plains");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Savannah Lions");
        // 2.11 the Bears refused: the Plains has been used, and gives no green
        checkPlayableAbility("2.11 no mana for the Bears", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Grizzly Bears", false);
        checkPlayableAbility("2.11 not later either", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Cast Grizzly Bears", false);
        // 2.15 Fin; the Rival's turn passes with nothing
        checkLife("2.15 nothing in his turn", 2, PhaseStep.END_TURN, playerA, 20);
        // 2.18 the Forest drawn, 2.20 put down, 2.21 the Bears, 2.22 the Lions attack
        checkHandCardCount("2.18 the Forest drawn", 3, PhaseStep.PRECOMBAT_MAIN, playerA, "Forest", 1);
        playLand(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Forest");
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");
        attack(3, playerA, "Savannah Lions");
        checkLife("2.22 the Lions hit: 6 to 4", 3, PhaseStep.POSTCOMBAT_MAIN, playerB, 4);
        // 2.24 a Plains drawn, put down; everybody attacks: 4 to 0
        checkHandCardCount("2.24 a Plains drawn", 5, PhaseStep.PRECOMBAT_MAIN, playerA, "Plains", 1);
        playLand(5, PhaseStep.PRECOMBAT_MAIN, playerA, "Plains");
        attack(5, playerA, "Savannah Lions");
        attack(5, playerA, "Grizzly Bears");
        setStopAt(5, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 0);
        assertLostTheGame(playerB);
        assertLife(playerA, 20);
        assertPermanentCount(playerB, 0);
    }

    @Test
    public void theLionsThatJustCameCannotAttack() {
        lesson("tuto2");
        playLand(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Plains");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Savannah Lions");
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        Permanent lions = getPermanent("Savannah Lions", playerA);
        Assert.assertFalse("2.15 they just came", lions.canAttack(playerB.getId(), currentGame));
        assertLife(playerB, 6);
    }

    @Test
    public void oneCreatureOnlyLeavesHimAt2() {
        lesson("tuto2");
        playLand(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Plains");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Savannah Lions");
        playLand(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Forest");
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");
        attack(3, playerA, "Savannah Lions");
        attack(5, playerA, "Grizzly Bears");
        setStopAt(5, PhaseStep.END_TURN);
        execute();

        // « Avec une seule, il lui en resterait 2 »
        assertLife(playerB, 2);
        Assert.assertTrue(playerB.isInGame());
    }
}

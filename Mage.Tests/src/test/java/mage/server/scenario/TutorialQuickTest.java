package mage.server.scenario;

import mage.constants.PhaseStep;
import mage.game.lesson.RivalScript;
import mage.game.permanent.Permanent;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * The tutorial's fourth lesson, « Les cartes rapides » (Scenarios tuto4, TUTO.md mission 4): the
 * Rival's Shock on the student, his Volcanic Hammer on the Giant once the Shock has resolved, the
 * Giant Growth that saves it, his Wind Drake in the second main phase after 4.23, the Drake that
 * blocks the Giant; life 9, 6, 3, 0 for him and 20, 18 for the student; and the wrong tries.
 */
public class TutorialQuickTest extends TutorialTestBase {

    @Test
    public void positionAsWritten() {
        RivalScript script = lesson("tuto4");
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        Assert.assertEquals(4, script.getLines().size());
        Assert.assertEquals(playerA.getId(), currentGame.getActivePlayerId());
        assertLife(playerA, 20);
        assertLife(playerB, 9);
        assertPermanentCount(playerA, "Hill Giant", 1);
        assertPermanentCount(playerA, "Forest", 2);
        assertPermanentCount(playerA, "Mountain", 2);
        assertHandCount(playerA, "Volcanic Hammer", 1);
        assertHandCount(playerA, "Giant Growth", 1);
        assertHandCount(playerA, 2);
        Assert.assertEquals("Shock", playerA.getLibrary().getFromTop(currentGame).getName());
        assertPermanentCount(playerB, "Mountain", 2);
        assertPermanentCount(playerB, "Island", 4);
        assertPermanentCount(playerB, 6);
        assertHandCount(playerB, "Shock", 1);
        assertHandCount(playerB, "Volcanic Hammer", 1);
        assertHandCount(playerB, "Wind Drake", 1);
    }

    @Test
    public void theLessonWithTheGuide() throws Exception {
        RivalScript script = lesson("tuto4");
        List<String> seen = seen();
        Thread site = guide(script, seen, () -> "drakes " + currentGame.getBattlefield().getAllActivePermanents().stream().filter(p -> p.getName().equals("Wind Drake")).count()
                        + ", stack " + currentGame.getStack().size(),
                2, PhaseStep.POSTCOMBAT_MAIN, "4.23");
        // 4.7 the Hammer, 4.8 on the Rival: 9 to 6; 4.9 the Giant: 6 to 3
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Volcanic Hammer", playerB);
        attack(1, playerA, "Hill Giant");
        checkLife("4.8", 1, PhaseStep.DECLARE_ATTACKERS, playerB, 6);
        checkLife("4.9", 1, PhaseStep.POSTCOMBAT_MAIN, playerB, 3);
        // 4.11 his Shock on the student, 4.14 it resolves: 20 to 18; 4.15 his Hammer on the Giant;
        // 4.19 the Growth on the Giant, over the Hammer; 4.22 the Giant holds
        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerA, "Giant Growth", "Hill Giant", "Volcanic Hammer", StackClause.WHILE_ON_STACK);
        checkLife("4.14", 2, PhaseStep.BEGIN_COMBAT, playerA, 18);
        checkPermanentCount("4.22 the Giant holds", 2, PhaseStep.BEGIN_COMBAT, playerA, "Hill Giant", 1);
        checkGraveyardCount("4.22 his Hammer", 2, PhaseStep.BEGIN_COMBAT, playerB, "Volcanic Hammer", 1);
        // 4.23 the Drake, paid with his Islands
        checkPermanentCount("4.23", 2, PhaseStep.END_TURN, playerB, "Wind Drake", 1);
        // 4.25 the Shock drawn, on the Drake; 4.26 the Giant: 3 to 0
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", "Wind Drake");
        waitStackResolved(3, PhaseStep.PRECOMBAT_MAIN, playerA);
        checkPermanentCount("4.25 the Drake died", 3, PhaseStep.BEGIN_COMBAT, playerB, "Wind Drake", 0);
        attack(3, playerA, "Hill Giant");
        setStopAt(3, PhaseStep.END_TURN);
        execute();
        site.join(5000);

        Assert.assertEquals("4.23 at 2 Postcombat Main: drakes 0, stack 0", seen.get(0));
        assertLife(playerB, 0);
        assertLostTheGame(playerB);
        assertLife(playerA, 18);
        // his three spells cast, nothing else
        assertHandCount(playerB, "Shock", 0);
        assertHandCount(playerB, "Volcanic Hammer", 0);
        assertHandCount(playerB, "Wind Drake", 0);
    }

    @Test
    public void passingOnHisHammerKillsTheGiant() {
        RivalScript script = lesson("tuto4");
        reached(script, "4.23");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Volcanic Hammer", playerB);
        attack(1, playerA, "Hill Giant");
        // 4.19 « Si tu passes, son Marteau tombe et ton Géant meurt »
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, "Hill Giant", 1);
        assertLife(playerA, 18);
    }

    @Test
    public void shockOnTheRivalLeavesHimAt1AndTheDrakeBlocks() {
        RivalScript script = lesson("tuto4");
        reached(script, "4.23");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Volcanic Hammer", playerB);
        attack(1, playerA, "Hill Giant");
        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerA, "Giant Growth", "Hill Giant", "Volcanic Hammer", StackClause.WHILE_ON_STACK);
        // 4.25 « Ton Choc ne lui ferait que 2 : il resterait à 1, et son Drakôn bloquerait ton Géant »
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerB);
        attack(3, playerA, "Hill Giant");
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 1);
        Assert.assertTrue(playerB.isInGame());
        assertGraveyardCount(playerB, "Wind Drake", 1);
        Permanent giant = getPermanent("Hill Giant", playerA);
        Assert.assertEquals("blocked by the Drake", 2, giant.getDamage());
    }
}

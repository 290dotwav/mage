package mage.server.scenario;

import mage.constants.PhaseStep;
import mage.game.lesson.RivalScript;
import mage.game.permanent.Permanent;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * The tutorial's third lesson, « La bagarre » (Scenarios tuto3, TUTO.md mission 3): the Rival
 * starts and does exactly the script's lines (one Bears attacks after 3.4; the other blocks the
 * Giant; the Wurm, then the Assailant after 3.20; no block; the Assailant and the Wurm), and the
 * life totals fall as the script says, for each of the choices it accepts in 3.23 and 3.24.
 */
public class TutorialFightTest extends TutorialTestBase {

    private int life(mage.players.Player player) {
        return currentGame.getPlayer(player.getId()).getLife();
    }

    @Test
    public void positionAsWritten() {
        RivalScript script = lesson("tuto3");
        setStopAt(1, PhaseStep.UPKEEP);
        execute();

        Assert.assertEquals(7, script.getLines().size());
        Assert.assertEquals("the Rival starts", playerB.getId(), currentGame.getActivePlayerId());
        assertLife(playerA, 20);
        assertLife(playerB, 5);
        assertPermanentCount(playerA, "Hill Giant", 1);
        assertPermanentCount(playerA, "Wind Drake", 1);
        assertPermanentCount(playerA, 2);
        assertHandCount(playerA, 0);
        assertPermanentCount(playerB, "Grizzly Bears", 2);
        assertPermanentCount(playerB, "Goblin Assailant", 1);
        assertPermanentCount(playerB, "Forest", 6);
        assertHandCount(playerB, "Craw Wurm", 1);
        Assert.assertEquals("KLD", getPermanent("Wind Drake", playerA).getExpansionSetCode());
        Assert.assertEquals("M20", getPermanent("Goblin Assailant", playerB).getExpansionSetCode());
    }

    @Test
    public void theLessonWithTheGuideGiantOnTheAssailant() throws Exception {
        // the guide's steps said by the site's thread when the game waits for them
        RivalScript script = lesson("tuto3");
        List<String> seen = seen();
        Thread site = guide(script, seen, () -> "student " + life(playerA) + ", attackers " + currentGame.getCombat().getAttackers().size()
                        + ", wurm " + currentGame.getBattlefield().getAllActivePermanents().stream().filter(p -> p.getName().equals("Craw Wurm")).count(),
                1, PhaseStep.DECLARE_ATTACKERS, "3.4",
                3, PhaseStep.DECLARE_ATTACKERS, "3.20");
        // 3.5 b the Giant blocks the Bears that attacks: it dies, the Giant holds
        block(1, playerA, "Hill Giant", "Grizzly Bears");
        checkLife("3.7 nothing through", 1, PhaseStep.END_TURN, playerA, 20);
        // 3.13 both attack; 3.14 the other Bears blocks the Giant; the Drake flies over: 5 to 3
        attack(2, playerA, "Hill Giant");
        attack(2, playerA, "Wind Drake");
        checkLife("3.15 the Drake's 2", 2, PhaseStep.POSTCOMBAT_MAIN, playerB, 3);
        checkPermanentCount("3.14 the second Bears died on the Giant", 2, PhaseStep.POSTCOMBAT_MAIN, playerB, "Grizzly Bears", 0);
        // 3.17 the Wurm; 3.20 the Assailant, no block possible (both tapped): 20 to 18
        checkPermanentCount("3.17 the Wurm", 3, PhaseStep.DECLARE_ATTACKERS, playerB, "Craw Wurm", 1);
        checkLife("3.21", 3, PhaseStep.POSTCOMBAT_MAIN, playerA, 18);
        // 3.23 the Drake alone: 3 to 1 (no block: the Wurm cannot block a flyer anyway)
        attack(4, playerA, "Wind Drake");
        checkLife("3.23 Drake alone", 4, PhaseStep.POSTCOMBAT_MAIN, playerB, 1);
        // 3.24 Assailant and Wurm attack; the Giant takes the Assailant: 18 to 12
        block(5, playerA, "Hill Giant", "Goblin Assailant");
        checkLife("3.24 « Il te reste 12 points de vie »", 5, PhaseStep.POSTCOMBAT_MAIN, playerA, 12);
        checkPermanentCount("3.24 the Assailant died", 5, PhaseStep.POSTCOMBAT_MAIN, playerB, "Goblin Assailant", 0);
        checkPermanentCount("3.24 the Giant lives", 5, PhaseStep.POSTCOMBAT_MAIN, playerA, "Hill Giant", 1);
        // 3.25 the Wurm attacked, it is tapped: the Giant finishes him
        attack(6, playerA, "Hill Giant");
        setStopAt(6, PhaseStep.END_TURN);
        execute();
        site.join(5000);
        // he draws nothing all the lesson: his hand stays empty, his library whole
        assertHandCount(playerA, 0);
        assertLibraryCount(playerA, 6);

        Assert.assertEquals(2, seen.size());
        Assert.assertEquals("3.4 at 1 Declare Attackers: student 20, attackers 0, wurm 0", seen.get(0));
        Assert.assertEquals("3.20 at 3 Declare Attackers: student 20, attackers 0, wurm 1", seen.get(1));
        assertLife(playerB, -2);
        assertLostTheGame(playerB);
        assertLife(playerA, 12);
    }

    @Test
    public void noAttackIn323TheDrakeOnTheAssailant() {
        RivalScript script = lesson("tuto3");
        reached(script, "3.4", "3.20");
        block(1, playerA, "Hill Giant", "Grizzly Bears");
        attack(2, playerA, "Hill Giant");
        attack(2, playerA, "Wind Drake");
        // 3.23 « Fin » without attacking; 3.24 the Drake on the Assailant: both die, 12
        block(5, playerA, "Wind Drake", "Goblin Assailant");
        checkLife("3.24 « Il te reste 12 points de vie »", 5, PhaseStep.POSTCOMBAT_MAIN, playerA, 12);
        checkPermanentCount("3.24 the Drake died", 5, PhaseStep.POSTCOMBAT_MAIN, playerA, "Wind Drake", 0);
        checkPermanentCount("3.24 the Assailant died", 5, PhaseStep.POSTCOMBAT_MAIN, playerB, "Goblin Assailant", 0);
        // 3.25 the Rival at 3: the Giant's 3
        attack(6, playerA, "Hill Giant");
        setStopAt(6, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 0);
        assertLostTheGame(playerB);
    }

    @Test
    public void noBlockIn324TakesEverything() {
        RivalScript script = lesson("tuto3");
        reached(script, "3.4", "3.20");
        block(1, playerA, "Hill Giant", "Grizzly Bears");
        attack(2, playerA, "Hill Giant");
        attack(2, playerA, "Wind Drake");
        attack(4, playerA, "Wind Drake");
        // « Tu prends tout : 8 dégâts. Il t'en reste 10. »
        setStopAt(5, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 10);
        assertLife(playerB, 1);
    }

    @Test
    public void tappedCreaturesCannotBlockIn320() {
        RivalScript script = lesson("tuto3");
        reached(script, "3.4", "3.20");
        block(1, playerA, "Hill Giant", "Grizzly Bears");
        attack(2, playerA, "Hill Giant");
        attack(2, playerA, "Wind Drake");
        setStopAt(3, PhaseStep.DECLARE_BLOCKERS);
        execute();

        Permanent assailant = getPermanent("Goblin Assailant", playerB);
        Assert.assertTrue(assailant.isAttacking());
        Assert.assertFalse("3.20 the Giant is tapped", getPermanent("Hill Giant", playerA).canBlock(assailant.getId(), currentGame));
        Assert.assertFalse("3.20 the Drake is tapped", getPermanent("Wind Drake", playerA).canBlock(assailant.getId(), currentGame));
        Assert.assertFalse("the Wurm just came", getPermanent("Craw Wurm", playerB).isAttacking());
    }
}

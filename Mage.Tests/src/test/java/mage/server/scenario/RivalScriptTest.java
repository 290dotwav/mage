package mage.server.scenario;

import mage.constants.PhaseStep;
import mage.game.lesson.RivalScript;
import mage.game.permanent.Permanent;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static mage.game.lesson.RivalScript.at;
import static mage.game.lesson.RivalScript.script;

/**
 * The Rival who follows the scenario (RivalScript in ComputerPlayerSimple): he does his lines and
 * nothing else, each at its moment, a line with waitFor only once the site has said that step, and
 * the free Rival never puts two blockers on one attacker.
 */
public class RivalScriptTest extends TutorialTestBase {

    private static Scenario rivalWith(RivalScript script) {
        return new Scenario("test-rival", Scenarios.DUEL)
                .student(s -> s.life(20)
                        .battlefield("10E:Hill Giant", "Forest", "Forest", "Forest", "Forest")
                        .hand("10E:Giant Growth")
                        .library("Forest", "Forest", "Forest", "Forest", "Forest"))
                .opponent(o -> o.life(20)
                        .battlefield("10E:Grizzly Bears", "10E:Grizzly Bears", "M20:Goblin Assailant", "Mountain", "Mountain", "Island", "Island", "Island", "Island")
                        .hand("10E:Shock", "9ED:Volcanic Hammer", "KLD:Wind Drake#70")
                        .library("Island", "Island", "Island", "Island", "Island"))
                .starts("opponent")
                .rival(script);
    }

    @Test
    public void noLineNothingDone() {
        lesson(rivalWith(script()));
        setStopAt(3, PhaseStep.PRECOMBAT_MAIN);
        execute();

        // three creatures ready, spells and mana in hand: he attacked with nobody, cast nothing, blocked nothing
        assertLife(playerA, 20);
        assertHandCount(playerB, 4);
        assertPermanentCount(playerB, 9);
        assertTappedCount("Grizzly Bears", false, 2);
    }

    @Test
    public void attacksWithTheOneNamedOnly() {
        lesson(rivalWith(script(at(1, PhaseStep.DECLARE_ATTACKERS).attack("Grizzly Bears"))));
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 18);
        assertTappedCount("Grizzly Bears", true, 1);
        assertTappedCount("Goblin Assailant", false, 1);
    }

    @Test
    public void attackWaitsForTheSiteStep() throws Exception {
        RivalScript script = lesson(rivalWith(script(at(1, PhaseStep.DECLARE_ATTACKERS).attack("Grizzly Bears").waitFor("3.4"))));
        setStopAt(1, PhaseStep.END_TURN);
        AtomicReference<String> whileWaiting = new AtomicReference<>("not looked");
        Thread site = new Thread(() -> {
            try {
                Thread.sleep(3000);
                whileWaiting.set(currentGame.getTurnNum() + " " + currentGame.getTurnStepType() + " life " + currentGame.getPlayer(playerA.getId()).getLife()
                        + " attackers " + currentGame.getCombat().getAttackers().size());
                script.reached("3.0"); // a step no line waits for: nothing
                Thread.sleep(500);
                script.reached("3.4");
            } catch (InterruptedException ignored) {
            }
        });
        site.start();
        execute();
        site.join();

        Assert.assertEquals("the game waited for the guide, at the declaration of attackers", "1 Declare Attackers life 20 attackers 0", whileWaiting.get());
        assertLife(playerA, 18);
    }

    @Test
    public void blocksTheNamedAttackerWithTheNamedBlocker() {
        lesson(rivalWith(script(at(2, PhaseStep.DECLARE_BLOCKERS).block("Grizzly Bears", "Hill Giant"))));
        attack(2, playerA, "Hill Giant");
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20);
        assertPermanentCount(playerB, "Grizzly Bears", 1);
        assertGraveyardCount(playerB, "Grizzly Bears", 1);
        assertPermanentCount(playerA, "Hill Giant", 1);
    }

    @Test
    public void aBlockOfAnAttackerThatNeverCameIsDropped() {
        lesson(rivalWith(script(
                at(2, PhaseStep.DECLARE_BLOCKERS).block("Grizzly Bears", "Hill Giant"),
                at(3, PhaseStep.DECLARE_ATTACKERS).attack("Goblin Assailant"))));
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        // no attack on turn 2, so no block; the next line still plays
        assertPermanentCount(playerB, "Grizzly Bears", 2);
        assertLife(playerA, 18);
    }

    @Test
    public void castsInOrderOnTheirTargetsTheSecondAfterTheFirstResolved() {
        // Shock on the student, then the Hammer on the Giant once the Shock has had its effect; the
        // student answers the Hammer with Giant Growth on his Giant; the Drake in the second main phase
        lesson(rivalWith(script(
                at(1, PhaseStep.PRECOMBAT_MAIN).cast("Shock", RivalScript.STUDENT),
                at(1, PhaseStep.PRECOMBAT_MAIN).cast("Volcanic Hammer", "Hill Giant"),
                at(1, PhaseStep.POSTCOMBAT_MAIN).castCreature("Wind Drake"))));
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Giant Growth", "Hill Giant", "Volcanic Hammer", StackClause.WHILE_ON_STACK);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 18);
        assertGraveyardCount(playerB, "Shock", 1);
        assertGraveyardCount(playerB, "Volcanic Hammer", 1);
        assertPermanentCount(playerA, "Hill Giant", 1);
        Permanent giant = getPermanent("Hill Giant", playerA);
        Assert.assertEquals(6, giant.getToughness().getValue());
        Assert.assertEquals(3, giant.getDamage());
        assertPermanentCount(playerB, "Wind Drake", 1);
        assertHandCount(playerB, 0);
    }

    @Test
    public void aCastWaitsForItsStepInItsPhase() throws Exception {
        RivalScript script = lesson(rivalWith(script(at(1, PhaseStep.POSTCOMBAT_MAIN).castCreature("Wind Drake").waitFor("4.23"))));
        setStopAt(1, PhaseStep.END_TURN);
        AtomicReference<String> whileWaiting = new AtomicReference<>("not looked");
        Thread site = new Thread(() -> {
            try {
                Thread.sleep(3000);
                whileWaiting.set(currentGame.getTurnStepType() + " drakes " + currentGame.getBattlefield().count(
                        new mage.filter.common.FilterCreaturePermanent(mage.constants.SubType.DRAKE, "Drake"), playerB.getId(), null, currentGame));
                script.reached("4.23");
            } catch (InterruptedException ignored) {
            }
        });
        site.start();
        execute();
        site.join();

        Assert.assertEquals("Postcombat Main drakes 0", whileWaiting.get());
        assertPermanentCount(playerB, "Wind Drake", 1);
    }

    @Test
    public void theFreeRivalNeverBlocksWithTwo() {
        // a 3/3 into two 2/2s: their AI would kill it with both; the free Rival of the last lesson blocks with one at most
        lesson(new Scenario("test-free", Scenarios.DUEL)
                .student(s -> s.life(20)
                        .battlefield("10E:Hill Giant")
                        .library("Forest", "Forest", "Forest", "Forest", "Forest"))
                .opponent(o -> o.life(3)
                        .battlefield("10E:Grizzly Bears", "10E:Grizzly Bears")
                        .library("Island", "Island", "Island", "Island", "Island"))
                .rival(RivalScript.free().noMultiBlock()));
        attack(1, playerA, "Hill Giant");
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        // at 3 life he must block the 3/3 to live: with one Bear (dead), never two (the Giant would die)
        assertPermanentCount(playerA, "Hill Giant", 1);
        assertGraveyardCount(playerB, "Grizzly Bears", 1);
        assertLife(playerB, 3);
    }

    @Test
    public void theRivalOfTheFirstGameNeverBlocks() {
        // the same 3/3 into two 2/2s, the Rival at 3: with noBlock he takes it and dies, both Bears untouched
        lesson(new Scenario("test-noblock", Scenarios.DUEL)
                .student(s -> s.life(20)
                        .battlefield("10E:Hill Giant")
                        .library("Forest", "Forest", "Forest", "Forest", "Forest"))
                .opponent(o -> o.life(3)
                        .battlefield("10E:Grizzly Bears", "10E:Grizzly Bears")
                        .library("Island", "Island", "Island", "Island", "Island"))
                .rival(RivalScript.free().noMultiBlock().noBlock()));
        attack(1, playerA, "Hill Giant");
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Hill Giant", 1);
        assertPermanentCount(playerB, "Grizzly Bears", 2);
        assertLife(playerB, 0);
    }

    private static Scenario menaceInto(RivalScript script) {
        return new Scenario("test-menace", Scenarios.DUEL)
                .student(s -> s.life(20)
                        .battlefield("Boggart Brute")
                        .library("Forest", "Forest", "Forest", "Forest", "Forest"))
                .opponent(o -> o.life(3)
                        .battlefield("10E:Grizzly Bears", "10E:Grizzly Bears")
                        .library("Island", "Island", "Island", "Island", "Island"))
                .rival(script);
    }

    @Test
    public void menaceTheirBotBlocksWithTwo() {
        // menace: one blocker cannot block it, their bot puts both Bears in front (what noMultiBlock prevents)
        lesson(menaceInto(RivalScript.free()));
        attack(1, playerA, "Boggart Brute");
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, "Boggart Brute", 1);
        assertLife(playerB, 3);
    }

    @Test
    public void menaceTheFreeRivalOfTheLessonDoesNot() {
        lesson(menaceInto(RivalScript.free().noMultiBlock()));
        attack(1, playerA, "Boggart Brute");
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Boggart Brute", 1);
        assertPermanentCount(playerB, "Grizzly Bears", 2);
        assertLife(playerB, 0);
    }
}

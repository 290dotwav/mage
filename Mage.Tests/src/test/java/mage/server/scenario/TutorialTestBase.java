package mage.server.scenario;

import mage.constants.RangeOfInfluence;
import mage.game.lesson.RivalScript;
import mage.constants.PhaseStep;
import org.junit.Assert;
import org.mage.test.player.TestComputerPlayerSimple;
import org.mage.test.player.TestPlayer;
import org.mage.test.serverside.base.CardTestPlayerBase;

import java.util.ArrayList;
import java.util.List;

/**
 * The site's tutorial (Scenarios tuto2...): playerA is the student, scripted by the test as the
 * site would play him; playerB is the Rival, the real "Computer - simple" bot of the table, every
 * decision its own (AIPlayer on), so what it does is what its RivalScript makes it do.
 */
public abstract class TutorialTestBase extends CardTestPlayerBase {

    @Override
    protected TestPlayer createPlayer(String name, RangeOfInfluence rangeOfInfluence) {
        if ("PlayerB".equals(name)) {
            TestPlayer rival = new TestPlayer(new TestComputerPlayerSimple(name, rangeOfInfluence, 1));
            rival.setAIPlayer(true);
            return rival;
        }
        return super.createPlayer(name, rangeOfInfluence);
    }

    /** That lesson set up for this game; its Rival's script, this game's copy (null when none). */
    protected RivalScript lesson(String id) {
        Scenario scenario = Scenarios.get(id);
        Assert.assertNotNull(id, scenario);
        return lesson(scenario);
    }

    protected RivalScript lesson(Scenario scenario) {
        Scenarios.apply(scenario, currentGame, gameOptions, playerA.getId());
        RivalScript script = gameOptions.rivalScript;
        if (script != null) {
            Assert.assertEquals("the Rival is the scripted player", playerB.getId(), script.getPlayerId());
        }
        return script;
    }

    /** Says the site's steps, as the door's lessonStep does, before the game runs. */
    protected void reached(RivalScript script, String... steps) {
        for (String step : steps) {
            script.reached(step);
        }
    }

    /**
     * The site, on its own thread, as the door's lessonStep: for each (turn, step, stepId), waits
     * until the game has stood at that turn and step for a second (the Rival waiting there), notes
     * what the board is then ({@code look}), and says the step.
     */
    protected Thread guide(RivalScript script, List<String> seen, java.util.function.Supplier<String> look, Object... turnStepIds) {
        Thread site = new Thread(() -> {
            try {
                for (int i = 0; i < turnStepIds.length; i += 3) {
                    int turn = (Integer) turnStepIds[i];
                    PhaseStep step = (PhaseStep) turnStepIds[i + 1];
                    String stepId = (String) turnStepIds[i + 2];
                    long since = 0;
                    for (int t = 0; t < 1200; t++) {
                        boolean there = currentGame.getTurnNum() == turn && currentGame.getTurnStepType() == step;
                        since = there ? (since == 0 ? System.currentTimeMillis() : since) : 0;
                        if (since != 0 && System.currentTimeMillis() - since > 1000) {
                            break;
                        }
                        Thread.sleep(50);
                    }
                    seen.add(stepId + " at " + currentGame.getTurnNum() + " " + currentGame.getTurnStepType() + ": " + look.get());
                    script.reached(stepId);
                }
            } catch (InterruptedException ignored) {
            }
        }, "site");
        site.setDaemon(true);
        site.start();
        return site;
    }

    protected static List<String> seen() {
        return java.util.Collections.synchronizedList(new ArrayList<>());
    }
}

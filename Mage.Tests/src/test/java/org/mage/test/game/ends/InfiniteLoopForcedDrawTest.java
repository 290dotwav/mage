package org.mage.test.game.ends;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.events.TableEvent;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ClaudeMTG fork: a mandatory infinite loop asks every player for a draw (GameImpl.checkInfiniteLoop).
 * Upstream asks again forever while one player refuses; the fork makes the game a draw at the 3rd
 * refusal for the same loop, and the log names the player who refused (upstream named the loop's controller).
 *
 * @author ClaudeMTG
 */
public class InfiniteLoopForcedDrawTest extends CardTestPlayerBase {

    private final List<String> log = new ArrayList<>();

    private void listenToLog() {
        currentGame.addTableEventListener(event -> {
            if (event.getEventType() == TableEvent.EventType.INFO) {
                log.add(event.getMessage());
            }
        });
    }

    private List<String> loopLines() {
        return log.stream()
                .map(line -> line.replaceAll("<[^>]*>", "")) // player names are coloured in the log
                .filter(line -> line.contains("infinite loop"))
                .collect(Collectors.toList());
    }

    // Stuffy Doll chooses its controller, who wears Pariah's Shield: the doll damages itself forever.
    private void stuffyDollLoop() {
        listenToLog();
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 5);
        // All damage that would be dealt to you is dealt to equipped creature instead.
        addCard(Zone.BATTLEFIELD, playerA, "Pariah's Shield", 1);
        // As Stuffy Doll enters, choose a player. Whenever Stuffy Doll is dealt damage, it deals that much
        // damage to the chosen player. {T}: Stuffy Doll deals 1 damage to itself.
        addCard(Zone.HAND, playerA, "Stuffy Doll", 1);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Stuffy Doll");
        setChoice(playerA, "PlayerA");
        activateAbility(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Equip", "Stuffy Doll");
        activateAbility(3, PhaseStep.POSTCOMBAT_MAIN, playerA, "{T}: {this} deals");
        setStopAt(3, PhaseStep.END_TURN);
    }

    private void assertDraw() {
        Assert.assertTrue("game has ended", currentGame.hasEnded());
        Assert.assertTrue("game is a draw", currentGame.isADraw());
        assertHasNotWonTheGame(playerA);
        assertHasNotWonTheGame(playerB);
    }

    @Test
    public void test_AllSayYes_Draw() {
        setStrictChooseMode(true);
        stuffyDollLoop();
        setChoice(playerA, true);
        setChoice(playerB, true);

        execute();

        assertDraw();
        Assert.assertEquals(2, loopLines().size());
        Assert.assertTrue(loopLines().get(0).startsWith("PlayerA has confirmed"));
        Assert.assertTrue(loopLines().get(1).startsWith("PlayerB has confirmed"));
    }

    @Test
    public void test_OneRefusalThenYes_Draw() {
        setStrictChooseMode(true);
        stuffyDollLoop();
        // first question: the opponent refuses
        setChoice(playerA, true);
        setChoice(playerB, false);
        // second question, 16 resolutions later: everyone agrees
        setChoice(playerA, true);
        setChoice(playerB, true);

        execute();

        assertDraw();
        List<String> lines = loopLines();
        Assert.assertTrue(lines.toString(), lines.contains(
                "PlayerB has NOT confirmed that the game is a draw because of infinite looping (refusal 1 of 3)."));
        Assert.assertTrue(lines.toString(), lines.stream().noneMatch(line -> line.startsWith("PlayerA has NOT")));
        Assert.assertTrue(lines.toString(), lines.stream().noneMatch(line -> line.startsWith("The infinite loop cannot end")));
    }

    @Test
    public void test_RepeatedRefusals_ForcedDrawAtThird() {
        setStrictChooseMode(true);
        stuffyDollLoop();
        setChoice(playerA, false); // refusal 1
        setChoice(playerA, true);
        setChoice(playerB, false); // refusal 2
        setChoice(playerA, true);
        setChoice(playerB, false); // refusal 3: the game is a draw, nobody is asked again

        execute();

        assertDraw();
        List<String> lines = loopLines();
        Assert.assertEquals(lines.toString(), 6, lines.size());
        Assert.assertEquals("PlayerA has NOT confirmed that the game is a draw because of infinite looping (refusal 1 of 3).", lines.get(0));
        Assert.assertEquals("PlayerA has confirmed that the game is a draw because of infinite looping.", lines.get(1));
        Assert.assertEquals("PlayerB has NOT confirmed that the game is a draw because of infinite looping (refusal 2 of 3).", lines.get(2));
        Assert.assertEquals("PlayerA has confirmed that the game is a draw because of infinite looping.", lines.get(3));
        Assert.assertEquals("PlayerB has NOT confirmed that the game is a draw because of infinite looping (refusal 3 of 3).", lines.get(4));
        Assert.assertEquals("The infinite loop cannot end: the draw was refused 3 times, so the game is a draw.", lines.get(5));
    }

    @Test
    public void test_LoopThatLosesLife_Unchanged() {
        // a loop where a player loses life is no infinite loop: nobody is asked (strict mode fails on any
        // question), the loop runs until the opponent dies
        setStrictChooseMode(true);
        listenToLog();
        // Whenever you gain life, target opponent loses that much life.
        addCard(Zone.BATTLEFIELD, playerA, "Sanguine Bond", 1);
        // Whenever an opponent loses life, you gain that much life.
        addCard(Zone.BATTLEFIELD, playerA, "Exquisite Blood", 1);
        // Scoured Barrens enters tapped. When it enters, you gain 1 life.
        addCard(Zone.HAND, playerA, "Scoured Barrens", 1);
        playLand(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Scoured Barrens");
        for (int i = 0; i < 20; i++) {
            addTarget(playerA, playerB); // Sanguine Bond, once per life point of the opponent
        }
        setStopAt(1, PhaseStep.END_TURN);

        execute();

        Assert.assertTrue("game has ended", currentGame.hasEnded());
        Assert.assertFalse("no draw", currentGame.isADraw());
        assertLife(playerB, 0);
        assertWonTheGame(playerA);
        Assert.assertTrue(loopLines().toString(), loopLines().isEmpty());
    }
}

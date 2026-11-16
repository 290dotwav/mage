package mage.server.scenario;

import mage.cards.Card;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.lesson.RivalScript;
import mage.players.Player;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

/**
 * The tutorial's last lesson, « Ta première partie » (Scenarios tuto7, TUTO.md mission 7): a game
 * dealt as a normal one from the two decks of 40 (shuffled, seven cards, the mulligan, who starts at
 * random), 20 life each, the Rival's Serra Angel among the first five cards of his library once the
 * hands are kept, and the Rival playing freely. The game is started as a real one (not in the tests'
 * mode, which draws no hand).
 */
public class TutorialFirstGameTest extends TutorialTestBase {

    private RivalScript deal(int stopTurn, PhaseStep stopStep) {
        RivalScript script = lesson("tuto7");
        gameOptions.testMode = false; // a real game's start: hands drawn, the mulligan
        gameOptions.stopOnTurn = stopTurn;
        gameOptions.stopAtStep = stopStep;
        currentGame.setGameOptions(gameOptions);
        currentGame.start(playerA.getId());
        return script;
    }

    private static int count(List<String> deck, String name) {
        return (int) deck.stream().filter(name::equals).count();
    }

    @Test
    public void theDecksAsWritten() {
        Scenario tuto7 = Scenarios.get("tuto7");
        Assert.assertTrue(tuto7.isDealt());
        Assert.assertTrue(tuto7.startsAtRandom());
        Assert.assertEquals(Scenarios.DUEL, tuto7.getGameType());
        List<String> student = tuto7.getStudent().getDeck();
        List<String> rival = tuto7.getOpponent().getDeck();
        Assert.assertEquals(40, student.size());
        Assert.assertEquals(40, rival.size());
        Assert.assertEquals("17 lands, Mountains and Forests", 17, count(student, "10E:Mountain") + count(student, "10E:Forest"));
        Assert.assertEquals(17, count(rival, "10E:Plains") + count(rival, "10E:Island"));
        Assert.assertEquals("one Angel", 1, count(rival, "CMD:Serra Angel"));
        for (String card : student) {
            Assert.assertTrue("only cards read in the lessons: " + card, card.matches("10E:(Mountain|Forest|Grizzly Bears|Hill Giant|Giant Growth|Shock)|M20:Goblin Assailant|9ED:Volcanic Hammer"));
        }
        for (String card : rival) {
            Assert.assertTrue("white and blue: " + card, card.matches("10E:(Plains|Island)|9ED:Savannah Lions|KLD:Wind Drake#70|J25:Raise the Alarm|CMD:Serra Angel"));
        }
        RivalScript script = tuto7.getRival();
        Assert.assertTrue(script.isFree());
        Assert.assertTrue(script.isNoMultiBlock());
    }

    private void dealtAsANormalGame() {
        RivalScript script = deal(1, PhaseStep.DRAW);
        Assert.assertTrue(script.isFree());
        Assert.assertTrue(script.isNoMultiBlock());
        Assert.assertEquals(playerB.getId(), script.getPlayerId());
        Assert.assertEquals(1, currentGame.getTurnNum());
        UUID starts = currentGame.getActivePlayerId();
        Assert.assertTrue(starts.equals(playerA.getId()) || starts.equals(playerB.getId()));
        for (Player player : new Player[]{currentGame.getPlayer(playerA.getId()), currentGame.getPlayer(playerB.getId())}) {
            Assert.assertEquals(20, player.getLife());
            int hand = player.getHand().size();
            Assert.assertTrue(player.getName() + " was dealt a hand: " + hand, hand >= 1 && hand <= 7);
            Assert.assertEquals(player.getName() + ": the 40 between hand and library", 40, hand + player.getLibrary().size());
            Assert.assertEquals(0, currentGame.getBattlefield().getAllActivePermanents(player.getId()).size());
        }
        Player rival = currentGame.getPlayer(playerB.getId());
        Assert.assertFalse("the Angel is never dealt", rival.getHand().getCards(currentGame).stream().anyMatch(c -> c.getName().equals("Serra Angel")));
        List<Card> top = rival.getLibrary().getTopCards(currentGame, 5).stream().collect(java.util.stream.Collectors.toList());
        Card angel = top.stream().filter(c -> c.getName().equals("Serra Angel")).findFirst().orElse(null);
        Assert.assertNotNull("the Angel among his first five: " + top, angel);
        Assert.assertEquals(Zone.LIBRARY, currentGame.getState().getZone(angel.getId()));
        Assert.assertEquals("CMD", angel.getExpansionSetCode());
        // the student's library is shuffled: not the deck's order (9 Mountains first)
        List<Card> studentTop = currentGame.getPlayer(playerA.getId()).getLibrary().getTopCards(currentGame, 9).stream().collect(java.util.stream.Collectors.toList());
        Assert.assertFalse("shuffled", studentTop.stream().allMatch(c -> c.getName().equals("Mountain")));
    }

    @Test
    public void dealtAsANormalGame1() {
        dealtAsANormalGame();
    }

    @Test
    public void dealtAsANormalGame2() {
        dealtAsANormalGame();
    }

    @Test
    public void dealtAsANormalGame3() {
        dealtAsANormalGame();
    }

    @Test
    public void theRivalPlaysFreely() {
        // the student does nothing for six turns: the Rival plays lands and creatures, and attacks
        deal(7, PhaseStep.PRECOMBAT_MAIN);
        Player rival = currentGame.getPlayer(playerB.getId());
        long lands = currentGame.getBattlefield().getAllActivePermanents(rival.getId()).stream().filter(p -> p.isLand(currentGame)).count();
        long creatures = currentGame.getBattlefield().getAllActivePermanents(rival.getId()).stream().filter(p -> p.isCreature(currentGame)).count();
        Assert.assertTrue("lands " + lands, lands >= 2);
        Assert.assertTrue("creatures " + creatures + ", student at " + currentGame.getPlayer(playerA.getId()).getLife(),
                creatures >= 1 || currentGame.getPlayer(playerA.getId()).getLife() < 20);
    }
}

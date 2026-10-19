package org.mage.test.rollback;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.RollbackPoints;
import mage.game.events.TableEvent;
import mage.view.ChatMessage;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestCommander4Players;

import java.util.ArrayList;
import java.util.List;

/**
 * Going back takes back the lines of the log that told of the future it undid.
 * <p>
 * The owner's game, 19 October: his K'rrik (8/8 lifelink) hit TreasureGoblin from 10 to 2; a turn
 * later he went back to "Combat damage", which resumes at the priority before the damage, and the
 * damage was dealt again. The screen's log kept both "loses 8 life" lines and the turn after them,
 * so it no longer matched the board: « Quand on revient en arrière, faut que les logs aussi
 * reviennent en arrière ». Every line of the log is numbered as it is said, a point remembers how
 * many had been said when it was taken, and going back to it takes back the ones after it, the
 * request line included and the line that says the game went back excluded.
 */
public class RollbackLogLinesTest extends CardTestCommander4Players {

    private static RollbackPoints.Point combatDamageOf(Game game, int turn) {
        for (RollbackPoints.Point p : game.getRollbackPoints().list()) {
            if (p.getKind() == RollbackPoints.Kind.STEP && p.getTurn() == turn && p.getStep() == PhaseStep.COMBAT_DAMAGE) {
                return p;
            }
        }
        Assert.fail("no combat damage point in turn " + turn);
        return null;
    }

    @Test
    public void test_BackToCombatDamage_TheLinesSinceItAreTakenBack() {
        // A attacks D with a 3/3, unblocked
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant");
        attack(5, playerA, "Hill Giant", playerD);

        List<Integer> numbers = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        currentGame.addTableEventListener(event -> {
            if (event.getEventType() == TableEvent.EventType.INFO || event.getEventType() == TableEvent.EventType.STATUS) {
                numbers.add(event.getLogLine());
                lines.add(event.getLogLine() + " " + event.getMessage());
            }
        });

        int[] point = new int[2]; // its log line, and the count when the table asked to go back
        runCode("back to combat damage", 5, PhaseStep.POSTCOMBAT_MAIN, playerA, (info, player, game) -> {
            if (point[0] > 0) {
                return;
            }
            Assert.assertEquals("the giant hit D", 17, game.getPlayer(playerD.getId()).getLife());
            RollbackPoints.Point damage = combatDamageOf(game, 5);
            point[0] = damage.getLogLine();
            game.informPlayers("⟲ PlayerA asks to roll back " + damage.getTarget());
            point[1] = game.getLogLinesSaid();
            Assert.assertTrue(game.rollbackToPoint(damage.getId()));
        });

        setStopAt(5, PhaseStep.END_TURN);
        execute();

        // the damage was dealt again after going back, once: the board is that of one hit
        assertLife(playerD, 17);

        // every line is numbered as it is said, one after the other
        for (int i = 0; i < numbers.size(); i++) {
            Assert.assertEquals("line " + i + " of " + lines, i + 1, (int) numbers.get(i));
        }

        List<int[]> taken = currentGame.getRollbackPoints().takenBack();
        Assert.assertEquals("gone back once", 1, taken.size());
        int first = taken.get(0)[0];
        int last = taken.get(0)[1];
        Assert.assertTrue("the point was taken after some lines: " + point[0], point[0] > 0);
        Assert.assertEquals("from the line after the point", point[0] + 1, first);
        Assert.assertEquals("up to the request to go back", point[1], last);

        String said = lines.get(last); // numbers start at 1: the line numbered last + 1
        Assert.assertTrue("the line after them says the game went back: " + said, said.startsWith((last + 1) + " ⟲ Rolled back to Turn 5 PlayerA — Combat damage"));

        // what a screen keeps: the lines outside the run, and the "⟲" ones inside it
        List<String> kept = new ArrayList<>();
        List<String> dropped = new ArrayList<>();
        for (String line : lines) {
            int n = Integer.parseInt(line.substring(0, line.indexOf(' ')));
            String text = line.substring(line.indexOf(' ') + 1);
            if (n >= first && n <= last && !text.startsWith("⟲")) {
                dropped.add(text);
            } else {
                kept.add(text);
            }
        }
        Assert.assertTrue("the gone hit is taken back: " + dropped, dropped.stream().anyMatch(t -> t.contains("loses 3 life")));
        Assert.assertEquals("the hit is told once in what is kept: " + kept, 1, kept.stream().filter(t -> t.contains("loses 3 life")).count());
        Assert.assertTrue("the request stays: " + kept, kept.stream().anyMatch(t -> t.startsWith("⟲ PlayerA asks to roll back")));
        Assert.assertTrue("the attack stays, said before the point: " + kept, kept.stream().anyMatch(t -> t.contains("attacks")));
        Assert.assertFalse("the attack is not taken back: " + dropped, dropped.stream().anyMatch(t -> t.contains("attacks")));
    }

    @Test
    public void test_AChatLineCarriesItsNumber_OnlyALogLineHasOne() {
        Assert.assertEquals(Integer.valueOf(12), new ChatMessage("", "PlayerA draws a card", null, null, ChatMessage.MessageColor.BLACK, ChatMessage.MessageType.GAME, null, 12).getGameLine());
        Assert.assertNull(new ChatMessage("", "joined", null, null, ChatMessage.MessageColor.ORANGE, ChatMessage.MessageType.GAME, null, 0).getGameLine());
        Assert.assertNull(new ChatMessage("Ann", "hello", null, null, ChatMessage.MessageColor.BLUE, ChatMessage.MessageType.TALK, null).getGameLine());
    }
}

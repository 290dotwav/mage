package org.mage.test.mulligan;

import mage.cards.decks.Deck;
import mage.game.Game;
import mage.game.GameOptions;
import mage.game.TwoPlayerDuel;
import mage.game.mulligan.MulliganType;
import mage.players.StubPlayer;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static mage.constants.MultiplayerAttackOption.LEFT;
import static mage.constants.RangeOfInfluence.ONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A game started over (Karn Liberated's -14 calls {@code game.start} again on
 * the same game) asks every player about their hand at once, and the server
 * takes an answer only from the priority player, or from anybody while there
 * is none ({@code GameController.sendMessage}). The old game's priority
 * player was still set: the other players' keeps were dropped and the
 * restarted game waited on them for ever. Seen on a web door.
 */
public class MulliganPriorityTest {

    static class Asked extends StubPlayer {
        final Set<String> priorityWhenAsked;

        Asked(String name, Set<String> seen) {
            super(name, ONE);
            this.priorityWhenAsked = seen;
        }

        @Override
        public boolean chooseMulligan(Game game) {
            UUID priority = game.getState().getPriorityPlayerId();
            priorityWhenAsked.add(priority == null ? "none" : priority.toString());
            return false;
        }
    }

    @Test
    public void nobodyHasPriorityWhileTheHandsAreDecided_evenInAGameStartedOver() {
        Set<String> seen = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Game game = new TwoPlayerDuel(LEFT, ONE, MulliganType.LONDON.getMulligan(0), 60, 20, 7) {
            @Override
            protected void play(UUID nextPlayerId) {
            }
        };
        GameOptions options = new GameOptions();
        options.skipInitShuffling = true;
        game.setGameOptions(options);
        Asked one = new Asked("p1", seen);
        Asked two = new Asked("p2", seen);
        for (Asked player : new Asked[]{one, two}) {
            Deck deck = MulliganTestBase.generateDeck(player.getId(), 40);
            game.loadCards(deck.getCards(), player.getId());
            game.addPlayer(player, deck);
        }
        // What the restarted game inherits from the one it replaces.
        game.getState().setPriorityPlayerId(two.getId());
        game.start(one.getId());
        assertTrue("both players were asked", !seen.isEmpty());
        assertEquals("no priority player while the hands are decided", Collections.singleton("none"), seen);
    }
}

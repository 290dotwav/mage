package mage.game;

import mage.cards.decks.Deck;
import mage.constants.MultiplayerAttackOption;
import mage.constants.RangeOfInfluence;
import mage.constants.TableState;
import mage.game.match.Match;
import mage.game.match.MatchOptions;
import mage.game.mulligan.MulliganType;
import mage.players.Player;
import mage.players.PlayerList;
import mage.players.PlayerType;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.player.TestComputerPlayer7;
import org.mage.test.player.TestPlayer;

import java.util.*;

/**
 * The turn goes round the table in the order of its seats, as at a real table: each player has the
 * same neighbours on his left and right as in the room before the start, at every game of the match,
 * and only who starts is drawn. Upstream shuffled the players at the start of each game
 * (MatchImpl.shufflePlayers), so the order chosen in the room was never the order of the turn.
 * The owner, 15 November 2026: « la table ressemble pas à la vraie table on doit pouvoir changer
 * les joueurs de place ».
 */
public class SeatOrderTest {

    private static final int GAMES = 20;

    private Match match;
    private Table table;
    private final Map<UUID, String> names = new HashMap<>();

    private void open(String gameType, PlayerType... seats) {
        MatchOptions options = new MatchOptions("seats", gameType, seats.length > 2);
        options.getPlayerTypes().addAll(Arrays.asList(seats));
        options.setAttackOption(MultiplayerAttackOption.MULTIPLE);
        options.setRange(RangeOfInfluence.ALL);
        options.setWinsNeeded(GAMES);
        options.setMullgianType(MulliganType.LONDON);
        match = seats.length > 2 ? new FreeForAllMatch(options) : new TwoPlayerMatch(options);
        table = new Table(UUID.randomUUID(), gameType, "seats", "Ana", null, Arrays.asList(seats), null, match, new HashSet<>(), false);
    }

    /** Joins as a door join does (TableController.joinTable): the first free seat of his kind. */
    private void join(String name, PlayerType type) throws GameException {
        Player player = new TestPlayer(new TestComputerPlayer7(name, RangeOfInfluence.ALL, 1));
        names.put(player.getId(), name);
        match.addPlayer(player, new Deck());
        table.joinTable(player, table.getNextAvailableSeat(type));
    }

    private List<String> seats() {
        List<String> seated = new ArrayList<>();
        for (Seat seat : table.getSeats()) {
            seated.add(seat.getPlayer() == null ? null : seat.getPlayer().getName());
        }
        return seated;
    }

    /** One more game of the match, started as TableController.startGame starts it. */
    private Game startGame() throws GameException {
        match.seatPlayers(table.seatedPlayerIds());
        match.startGame();
        return match.getGame();
    }

    /** The players in the order the turn passes, from {@code first}: the list GameImpl.play walks. */
    private List<String> turns(Game game, UUID first) {
        PlayerList list = game.getState().getPlayerList(first);
        List<String> order = new ArrayList<>();
        order.add(names.get(list.get()));
        for (int i = 1; i < list.size(); i++) {
            order.add(names.get(list.getNext()));
        }
        return order;
    }

    /** Whoever starts, the turn goes from each seat to the next one. */
    private void assertTurnsGoRoundTheSeats(List<String> seated) throws GameException {
        for (int g = 0; g < GAMES; g++) {
            Game game = startGame();
            Assert.assertEquals(seated.size(), game.getPlayers().size());
            for (int s = 0; s < seated.size(); s++) {
                List<String> rotation = new ArrayList<>(seated.subList(s, seated.size()));
                rotation.addAll(seated.subList(0, s));
                UUID first = null;
                for (Map.Entry<UUID, String> e : names.entrySet()) {
                    if (e.getValue().equals(seated.get(s))) {
                        first = e.getKey();
                    }
                }
                Assert.assertEquals("game " + (g + 1) + ", " + seated.get(s) + " first", rotation, turns(game, first));
            }
        }
    }

    @Test
    public void fourSeatsTheTurnGoesSeatToSeatAtEveryGame() throws GameException {
        open("Free For All", PlayerType.HUMAN, PlayerType.COMPUTER_MAD, PlayerType.HUMAN, PlayerType.COMPUTER_MAD);
        // the bots sit down first, the people after, each on the first free seat of his kind
        join("Bot1", PlayerType.COMPUTER_MAD);
        join("Bot2", PlayerType.COMPUTER_MAD);
        join("Ana", PlayerType.HUMAN);
        join("Ben", PlayerType.HUMAN);
        Assert.assertEquals(Arrays.asList("Ana", "Bot1", "Ben", "Bot2"), seats());
        assertTurnsGoRoundTheSeats(seats());
    }

    @Test
    public void twoSeatsSwappedBeforeTheStartAreTheNewOrder() throws GameException {
        open("Free For All", PlayerType.HUMAN, PlayerType.HUMAN, PlayerType.COMPUTER_MAD, PlayerType.COMPUTER_MAD);
        join("Ana", PlayerType.HUMAN);
        join("Ben", PlayerType.HUMAN);
        join("Bot1", PlayerType.COMPUTER_MAD);
        join("Bot2", PlayerType.COMPUTER_MAD);
        Assert.assertEquals(TableState.READY_TO_START, table.getState());
        Assert.assertTrue(table.swapSeats(1, 2));
        Assert.assertEquals(Arrays.asList("Ana", "Bot1", "Ben", "Bot2"), seats());
        Assert.assertEquals("the seat keeps the kind of player it is for", PlayerType.COMPUTER_MAD, table.getSeats()[1].getPlayerType());
        assertTurnsGoRoundTheSeats(seats());
    }

    @Test
    public void aSeatSwappedWhileTheTableWaitsMovesWithItsEmptyChair() throws GameException {
        open("Free For All", PlayerType.HUMAN, PlayerType.HUMAN, PlayerType.COMPUTER_MAD, PlayerType.COMPUTER_MAD);
        join("Ana", PlayerType.HUMAN);
        join("Bot1", PlayerType.COMPUTER_MAD);
        join("Bot2", PlayerType.COMPUTER_MAD);
        Assert.assertEquals(TableState.WAITING, table.getState());
        Assert.assertEquals(Arrays.asList("Ana", null, "Bot1", "Bot2"), seats());
        // the friend's empty chair goes between the two bots; he sits there when he comes
        Assert.assertTrue(table.swapSeats(1, 2));
        Assert.assertEquals(Arrays.asList("Ana", "Bot1", null, "Bot2"), seats());
        join("Ben", PlayerType.HUMAN);
        Assert.assertEquals(Arrays.asList("Ana", "Bot1", "Ben", "Bot2"), seats());
        Assert.assertEquals(TableState.READY_TO_START, table.getState());
        assertTurnsGoRoundTheSeats(seats());
    }

    @Test
    public void noSwapOnceTheMatchHasStarted() throws GameException {
        open("Free For All", PlayerType.HUMAN, PlayerType.COMPUTER_MAD, PlayerType.COMPUTER_MAD);
        join("Ana", PlayerType.HUMAN);
        join("Bot1", PlayerType.COMPUTER_MAD);
        join("Bot2", PlayerType.COMPUTER_MAD);
        table.initGame();
        Assert.assertFalse(table.swapSeats(0, 1));
        Assert.assertFalse("no seat outside the table", new Table(UUID.randomUUID(), "Free For All", "t", "Ana", null,
                Arrays.asList(PlayerType.HUMAN, PlayerType.HUMAN), null, new FreeForAllMatch(new MatchOptions("t", "Free For All", true)), new HashSet<>(), false).swapSeats(0, 2));
        Assert.assertEquals(Arrays.asList("Ana", "Bot1", "Bot2"), seats());
    }

    @Test
    public void twoSeatsPlayInTurnAsBefore() throws GameException {
        open("Two Player Duel", PlayerType.HUMAN, PlayerType.COMPUTER_MAD);
        join("Bot1", PlayerType.COMPUTER_MAD);
        join("Ana", PlayerType.HUMAN);
        Assert.assertEquals(Arrays.asList("Ana", "Bot1"), seats());
        assertTurnsGoRoundTheSeats(seats());
    }
}

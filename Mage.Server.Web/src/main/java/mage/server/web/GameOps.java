package mage.server.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mage.game.Game;
import mage.game.GameHold;
import mage.view.ChatMessage.MessageColor;
import mage.view.ChatMessage.MessageType;
import mage.server.Session;
import mage.server.User;
import mage.server.game.GameController;
import mage.server.managers.ManagerFactory;
import org.apache.log4j.Logger;

import java.util.UUID;

/**
 * "game" frames: what a seated player asks of the game he is in, beyond answering its questions.
 * <pre>
 * { kind: "game", op: "pause",  gameId, seconds?: 120 }
 * { kind: "game", op: "resume", gameId }
 * { kind: "game", op: "status", gameId }
 * </pre>
 * <b>Pause</b> is "hold on, I am reading that card": while it is held, no other player acts -
 * no bot plays, no spell resolves, no step passes - and the seat that took the hold may still
 * act. Everybody is told through the game's own chat ("&#9208; F-G paused the game"), which is
 * the line the browsers already read.
 * <p>
 * The guards, in order: the game must be running; the asker must hold a <b>seat at that game</b>
 * (a watcher cannot stop a table); a hold lapses by itself after at most
 * {@link GameHold#MAX_MILLIS} (two minutes), so a browser that vanished mid-read cannot freeze
 * a table for good; and <b>anybody at the table</b> may lift it, not only the seat that took it.
 * <p>
 * What holds the game is {@link GameHold} and not their {@code Game.pause()}: their pause makes
 * every game loop <i>return</i>, which {@code GameWorker} takes for the end of the game (it
 * calls {@code endGameWithResult} at once), and their {@code resume()} would run the rest of the
 * match on this door's thread. See the note on {@code GameHold} for the whole reading.
 */
final class GameOps {

    private static final Logger logger = Logger.getLogger(GameOps.class);

    /** How long a hold lasts when the frame does not say. */
    static final int DEFAULT_SECONDS = 60;

    private final ManagerFactory managerFactory;

    GameOps(ManagerFactory managerFactory) {
        this.managerFactory = managerFactory;
    }

    void handle(WebSession web, JsonObject frame, JsonElement id) throws Exception {
        String op = Frames.requireString(frame, "op");
        UUID gameId = UUID.fromString(Frames.requireString(frame, "gameId"));
        Seat seat = seatOf(web, gameId);
        switch (op) {
            case "pause": {
                int seconds = Frames.optInt(frame, "seconds", DEFAULT_SECONDS);
                if (seconds < 1) {
                    throw new IllegalArgumentException("seconds must be 1.." + (GameHold.MAX_MILLIS / 1000));
                }
                GameHold.Hold hold = GameHold.pause(gameId, seat.playerId, seat.playerName, seconds * 1000L);
                say(seat, gameId, seat.playerName + " paused the game");
                logger.info("Web door: " + seat.playerName + " paused game " + gameId + " for " + (hold.getMillisLeft() / 1000) + " s");
                web.send(Frames.result("game.pause", state(gameId), id));
                break;
            }
            case "resume": {
                GameHold.Hold hold = GameHold.resume(gameId);
                // Nothing held: the answer is still the truth about the game, and no line is
                // said - a release that releases nothing is not news for the other seats.
                if (hold != null) {
                    say(seat, gameId, seat.playerName + " resumed the game");
                    logger.info("Web door: " + seat.playerName + " resumed game " + gameId + " (held by " + hold.getPlayerName() + ")");
                }
                web.send(Frames.result("game.resume", state(gameId), id));
                break;
            }
            case "status":
                web.send(Frames.result("game.status", state(gameId), id));
                break;
            default:
                throw new IllegalArgumentException("unknown game op '" + op + "' (pause, resume, status)");
        }
    }

    /** { paused, by, playerId, millisLeft } - what every answer says about the game now. */
    private JsonObject state(UUID gameId) {
        GameHold.Hold hold = GameHold.current(gameId);
        JsonObject o = new JsonObject();
        String by = hold == null ? null : hold.getPlayerName();
        String playerId = hold == null || hold.getPlayerId() == null ? null : hold.getPlayerId().toString();
        o.addProperty("paused", hold != null);
        o.addProperty("by", by);
        o.addProperty("playerId", playerId);
        o.addProperty("millisLeft", hold == null ? 0L : hold.getMillisLeft());
        return o;
    }

    /** The seat this socket holds at that game, or an exception naming why it holds none. */
    private Seat seatOf(WebSession web, UUID gameId) {
        Game game = LiveGame.of(gameId, managerFactory);
        if (game == null) {
            throw new IllegalStateException("no running game " + gameId);
        }
        GameController controller = managerFactory.gameManager().getGameController().get(gameId);
        if (controller == null) {
            throw new IllegalStateException("no controller for game " + gameId);
        }
        Session session = managerFactory.sessionManager().getSession(web.sessionId).orElse(null);
        if (session == null || session.getUserId() == null) {
            throw new IllegalStateException("this socket has no user: connect (table create/join) first");
        }
        UUID playerId = controller.getPlayerId(session.getUserId());
        if (playerId == null) {
            throw new IllegalStateException("you do not hold a seat at game " + gameId + " (a watcher cannot stop a table)");
        }
        User user = managerFactory.userManager().getUser(session.getUserId()).orElse(null);
        String name = game.getPlayer(playerId) != null ? game.getPlayer(playerId).getName() : (user == null ? "A player" : user.getName());
        return new Seat(game, playerId, name);
    }

    /** One line in the game's own chat, to every seat and every watcher. */
    private void say(Seat seat, UUID gameId, String message) {
        GameController controller = managerFactory.gameManager().getGameController().get(gameId);
        if (controller == null) {
            return;
        }
        managerFactory.chatManager().broadcast(controller.getChatId(), "", "⏸ " + message,
                MessageColor.BLUE, false, seat.game, MessageType.STATUS, null);
    }

    private static final class Seat {

        private final Game game;
        private final UUID playerId;
        private final String playerName;

        Seat(Game game, UUID playerId, String playerName) {
            this.game = game;
            this.playerId = playerId;
            this.playerName = playerName;
        }
    }
}

package mage.server.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mage.game.Game;
import mage.game.RollbackPoints;
import mage.server.game.GameController;
import mage.server.game.RollbackVote;
import mage.server.managers.ManagerFactory;

import java.util.Locale;
import java.util.UUID;

/**
 * Rollback points and the vote on one, as the interface reads them.
 * <p>
 * Every GameView the door forwards gets the points the table may go back to (oldest first),
 * and the vote under way when there is one:
 * <pre>
 *   "rollbackPoints": [ { "id": 17, "kind": "cast", "turn": 5, "label": "Bob casts Cyclonic Rift",
 *                         "player": "Bob", "playerId": "…", "step": "PRECOMBAT_MAIN",
 *                         "card": "Cyclonic Rift", "at": 1759600000000 }, … ]
 *   "rollbackVote":   { …see {@link #vote}… }
 * </pre>
 * {@code kind} is {@code step}, {@code cast}, {@code land} or {@code turn} (the start of a turn
 * older than the finer points). A point is asked for with
 * {@code sendPlayerAction("ROLLBACK_TO_POINT", id)}, and answered with their own
 * {@code ADD_PERMISSION_TO_ROLLBACK_TURN} / {@code DENY_PERMISSION_TO_ROLLBACK_TURN}.
 * <p>
 * A vote that changes is also pushed at once, on its own, to every socket of a seat or a
 * watcher of that game (a vote does not move the game, so no view would carry it):
 * <pre>
 *   { "kind": "rollbackVote", "gameId": "…", "vote": { … } }
 * </pre>
 * The field is left out of a game created without rollbacks (their table option), so a door
 * without this feature and a table without it look the same to the interface.
 */
final class Rollbacks {

    private Rollbacks() {
    }

    static void enrich(JsonObject frame, UUID gameId, ManagerFactory managerFactory) {
        JsonObject view = Frames.gameView(frame);
        if (view == null) {
            return;
        }
        try {
            Game game = LiveGame.of(gameId, managerFactory);
            if (game == null || !game.getOptions().rollbackTurnsAllowed || game.getRollbackPoints() == null) {
                return;
            }
            view.add("rollbackPoints", points(game.getRollbackPoints()));
            GameController controller = managerFactory.gameManager().getGameController().get(gameId);
            RollbackVote vote = controller == null ? null : controller.getRollbackVote();
            if (vote != null) {
                view.add("rollbackVote", vote(vote));
            }
        } catch (RuntimeException ignore) {
            // the game changed under us: leave the frame as their view made it
        }
    }

    static JsonArray points(RollbackPoints points) {
        JsonArray out = new JsonArray();
        for (RollbackPoints.Point p : points.list()) {
            out.add(point(p));
        }
        return out;
    }

    static JsonObject point(RollbackPoints.Point p) {
        JsonObject o = new JsonObject();
        o.addProperty("id", p.getId());
        o.addProperty("kind", p.getKind().name().toLowerCase(Locale.ROOT));
        o.addProperty("turn", p.getTurn());
        o.addProperty("label", p.getLabel());
        o.addProperty("player", p.getPlayerName());
        o.addProperty("playerId", p.getPlayerId() == null ? null : p.getPlayerId().toString());
        o.addProperty("step", p.getStep() == null ? null : p.getStep().name());
        o.addProperty("card", p.getCardName());
        o.addProperty("at", p.getCreatedAt());
        return o;
    }

    /**
     * <pre>
     * { "id": 3, "pointId": 17, "point": { …as in rollbackPoints… }, "kind": "cast", "turn": 5, "label": "Bob casts Cyclonic Rift",
     *   "target": "before Bob casts Cyclonic Rift", "requester": "Bob", "requesterId": "…",
     *   "deadline": 1759600030000, "millisLeft": 29000,
     *   "seats": [ { "playerId": "…", "name": "Ann", "human": true, "out": false, "answer": "pending" }, … ],
     *   (a seat "out" of the game, lost, conceded or left, does not vote: its answer is "yes")
     *   "outcome": null | "accepted" | "refused" | "timeout" | "failed", "refusedBy": null | "Ann" }
     * </pre>
     */
    static JsonObject vote(RollbackVote v) {
        JsonObject o = new JsonObject();
        o.addProperty("id", v.getId());
        o.addProperty("pointId", v.getPointId());
        o.add("point", point(v.getPoint()));
        o.addProperty("kind", v.getKind().name().toLowerCase(Locale.ROOT));
        o.addProperty("turn", v.getTurn());
        o.addProperty("label", v.getLabel());
        o.addProperty("target", v.getTarget());
        o.addProperty("requester", v.getRequesterName());
        o.addProperty("requesterId", v.getRequesterId().toString());
        o.addProperty("deadline", v.getDeadline());
        o.addProperty("millisLeft", Math.max(0L, v.getDeadline() - System.currentTimeMillis()));
        JsonArray seats = new JsonArray();
        for (RollbackVote.Seat seat : v.getSeats()) {
            JsonObject s = new JsonObject();
            s.addProperty("playerId", seat.getPlayerId().toString());
            s.addProperty("name", seat.getName());
            s.addProperty("human", seat.isHuman());
            s.addProperty("out", seat.isOut());
            s.addProperty("answer", seat.getAnswer().name().toLowerCase(Locale.ROOT));
            seats.add(s);
        }
        o.add("seats", seats);
        RollbackVote.Outcome outcome = v.getOutcome();
        o.addProperty("outcome", outcome == null ? null : outcome.name().toLowerCase(Locale.ROOT));
        o.addProperty("refusedBy", v.getRefusedBy());
        return o;
    }

    /** The frame pushed when a vote changes. */
    static String voteFrame(UUID gameId, RollbackVote v) {
        JsonObject o = new JsonObject();
        o.addProperty("kind", "rollbackVote");
        o.addProperty("gameId", gameId.toString());
        o.add("vote", vote(v));
        return Frames.GSON.toJson(o);
    }
}

package mage.server.game;

import mage.constants.PlayerAction;
import mage.game.Game;
import mage.game.RollbackPoints;
import mage.interfaces.callback.ClientCallback;
import mage.interfaces.callback.ClientCallbackMethod;
import mage.players.Player;
import mage.server.User;
import mage.view.UserRequestMessage;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * {@code ROLLBACK_TO_POINT} for one game: a seat asks to go back to a rollback point, the human
 * seats vote ({@link RollbackVote}), and the game goes back when they all agree.
 * <p>
 * Unlike their {@code ROLLBACK_TURNS}, any seat may ask at any moment, not only with priority:
 * the game is not stopped by the request, and going back unwinds whatever it is doing
 * ({@code Game.rollbackToPoint}). The seats asked get their usual request dialog, with the buttons
 * of their rollback request ({@code ADD_PERMISSION_TO_ROLLBACK_TURN} /
 * {@code DENY_PERMISSION_TO_ROLLBACK_TURN}); every change is told to the {@link RollbackVote}
 * listener and said in the game log.
 * <p>
 * A seat out of the game (lost, conceded, left) does not vote: its answer is yes, from the start
 * or from the moment it goes out ({@link #recheck}, on every update of the game and every second
 * while a vote is open).
 */
final class RollbackRequests {

    private static final Logger logger = Logger.getLogger(RollbackRequests.class);

    private final Game game;
    private final Function<UUID, UUID> playerOfUser;
    private final Function<UUID, Optional<User>> userOfPlayer;
    private final Predicate<Player> human;
    private final Predicate<UUID> somebodyAt;
    private final Supplier<Set<UUID>> audience;
    private final ScheduledExecutorService timer;

    private RollbackVote vote;
    private ScheduledFuture<?> timeout;
    private ScheduledFuture<?> watch;

    RollbackRequests(Game game, Function<UUID, UUID> playerOfUser, Function<UUID, Optional<User>> userOfPlayer,
                     Supplier<Set<UUID>> audience, ScheduledExecutorService timer) {
        this(game, playerOfUser, userOfPlayer, Player::isHuman, id -> userOfPlayer.apply(id).isPresent(), audience, timer);
    }

    /**
     * {@code human}: a seat a person plays; {@code somebodyAt}: a person is still behind that seat
     * (a human seat nobody is behind has left the table: it does not vote).
     */
    RollbackRequests(Game game, Function<UUID, UUID> playerOfUser, Function<UUID, Optional<User>> userOfPlayer,
                     Predicate<Player> human, Predicate<UUID> somebodyAt,
                     Supplier<Set<UUID>> audience, ScheduledExecutorService timer) {
        this.game = game;
        this.playerOfUser = playerOfUser;
        this.userOfPlayer = userOfPlayer;
        this.human = human;
        this.somebodyAt = somebodyAt;
        this.audience = audience;
        this.timer = timer;
    }

    /** The vote under way, or null. */
    synchronized RollbackVote current() {
        return vote != null && vote.isOpen() ? vote : null;
    }

    /** ROLLBACK_TO_POINT: data is the point id (a number, or its text). */
    void request(UUID userId, Object data) {
        UUID playerId = playerOfUser.apply(userId);
        Player player = playerId == null ? null : game.getPlayer(playerId);
        if (player == null || !player.isInGame()) {
            return;
        }
        Integer pointId = pointId(data);
        RollbackPoints points = game.getRollbackPoints();
        RollbackPoints.Point point = pointId == null || points == null ? null : points.get(pointId);
        if (point == null || game.executingRollback() || game.hasEnded()) {
            game.informPlayer(player, "That rollback point is no longer available.");
            return;
        }
        RollbackVote opened;
        synchronized (this) {
            if (current() != null) {
                game.informPlayer(player, "A rollback vote is already open.");
                return;
            }
            List<RollbackVote.Seat> seats = new ArrayList<>();
            for (Player p : game.getState().getPlayers().values()) {
                boolean person = human.test(p);
                // out of the game, or a human seat with nobody behind it (left the table): no vote, yes
                boolean out = !p.isInGame() || (person && !somebodyAt.test(p.getId()));
                seats.add(new RollbackVote.Seat(p.getId(), p.getName(), person, out));
            }
            opened = new RollbackVote(point, playerId, player.getName(), seats, System.currentTimeMillis());
            vote = opened;
        }
        game.informPlayers("⟲ " + player.getName() + " asks to roll back " + point.getTarget());
        if (opened.settleIfUnanimous()) {
            settle(opened);
            return;
        }
        for (RollbackVote.Seat seat : opened.getSeats()) {
            if (seat.getAnswer() == RollbackVote.Answer.PENDING) {
                ask(seat.getPlayerId(), userId, player.getName(), point);
            }
        }
        synchronized (this) {
            if (vote != opened || !opened.isOpen()) {
                return; // answered already, before the clocks were set
            }
            timeout = timer.schedule(() -> timedOut(opened), RollbackVote.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            watch = timer.scheduleWithFixedDelay(this::recheck, 1, 1, TimeUnit.SECONDS);
        }
        publish(opened);
        recheck(); // somebody may have gone out while the seats were asked
    }

    /** The game changed: a seat may have gone out. Rechecked off the game's thread. */
    void gameChanged() {
        if (current() != null) {
            timer.execute(this::recheck);
        }
    }

    /**
     * Each seat still pending whose player is out of the game now (lost, conceded, left) says
     * yes; the vote is accepted when that leaves nobody to answer. Cheap with no vote open.
     */
    void recheck() {
        RollbackVote v = current();
        if (v == null) {
            return;
        }
        boolean changed = false;
        for (RollbackVote.Seat seat : v.getSeats()) {
            if (seat.isOut()) {
                continue;
            }
            Player p = game.getPlayer(seat.getPlayerId());
            if (p == null || !p.isInGame()) {
                changed |= v.out(seat.getPlayerId());
            }
        }
        if (!changed) {
            return;
        }
        if (v.isOpen()) {
            publish(v);
        } else {
            settle(v);
        }
    }

    /**
     * ADD / DENY_PERMISSION_TO_ROLLBACK_TURN from a seat this vote is waiting on; false when it is
     * not (their own turn rollback request then takes it).
     */
    boolean answer(UUID userId, boolean yes) {
        UUID playerId = playerOfUser.apply(userId);
        RollbackVote v = current();
        if (v == null || playerId == null || !v.answer(playerId, yes)) {
            return false;
        }
        if (v.isOpen()) {
            publish(v);
        } else {
            settle(v);
        }
        return true;
    }

    private void timedOut(RollbackVote v) {
        if (v.timeOut()) {
            settle(v);
        }
    }

    /** The vote is over: go back, or say who refused; then tell everybody and forget it. */
    private void settle(RollbackVote v) {
        if (!v.claimSettle()) {
            return; // another thread closed it and acts on it
        }
        synchronized (this) {
            if (timeout != null) {
                timeout.cancel(false);
                timeout = null;
            }
            if (watch != null) {
                watch.cancel(false);
                watch = null;
            }
        }
        switch (v.getOutcome()) {
            case ACCEPTED:
                if (!game.rollbackToPoint(v.getPointId())) {
                    v.failed();
                    game.informPlayers("⟲ The rollback could not be done: that point is no longer available");
                }
                break;
            case REFUSED:
                game.informPlayers("⟲ " + v.getRefusedBy() + " refused the rollback");
                break;
            case TIMEOUT:
                game.informPlayers("⟲ No rollback: " + v.getRefusedBy() + " did not answer");
                break;
            default:
                break;
        }
        publish(v);
        synchronized (this) {
            if (vote == v) {
                vote = null;
            }
        }
    }

    /** Their request dialog, with their rollback buttons, to one seat asked. */
    private void ask(UUID playerId, UUID requesterUserId, String requesterName, RollbackPoints.Point point) {
        Optional<User> user = userOfPlayer.apply(playerId);
        if (!user.isPresent()) {
            return;
        }
        UserRequestMessage message = new UserRequestMessage("Request by " + requesterName,
                "Allow rollback " + point.getTarget() + "?");
        message.setRelatedUser(requesterUserId, requesterName);
        message.setGameId(game.getId());
        message.setButton1("Accept", PlayerAction.ADD_PERMISSION_TO_ROLLBACK_TURN);
        message.setButton2("Deny", PlayerAction.DENY_PERMISSION_TO_ROLLBACK_TURN);
        user.get().fireCallback(new ClientCallback(ClientCallbackMethod.USER_REQUEST_DIALOG, game.getId(), message));
    }

    private void publish(RollbackVote v) {
        try {
            RollbackVote.publish(game.getId(), v, audience.get());
        } catch (RuntimeException ex) {
            logger.warn("rollback vote of game " + game.getId() + " not told: " + ex);
        }
    }

    static Integer pointId(Object data) {
        if (data instanceof Number) {
            return ((Number) data).intValue();
        }
        if (data != null) {
            try {
                return Integer.parseInt(data.toString().trim());
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }
}

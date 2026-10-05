package mage.server.game;

import mage.game.RollbackPoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One request to go back to a rollback point ({@link RollbackPoints}), and the table's answer.
 * <p>
 * Only the human seats still in the game vote. A seat played by their AI says yes by itself, and
 * so does the seat that asked, and so does a seat out of the game (lost, conceded, left): « Les
 * joueurs morts ne peuvent pas voter, c'est oui d'office ». A seat that goes out while the vote is
 * open says yes then ({@link #out}), and the vote is accepted if nobody else is left to answer.
 * One no ends it, and a seat that has not answered when {@link #TIMEOUT_MILLIS} run out says no.
 * Every answer is kept per seat, so a table can show who has answered what.
 * <p>
 * The decision is here and nothing else: the {@link GameController} opens a vote, feeds it the
 * answers ({@code ADD_PERMISSION_TO_ROLLBACK_TURN} / {@code DENY_PERMISSION_TO_ROLLBACK_TURN},
 * the buttons their own rollback request already has) and goes back when it is accepted.
 */
public final class RollbackVote {

    public enum Answer {
        YES, NO, PENDING
    }

    public enum Outcome {
        ACCEPTED, REFUSED, TIMEOUT, FAILED
    }

    /** How long a seat has to answer. */
    public static final long TIMEOUT_MILLIS = 30_000L;

    /** Told of every change of every vote (the web door pushes it to the seats and watchers). */
    public interface Listener {
        void changed(UUID gameId, RollbackVote vote, Set<UUID> userIds);
    }

    private static volatile Listener listener;

    public static void setListener(Listener l) {
        listener = l;
    }

    static void publish(UUID gameId, RollbackVote vote, Set<UUID> userIds) {
        Listener l = listener;
        if (l != null && vote != null) {
            try {
                l.changed(gameId, vote, userIds);
            } catch (RuntimeException ignore) {
                // a listener that fails must not stop the vote
            }
        }
    }

    /** One seat of the game and its answer. */
    public static final class Seat {

        private final UUID playerId;
        private final String name;
        private final boolean human;
        private volatile boolean out;
        private volatile Answer answer;

        public Seat(UUID playerId, String name, boolean human) {
            this(playerId, name, human, false);
        }

        /** {@code out}: the seat is out of the game (lost, conceded, left) and does not vote. */
        public Seat(UUID playerId, String name, boolean human, boolean out) {
            this.playerId = playerId;
            this.name = name;
            this.human = human;
            this.out = out;
            this.answer = Answer.PENDING;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public String getName() {
            return name;
        }

        public boolean isHuman() {
            return human;
        }

        public Answer getAnswer() {
            return answer;
        }

        /** Out of the game: it does not vote, its answer is yes. */
        public boolean isOut() {
            return out;
        }
    }

    private static final AtomicInteger SEQ = new AtomicInteger();

    private final int id;
    private final RollbackPoints.Point point;
    private final int pointId;
    private final RollbackPoints.Kind kind;
    private final int turn;
    private final String label;
    private final String target;
    private final UUID requesterId;
    private final String requesterName;
    private final List<Seat> seats;
    private final long deadline;
    private Outcome outcome;
    private String refusedBy;
    private boolean settled;

    /**
     * Opens a vote: the asker, every seat that is not human and every seat out of the game say yes
     * at once, the others are pending until they answer or the time runs out.
     */
    public RollbackVote(RollbackPoints.Point point, UUID requesterId, String requesterName, List<Seat> seats, long now) {
        this.id = SEQ.incrementAndGet();
        this.point = point;
        this.pointId = point.getId();
        this.kind = point.getKind();
        this.turn = point.getTurn();
        this.label = point.getLabel();
        this.target = point.getTarget();
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.seats = new ArrayList<>(seats);
        this.deadline = now + TIMEOUT_MILLIS;
        for (Seat seat : this.seats) {
            if (!seat.human || seat.out || seat.playerId.equals(requesterId)) {
                seat.answer = Answer.YES;
            }
        }
    }

    /** True when {@code playerId} still has to answer. */
    public synchronized boolean isPending(UUID playerId) {
        Seat seat = seat(playerId);
        return outcome == null && seat != null && seat.answer == Answer.PENDING;
    }

    /**
     * A seat answers; false when the vote is over or that seat has nothing to answer. A no closes
     * the vote as refused; the last yes closes it as accepted.
     */
    public synchronized boolean answer(UUID playerId, boolean yes) {
        if (!isPending(playerId)) {
            return false;
        }
        Seat seat = seat(playerId);
        seat.answer = yes ? Answer.YES : Answer.NO;
        if (!yes) {
            outcome = Outcome.REFUSED;
            refusedBy = seat.name;
        } else if (allYes()) {
            outcome = Outcome.ACCEPTED;
        }
        return true;
    }

    /**
     * A seat went out of the game while the vote is open (lost, conceded, left): it does not vote
     * any more, and a pending answer of its becomes yes; the last one pending closes the vote as
     * accepted. False when the vote is over, the seat is unknown or was already out.
     */
    public synchronized boolean out(UUID playerId) {
        Seat seat = seat(playerId);
        if (outcome != null || seat == null || seat.out) {
            return false;
        }
        seat.out = true;
        if (seat.answer == Answer.PENDING) {
            seat.answer = Answer.YES;
        }
        if (allYes()) {
            outcome = Outcome.ACCEPTED;
        }
        return true;
    }

    /**
     * True once only, for the one caller that acts on the outcome (an answer, a seat going out
     * and the time running out can close the vote on different threads).
     */
    public synchronized boolean claimSettle() {
        if (outcome == null || settled) {
            return false;
        }
        settled = true;
        return true;
    }

    /** Nobody voted against and nobody is left to answer (no human but the asker: accepted at once). */
    public synchronized boolean settleIfUnanimous() {
        if (outcome == null && allYes()) {
            outcome = Outcome.ACCEPTED;
            return true;
        }
        return false;
    }

    /** The time ran out: every seat still pending says no. False when the vote was already over. */
    public synchronized boolean timeOut() {
        if (outcome != null) {
            return false;
        }
        List<String> silent = new ArrayList<>();
        for (Seat seat : seats) {
            if (seat.answer == Answer.PENDING) {
                seat.answer = Answer.NO;
                silent.add(seat.name);
            }
        }
        outcome = Outcome.TIMEOUT;
        refusedBy = String.join(", ", silent);
        return true;
    }

    /** Accepted, but the point could not be gone back to (pruned, game over). */
    public synchronized void failed() {
        outcome = Outcome.FAILED;
    }

    private boolean allYes() {
        for (Seat seat : seats) {
            if (seat.answer != Answer.YES) {
                return false;
            }
        }
        return true;
    }

    private Seat seat(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        for (Seat seat : seats) {
            if (seat.playerId.equals(playerId)) {
                return seat;
            }
        }
        return null;
    }

    public int getId() {
        return id;
    }

    public int getPointId() {
        return pointId;
    }

    /** The point asked for (its kind, turn, player, step and card). */
    public RollbackPoints.Point getPoint() {
        return point;
    }

    public RollbackPoints.Kind getKind() {
        return kind;
    }

    public int getTurn() {
        return turn;
    }

    /** "Bob casts Cyclonic Rift", "Turn 5 Bob — Upkeep". */
    public String getLabel() {
        return label;
    }

    /** "before Bob casts Cyclonic Rift", "to Turn 5 Bob — Upkeep". */
    public String getTarget() {
        return target;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public long getDeadline() {
        return deadline;
    }

    public synchronized List<Seat> getSeats() {
        return Collections.unmodifiableList(new ArrayList<>(seats));
    }

    public synchronized Answer answerOf(UUID playerId) {
        Seat seat = seat(playerId);
        return seat == null ? null : seat.answer;
    }

    /** Null while the vote is open. */
    public synchronized Outcome getOutcome() {
        return outcome;
    }

    public synchronized boolean isOpen() {
        return outcome == null;
    }

    /** Who said no ("Ann"), or who did not answer in time ("Ann, Greg"); null otherwise. */
    public synchronized String getRefusedBy() {
        return refusedBy;
    }
}

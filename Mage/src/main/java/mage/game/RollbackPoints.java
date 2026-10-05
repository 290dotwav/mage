package mage.game;

import mage.cards.Card;
import mage.constants.PhaseStep;
import mage.game.turn.Phase;
import mage.game.turn.Step;
import mage.game.turn.Turn;
import mage.players.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Rollback points: moments of the game a table can go back to, finer than a whole turn.
 * <p>
 * Their engine keeps one copy of the {@link GameState} per turn start ({@code gameStatesRollBack},
 * up to four turns) and goes back by unwinding every game loop to {@code GameImpl.playTurn} and
 * playing that turn again. This keeps more copies, taken at three kinds of moments:
 * <ul>
 * <li>{@link Kind#STEP} - the start of each step of each turn, before its turn-based actions
 *     (untap, draw, the attack declaration);</li>
 * <li>{@link Kind#CAST} - just before a spell is cast from a priority (a card in hand, its mana not
 *     paid yet); not a trigger, not an activated ability, not a mana ability, and not a spell
 *     cast while something resolves (cascade);</li>
 * <li>{@link Kind#LAND} - just before a land is played.</li>
 * </ul>
 * A cast or a land that does not happen (cancelled, refused) leaves no point. Points are kept for
 * the current turn and one round of the table before it (as many turns as there are players in
 * the game), {@link #MAX_POINTS} at the most; older turns are still reachable by their own
 * turn-start copies, listed as {@link Kind#TURN}.
 * <p>
 * Going back to a point unwinds the loops the same way their turn rollback does, restores the
 * copy, and resumes the turn where it stood ({@code Turn.resumePlay}) - at the start of the step,
 * or at the priority of the player who then cast or played. Simulated games (the AIs' copies)
 * and the playable checks never take a point.
 */
public final class RollbackPoints {

    public enum Kind {
        STEP, CAST, LAND, TURN
    }

    /** At most this many points with a copy of the game kept, oldest dropped first. */
    public static final int MAX_POINTS = 120;

    /** One point; {@code state} is null for a {@link Kind#TURN} entry (their own copy holds it). */
    public static final class Point {

        private final int id;
        private final Kind kind;
        private final int turn;
        private final UUID playerId;
        private final String playerName;
        private final PhaseStep step;
        private final String cardName;
        private final long createdAt;
        private final transient GameState state;

        Point(int id, Kind kind, int turn, UUID playerId, String playerName, PhaseStep step, String cardName, GameState state) {
            this.id = id;
            this.kind = kind;
            this.turn = turn;
            this.playerId = playerId;
            this.playerName = playerName;
            this.step = step;
            this.cardName = cardName;
            this.createdAt = System.currentTimeMillis();
            this.state = state;
        }

        public int getId() {
            return id;
        }

        public Kind getKind() {
            return kind;
        }

        public int getTurn() {
            return turn;
        }

        /** The active player for a step or a turn, the caster or the land's player otherwise. */
        public UUID getPlayerId() {
            return playerId;
        }

        public String getPlayerName() {
            return playerName;
        }

        /** The step the point was taken in (null for a turn start). */
        public PhaseStep getStep() {
            return step;
        }

        /** The spell or the land (null for a step or a turn). */
        public String getCardName() {
            return cardName;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        GameState getState() {
            return state;
        }

        /**
         * "Turn 5 Bob — Upkeep", "Bob casts Cyclonic Rift", "Bob plays Forest", "Turn 3 Bob — start".
         */
        public String getLabel() {
            switch (kind) {
                case CAST:
                    return playerName + " casts " + cardName;
                case LAND:
                    return playerName + " plays " + cardName;
                case TURN:
                    return "Turn " + turn + " " + playerName + " — start";
                default:
                    return "Turn " + turn + " " + playerName + " — " + stepName(step);
            }
        }

        /** "before Bob casts Cyclonic Rift", "to Turn 5 Bob — Upkeep": what a log line says it went back to. */
        public String getTarget() {
            return (kind == Kind.CAST || kind == Kind.LAND ? "before " : "to ") + getLabel();
        }
    }

    private final List<Point> points = new ArrayList<>();
    private int nextId = 1;

    /**
     * The points a table may go back to, oldest first: every step, cast and land point, and the
     * start of a turn only where no finer point of that turn is left.
     */
    public synchronized List<Point> list() {
        Set<Integer> fineTurns = new HashSet<>();
        for (Point p : points) {
            if (p.kind != Kind.TURN) {
                fineTurns.add(p.turn);
            }
        }
        List<Point> out = new ArrayList<>();
        for (Point p : points) {
            if (p.kind != Kind.TURN || !fineTurns.contains(p.turn)) {
                out.add(p);
            }
        }
        return out;
    }

    public synchronized Point get(int id) {
        for (Point p : points) {
            if (p.id == id) {
                return p;
            }
        }
        return null;
    }

    // --- taking points: called from the game loop (game thread) ---

    /** A step begins: Phase.playStep, after its skip check, before its turn-based actions. */
    public static void stepStarted(Game game) {
        RollbackPoints holder = holderFor(game);
        if (holder == null || !resumable(game)) {
            return;
        }
        Player active = game.getPlayer(game.getActivePlayerId());
        holder.add(game, new Point(holder.nextId(), Kind.STEP, game.getTurnNum(), game.getActivePlayerId(),
                active == null ? "?" : active.getName(), game.getTurnStepType(), null, game.getState().copy()));
    }

    /** A turn begins and their own turn-start copy was taken (GameImpl.saveRollBackGameState). */
    public static void turnStarted(Game game) {
        RollbackPoints holder = holderFor(game);
        if (holder == null) {
            return;
        }
        Player active = game.getPlayer(game.getActivePlayerId());
        holder.add(game, new Point(holder.nextId(), Kind.TURN, game.getTurnNum(), game.getActivePlayerId(),
                active == null ? "?" : active.getName(), null, null, null));
    }

    /**
     * A copy taken just before a cast or a land play from a priority, kept by {@link #commit} only
     * if the action happens; null when no point is taken here.
     */
    public static Point capture(Game game, Kind kind, UUID playerId, Card card) {
        RollbackPoints holder = holderFor(game);
        if (holder == null || card == null || !resumable(game)) {
            return null;
        }
        Step step = game.getStep();
        if (step == null || step.getStepPart() != Step.StepPart.PRIORITY) {
            return null;
        }
        if (!playerId.equals(game.getState().getPriorityPlayerId())) {
            return null; // not this player's priority: a spell cast while something resolves
        }
        Player player = game.getPlayer(playerId);
        return new Point(0, kind, game.getTurnNum(), playerId, player == null ? "?" : player.getName(),
                game.getTurnStepType(), card.getName(), game.getState().copy());
    }

    public static void commit(Game game, Point captured) {
        RollbackPoints holder = holderFor(game);
        if (holder == null || captured == null) {
            return;
        }
        holder.add(game, new Point(holder.nextId(), captured.kind, captured.turn, captured.playerId,
                captured.playerName, captured.step, captured.cardName, captured.state));
    }

    private static RollbackPoints holderFor(Game game) {
        if (game == null || game.isSimulation() || game.inCheckPlayableState() || !game.getOptions().rollbackTurnsAllowed) {
            return null;
        }
        return game.getRollbackPoints();
    }

    /** Only a regular phase and step of the turn can be resumed (an extra phase or step cannot). */
    private static boolean resumable(Game game) {
        Turn turn = game.getTurn();
        if (turn == null || turn.getPhase() == null || game.executingRollback()) {
            return false;
        }
        Phase phase = turn.getPhase();
        return turn.getPhase(phase.getType()) == phase && phase.isPlayingOwnStep();
    }

    private synchronized int nextId() {
        return nextId++;
    }

    private synchronized void add(Game game, Point point) {
        points.add(point);
        prune(game);
    }

    /**
     * Keeps the current turn and one round before it (as many turns as there are players still in
     * the game); a turn start is kept while their own copy for it is (four turns) and only where no
     * finer point of that turn is left.
     */
    private void prune(Game game) {
        int now = game.getTurnNum();
        int round = 0;
        for (Player player : game.getPlayers().values()) {
            if (player.isInGame()) {
                round++;
            }
        }
        int oldestFine = now - Math.max(1, round);
        points.removeIf(p -> p.kind != Kind.TURN && p.turn < oldestFine);
        points.removeIf(p -> p.kind == Kind.TURN && p.turn <= now - 4); // their own copies keep four turns
        int fine = 0;
        for (Point p : points) {
            if (p.kind != Kind.TURN) {
                fine++;
            }
        }
        for (int i = 0; fine > MAX_POINTS && i < points.size(); ) {
            if (points.get(i).kind != Kind.TURN) {
                points.remove(i);
                fine--;
            } else {
                i++;
            }
        }
    }

    /**
     * The game went back to {@code point}: what came after it is gone. A step start stays (the
     * game resumes at it and will not take it again); a cast or a land goes too, since the game is
     * back at the priority it was taken at, and doing it again takes it again.
     */
    public synchronized void wentBackTo(Point point) {
        int at = points.indexOf(point);
        if (at < 0) {
            return;
        }
        int keep = point.kind == Kind.CAST || point.kind == Kind.LAND ? at : at + 1;
        while (points.size() > keep) {
            points.remove(points.size() - 1);
        }
    }

    /** Their turn rollback went back to the start of {@code turn}: every point taken after that start is gone. */
    public synchronized void wentBackToTurn(int turn) {
        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            if (p.kind == Kind.TURN && p.turn == turn) {
                while (points.size() > i + 1) {
                    points.remove(points.size() - 1);
                }
                return;
            }
        }
        points.removeIf(p -> p.turn >= turn && p.kind != Kind.TURN);
    }

    public static String stepName(PhaseStep step) {
        if (step == null) {
            return "start";
        }
        switch (step) {
            case UNTAP:
                return "Untap";
            case UPKEEP:
                return "Upkeep";
            case DRAW:
                return "Draw";
            case PRECOMBAT_MAIN:
                return "Main 1";
            case BEGIN_COMBAT:
                return "Combat";
            case DECLARE_ATTACKERS:
                return "Declare attackers";
            case DECLARE_BLOCKERS:
                return "Declare blockers";
            case FIRST_COMBAT_DAMAGE:
                return "First strike damage";
            case COMBAT_DAMAGE:
                return "Combat damage";
            case END_COMBAT:
                return "End of combat";
            case POSTCOMBAT_MAIN:
                return "Main 2";
            case END_TURN:
                return "End step";
            case CLEANUP:
                return "Cleanup";
            default:
                return step.getStepText();
        }
    }

    /** For tests: how many points hold a copy of the game. */
    public synchronized int countWithState() {
        int n = 0;
        for (Point p : points) {
            if (p.state != null) {
                n++;
            }
        }
        return n;
    }
}

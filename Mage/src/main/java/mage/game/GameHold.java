package mage.game;

import org.apache.log4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Hold on, I am reading that card": one seat stops the game for everybody, and gives it back.
 * <p>
 * <b>Why this exists, and why {@link Game#pause()} is not what it looks like.</b> {@code Game}
 * already has {@code pause()} / {@code isPaused()} / {@code resume()}, and the game loop checks
 * {@code isPaused()} at every step - but a paused game does not <i>wait</i>: every loop
 * ({@code GameImpl.play}, {@code playTurn}, {@code playPriority}, {@code Turn.play}) simply
 * <b>returns</b>, so {@code Game.start()} returns to {@link mage.server.game.GameWorker}, which
 * takes that for the end of the game and calls {@code endGameWithResult(game.getWinner())} - the
 * table is closed and every seat is told the game is over. And {@code resume()} runs the whole
 * game loop <b>on the calling thread</b>, so calling it from a network callback would hand that
 * thread to the game for the rest of the match. Their pause is a hook for the simulating AIs
 * (MCTS, {@code SimulatedPlayer2}) and for the {@code stopOnTurn} test option, not for a table
 * that must still be there afterwards.
 * <p>
 * <b>What this does instead.</b> A hold is a note on the side, read by the game thread itself at
 * the one place a player is about to act: just before {@code player.priority(game)} in
 * {@code GameImpl.playPriority}. While a hold is in force the game thread waits there - inside
 * the loop, with the state saved and nothing half-done - so no player takes an action, no spell
 * resolves, no step passes, and nothing unwinds. Letting the hold go (or its expiry) lets the
 * thread walk on exactly where it stood.
 * <ul>
 * <li>The seat that holds the table is <b>not</b> held itself: he may still act, which is the
 *     point of stopping the others.</li>
 * <li>A hold expires by itself ({@link #MAX_MILLIS} at the most), so a browser that vanished
 *     mid-read cannot freeze a table for good.</li>
 * <li>Anybody at the table may lift it ({@link #resume}).</li>
 * <li>Simulated games are never held: the AIs copy the game <i>with its id</i> and run it on the
 *     game thread, so a hold that caught them would deadlock the table it was meant to pause.</li>
 * </ul>
 * A game whose players are on a clock ({@code MatchTimeLimit}) keeps counting: the hold stops the
 * game, not their timers.
 */
public final class GameHold {

    private static final Logger logger = Logger.getLogger(GameHold.class);

    /** The longest a seat can hold a table, however long he asks for. */
    public static final long MAX_MILLIS = 120_000L;

    /** How often the waiting game thread looks at the note. */
    private static final long POLL_MILLIS = 50L;

    private static final Map<UUID, Hold> HOLDS = new ConcurrentHashMap<>();

    private GameHold() {
    }

    /** One hold: who took it, under what name (for the line the table says), and when it lapses. */
    public static final class Hold {

        private final UUID playerId;
        private final String playerName;
        private final long until;

        Hold(UUID playerId, String playerName, long until) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.until = until;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public String getPlayerName() {
            return playerName;
        }

        /** Milliseconds left before it lapses on its own, never below zero. */
        public long getMillisLeft() {
            return Math.max(0L, until - System.currentTimeMillis());
        }

        boolean isOver() {
            return System.currentTimeMillis() >= until;
        }
    }

    /**
     * Hold the game for everybody but {@code playerId}, for {@code millis} (capped at
     * {@link #MAX_MILLIS}). A second hold replaces the first, whoever took it.
     */
    public static Hold pause(UUID gameId, UUID playerId, String playerName, long millis) {
        if (gameId == null || playerId == null) {
            return null;
        }
        long span = Math.max(1_000L, Math.min(MAX_MILLIS, millis));
        Hold hold = new Hold(playerId, playerName, System.currentTimeMillis() + span);
        HOLDS.put(gameId, hold);
        logger.info("Game hold: " + playerName + " holds game " + gameId + " for " + (span / 1000) + " s");
        return hold;
    }

    /** Let the game go again. Returns the hold that was lifted, or null when there was none. */
    public static Hold resume(UUID gameId) {
        if (gameId == null) {
            return null;
        }
        Hold hold = HOLDS.remove(gameId);
        if (hold != null) {
            logger.info("Game hold: game " + gameId + " released (held by " + hold.getPlayerName() + ")");
        }
        return hold;
    }

    /** The hold in force on that game, or null - an expired one is forgotten here. */
    public static Hold current(UUID gameId) {
        if (gameId == null) {
            return null;
        }
        Hold hold = HOLDS.get(gameId);
        if (hold == null) {
            return null;
        }
        if (hold.isOver()) {
            if (HOLDS.remove(gameId, hold)) {
                logger.info("Game hold: game " + gameId + " released by itself (" + hold.getPlayerName() + " held it to the end)");
            }
            return null;
        }
        return hold;
    }

    /**
     * Called by the game thread just before it asks {@code playerId} to act: it returns at once
     * unless the table is held by somebody else, and then waits until the hold is given back or
     * lapses. Never called on a simulation, and never for the holder himself.
     */
    public static void await(Game game, UUID playerId) {
        if (game == null || playerId == null || game.isSimulation() || HOLDS.isEmpty()) {
            return;
        }
        Hold hold = current(game.getId());
        if (hold == null || playerId.equals(hold.getPlayerId())) {
            return;
        }
        logger.info("Game hold: game " + game.getId() + " waits on " + hold.getPlayerName() + " before " + playerId + " acts");
        while (true) {
            hold = current(game.getId());
            if (hold == null || playerId.equals(hold.getPlayerId()) || game.getState().isGameOver()) {
                return;
            }
            try {
                Thread.sleep(POLL_MILLIS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /** A game that is over (or was never played) leaves no note behind. */
    public static void forget(UUID gameId) {
        resume(gameId);
    }
}

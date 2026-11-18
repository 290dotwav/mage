package mage.game.lesson;

import mage.constants.PhaseStep;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/**
 * Ours: what the computer player of a lesson does (the site's tutorial, mage.server.scenario). A
 * script of lines: while a game has one ({@link mage.game.GameOptions#rivalScript}), the computer
 * player it names decides nothing by itself. At each question of the engine it does what the
 * script says, or nothing: it passes, attacks with nobody, blocks with nobody, casts nothing.
 * <p>
 * A line is { turn, step, action, card, target, blocker, attacker, waitFor } (the site's contract,
 * CONSTRUIRE.md): on that turn of the game, at that step, {@code attack} with {@code card},
 * {@code block} {@code attacker} with {@code blocker}, {@code noBlock}, {@code cast} {@code card}
 * on {@code target} ({@link #STUDENT}, {@link #SELF} or a permanent's name) or {@code castCreature}
 * {@code card}. Lines are played in their order: one cast waits until the spell of the cast before
 * it has left the stack. A line with {@code waitFor} waits, at its own moment (a cast at its
 * priority in its step, an attack in the declaration of attackers, a block in the declaration of
 * blockers), until the site has said it reached that step of its own script
 * ({@link #reached(String)}, the door's {@code lessonStep}); the game waits with it. A line whose
 * moment has passed (an attacker that never came, a turn gone by) is dropped.
 * <p>
 * {@link #free()} is the other kind: the computer player plays as it always does, and with
 * {@link #noMultiBlock()} never puts two blockers on one attacker, with {@link #noBlock()} never blocks.
 * <p>
 * The scenarios hold a script as a template; {@link #forPlayer(UUID)} makes the copy one game
 * plays, with its own progress. The progress is not part of the game state: a rollback does not
 * give back a line already played.
 */
public final class RivalScript implements Serializable {

    /** A cast's target: the other player of the duel. */
    public static final String STUDENT = "student";
    /** A cast's target: the scripted player himself. */
    public static final String SELF = "self";

    public enum Action {
        ATTACK("attack"),
        BLOCK("block"),
        NO_BLOCK("noBlock"),
        CAST("cast"),
        CAST_CREATURE("castCreature");

        private final String text;

        Action(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /** One line of the script. */
    public static final class Line implements Serializable {

        private final int turn;
        private final PhaseStep step;
        private Action action;
        private String card;
        private String target;
        private String blocker;
        private String attacker;
        private String waitFor;

        private Line(int turn, PhaseStep step) {
            this.turn = turn;
            this.step = step;
        }

        public Line attack(String card) {
            this.action = Action.ATTACK;
            this.card = card;
            return this;
        }

        public Line block(String blocker, String attacker) {
            this.action = Action.BLOCK;
            this.blocker = blocker;
            this.attacker = attacker;
            return this;
        }

        public Line noBlock() {
            this.action = Action.NO_BLOCK;
            return this;
        }

        /** {@code target}: {@link #STUDENT}, {@link #SELF} or the name of a permanent. */
        public Line cast(String card, String target) {
            this.action = Action.CAST;
            this.card = card;
            this.target = target;
            return this;
        }

        public Line castCreature(String card) {
            this.action = Action.CAST_CREATURE;
            this.card = card;
            return this;
        }

        /** The site's step id (« 3.4 ») this line waits for, at its own moment. */
        public Line waitFor(String stepId) {
            this.waitFor = stepId;
            return this;
        }

        public int getTurn() {
            return turn;
        }

        public PhaseStep getStep() {
            return step;
        }

        public Action getAction() {
            return action;
        }

        public String getCard() {
            return card;
        }

        public String getTarget() {
            return target;
        }

        public String getBlocker() {
            return blocker;
        }

        public String getAttacker() {
            return attacker;
        }

        public String getWaitFor() {
            return waitFor;
        }

        public boolean isCast() {
            return action == Action.CAST || action == Action.CAST_CREATURE;
        }

        public boolean isBlock() {
            return action == Action.BLOCK || action == Action.NO_BLOCK;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("turn ").append(turn).append(' ').append(step).append(' ').append(action);
            if (card != null) {
                sb.append(' ').append(card);
            }
            if (target != null) {
                sb.append(" on ").append(target);
            }
            if (blocker != null) {
                sb.append(' ').append(blocker).append(" before ").append(attacker);
            }
            if (waitFor != null) {
                sb.append(" after step ").append(waitFor);
            }
            return sb.toString();
        }
    }

    /** A line of the script: on that turn of the game, at that step. */
    public static Line at(int turn, PhaseStep step) {
        return new Line(turn, step);
    }

    /** The computer player does these lines and nothing else (no line: nothing at all). */
    public static RivalScript script(Line... lines) {
        for (Line line : lines) {
            if (line.action == null) {
                throw new IllegalArgumentException("a line of a rival script has no action: " + line);
            }
        }
        return new RivalScript(null, false, false, false, Arrays.asList(lines));
    }

    /** The computer player plays freely. */
    public static RivalScript free() {
        return new RivalScript(null, true, false, false, Collections.emptyList());
    }

    /** Free play, but never two blockers (or more) on one attacker. */
    public RivalScript noMultiBlock() {
        if (!free) {
            throw new IllegalStateException("noMultiBlock is for a free rival");
        }
        return new RivalScript(playerId, true, true, noBlock, lines);
    }

    /** Free play, but never a blocker: the first game's Rival, who races (tuto7). */
    public RivalScript noBlock() {
        if (!free) {
            throw new IllegalStateException("noBlock is for a free rival");
        }
        return new RivalScript(playerId, true, noMultiBlock, true, lines);
    }

    private final UUID playerId;
    private final boolean free;
    private final boolean noMultiBlock;
    private final boolean noBlock;
    private final List<Line> lines;
    // progress, one game's copy only
    private int next = 0;
    private UUID lastSpellId = null;
    private final Set<String> reached = ConcurrentHashMap.newKeySet();

    private RivalScript(UUID playerId, boolean free, boolean noMultiBlock, boolean noBlock, List<Line> lines) {
        this.playerId = playerId;
        this.free = free;
        this.noMultiBlock = noMultiBlock;
        this.noBlock = noBlock;
        this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
    }

    /** The copy one game plays, for its computer player {@code playerId}, from the first line. */
    public RivalScript forPlayer(UUID playerId) {
        return new RivalScript(playerId, free, noMultiBlock, noBlock, lines);
    }

    /** The scripted player (null on a scenario's template). */
    public UUID getPlayerId() {
        return playerId;
    }

    public boolean isFree() {
        return free;
    }

    public boolean isNoMultiBlock() {
        return noMultiBlock;
    }

    public boolean isNoBlock() {
        return noBlock;
    }

    public List<Line> getLines() {
        return lines;
    }

    // --- the site's steps ---

    /** The site has reached that step of its script: the lines waiting for it may go. */
    public void reached(String stepId) {
        if (stepId == null) {
            return;
        }
        reached.add(stepId);
        synchronized (this) {
            notifyAll();
        }
    }

    public boolean hasReached(String stepId) {
        return stepId == null || reached.contains(stepId);
    }

    /**
     * Waits (the game's own thread) until the site has reached {@code stepId}, or the game is
     * over for the scripted player ({@code playing} false).
     *
     * @return true when the step was reached
     */
    public boolean awaitStep(String stepId, BooleanSupplier playing) {
        while (!hasReached(stepId)) {
            if (!playing.getAsBoolean()) {
                return false;
            }
            synchronized (this) {
                if (hasReached(stepId)) {
                    break;
                }
                try {
                    wait(250);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return true;
    }

    // --- the lines ---

    /**
     * The next line to play, the lines whose moment has passed dropped: an earlier turn, or an
     * earlier step of this turn. Null when the script is over.
     */
    public synchronized Line current(int turn, PhaseStep step) {
        while (next < lines.size()) {
            Line line = lines.get(next);
            if (line.turn < turn || (line.turn == turn && line.step.getIndex() < step.getIndex())) {
                next++;
                continue;
            }
            return line;
        }
        return null;
    }

    /** The line {@link #current} gave is played (or given up). */
    public synchronized void done(Line line) {
        if (next < lines.size() && lines.get(next) == line) {
            next++;
        }
    }

    /** The spell of the last scripted cast, which the next cast waits for until it left the stack. */
    public synchronized UUID getLastSpellId() {
        return lastSpellId;
    }

    public synchronized void setLastSpellId(UUID lastSpellId) {
        this.lastSpellId = lastSpellId;
    }
}

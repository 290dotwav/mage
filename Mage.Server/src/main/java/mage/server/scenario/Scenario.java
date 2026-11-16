package mage.server.scenario;

import mage.game.lesson.RivalScript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * One position of the site's lessons, set up before a game starts: what each of the two players
 * holds, where, and who plays first. The list lives on the server ({@link Scenarios}); a browser
 * only names an id of it.
 * <p>
 * The student is the table's creator, the opponent the other seat. Cards are named in English,
 * as their repository knows them, and may name their printing ("TMP:Goblin Bombardment", "KHM:Arni
 * Brokenbrow#120"); a library is written from its top card down.
 * <p>
 * Ours too, for the site's tutorial: what the opponent does ({@link #rival(RivalScript)}, the
 * computer player follows a script or plays freely), and a start dealt as a normal game's
 * ({@link #dealt()}: each side's {@link Side#deck}, shuffled, seven cards and the mulligan, who
 * starts drawn at random; a card {@link Side#placed} among the first of its library).
 */
public final class Scenario {

    /** One player's side of the position. */
    public static final class Side {
        int life = -1; // -1: the game's own starting life
        final List<String> library = new ArrayList<>();
        final List<String> hand = new ArrayList<>();
        final List<String> battlefield = new ArrayList<>();
        final List<String> tapped = new ArrayList<>();
        final List<String> arrived = new ArrayList<>();
        final List<String> graveyard = new ArrayList<>();
        final List<String> command = new ArrayList<>();
        final List<String> deck = new ArrayList<>();
        final List<String> placed = new ArrayList<>();
        final List<Integer> placedWithin = new ArrayList<>();

        public Side life(int life) {
            this.life = life;
            return this;
        }

        /** From the top card down. */
        public Side library(String... names) {
            library.addAll(Arrays.asList(names));
            return this;
        }

        public Side hand(String... names) {
            hand.addAll(Arrays.asList(names));
            return this;
        }

        /** Untapped, and under its controller's control since the turn began. */
        public Side battlefield(String... names) {
            battlefield.addAll(Arrays.asList(names));
            return this;
        }

        public Side tapped(String... names) {
            tapped.addAll(Arrays.asList(names));
            return this;
        }

        /** On the battlefield as if it had come this turn: summoning sick on the first turn (302.6). */
        public Side arrived(String... names) {
            arrived.addAll(Arrays.asList(names));
            return this;
        }

        public Side graveyard(String... names) {
            graveyard.addAll(Arrays.asList(names));
            return this;
        }

        /** Commanders, in the command zone (a commander game type only). */
        public Side command(String... names) {
            command.addAll(Arrays.asList(names));
            return this;
        }

        /** A dealt scenario's deck ({@link Scenario#dealt()}): {@code count} copies of that card. */
        public Side deck(int count, String name) {
            for (int i = 0; i < count; i++) {
                deck.add(name);
            }
            return this;
        }

        /** A dealt scenario's deck: these cards, one each. */
        public Side deck(String... names) {
            deck.addAll(Arrays.asList(names));
            return this;
        }

        /**
         * A card of the deck (counted in it) put, once the hands are kept, at a place drawn among the
         * first {@code within} of the shuffled library: never in the opening hand.
         */
        public Side placed(String name, int within) {
            if (within < 1) {
                throw new IllegalArgumentException("placed within the first " + within);
            }
            placed.add(name);
            placedWithin.add(within);
            return this;
        }

        public List<String> getDeck() {
            return Collections.unmodifiableList(deck);
        }

        public List<String> getPlaced() {
            return Collections.unmodifiableList(placed);
        }

        public List<Integer> getPlacedWithin() {
            return Collections.unmodifiableList(placedWithin);
        }

        public int getLife() {
            return life;
        }

        public List<String> getLibrary() {
            return Collections.unmodifiableList(library);
        }

        public List<String> getHand() {
            return Collections.unmodifiableList(hand);
        }

        public List<String> getBattlefield() {
            return Collections.unmodifiableList(battlefield);
        }

        public List<String> getTapped() {
            return Collections.unmodifiableList(tapped);
        }

        public List<String> getArrived() {
            return Collections.unmodifiableList(arrived);
        }

        public List<String> getGraveyard() {
            return Collections.unmodifiableList(graveyard);
        }

        public List<String> getCommand() {
            return Collections.unmodifiableList(command);
        }
    }

    private final String id;
    private final String gameType;
    private final Side student = new Side();
    private final Side opponent = new Side();
    private boolean studentStarts = true;
    private boolean startsAtRandom = false;
    private boolean dealt = false;
    private RivalScript rival = null;

    Scenario(String id, String gameType) {
        this.id = id;
        this.gameType = gameType;
    }

    Scenario student(java.util.function.Consumer<Side> set) {
        set.accept(student);
        return this;
    }

    Scenario opponent(java.util.function.Consumer<Side> set) {
        set.accept(opponent);
        return this;
    }

    Scenario opponentStarts() {
        this.studentStarts = false;
        return this;
    }

    /** Who plays first: "student" or "opponent". */
    Scenario starts(String who) {
        if ("student".equals(who)) {
            this.studentStarts = true;
        } else if ("opponent".equals(who)) {
            this.studentStarts = false;
        } else {
            throw new IllegalArgumentException("starts: student or opponent, not " + who);
        }
        return this;
    }

    /** What the opponent, the table's computer player, does: a script, or free play. */
    Scenario rival(RivalScript script) {
        this.rival = script;
        return this;
    }

    /**
     * The start of a normal game from each side's {@link Side#deck}: shuffled, seven cards drawn, the
     * mulligan, who starts drawn at random. Life is the game type's own.
     */
    Scenario dealt() {
        this.dealt = true;
        this.startsAtRandom = true;
        return this;
    }

    /** The opponent's script, or null when the computer player is the table's ordinary bot. */
    public RivalScript getRival() {
        return rival;
    }

    public boolean isDealt() {
        return dealt;
    }

    public boolean startsAtRandom() {
        return startsAtRandom;
    }

    public String getId() {
        return id;
    }

    /** Their game type name (config.xml), which the table is opened with whatever the browser asked. */
    public String getGameType() {
        return gameType;
    }

    public Side getStudent() {
        return student;
    }

    public Side getOpponent() {
        return opponent;
    }

    public boolean studentStarts() {
        return studentStarts;
    }
}

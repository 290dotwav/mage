package mage.server.scenario;

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
 * as their repository knows them; a library is written from its top card down.
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

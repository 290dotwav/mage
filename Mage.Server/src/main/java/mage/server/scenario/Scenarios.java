package mage.server.scenario;

import mage.cards.Card;
import mage.cards.repository.CardInfo;
import mage.cards.repository.CardRepository;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.GameOptions;
import mage.game.PutToBattlefieldInfo;
import mage.game.lesson.RivalScript;
import mage.players.Player;
import mage.util.RandomUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static mage.game.lesson.RivalScript.at;

/**
 * The site's lessons, the only positions a table can be set up with: a browser sends an id of
 * this list (the door's {@code scenario} on {@code table create}), never a position of its own.
 * Nothing here goes through their cheat commands or the server's test mode: the cards are made
 * from the repository and put in place before the game starts, as their own tests do
 * ({@code GameImpl.cheat}), and the game is told it starts from a position
 * ({@link GameOptions#scenario}: no hand drawn, no life set, no mulligan).
 */
public final class Scenarios {

    public static final String DUEL = "Two Player Duel";
    public static final String COMMANDER_DUEL = "Commander Two Player Duel";

    private static final Map<String, Scenario> LIST = new LinkedHashMap<>();

    static {
        // 1. « Coup de grâce »: Claude at 3, one red mana. Shock spends it for 2;
        // Raging Goblin with it, then Giant Growth on the Forest, is 4 this turn.
        add(new Scenario("m1", DUEL)
                .student(s -> s.life(20)
                        .battlefield("Mountain", "Forest")
                        .hand("Shock", "Raging Goblin", "Giant Growth")
                        .library("Mountain", "Forest", "Mountain", "Forest", "Mountain"))
                .opponent(o -> o.life(3)
                        .library("Plains", "Plains", "Plains", "Plains", "Plains")));
        // 2. « À l'assaut »: Claude at 7 behind one 2/2. The Craw Wurm came this turn (the lands
        // that paid for it are tapped); the Drake flies over; Drake, Giant and Lions are 7 with the
        // Giant Growth that only the Elves' green can pay: the Elves stay home. Everything that can
        // attack, the Elves too, is 5 when the Bears block the Giant.
        add(new Scenario("m2", DUEL)
                .student(s -> s.life(20)
                        .battlefield("Wind Drake", "10E:Hill Giant", "Savannah Lions", "Llanowar Elves")
                        .arrived("10E:Craw Wurm")
                        .tapped("Forest", "Forest", "Forest", "Forest", "Forest", "Island")
                        .hand("Giant Growth")
                        .library("Forest", "Island", "Forest", "Island", "Forest"))
                .opponent(o -> o.life(7)
                        .battlefield("Grizzly Bears", "Forest", "Forest")
                        .library("Forest", "Forest", "Forest", "Forest", "Forest")));
        // 3. « Tiens bon »: Claude's three attack each combat if able (10 in all, he is at 5,
        // you at 6). Walls can't block Juggernaut: the Wall takes the Cyclops, the Giant the
        // Dasher, Juggernaut's 5 leave you at 1, and Bears and Giant are 5 next turn
        // while his attackers are still tapped.
        add(new Scenario("m3", DUEL)
                .student(s -> s.life(6)
                        .battlefield("10E:Wall of Wood#309", "10E:Hill Giant", "Grizzly Bears", "Forest", "Forest", "Forest")
                        .library("Forest", "Forest", "Forest", "Forest", "Forest"))
                .opponent(o -> o.life(5)
                        .battlefield("Juggernaut", "10E:Bloodrock Cyclops", "KTK:Valley Dasher", "Mountain", "Mountain", "Mountain", "Mountain")
                        .library("Mountain", "Mountain", "Mountain", "Mountain", "Mountain"))
                .opponentStarts());
        // 4. « Coup fourré »: Juggernaut and the Cyclops attack each combat (8, you are at 3).
        // Gideon's Reproach only hits an attacking or blocking creature: the Mastodon blocks
        // the Cyclops, the Reproach kills Juggernaut, nothing gets through, and the Mastodon
        // is 3 next turn against his 3.
        add(new Scenario("m4", DUEL)
                .student(s -> s.life(3)
                        .battlefield("Siege Mastodon", "Plains", "Plains")
                        .hand("Gideon's Reproach")
                        .library("Plains", "Plains", "Plains", "Plains", "Plains"))
                .opponent(o -> o.life(3)
                        .battlefield("Juggernaut", "10E:Bloodrock Cyclops", "Mountain", "Mountain", "Mountain", "Mountain")
                        .library("Mountain", "Mountain", "Mountain", "Mountain", "Mountain"))
                .opponentStarts());
        // 5. « La machine »: Claude at 10 behind a Craw Wurm. Siege-Gang Commander brings three
        // Goblins; each creature sacrificed to the Bombardment is 1 damage and Blood Artist's
        // 1 more, 2 each while the Artist lives: five creatures, the Artist last, are 10.
        add(new Scenario("m5", DUEL)
                .student(s -> s.life(20)
                        .battlefield("C13:Goblin Bombardment", "AVR:Blood Artist", "Mountain", "Mountain", "Mountain", "Mountain", "Mountain")
                        .hand("10E:Siege-Gang Commander")
                        .library("Mountain", "Mountain", "Mountain", "Mountain", "Mountain"))
                .opponent(o -> o.life(10)
                        .battlefield("10E:Craw Wurm", "Forest", "Forest", "Forest", "Forest", "Forest", "Forest")
                        .library("Forest", "Forest", "Forest", "Forest", "Forest")));
        // 6. « Ton commandant »: a short game of Commander, you at 40, Claude at 9 with a
        // Juggernaut that attacked last turn. Arni comes out of the command zone with haste;
        // three hits of 3 win, through Fling and a second cast from the command zone (the tax)
        // or by attacking twice. Arni dead goes back to the command zone if you say so.
        add(new Scenario("m6", COMMANDER_DUEL)
                .student(s -> s.life(40)
                        .command("KHM:Arni Brokenbrow#120")
                        .battlefield("Mountain", "Mountain", "Mountain", "Mountain", "Mountain", "Mountain", "Mountain")
                        .hand("Fling")
                        .library("Mountain", "Mountain", "Mountain", "Mountain", "Mountain"))
                .opponent(o -> o.life(9)
                        .tapped("Juggernaut")
                        .battlefield("Mountain", "Mountain", "Mountain", "Mountain")
                        .library("Mountain", "Mountain", "Mountain", "Mountain", "Mountain")));

        // The tutorial (the site's TUTO.md), lesson 1, « Bienvenue »: the Rival at 4 with nothing, the student's
        // Grizzly Bears ready. Two attacks, one on each of the student's turns, take him to 0; the student's draw on
        // his second turn is another Grizzly Bears, which he cannot cast (no land). The Rival's library holds cards
        // he cannot play either (no land), so his turns pass with nothing on the table.
        add(new Scenario("tuto1", DUEL)
                .student(s -> s.life(20)
                        .battlefield("10E:Grizzly Bears")
                        .library("10E:Grizzly Bears", "10E:Grizzly Bears", "10E:Grizzly Bears", "10E:Grizzly Bears", "10E:Grizzly Bears"))
                .opponent(o -> o.life(4)
                        .library("10E:Grizzly Bears", "10E:Grizzly Bears", "10E:Grizzly Bears", "10E:Grizzly Bears", "10E:Grizzly Bears")));
    }

    // The tutorial (the site's TUTO.md, CONSTRUIRE.md): tuto1 to tuto7, the Rival a script of his own
    // (RivalScript, the opponent seat's "Computer - simple" playing nothing else). Its cards are the
    // printings of the script's table « Les cartes du tutoriel ».
    static {
        // 2. « Le mana »: the Rival at 6 with nothing, and nothing done all the lesson. The student
        // starts (no draw on his first turn) with a Plains, Savannah Lions and Grizzly Bears: the
        // Plains and the Lions on turn 1, the Bears refused (one land, no green); on turn 3 the Forest
        // drawn pays the Bears and the Lions hit (6 to 4); on turn 5, the Plains drawn, both attack
        // (4 to 0). The Rival's library is cards he would not play anyway (no land).
        add(new Scenario("tuto2", DUEL)
                .student(s -> s.life(20)
                        .hand("10E:Plains", "9ED:Savannah Lions", "10E:Grizzly Bears")
                        .library("10E:Forest", "10E:Plains", "10E:Plains", "10E:Plains", "10E:Plains", "10E:Plains"))
                .opponent(o -> o.life(6)
                        .library("10E:Grizzly Bears", "10E:Grizzly Bears", "10E:Grizzly Bears", "10E:Grizzly Bears", "10E:Grizzly Bears", "10E:Grizzly Bears"))
                .starts("student")
                .rival(RivalScript.script()));
    }

    private Scenarios() {
    }

    private static void add(Scenario scenario) {
        LIST.put(scenario.getId(), scenario);
    }

    /** The scenario of that id, or null when the server has none by that id. */
    public static Scenario get(String id) {
        return id == null ? null : LIST.get(id);
    }

    public static List<String> ids() {
        return Collections.unmodifiableList(new ArrayList<>(LIST.keySet()));
    }

    /**
     * Sets the game up as the scenario says, before it starts: both players' zones emptied of
     * the decks they joined with and filled with the scenario's cards, their life, who starts,
     * and the options that make the start keep it (no shuffle, no hand drawn, no mulligan).
     * The student is {@code studentId}; the opponent is the game's other player.
     */
    public static void apply(Scenario scenario, Game game, GameOptions options, UUID studentId) {
        Player student = game.getPlayer(studentId);
        Player opponent = null;
        for (Player player : game.getPlayers().values()) {
            if (!player.getId().equals(studentId)) {
                opponent = player;
            }
        }
        if (student == null || opponent == null || game.getPlayers().size() != 2) {
            throw new IllegalStateException("scenario " + scenario.getId() + " needs two players, the student " + studentId + " one of them");
        }
        if (scenario.getRival() != null) {
            // the computer player follows the scenario: this game's own copy of its script
            options.rivalScript = scenario.getRival().forPlayer(opponent.getId());
        }
        if (scenario.isDealt()) {
            // a normal game's start from the scenario's decks: shuffled, seven cards, the mulligan
            Map<UUID, Integer> placed = new LinkedHashMap<>();
            deal(scenario.getStudent(), student, game, placed);
            deal(scenario.getOpponent(), opponent, game, placed);
            if (!placed.isEmpty()) {
                game.getState().addWatcher(new ScenarioPlacedWatcher(placed));
            }
            // who starts, at random, without asking the toss's winner (the tutorial: « c'est le hasard »)
            game.setStartingPlayerId(RandomUtil.nextBoolean() ? student.getId() : opponent.getId());
            return;
        }
        Set<UUID> arrived = new HashSet<>();
        Set<UUID> tapped = new HashSet<>();
        setUp(scenario.getStudent(), student, game, arrived, tapped);
        setUp(scenario.getOpponent(), opponent, game, arrived, tapped);
        if (!arrived.isEmpty() || !tapped.isEmpty()) {
            game.getState().addWatcher(new ScenarioArrivalsWatcher(arrived, tapped));
        }
        game.setStartingPlayerId(scenario.studentStarts() ? student.getId() : opponent.getId());
        options.scenario = true;
        options.skipInitShuffling = true;
    }

    /** The zones emptied of what the player joined with, as {@link #setUp} does. */
    private static void clear(Player player, Game game) {
        for (UUID id : player.getLibrary().getCardList()) {
            game.getState().setZone(id, Zone.OUTSIDE);
        }
        player.getLibrary().clear();
        player.getHand().clear();
        for (UUID id : player.getSideboard()) {
            game.getState().setZone(id, Zone.OUTSIDE);
        }
        player.getSideboard().clear();
    }

    /** A dealt side: its deck as the library (the game shuffles it), its placed cards set aside. */
    private static void deal(Scenario.Side side, Player player, Game game, Map<UUID, Integer> placed) {
        clear(player, game);
        player.initLife(side.getLife() < 0 ? game.getStartingLife() : side.getLife());
        player.updateRange(game);
        List<String> library = new ArrayList<>(side.getDeck());
        List<Card> aside = new ArrayList<>();
        for (int i = 0; i < side.getPlaced().size(); i++) {
            String name = side.getPlaced().get(i);
            if (!library.remove(name)) {
                throw new IllegalStateException("placed card " + name + " is not in the deck");
            }
            Card card = cards(Collections.singletonList(name)).get(0);
            aside.add(card);
            placed.put(card.getId(), side.getPlacedWithin().get(i));
        }
        game.cheat(player.getId(), cards(library), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        if (!aside.isEmpty()) {
            game.loadCards(new LinkedHashSet<>(aside), player.getId());
            for (Card card : aside) {
                game.getState().setZone(card.getId(), Zone.OUTSIDE);
            }
        }
    }

    private static void setUp(Scenario.Side side, Player player, Game game, Set<UUID> arrived, Set<UUID> tapped) {
        // The cards the deck brought stay known to the game but in no zone
        clear(player, game);
        player.initLife(side.getLife() < 0 ? game.getStartingLife() : side.getLife());
        player.updateRange(game);

        // cheat puts each library card on top in turn: the last one put is the top
        List<Card> library = cards(side.getLibrary());
        Collections.reverse(library);
        List<PutToBattlefieldInfo> battlefield = new ArrayList<>();
        for (Card card : cards(side.getBattlefield())) {
            battlefield.add(new PutToBattlefieldInfo(card, false));
        }
        for (Card card : cards(side.getTapped())) {
            battlefield.add(new PutToBattlefieldInfo(card, true));
            tapped.add(card.getId());
        }
        for (Card card : cards(side.getArrived())) {
            battlefield.add(new PutToBattlefieldInfo(card, false));
            arrived.add(card.getId());
        }
        game.cheat(player.getId(), library, cards(side.getHand()), battlefield,
                cards(side.getGraveyard()), cards(side.getCommand()), new ArrayList<>());
    }

    private static List<Card> cards(List<String> names) {
        List<Card> cards = new ArrayList<>();
        for (String written : names) {
            // "C13:Goblin Bombardment" or "KHM:Arni Brokenbrow#120": that printing (its picture on the site), else any
            String name = written;
            String set = null;
            String number = null;
            int colon = written.indexOf(':');
            if (colon > 0 && written.substring(0, colon).matches("[A-Z0-9]{2,5}")) {
                set = written.substring(0, colon);
                name = written.substring(colon + 1);
            }
            int hash = name.lastIndexOf('#');
            if (hash > 0) {
                number = name.substring(hash + 1);
                name = name.substring(0, hash);
            }
            CardInfo info = set == null ? CardRepository.instance.findCard(name, true)
                    : CardRepository.instance.findCardWithPreferredSetAndNumber(name, set, number);
            if (info == null) {
                throw new IllegalStateException("scenario card not in the repository: " + name);
            }
            cards.add(info.createCard());
        }
        return cards;
    }
}

package mage.game.mulligan;

import mage.cards.CardsImpl;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.players.Player;
import mage.util.ThreadUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "Mulligan à 10", a house rule: a mulligan draws more cards than the hand you keep, and the
 * surplus goes on the bottom of the library, chosen by the player, when the hand is kept - so
 * the player decides to keep or mulligan again after seeing all the cards.
 * <p>
 * The opening hand is {@code keep + 3} cards, keep being the starting hand size (ten, keep
 * seven); the n-th mulligan draws {@code max(keep, keep + 3 - floor((n - 1) / 2))} cards: with 7
 * that is 10, 10, 9, 9, 8, 8, then 7. The first two mulligans therefore cost nothing (see 10,
 * keep 7, as the opening hand did); {@code freeMulligans} is ignored.
 * <p>
 * A player may take at most {@link #MAX_MULLIGANS} mulligans: that is where London leaves a
 * player with no cards, and it keeps an AI that always mulligans (a land-only deck) from
 * looping forever - London's {@code openingHandSizes > 0} guard plays the same role.
 */
public class TenMulligan extends Mulligan {

    public static final int EXTRA_CARDS = 3;
    public static final int MAX_MULLIGANS = 7;

    private final Map<UUID, Integer> keepSizes = new HashMap<>();
    private final Map<UUID, Integer> takenMulligans = new HashMap<>();

    public TenMulligan(int freeMulligans) {
        super(freeMulligans);
    }

    TenMulligan(final TenMulligan mulligan) {
        super(mulligan);
        this.keepSizes.putAll(mulligan.keepSizes);
        this.takenMulligans.putAll(mulligan.takenMulligans);
    }

    /**
     * Cards drawn by the n-th mulligan (n from 1) when the kept hand is {@code keep} cards.
     */
    public static int cardsDrawn(int keep, int n) {
        return Math.max(keep, keep + EXTRA_CARDS - (n - 1) / 2);
    }

    /**
     * The opening hand itself is ten (keep + EXTRA_CARDS), kept down to seven like a mulligan's
     * hand is: the house rule starts with the big hand, it does not wait for a mulligan to show
     * it (the site's owner: « il me met 7 cartes au lancement de la partie alors que j'ai
     * sélectionné mulligan à 10 »). GameImpl deals that first hand with {@link #drawHand} before
     * {@link #executeMulliganPhase} runs, so the extra cards are added there, while no keep size
     * is recorded yet; a mulligan's redraw (a keep size recorded) draws exactly what it asks.
     */
    @Override
    public void drawHand(int numCards, Player player, Game game) {
        int extra = keepSizes.containsKey(player.getId()) ? 0 : EXTRA_CARDS;
        super.drawHand(numCards + extra, player, game);
    }

    /**
     * **Everybody mulligans at their own pace, and nobody waits for anybody.**
     *
     * The rules mulligan (103.4, and the base class) goes in ROUNDS: every
     * player declares, the round closes when the LAST declaration is in, only
     * then does anybody redraw, and round two asks again. It is simultaneous in
     * the sense the rule means — no player hears another's declaration before
     * making their own — and it still makes three people sit in front of a
     * screen while the fourth thinks about its first hand. The owner, who plays
     * this house rule precisely so that a table of four is not an evening of
     * waiting: « Fais le pour celui a 10 ».
     *
     * So, for THIS variant only: one thread per player, each running its own
     * mulligan from the first question to the cards that go under, at the speed
     * of the person in the chair. A player who keeps their first hand is done
     * while another is on their third. The phase ends when the last player has
     * kept — which is the only thing everybody still waits for, because the
     * game cannot start before it.
     *
     * What this trades away, said plainly: a player who keeps is announced
     * ("X keeps hand") while others are still deciding, so a late decider can
     * hear that three hands are already down. At a kitchen table that is what
     * happens anyway, and the house rule is the owner's to set. London
     * (`LondonMulligan`) keeps the rules' rounds, untouched.
     *
     * The questions run on those threads; **everything that touches the game —
     * the shuffle, the draw, the cards moved under, the end of a mulligan —
     * runs under one lock**, so the state is mutated by one thread at a time
     * whatever order the answers come back in.
     */
    @Override
    public void executeMulliganPhase(Game game, int startingHandSize) {
        List<UUID> players = new ArrayList<>();
        for (UUID playerId : game.getState().getPlayerList(game.getStartingPlayerId())) {
            keepSizes.put(playerId, startingHandSize);
            takenMulligans.put(playerId, 0);
            players.add(playerId);
        }
        if (players.size() < 2) {
            // One player: there is nobody to wait for, and the rounds of the
            // base class are the simpler path through the same thing.
            super.executeMulliganPhase(game, startingHandSize);
            return;
        }
        // Nobody in particular is "the choosing player" while everyone chooses.
        game.getState().setChoosingPlayerId(null);
        Object lock = new Object();
        List<Thread> running = new ArrayList<>();
        for (UUID playerId : players) {
            Player player = game.getPlayer(playerId);
            if (player == null) {
                continue;
            }
            // The name matters: `ThreadUtils.isRunGameThread` reads it, and
            // everything a player question touches insists on being on a game
            // thread ("GAME…"). A thread named anything else throws there.
            Thread own = new Thread(() -> ownMulligan(game, player, lock), ThreadUtils.THREAD_PREFIX_GAME + " mulligan " + player.getName());
            own.setDaemon(true);
            running.add(own);
        }
        for (Thread thread : running) {
            thread.start();
        }
        for (Thread thread : running) {
            try {
                thread.join();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        synchronized (lock) {
            game.saveState(false);
        }
    }

    /** One player's whole mulligan, from the first question to the cards that go under. */
    private void ownMulligan(Game game, Player player, Object lock) {
        UUID playerId = player.getId();
        try {
            while (true) {
                boolean mayMulligan;
                synchronized (lock) {
                    mayMulligan = mayAsk(game, player);
                }
                if (!mayMulligan) {
                    break;
                }
                // The question, off the lock: this is where the person thinks,
                // and holding the lock here is exactly the queue being removed.
                if (!player.chooseMulligan(game)) {
                    break;
                }
                synchronized (lock) {
                    game.informPlayers(player.getLogName() + " decides to take mulligan");
                    mulligan(game, playerId);
                }
            }
        } catch (Throwable error) {
            /*
             * A question that broke is a hand kept: never a table stuck here.
             *
             * But kept like any other hand — the surplus still goes under,
             * below. This used to end the mulligan right here, so a player
             * whose question threw started the game with every card they were
             * looking at (ten, not seven). `cardsToBottom` reads the hand as
             * it stands: a redraw that itself broke halfway leaves a short
             * hand, which owes nothing.
             */
        }
        // Kept. The surplus goes under — chosen off the lock, moved on it.
        // `askForBottom` never throws: a player who answers nothing has the
        // front of their hand taken, since a hand left too big stops the game.
        try {
            int owed;
            synchronized (lock) {
                game.informPlayers(player.getLogName() + " keeps hand");
                owed = cardsToBottom(game, playerId, true);
            }
            List<UUID> under = owed > 0 ? askForBottom(game, player, owed) : new ArrayList<>();
            synchronized (lock) {
                if (!under.isEmpty()) {
                    // `false`: the three cards go under shuffled among
                    // themselves. `true` would ask this player, card by card,
                    // for the order — a second question nobody wants.
                    player.putCardsOnBottomOfLibrary(new CardsImpl(under), game, null, false);
                }
            }
        } catch (Throwable error) {
            // Nothing more can be done for this hand; the table still goes on.
        } finally {
            synchronized (lock) {
                game.endMulligan(playerId);
            }
        }
    }

    /**
     * May this player be asked again? The hand must be one a mulligan can be
     * taken from, and something may have replaced the chance to take one.
     * Called under the lock: it fires events into the game.
     */
    private boolean mayAsk(Game game, Player player) {
        while (true) {
            if (!canTakeMulligan(game, player)) {
                return false;
            }
            GameEvent event = new GameEvent(GameEvent.EventType.CAN_TAKE_MULLIGAN, null, null, player.getId());
            if (!game.replaceEvent(event)) {
                game.fireEvent(event);
                return true;
            }
        }
    }

    private int keepSize(UUID playerId) {
        return keepSizes.getOrDefault(playerId, 7);
    }

    private int taken(UUID playerId) {
        return takenMulligans.getOrDefault(playerId, 0);
    }

    /**
     * The size of the next hand, shown by the question ("Mulligan for free, draw another 10 cards?").
     */
    @Override
    public int mulliganDownTo(Game game, UUID playerId) {
        return cardsDrawn(keepSize(playerId), taken(playerId) + 1);
    }

    @Override
    public boolean canTakeMulligan(Game game, Player player) {
        return super.canTakeMulligan(game, player) && taken(player.getId()) < MAX_MULLIGANS;
    }

    @Override
    public void mulligan(Game game, UUID playerId) {
        Player player = game.getPlayer(playerId);
        int n = taken(playerId) + 1;
        takenMulligans.put(playerId, n);
        int keep = keepSize(playerId);
        int draw = cardsDrawn(keep, n);
        player.getLibrary().addAll(player.getHand().getCards(game), game);
        player.getHand().clear();
        player.shuffleLibrary(null, game);
        game.informPlayers(player.getLogName() + " mulligans (" + n + (n == 1 ? "st" : n == 2 ? "nd" : n == 3 ? "rd" : "th")
                + "): draws " + draw + " cards" + (draw > keep ? ", keeps " + keep : ""));
        drawHand(draw, player, game);
    }

    /**
     * The hand is kept: the surplus goes on the bottom — chosen by the player,
     * at the same time as every other player who owes cards this round
     * (`Mulligan.chooseBottomTogether`), and moved before the keep is final.
     * The owner: « puis sélectionner les cartes qui partent en dessous, en meme
     * temps, et quand ils ont terminé, la main disparaît ».
     */
    @Override
    protected int cardsToBottom(Game game, UUID playerId, boolean kept) {
        if (!kept) {
            return 0;
        }
        Player player = game.getPlayer(playerId);
        return player == null ? 0 : Math.max(0, player.getHand().size() - keepSize(playerId));
    }

    @Override
    public void endMulligan(Game game, UUID playerId) {
        // Nothing left to do: the surplus went on the bottom before the keep.
    }

    @Override
    public TenMulligan copy() {
        return new TenMulligan(this);
    }
}

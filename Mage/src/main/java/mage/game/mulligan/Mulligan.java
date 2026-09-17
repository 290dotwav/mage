package mage.game.mulligan;

import mage.cards.CardsImpl;
import mage.constants.Outcome;
import mage.filter.FilterCard;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.players.Player;
import mage.target.Target;
import mage.target.common.TargetCardInHand;
import mage.util.ThreadUtils;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Mulligan implements Serializable {

    protected final int freeMulligans;
    protected final Map<UUID, Integer> usedFreeMulligans = new HashMap<>();

    Mulligan(int freeMulligans) {
        this.freeMulligans = freeMulligans;
    }

    Mulligan(final Mulligan mulligan) {
        super();
        this.freeMulligans = mulligan.freeMulligans;
        this.usedFreeMulligans.putAll(mulligan.usedFreeMulligans);
    }

    public void executeMulliganPhase(Game game, int startingHandSize) {
        /*
         * 103.4. Each player draws a number of cards equal to their starting hand size,
         * which is normally seven. (Some effects can modify a player’s starting hand size.)
         * A player who is dissatisfied with their initial hand may take a mulligan. First
         * the starting player declares whether they will take a mulligan. Then each other
         * player in turn order does the same. Once each player has made a declaration, all
         * players who decided to take mulligans do so at the same time. To take a mulligan,
         * a player shuffles their hand back into their library, then draws a new hand of one
         * fewer cards than they had before. If a player kept their hand of cards, those cards
         * become the player’s opening hand, and that player may not take any further mulligans.
         * This process is then repeated until no player takes a mulligan. (Note that if a
         * player’s hand size reaches zero cards, that player must keep that hand.)
         */
        List<UUID> keepPlayers = new ArrayList<>();
        List<UUID> mulliganPlayers = new ArrayList<>();
        do {
            mulliganPlayers.clear();
            /*
             * The declarations happen TOGETHER.
             *
             * The rule asks each player in turn order, and this used to do it
             * literally: the loop blocked on one player's answer before the
             * next was even asked, so at a table of four the last player sat
             * watching three other people think before his own hand was worth
             * looking at. The owner of this door: « le mulligan doit etre
             * indépendant des autres joueurs, chacun fait son truc de son côté
             * en meme temps que les autres et puis valide pour attendre les
             * autres joueurs. »
             *
             * What the rule protects — that no player mulligans before every
             * declaration is in — is kept exactly: the questions go out at
             * once, every answer is waited for, and only then does anybody
             * shuffle. What is lost is the small amount of information a later
             * player had from hearing an earlier one declare first, which at a
             * kitchen table nobody waits for either.
             *
             * Everything that touches the game — the replacement event, the
             * log, the mulligans themselves — stays on this thread, in turn
             * order. The only thing on the other threads is the question and
             * the wait for its answer, which is per-player state
             * (`prepareForResponse`/`waitForResponse` in HumanPlayer).
             */
            List<UUID> asking = new ArrayList<>();
            List<UUID> keptNow = new ArrayList<>();
            for (UUID playerId : game.getState().getPlayerList(game.getStartingPlayerId())) {
                if (keepPlayers.contains(playerId)) {
                    continue;
                }
                Player player = game.getPlayer(playerId);
                boolean ask = false;
                while (true) {
                    if (!canTakeMulligan(game, player)) {
                        break;
                    }
                    GameEvent event = new GameEvent(GameEvent.EventType.CAN_TAKE_MULLIGAN, null, null, playerId);
                    if (!game.replaceEvent(event)) {
                        game.fireEvent(event);
                        ask = true;
                        break;
                    }
                }
                if (ask) {
                    asking.add(playerId);
                } else {
                    // Nothing to ask: this hand is kept where it stands.
                    keptNow.add(playerId);
                    game.informPlayers(player.getLogName() + " keeps hand");
                }
            }

            Map<UUID, Boolean> takesMulligan = askTogether(game, asking);

            for (UUID playerId : asking) {
                Player player = game.getPlayer(playerId);
                if (Boolean.TRUE.equals(takesMulligan.get(playerId))) {
                    mulliganPlayers.add(playerId);
                    game.informPlayers(player.getLogName() + " decides to take mulligan");
                } else {
                    keptNow.add(playerId);
                    // Said here, as the declaration comes in, and not at the
                    // end of the round: while everybody declares at the same
                    // time this line is how the other players' screens know
                    // who they are still waiting on (the site reads it —
                    // xmage/table.ts, `mulliganSaid`). The keep itself is not
                    // final until the surplus has gone to the bottom, below.
                    game.informPlayers(player.getLogName() + " keeps hand");
                }
            }
            // The redraws, on this thread and in turn order. They ask nothing
            // any more: the cards that go to the bottom are chosen below, by
            // everybody at once.
            for (UUID mulliganPlayerId : mulliganPlayers) {
                mulligan(game, mulliganPlayerId);
            }
            /*
             * The cards that go to the bottom, TOGETHER — the other half of
             * the owner's rule: « Les 6 doivent pouvoir mulligan, puis valider
             * puis sélectionner les cartes qui partent en dessous, en meme
             * temps ». Who owes how many depends on the variant and on what
             * has just happened to that player: London trims right after a
             * redraw, the house rule "à 10" trims when the hand is kept.
             */
            Map<UUID, Integer> owed = new LinkedHashMap<>();
            for (UUID playerId : keptNow) {
                int n = cardsToBottom(game, playerId, true);
                if (n > 0) {
                    owed.put(playerId, n);
                }
            }
            for (UUID playerId : mulliganPlayers) {
                int n = cardsToBottom(game, playerId, false);
                if (n > 0) {
                    owed.put(playerId, n);
                }
            }
            Map<UUID, List<UUID>> bottoms = chooseBottomTogether(game, owed);
            /*
             * Chosen on their own threads, all at once; moved on this one.
             *
             * `anyOrder = false`, and that flag is the whole of the second
             * half of this bug. It does NOT mean "any order you like": true
             * means the PLAYER IS ASKED, card by card, for the order the cards
             * land in ("Select a card ORDER to put on the BOTTOM of your
             * library") — and asked HERE, on this thread, so the three players
             * who had just chosen their three cards at the same time were then
             * questioned one after the other, each waiting for the one before
             * to finish. Measured on the live door: the choices land together
             * at 8.3 s, and the ordering runs 9.1→10.0 s, 10.0→10.9 s,
             * 11.0→12.0 s, one seat at a time.
             *
             * That is both of the things the owner has been saying: « il m'a
             * demandé 2x de virer des cartes » — the choice, then the order —
             * and « c'est quoi que tu captes pas dans le mot simultané ? ».
             * The order three cards take at the bottom of a ninety-card
             * library is worth nobody's question, let alone a queue: they go
             * down shuffled among themselves, which is what the paper rule
             * does when nobody is watching.
             */
            for (Map.Entry<UUID, List<UUID>> chosen : bottoms.entrySet()) {
                Player player = game.getPlayer(chosen.getKey());
                if (player != null && !chosen.getValue().isEmpty()) {
                    player.putCardsOnBottomOfLibrary(new CardsImpl(chosen.getValue()), game, null, false);
                }
            }
            // And only now is a kept hand final: the surplus is gone from it.
            // (The line saying so was said as the declaration came in.)
            for (UUID playerId : keptNow) {
                game.endMulligan(playerId);
                keepPlayers.add(playerId);
            }
            game.saveState(false);
        } while (!mulliganPlayers.isEmpty());
    }

    /**
     * Ask every player at once whether they take a mulligan, and come back
     * when they have all answered.
     *
     * One thread per player, each doing nothing but `chooseMulligan` — which
     * fires that player's own question and waits on that player's own monitor.
     * A player who never answers is a player whose client has gone: their own
     * timer concedes them and the wait ends with it, so there is no wait here
     * that their server does not already end. A seat that answers at once (a
     * bot) simply returns first.
     */
    private Map<UUID, Boolean> askTogether(Game game, List<UUID> playerIds) {
        Map<UUID, Boolean> answers = new ConcurrentHashMap<>();
        if (playerIds.isEmpty()) {
            return answers;
        }
        if (playerIds.size() == 1) {
            UUID only = playerIds.get(0);
            game.getState().setChoosingPlayerId(only);
            answers.put(only, game.getPlayer(only).chooseMulligan(game));
            return answers;
        }
        // Nobody in particular is "the choosing player" while everyone chooses.
        game.getState().setChoosingPlayerId(null);
        List<Thread> asks = new ArrayList<>();
        for (UUID playerId : playerIds) {
            Player player = game.getPlayer(playerId);
            Thread ask = new Thread(() -> {
                try {
                    answers.put(playerId, player.chooseMulligan(game));
                } catch (Throwable error) {
                    // A question that broke is a hand kept: never a game stuck here.
                    answers.put(playerId, Boolean.FALSE);
                }
                /*
                 * That this player has answered — not what he answered.
                 *
                 * Everyone declares at the same time, so no declaration may be
                 * revealed before the last one is in: that is the whole point
                 * of 103.4, and saying "X keeps hand" early would hand the
                 * players still thinking the information the rule protects.
                 * But that somebody has put their hand down is a thing you see
                 * at a kitchen table, and it is what the other five screens
                 * need to name who they are still waiting on (the site reads
                 * this line — xmage/table.ts, `mulliganSaid`).
                 */
                game.informPlayers(player.getLogName() + " has decided");
                // The name matters: `ThreadUtils.isRunGameThread` reads it, and
                // everything a player question touches insists on being on a
                // game thread ("GAME…"). A thread named anything else throws
                // there, which is how the first attempt at this left every
                // table stuck at "GAME started" with no hand dealt.
            }, ThreadUtils.THREAD_PREFIX_GAME + " mulligan " + player.getName());
            ask.setDaemon(true);
            asks.add(ask);
            ask.start();
        }
        for (Thread ask : asks) {
            try {
                ask.join();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        for (UUID playerId : playerIds) {
            answers.putIfAbsent(playerId, Boolean.FALSE);
        }
        return answers;
    }

    /**
     * How many cards this player must put on the bottom of their library right
     * now: after their redraw ({@code kept} false — London), or as the price of
     * keeping the hand they are looking at ({@code kept} true — the house rule
     * "à 10"). Zero for a variant that asks for none, which is every other one.
     * <p>
     * It exists so the question can be asked away from the game thread: the
     * variants used to move the cards themselves, one player at a time, inside
     * a loop that blocked the whole table on each answer.
     */
    protected int cardsToBottom(Game game, UUID playerId, boolean kept) {
        return 0;
    }

    /**
     * Ask ONE player which cards go on the bottom, and come back with them.
     * Nothing here touches the game — it is a question and its answer — so it
     * may be asked from a thread of its own, which is the whole point: every
     * player who owes cards is asked at the same time, and (for the house rule
     * "à 10") each player is asked the moment THEY keep, without waiting for
     * anybody else's declaration.
     * <p>
     * A player who answers nothing — a client that went away, a question that
     * broke — has the choice made for them out of the front of their hand: a
     * hand left too big would stop the game.
     */
    protected List<UUID> askForBottom(Game game, Player player, int n) {
        List<UUID> cards = new ArrayList<>();
        try {
            // The wording is theirs, unchanged on purpose: the site reads
            // "(N more) to put on the bottom of your library" off the message
            // to show the pick on the opening-hand sheet (dialogs.ts,
            // `bottomPutCount`).
            Target target = new TargetCardInHand(n, n, new FilterCard("card (" + n + " more) to put on the bottom of your library"));
            player.chooseTarget(Outcome.Discard, target, null, game);
            cards.addAll(target.getTargets());
        } catch (Throwable error) {
            // fall through to the hand's own order below
        }
        if (cards.size() < n) {
            for (UUID cardId : player.getHand()) {
                if (cards.size() >= n) {
                    break;
                }
                if (!cards.contains(cardId)) {
                    cards.add(cardId);
                }
            }
        }
        return cards;
    }

    /**
     * Ask every player who owes cards to the bottom which ones, all at the same
     * time, and come back with the answers. Nothing here touches the game: the
     * cards are moved by the caller, on the game thread, in turn order.
     * <p>
     * Same shape as {@link #askTogether}, and for the same reason — one thread
     * per player, named as a game thread because everything a player question
     * touches insists on it. A player who answers nothing (a client that went
     * away, a question that broke) has the choice made for them: the first
     * cards of their hand, since a hand left too big would stop the game.
     */
    private Map<UUID, List<UUID>> chooseBottomTogether(Game game, Map<UUID, Integer> owed) {
        Map<UUID, List<UUID>> chosen = new LinkedHashMap<>();
        if (owed.isEmpty()) {
            return chosen;
        }
        Map<UUID, List<UUID>> answers = new ConcurrentHashMap<>();
        List<Thread> asks = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : owed.entrySet()) {
            UUID playerId = entry.getKey();
            int n = entry.getValue();
            Player player = game.getPlayer(playerId);
            if (player == null) {
                continue;
            }
            Runnable ask = () -> answers.put(playerId, askForBottom(game, player, n));
            if (owed.size() == 1) {
                // One player owing: no thread, and he is the choosing player as
                // he has always been.
                game.getState().setChoosingPlayerId(playerId);
                ask.run();
                continue;
            }
            Thread thread = new Thread(ask, ThreadUtils.THREAD_PREFIX_GAME + " bottom " + player.getName());
            thread.setDaemon(true);
            asks.add(thread);
        }
        if (!asks.isEmpty()) {
            // Nobody in particular is "the choosing player" while everyone chooses.
            game.getState().setChoosingPlayerId(null);
            for (Thread thread : asks) {
                thread.start();
            }
            for (Thread thread : asks) {
                try {
                    thread.join();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        // Turn order, whatever order the answers came back in.
        for (UUID playerId : owed.keySet()) {
            List<UUID> cards = answers.get(playerId);
            if (cards != null && !cards.isEmpty()) {
                chosen.put(playerId, cards);
            }
        }
        return chosen;
    }

    public abstract int mulliganDownTo(Game game, UUID playerId);

    public abstract void mulligan(Game game, UUID playerId);

    public abstract void endMulligan(Game game, UUID playerId);

    public abstract Mulligan copy();

    public boolean canTakeMulligan(Game game, Player player) {
        return !player.getHand().isEmpty();
    }

    public int getFreeMulligans() {
        return freeMulligans;
    }

    public void drawHand(int numCards, Player player, Game game){
        player.drawCards(numCards, null, game);

        // default hand sorting
        player.getHand().sortCards(game, new MulliganDefaultHandSorter());
    }
}

package mage.game.mulligan;

import mage.game.Game;
import mage.game.events.GameEvent;
import mage.players.Player;
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
                    game.endMulligan(playerId);
                    keepPlayers.add(playerId);
                    game.informPlayers(game.getPlayer(playerId).getLogName() + " keeps hand");
                }
            }

            Map<UUID, Boolean> takesMulligan = askTogether(game, asking);

            for (UUID playerId : asking) {
                Player player = game.getPlayer(playerId);
                if (Boolean.TRUE.equals(takesMulligan.get(playerId))) {
                    mulliganPlayers.add(playerId);
                    game.informPlayers(player.getLogName() + " decides to take mulligan");
                } else {
                    game.endMulligan(player.getId());
                    keepPlayers.add(playerId);
                    game.informPlayers(player.getLogName() + " keeps hand");
                }
            }
            for (UUID mulliganPlayerId : mulliganPlayers) {
                mulligan(game, mulliganPlayerId);
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

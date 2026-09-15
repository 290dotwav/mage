package mage.game.mulligan;

import mage.game.Game;
import mage.players.Player;

import java.util.HashMap;
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

    @Override
    public void executeMulliganPhase(Game game, int startingHandSize) {
        for (UUID playerId : game.getState().getPlayerList(game.getStartingPlayerId())) {
            keepSizes.put(playerId, startingHandSize);
            takenMulligans.put(playerId, 0);
        }
        super.executeMulliganPhase(game, startingHandSize);
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

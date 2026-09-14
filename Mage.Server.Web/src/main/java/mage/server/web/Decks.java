package mage.server.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mage.cards.Card;
import mage.cards.decks.Deck;
import mage.game.Table;
import mage.game.match.MatchPlayer;
import mage.server.managers.ManagerFactory;
import mage.view.TableClientMessage;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The "decks" frame: every seat's deck as printings, sent to a browser right after its
 * START_GAME so the site can fetch every card's picture before the table shows
 * (docs/XMAGE-WIRE.md). A browser only knows the decks it sent itself (its own, the bots
 * it seated); the opponents' were unknown to it, so their cards came late and the zoom
 * of a card never seen before was blank.
 * <pre>
 * { kind: "decks", gameId, tableId, decks: [ { player: "FG", cards: [ { name, set, number }... ] }... ] }
 * </pre>
 * Names and printings only, each printing once per deck, main deck and sideboard
 * together (in Commander the sideboard is the command zone) - nothing about order,
 * counts or which zone a card starts in. The decks are the match's ({@code MatchPlayer.getDeck()},
 * what the game is dealt from), the printings are the cards' ({@code Card.getExpansionSetCode()}
 * / {@code getCardNumber()}, the pair the GameView carries and the site's picture URL is built from).
 * Sent to every seat alike: the AI seats hear nothing (they have no socket).
 */
final class Decks {

    private Decks() {
    }

    /**
     * The frame for the table a START_GAME is about, or null when its table (or match) cannot
     * be found - then nothing is sent, the site fetches pictures as the cards show, as before.
     */
    static String frame(TableClientMessage started, UUID gameId, ManagerFactory managerFactory) {
        UUID tableId = started == null ? null : started.getCurrentTableId();
        if (tableId == null) {
            return null;
        }
        Table table = managerFactory.tableManager().getTable(tableId);
        if (table == null || table.getMatch() == null) {
            return null;
        }
        JsonArray decks = new JsonArray();
        for (MatchPlayer player : table.getMatch().getPlayers()) {
            Deck deck = player.getDeck();
            if (deck == null) {
                continue;
            }
            JsonObject one = new JsonObject();
            one.addProperty("player", player.getName());
            one.add("cards", cards(deck));
            decks.add(one);
        }
        JsonObject o = new JsonObject();
        o.addProperty("kind", "decks");
        o.addProperty("gameId", gameId == null ? null : gameId.toString());
        o.addProperty("tableId", tableId.toString());
        o.add("decks", decks);
        return Frames.GSON.toJson(o);
    }

    /** Main deck then sideboard, each (name, set, number) once, in the decks' own iteration order. */
    static JsonArray cards(Deck deck) {
        Set<String> seen = new LinkedHashSet<>();
        JsonArray out = new JsonArray();
        add(out, seen, deck.getCards());
        add(out, seen, deck.getSideboard());
        return out;
    }

    private static void add(JsonArray out, Set<String> seen, Collection<Card> cards) {
        if (cards == null) {
            return;
        }
        for (Card card : cards) {
            if (card == null) {
                continue;
            }
            String name = card.getName();
            String set = card.getExpansionSetCode();
            String number = card.getCardNumber();
            if (name == null || name.isEmpty()) {
                continue;
            }
            String key = name + "|" + set + "|" + number;
            if (!seen.add(key)) {
                continue;
            }
            JsonObject c = new JsonObject();
            c.addProperty("name", name);
            c.addProperty("set", set);
            c.addProperty("number", number);
            out.add(c);
        }
    }
}

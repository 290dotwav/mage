package mage.server.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mage.cards.Card;
import mage.cards.CardSetInfo;
import mage.cards.basiclands.Island;
import mage.cards.s.Shock;
import mage.constants.Rarity;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The decks frame of a table that plays a scenario: each seat's cards are the game's cards it owns (the scenario's),
 * not the deck it joined with. Mission 4's Rival casts a Tenth Edition Shock the site had never been told of, and the
 * stack drew it in English.
 */
class DecksTest {

    @Test
    void aScenarioSeatListsTheGamesCardsItOwnsEachPrintingOnce() {
        UUID rival = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        List<Card> game = Arrays.asList(
                new Shock(rival, new CardSetInfo("Shock", "10E", "232", Rarity.COMMON)),
                new Shock(rival, new CardSetInfo("Shock", "10E", "232", Rarity.COMMON)),
                new Island(rival, new CardSetInfo("Island", "10E", "368", Rarity.LAND)),
                new Shock(student, new CardSetInfo("Shock", "M21", "159", Rarity.COMMON)));
        JsonArray his = Decks.owned(game, rival);
        assertEquals(2, his.size());
        JsonObject shock = his.get(0).getAsJsonObject();
        assertEquals("Shock", shock.get("name").getAsString());
        assertEquals("10E", shock.get("set").getAsString());
        assertEquals("232", shock.get("number").getAsString());
        assertEquals("Island", his.get(1).getAsJsonObject().get("name").getAsString());
        assertEquals("M21", Decks.owned(game, student).get(0).getAsJsonObject().get("set").getAsString());
    }
}

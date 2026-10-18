package mage.server.web;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The cards a question hands over carry their owner: an opponent's hand under Duress is his,
 * not the asking player's; a card the game does not know keeps no such field.
 */
class DialogOwnersTest {

    @Test
    void eachHandedCardIsMarkedWithItsOwner() {
        UUID bolt = UUID.randomUUID();
        UUID bears = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        UUID opponent = UUID.randomUUID();
        JsonObject cards = new JsonObject();
        cards.add(bolt.toString(), new JsonObject());
        cards.add(bears.toString(), new JsonObject());
        cards.add(unknown.toString(), new JsonObject());
        cards.addProperty("not-a-card", 3);
        DialogOwners.mark(cards, id -> id.equals(unknown) ? null : opponent);
        assertEquals(opponent.toString(), cards.getAsJsonObject(bolt.toString()).get("ownerId").getAsString());
        assertEquals(opponent.toString(), cards.getAsJsonObject(bears.toString()).get("ownerId").getAsString());
        assertFalse(cards.getAsJsonObject(unknown.toString()).has("ownerId"));
    }

    @Test
    void noListIsNothingToMark() {
        DialogOwners.mark(null, id -> UUID.randomUUID());
        DialogOwners.mark(new JsonObject(), id -> UUID.randomUUID());
    }
}

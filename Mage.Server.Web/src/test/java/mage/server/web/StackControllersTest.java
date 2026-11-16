package mage.server.web;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A stack entry says who controls it and, for a spell, whose card it is: a card of one player
 * cast by another stays its owner's (the page draws it in the printing its owner chose).
 */
class StackControllersTest {

    @Test
    void aStolenSpellSaysItsOwnerBesideItsController() {
        UUID caster = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        JsonObject entry = new JsonObject();
        StackControllers.mark(entry, caster, owner);
        assertEquals(caster.toString(), entry.get("controllerId").getAsString());
        assertEquals(owner.toString(), entry.get("ownerId").getAsString());
    }

    @Test
    void anAbilityHasNoOwnerField() {
        UUID controller = UUID.randomUUID();
        JsonObject entry = new JsonObject();
        StackControllers.mark(entry, controller, null);
        assertEquals(controller.toString(), entry.get("controllerId").getAsString());
        assertFalse(entry.has("ownerId"));
    }

    @Test
    void nothingKnownWritesNothing() {
        JsonObject entry = new JsonObject();
        StackControllers.mark(entry, null, null);
        assertFalse(entry.has("controllerId"));
        assertFalse(entry.has("ownerId"));
    }
}

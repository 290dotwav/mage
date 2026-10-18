package mage.server.web;

import com.google.gson.JsonObject;
import mage.abilities.Ability;
import mage.abilities.AbilitiesImpl;
import mage.abilities.keyword.FlyingAbility;
import mage.abilities.keyword.ProtectionFromEverythingAbility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A player's protection from everything (702.16j, Teferi's Protection) is on his entry of the
 * view, so every screen can say it; a player without it carries no such field.
 */
class BoardStatesTest {

    @Test
    void aPlayerWithProtectionFromEverythingIsMarked() {
        JsonObject entry = new JsonObject();
        BoardStates.markProtection(entry, new AbilitiesImpl<Ability>(new ProtectionFromEverythingAbility()));
        assertTrue(entry.get("protectionFromEverything").getAsBoolean());
    }

    @Test
    void anyOtherPlayerIsNot() {
        JsonObject entry = new JsonObject();
        BoardStates.markProtection(entry, new AbilitiesImpl<Ability>());
        BoardStates.markProtection(entry, new AbilitiesImpl<Ability>(FlyingAbility.getInstance()));
        BoardStates.markProtection(entry, null);
        assertFalse(entry.has("protectionFromEverything"));
    }
}

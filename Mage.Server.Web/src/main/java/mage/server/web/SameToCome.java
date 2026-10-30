package mage.server.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.TriggeredAbility;
import mage.constants.AbilityType;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.GameImpl;
import mage.game.stack.StackAbility;
import mage.game.stack.StackObject;
import mage.server.managers.ManagerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * "Same choice for the next N" on the site starts at the real number of identical questions
 * still to come, the owner: « non faut mettre l'option max réelle ». A question asked of a
 * player for a triggered ability of his gets, in its GameClientMessage:
 * <pre>
 *   "sameToCome": 3   // identical triggers of his still to ask the same question after this one
 * </pre>
 * "Identical" is the site's rule (its {@code promptKey}): a triggered ability with the same rule
 * text from a source of the same name (two Blood Artists are one source). Read off the live
 * game, on the game thread:
 * <ul>
 *   <li>while the ability is put on the stack ({@code checkTriggered}: its targets, its mode),
 *   the ones still to come are his identical triggered abilities waiting to be put on the stack
 *   ({@code GameState.getTriggered}); the one asking was taken off that list and pushed;</li>
 *   <li>while it resolves ({@code GameImpl.getResolvingId} is the top of the stack), they are
 *   his identical abilities below it on the stack.</li>
 * </ul>
 * Left out when it cannot be told: the question is not about a triggered ability of his on top
 * of the stack (a spell, an activated ability, a replacement effect, a question put to him by
 * another player's object), or the frame was not built on the game thread.
 */
final class SameToCome {

    private SameToCome() {
    }

    static void enrich(JsonObject frame, UUID gameId, ManagerFactory managerFactory) {
        JsonElement data = frame == null ? null : frame.get("data");
        if (data == null || !data.isJsonObject()) {
            return;
        }
        JsonObject message = data.getAsJsonObject();
        JsonElement viewEl = message.get("gameView");
        if (viewEl == null || !viewEl.isJsonObject() || message.has("players")) {
            return; // a GAME_UPDATE, not a question
        }
        JsonElement meEl = viewEl.getAsJsonObject().get("myPlayerId");
        if (meEl == null || !meEl.isJsonPrimitive()) {
            return;
        }
        try {
            UUID me = UUID.fromString(meEl.getAsString());
            Game game = LiveGame.of(gameId, managerFactory);
            if (game == null) {
                return;
            }
            Integer n = count(game, me);
            if (n != null) {
                message.addProperty("sameToCome", n);
            }
        } catch (RuntimeException ignore) {
            // not on the game thread, the game changed under us: the count is left out
        }
    }

    /** The identical questions still to come after the one asked now, or null when it cannot be told. */
    static Integer count(Game game, UUID me) {
        StackObject top = game.getStack().peekFirst();
        if (!(top instanceof StackAbility) || !me.equals(top.getControllerId())) {
            return null;
        }
        Ability asking = ((StackAbility) top).getStackAbility();
        if (asking == null || asking.getAbilityType() != AbilityType.TRIGGERED_NONMANA) {
            return null;
        }
        String key = key(game, asking);
        if (key == null) {
            return null;
        }
        boolean resolving = game instanceof GameImpl && top.getId().equals(((GameImpl) game).getResolvingId());
        int n = 0;
        if (resolving) {
            Iterator<StackObject> below = game.getStack().iterator();
            below.next(); // the one resolving
            while (below.hasNext()) {
                StackObject o = below.next();
                if (o instanceof StackAbility && me.equals(o.getControllerId())
                        && key.equals(key(game, ((StackAbility) o).getStackAbility()))) {
                    n++;
                }
            }
        } else {
            List<TriggeredAbility> waiting = game.getState().getTriggered(me);
            for (TriggeredAbility t : waiting) {
                if (key.equals(key(game, t))) {
                    n++;
                }
            }
        }
        return n;
    }

    /** Source name and rule text: what makes two triggered abilities ask the same question. */
    private static String key(Game game, Ability ability) {
        if (ability == null || ability.getAbilityType() != AbilityType.TRIGGERED_NONMANA) {
            return null;
        }
        MageObject source = ability.getSourceObject(game);
        if (source == null) {
            source = game.getLastKnownInformation(ability.getSourceId(), Zone.BATTLEFIELD);
        }
        String name = source == null ? null : source.getName();
        if (name == null) {
            return null;
        }
        return name + "\u0000" + Objects.toString(ability.getRule(name), "");
    }
}

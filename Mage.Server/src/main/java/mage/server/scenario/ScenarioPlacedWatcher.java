package mage.server.scenario;

import mage.cards.Card;
import mage.constants.WatcherScope;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.players.Player;
import mage.util.RandomUtil;
import mage.watchers.Watcher;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The cards a dealt scenario places ({@link Scenario.Side#placed}): kept out of the library while
 * it is shuffled and the hands are drawn and kept, then, once the first turn has begun (its upkeep,
 * before anybody draws), each put at a place drawn among the first ones of its owner's library.
 */
public class ScenarioPlacedWatcher extends Watcher {

    private Map<UUID, Integer> within = new LinkedHashMap<>();
    private boolean done;

    public ScenarioPlacedWatcher(Map<UUID, Integer> within) {
        super(WatcherScope.GAME);
        // their copy() calls the constructor with nulls, then copies the fields
        if (within != null) {
            this.within.putAll(within);
        }
    }

    @Override
    public void watch(GameEvent event, Game game) {
        if (done || event.getType() != GameEvent.EventType.UPKEEP_STEP_PRE) {
            return;
        }
        done = true;
        for (Map.Entry<UUID, Integer> entry : within.entrySet()) {
            Card card = game.getCard(entry.getKey());
            Player owner = card == null ? null : game.getPlayer(card.getOwnerId());
            if (owner == null) {
                continue;
            }
            int place = 1 + RandomUtil.nextInt(Math.max(1, Math.min(entry.getValue(), owner.getLibrary().size() + 1)));
            owner.getLibrary().putCardToTopXPos(card, place, game);
        }
    }

    @Override
    public void reset() {
        // the game resets its watchers once the hands are kept: what is left to do stays
    }
}

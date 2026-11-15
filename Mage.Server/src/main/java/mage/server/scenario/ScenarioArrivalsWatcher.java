package mage.server.scenario;

import mage.constants.WatcherScope;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.game.permanent.PermanentImpl;
import mage.watchers.Watcher;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The permanents a scenario sets up as having just arrived, or tapped: put on the battlefield
 * before the game starts, they would count as controlled since the first turn began, and the
 * first untap step untaps them. Once that turn has begun (its upkeep), the first are given back
 * their summoning sickness (302.6) and the second are tapped again, once.
 */
public class ScenarioArrivalsWatcher extends Watcher {

    private Set<UUID> arrived = new HashSet<>();
    private Set<UUID> tapped = new HashSet<>();
    private boolean done;

    public ScenarioArrivalsWatcher(Set<UUID> arrived, Set<UUID> tapped) {
        super(WatcherScope.GAME);
        // their copy() calls the constructor with nulls, then copies the fields
        if (arrived != null) {
            this.arrived.addAll(arrived);
        }
        if (tapped != null) {
            this.tapped.addAll(tapped);
        }
    }

    @Override
    public void watch(GameEvent event, Game game) {
        if (done || event.getType() != GameEvent.EventType.UPKEEP_STEP_PRE) {
            return;
        }
        done = true;
        for (UUID id : arrived) {
            Permanent permanent = game.getPermanent(id);
            if (permanent instanceof PermanentImpl) {
                ((PermanentImpl) permanent).giveSummoningSickness();
            }
        }
        // the first untap step untapped them all: what was written tapped is tapped again
        for (UUID id : tapped) {
            Permanent permanent = game.getPermanent(id);
            if (permanent != null) {
                permanent.setTapped(true);
            }
        }
    }
}

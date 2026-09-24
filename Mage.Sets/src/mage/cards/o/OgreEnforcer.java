package mage.cards.o;

import mage.MageInt;
import mage.MageObjectReference;
import mage.abilities.Ability;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.effects.ReplacementEffectImpl;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.WatcherScope;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.watchers.Watcher;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Claude
 */
public final class OgreEnforcer extends CardImpl {

    public OgreEnforcer(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{3}{R}{R}");

        this.subtype.add(SubType.OGRE);
        this.power = new MageInt(4);
        this.toughness = new MageInt(4);

        // This creature can't be destroyed by lethal damage unless lethal damage dealt by a single source is marked on it.
        this.addAbility(new SimpleStaticAbility(new OgreEnforcerEffect()), new OgreEnforcerWatcher());
    }

    private OgreEnforcer(final OgreEnforcer card) {
        super(card);
    }

    @Override
    public OgreEnforcer copy() {
        return new OgreEnforcer(this);
    }
}

/**
 * The state-based action for lethal damage destroys the creature with no source (704.5g).
 * That destruction is replaced unless one single source has marked lethal damage on it,
 * or it was dealt damage by a source with deathtouch (704.5h).
 */
class OgreEnforcerEffect extends ReplacementEffectImpl {

    OgreEnforcerEffect() {
        super(Duration.WhileOnBattlefield, Outcome.Benefit);
        staticText = "{this} can't be destroyed by lethal damage unless lethal damage dealt by a single source is marked on it";
    }

    private OgreEnforcerEffect(final OgreEnforcerEffect effect) {
        super(effect);
    }

    @Override
    public OgreEnforcerEffect copy() {
        return new OgreEnforcerEffect(this);
    }

    @Override
    public boolean checksEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.DESTROY_PERMANENT;
    }

    @Override
    public boolean applies(GameEvent event, Ability source, Game game) {
        if (event.getSourceId() != null || !event.getTargetId().equals(source.getSourceId())) {
            return false;
        }
        Permanent permanent = game.getPermanent(source.getSourceId());
        if (permanent == null || permanent.isDeathtouched()) {
            return false;
        }
        int toughness = permanent.getToughness().getValue();
        if (toughness <= 0 || permanent.getDamage() < toughness) {
            // not the lethal damage state-based action
            return false;
        }
        return OgreEnforcerWatcher.getMaxDamageBySingleSource(permanent, game) < toughness;
    }

    @Override
    public boolean replaceEvent(GameEvent event, Ability source, Game game) {
        return true;
    }
}

class OgreEnforcerWatcher extends Watcher {

    // damage marked on a permanent, by source
    private final Map<MageObjectReference, Map<UUID, Integer>> damageBySource = new HashMap<>();

    OgreEnforcerWatcher() {
        super(WatcherScope.GAME);
    }

    @Override
    public void watch(GameEvent event, Game game) {
        if (event.getType() != GameEvent.EventType.DAMAGED_PERMANENT || event.getSourceId() == null) {
            return;
        }
        Permanent permanent = game.getPermanent(event.getTargetId());
        if (permanent == null) {
            return;
        }
        MageObjectReference mor = new MageObjectReference(permanent, game);
        if (permanent.getDamage() <= event.getAmount()) {
            // there was no damage marked before this one (it wore off or was removed)
            damageBySource.remove(mor);
        }
        damageBySource
                .computeIfAbsent(mor, x -> new HashMap<>())
                .merge(event.getSourceId(), event.getAmount(), Integer::sum);
    }

    @Override
    public void reset() {
        super.reset();
        damageBySource.clear();
    }

    static int getMaxDamageBySingleSource(Permanent permanent, Game game) {
        OgreEnforcerWatcher watcher = game.getState().getWatcher(OgreEnforcerWatcher.class);
        if (watcher == null) {
            return 0;
        }
        return watcher.damageBySource
                .getOrDefault(new MageObjectReference(permanent, game), new HashMap<>())
                .values()
                .stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }
}

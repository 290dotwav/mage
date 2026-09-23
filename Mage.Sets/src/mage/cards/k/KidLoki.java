package mage.cards.k;

import mage.MageInt;
import mage.MageObjectReference;
import mage.abilities.common.DrawNthCardTriggeredAbility;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.effects.common.continuous.GainAbilityControlledEffect;
import mage.abilities.effects.common.counter.AddCountersSourceEffect;
import mage.abilities.keyword.HexproofAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.WatcherScope;
import mage.counters.CounterType;
import mage.filter.common.FilterCreaturePermanent;
import mage.filter.predicate.ObjectSourcePlayer;
import mage.filter.predicate.ObjectSourcePlayerPredicate;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.watchers.Watcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Claude
 */
public final class KidLoki extends CardImpl {

    private static final FilterCreaturePermanent filter
            = new FilterCreaturePermanent("creature you control that you've put one or more +1/+1 counters on this turn");

    static {
        filter.add(KidLokiPredicate.instance);
    }

    public KidLoki(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{U}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.GOD);
        this.subtype.add(SubType.HERO);
        this.subtype.add(SubType.VILLAIN);
        this.power = new MageInt(1);
        this.toughness = new MageInt(1);

        // Each creature you control that you've put one or more +1/+1 counters on this turn has hexproof.
        this.addAbility(new SimpleStaticAbility(new GainAbilityControlledEffect(
                HexproofAbility.getInstance(), Duration.WhileOnBattlefield, filter
        ).setText("each creature you control that you've put one or more +1/+1 counters on this turn has hexproof")), new KidLokiWatcher());

        // Whenever you draw your second card each turn, put a +1/+1 counter on Kid Loki.
        this.addAbility(new DrawNthCardTriggeredAbility(
                new AddCountersSourceEffect(CounterType.P1P1.createInstance()), false, 2
        ));
    }

    private KidLoki(final KidLoki card) {
        super(card);
    }

    @Override
    public KidLoki copy() {
        return new KidLoki(this);
    }
}

enum KidLokiPredicate implements ObjectSourcePlayerPredicate<Permanent> {
    instance;

    @Override
    public boolean apply(ObjectSourcePlayer<Permanent> input, Game game) {
        return KidLokiWatcher.checkPermanent(input.getPlayerId(), input.getObject(), game);
    }
}

class KidLokiWatcher extends Watcher {

    // player -> permanents that player put one or more +1/+1 counters on this turn
    private final Map<UUID, Set<MageObjectReference>> map = new HashMap<>();

    KidLokiWatcher() {
        super(WatcherScope.GAME);
    }

    @Override
    public void watch(GameEvent event, Game game) {
        if (event.getType() != GameEvent.EventType.COUNTER_ADDED
                || event.getPlayerId() == null
                || !CounterType.P1P1.getName().equals(event.getData())) {
            return;
        }
        Permanent permanent = game.getPermanentOrLKIBattlefield(event.getTargetId());
        if (permanent == null) {
            permanent = game.getPermanentEntering(event.getTargetId());
        }
        if (permanent != null) {
            map.computeIfAbsent(event.getPlayerId(), x -> new HashSet<>())
                    .add(new MageObjectReference(permanent, game));
        }
    }

    @Override
    public void reset() {
        super.reset();
        map.clear();
    }

    static boolean checkPermanent(UUID playerId, Permanent permanent, Game game) {
        KidLokiWatcher watcher = game.getState().getWatcher(KidLokiWatcher.class);
        return watcher != null && watcher
                .map
                .getOrDefault(playerId, new HashSet<>())
                .contains(new MageObjectReference(permanent, game));
    }
}

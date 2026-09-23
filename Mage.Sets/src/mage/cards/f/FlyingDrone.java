package mage.cards.f;

import mage.MageInt;
import mage.MageObjectReference;
import mage.abilities.Ability;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.condition.Condition;
import mage.abilities.costs.CostAdjuster;
import mage.abilities.costs.common.TapSourceCost;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.common.DrawDiscardControllerEffect;
import mage.abilities.effects.common.InfoEffect;
import mage.abilities.hint.ConditionHint;
import mage.abilities.hint.Hint;
import mage.abilities.keyword.FlyingAbility;
import mage.abilities.keyword.VigilanceAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.SubType;
import mage.constants.WatcherScope;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.util.CardUtil;
import mage.watchers.Watcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Claude
 */
public final class FlyingDrone extends CardImpl {

    public FlyingDrone(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ARTIFACT, CardType.CREATURE}, "{1}{U}");

        this.subtype.add(SubType.ROBOT);
        this.subtype.add(SubType.SCOUT);
        this.power = new MageInt(1);
        this.toughness = new MageInt(2);

        // Flying
        this.addAbility(FlyingAbility.getInstance());

        // Vigilance
        this.addAbility(VigilanceAbility.getInstance());

        // {1}{U}, {T}: Draw a card, then discard a card. This ability costs {1}{U} less to activate if another creature with flying entered the battlefield under your control this turn.
        Ability ability = new SimpleActivatedAbility(new DrawDiscardControllerEffect(1, 1), new ManaCostsImpl<>("{1}{U}"));
        ability.addCost(new TapSourceCost());
        ability.addEffect(new InfoEffect("This ability costs {1}{U} less to activate if another creature " +
                "with flying entered the battlefield under your control this turn"));
        ability.setCostAdjuster(FlyingDroneAdjuster.instance);
        this.addAbility(ability.addHint(FlyingDroneCondition.getHint()), new FlyingDroneWatcher());
    }

    private FlyingDrone(final FlyingDrone card) {
        super(card);
    }

    @Override
    public FlyingDrone copy() {
        return new FlyingDrone(this);
    }
}

enum FlyingDroneCondition implements Condition {
    instance;
    private static final Hint hint = new ConditionHint(instance);

    public static Hint getHint() {
        return hint;
    }

    @Override
    public boolean apply(Game game, Ability source) {
        return FlyingDroneWatcher.check(source, game);
    }

    @Override
    public String toString() {
        return "another creature with flying entered the battlefield under your control this turn";
    }
}

enum FlyingDroneAdjuster implements CostAdjuster {
    instance;

    @Override
    public void reduceCost(Ability ability, Game game) {
        if (FlyingDroneCondition.instance.apply(game, ability)) {
            CardUtil.adjustCost(ability, new ManaCostsImpl<>("{1}{U}"), false);
        }
    }
}

class FlyingDroneWatcher extends Watcher {

    // player -> creatures with flying that entered under that player's control this turn
    private final Map<UUID, Set<MageObjectReference>> map = new HashMap<>();

    FlyingDroneWatcher() {
        super(WatcherScope.GAME);
    }

    @Override
    public void watch(GameEvent event, Game game) {
        if (event.getType() != GameEvent.EventType.ENTERS_THE_BATTLEFIELD) {
            return;
        }
        Permanent permanent = game.getPermanent(event.getTargetId());
        if (permanent != null
                && permanent.isCreature(game)
                && permanent.hasAbility(FlyingAbility.getInstance(), game)) {
            map.computeIfAbsent(permanent.getControllerId(), x -> new HashSet<>())
                    .add(new MageObjectReference(permanent, game));
        }
    }

    @Override
    public void reset() {
        super.reset();
        map.clear();
    }

    static boolean check(Ability source, Game game) {
        FlyingDroneWatcher watcher = game.getState().getWatcher(FlyingDroneWatcher.class);
        if (watcher == null) {
            return false;
        }
        return watcher.map
                .getOrDefault(source.getControllerId(), new HashSet<>())
                .stream()
                .anyMatch(mor -> !mor.refersTo(source.getSourceObject(game), game));
    }
}

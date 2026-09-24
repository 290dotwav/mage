package mage.cards.o;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.DiesSourceTriggeredAbility;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.condition.Condition;
import mage.abilities.costs.common.RemoveCountersSourceCost;
import mage.abilities.costs.common.TapSourceCost;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.ContinuousEffectImpl;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.DestroyTargetEffect;
import mage.abilities.keyword.FlashAbility;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Layer;
import mage.constants.Outcome;
import mage.constants.SubLayer;
import mage.constants.SubType;
import mage.constants.TargetController;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.counters.Counters;
import mage.filter.common.FilterCreaturePermanent;
import mage.filter.predicate.other.DamagedPlayerThisTurnPredicate;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.TargetPermanent;
import mage.target.targetpointer.FixedTarget;

import java.util.UUID;

/**
 * @author Claude
 */
public final class OtherworldlyEscort extends CardImpl {

    private static final FilterCreaturePermanent filter = new FilterCreaturePermanent("creature that dealt damage to you this turn");

    static {
        filter.add(new DamagedPlayerThisTurnPredicate(TargetController.YOU));
    }

    public OtherworldlyEscort(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{3}{W}");

        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.DETECTIVE);
        this.power = new MageInt(4);
        this.toughness = new MageInt(3);

        // Flash
        this.addAbility(FlashAbility.getInstance());

        // When this creature dies, if it's not a Spirit, return it to the battlefield under its owner's control with four charge counters on it. It's a Spirit Detective.
        this.addAbility(new DiesSourceTriggeredAbility(new OtherworldlyEscortEffect())
                .withInterveningIf(OtherworldlyEscortCondition.instance));

        // {1}{W}, {T}, Remove a charge counter from this creature: Destroy target creature that dealt damage to you this turn.
        Ability ability = new SimpleActivatedAbility(new DestroyTargetEffect(), new ManaCostsImpl<>("{1}{W}"));
        ability.addCost(new TapSourceCost());
        ability.addCost(new RemoveCountersSourceCost(CounterType.CHARGE.createInstance()));
        ability.addTarget(new TargetPermanent(filter));
        this.addAbility(ability);
    }

    private OtherworldlyEscort(final OtherworldlyEscort card) {
        super(card);
    }

    @Override
    public OtherworldlyEscort copy() {
        return new OtherworldlyEscort(this);
    }
}

enum OtherworldlyEscortCondition implements Condition {
    instance;

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent permanent = source.getSourcePermanentOrLKI(game);
        return permanent != null && !permanent.hasSubtype(SubType.SPIRIT, game);
    }

    @Override
    public String toString() {
        return "it's not a Spirit";
    }
}

class OtherworldlyEscortEffect extends OneShotEffect {

    OtherworldlyEscortEffect() {
        super(Outcome.PutCreatureInPlay);
        staticText = "return it to the battlefield under its owner's control with four charge counters on it. " +
                "It's a Spirit Detective. <i>(It's no longer a Human.)</i>";
    }

    private OtherworldlyEscortEffect(final OtherworldlyEscortEffect effect) {
        super(effect);
    }

    @Override
    public OtherworldlyEscortEffect copy() {
        return new OtherworldlyEscortEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Card card = game.getCard(source.getSourceId());
        if (card == null
                || game.getState().getZone(card.getId()) != Zone.GRAVEYARD
                || card.getZoneChangeCounter(game) != source.getStackMomentSourceZCC()) {
            return false;
        }
        Player owner = game.getPlayer(card.getOwnerId());
        if (owner == null) {
            return false;
        }
        Counters counters = new Counters();
        counters.addCounter(CounterType.CHARGE.createInstance(4));
        game.setEnterWithCounters(card.getId(), counters);
        // it's a Spirit Detective as it enters
        game.addEffect(new OtherworldlyEscortTypeEffect().setTargetPointer(
                new FixedTarget(card.getId(), card.getZoneChangeCounter(game) + 1)
        ), source);
        return owner.moveCards(card, Zone.BATTLEFIELD, source, game);
    }
}

class OtherworldlyEscortTypeEffect extends ContinuousEffectImpl {

    OtherworldlyEscortTypeEffect() {
        super(Duration.Custom, Layer.TypeChangingEffects_4, SubLayer.NA, Outcome.Neutral);
    }

    private OtherworldlyEscortTypeEffect(final OtherworldlyEscortTypeEffect effect) {
        super(effect);
    }

    @Override
    public OtherworldlyEscortTypeEffect copy() {
        return new OtherworldlyEscortTypeEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent permanent = game.getPermanent(getTargetPointer().getFirst(game, source));
        if (permanent == null) {
            discard();
            return false;
        }
        permanent.removeAllCreatureTypes(game);
        permanent.addSubType(game, SubType.SPIRIT, SubType.DETECTIVE);
        return true;
    }
}

package mage.cards.o;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldOrAttacksSourceTriggeredAbility;
import mage.abilities.effects.ContinuousEffect;
import mage.abilities.effects.ContinuousEffectImpl;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.LoseLifeSourceControllerEffect;
import mage.abilities.triggers.BeginningOfUpkeepTriggeredAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Layer;
import mage.constants.Outcome;
import mage.constants.SubLayer;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.counters.CounterType;
import mage.filter.common.FilterCreaturePermanent;
import mage.filter.predicate.Predicates;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.target.TargetPermanent;
import mage.target.targetadjustment.ForEachPlayerTargetsAdjuster;
import mage.target.targetpointer.FixedTarget;

import java.util.UUID;

/**
 * @author Claude
 */
public final class OlindaTheOblivious extends CardImpl {

    private static final FilterCreaturePermanent filter = new FilterCreaturePermanent("creature without an odor counter on it");

    static {
        filter.add(Predicates.not(CounterType.ODOR.getPredicate()));
    }

    public OlindaTheOblivious(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{3}{B}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.SKUNK);
        this.subtype.add(SubType.ROGUE);
        this.power = new MageInt(3);
        this.toughness = new MageInt(3);

        // Whenever Olinda enters or attacks, for each opponent, put an odor counter on up to one target creature that player controls without an odor counter on it. Each of those creatures has "At the beginning of your upkeep, you lose 2 life" for as long as it has an odor counter on it.
        Ability ability = new EntersBattlefieldOrAttacksSourceTriggeredAbility(new OlindaTheObliviousEffect());
        ability.addTarget(new TargetPermanent(0, 1, filter));
        ability.setTargetAdjuster(new ForEachPlayerTargetsAdjuster(false, true));
        this.addAbility(ability);
    }

    private OlindaTheOblivious(final OlindaTheOblivious card) {
        super(card);
    }

    @Override
    public OlindaTheOblivious copy() {
        return new OlindaTheOblivious(this);
    }
}

class OlindaTheObliviousEffect extends OneShotEffect {

    OlindaTheObliviousEffect() {
        super(Outcome.Detriment);
        staticText = "for each opponent, put an odor counter on up to one target creature that player controls "
                + "without an odor counter on it. Each of those creatures has \"At the beginning of your upkeep, "
                + "you lose 2 life\" for as long as it has an odor counter on it. "
                + "<i>(They continue to smell after Olinda has left the battlefield.)</i>";
    }

    private OlindaTheObliviousEffect(final OlindaTheObliviousEffect effect) {
        super(effect);
    }

    @Override
    public OlindaTheObliviousEffect copy() {
        return new OlindaTheObliviousEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        for (mage.target.Target target : source.getTargets()) {
            for (UUID targetId : target.getTargets()) {
                Permanent permanent = game.getPermanent(targetId);
                if (permanent == null || permanent.getCounters(game).getCount(CounterType.ODOR) > 0) {
                    continue;
                }
                permanent.addCounters(CounterType.ODOR.createInstance(), source, game);
                if (permanent.getCounters(game).getCount(CounterType.ODOR) < 1) {
                    continue;
                }
                ContinuousEffect effect = new OlindaTheObliviousSmellEffect();
                effect.setTargetPointer(new FixedTarget(permanent, game));
                game.addEffect(effect, source);
            }
        }
        return true;
    }
}

class OlindaTheObliviousSmellEffect extends ContinuousEffectImpl {

    private final Ability smell;

    OlindaTheObliviousSmellEffect() {
        super(Duration.Custom, Layer.AbilityAddingRemovingEffects_6, SubLayer.NA, Outcome.Detriment);
        this.smell = new BeginningOfUpkeepTriggeredAbility(new LoseLifeSourceControllerEffect(2));
    }

    private OlindaTheObliviousSmellEffect(final OlindaTheObliviousSmellEffect effect) {
        super(effect);
        this.smell = effect.smell.copy();
    }

    @Override
    public OlindaTheObliviousSmellEffect copy() {
        return new OlindaTheObliviousSmellEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent permanent = game.getPermanent(getTargetPointer().getFirst(game, source));
        if (permanent == null || permanent.getCounters(game).getCount(CounterType.ODOR) < 1) {
            discard();
            return false;
        }
        permanent.addAbility(smell, source.getSourceId(), game);
        return true;
    }
}

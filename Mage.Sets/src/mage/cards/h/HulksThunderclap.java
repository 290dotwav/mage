package mage.cards.h;

import mage.abilities.Ability;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.DestroyTargetEffect;
import mage.abilities.keyword.BeholdAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.filter.FilterPermanent;
import mage.filter.StaticFilters;
import mage.filter.common.FilterCreaturePermanent;
import mage.filter.predicate.Predicates;
import mage.filter.predicate.other.AnotherTargetPredicate;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.target.TargetPermanent;
import mage.target.common.TargetControlledCreaturePermanent;
import mage.target.targetadjustment.TargetAdjuster;
import mage.target.targetpointer.ThirdTargetPointer;
import mage.util.CardUtil;

import java.util.UUID;

/**
 * @author Claude
 */
public final class HulksThunderclap extends CardImpl {

    private static final FilterPermanent filter = new FilterCreaturePermanent("another target creature");

    static {
        filter.add(new AnotherTargetPredicate(2));
    }

    public HulksThunderclap(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.SORCERY}, "{1}{G}");

        // As an additional cost to cast this spell, you may behold a Gamma creature.
        this.addAbility(new BeholdAbility(SubType.GAMMA));

        // Target creature you control deals damage equal to its power to another target creature. If this spell's additional cost was paid, destroy target noncreature artifact or noncreature enchantment.
        this.getSpellAbility().addEffect(new HulksThunderclapEffect());
        this.getSpellAbility().addEffect(new DestroyTargetEffect()
                .setTargetPointer(new ThirdTargetPointer())
                .setText("If this spell's additional cost was paid, destroy target noncreature artifact or noncreature enchantment"));
        this.getSpellAbility().addTarget(new TargetControlledCreaturePermanent().setTargetTag(1));
        this.getSpellAbility().addTarget(new TargetPermanent(filter).setTargetTag(2));
        this.getSpellAbility().setTargetAdjuster(HulksThunderclapAdjuster.instance);
    }

    private HulksThunderclap(final HulksThunderclap card) {
        super(card);
    }

    @Override
    public HulksThunderclap copy() {
        return new HulksThunderclap(this);
    }
}

/**
 * The third target only exists when the behold cost was paid (601.2c: targets are chosen after the additional costs are announced).
 */
enum HulksThunderclapAdjuster implements TargetAdjuster {
    instance;

    private static final FilterPermanent filter = new FilterPermanent("noncreature artifact or noncreature enchantment");

    static {
        filter.add(Predicates.not(CardType.CREATURE.getPredicate()));
        filter.add(Predicates.or(
                CardType.ARTIFACT.getPredicate(),
                CardType.ENCHANTMENT.getPredicate()
        ));
    }

    @Override
    public void adjustTargets(Ability ability, Game game) {
        if (ability.getTargets().size() < 3
                && CardUtil.checkSourceCostsTagExists(game, ability, BeholdAbility.BEHOLD_ACTIVATION_VALUE_KEY)) {
            ability.addTarget(new TargetPermanent(filter));
        }
    }
}

/**
 * Same as DamageWithPowerFromOneToAnotherTargetEffect, which insists on exactly two targets: this spell may have a third.
 */
class HulksThunderclapEffect extends OneShotEffect {

    HulksThunderclapEffect() {
        super(Outcome.Damage);
        staticText = "Target creature you control deals damage equal to its power to another target creature";
    }

    private HulksThunderclapEffect(final HulksThunderclapEffect effect) {
        super(effect);
    }

    @Override
    public HulksThunderclapEffect copy() {
        return new HulksThunderclapEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent myPermanent = game.getPermanent(source.getTargets().get(0).getFirstTarget());
        Permanent anotherPermanent = game.getPermanent(source.getTargets().get(1).getFirstTarget());
        if (myPermanent == null || anotherPermanent == null) {
            return false;
        }
        anotherPermanent.damage(myPermanent.getPower().getValue(), myPermanent.getId(), source, game);
        return true;
    }
}

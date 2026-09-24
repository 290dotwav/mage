package mage.cards.a;

import mage.abilities.Ability;
import mage.abilities.common.ActivateIfConditionActivatedAbility;
import mage.abilities.common.delayed.AtTheBeginOfNextEndStepDelayedTriggeredAbility;
import mage.abilities.condition.InvertCondition;
import mage.abilities.condition.common.BeforeAttackersAreDeclaredCondition;
import mage.abilities.condition.common.TargetAttackedThisTurnCondition;
import mage.abilities.costs.Cost;
import mage.abilities.costs.common.TapSourceCost;
import mage.abilities.costs.mana.GenericManaCost;
import mage.abilities.effects.ContinuousEffect;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.DestroyTargetEffect;
import mage.abilities.effects.common.combat.AttacksIfAbleTargetEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.TargetController;
import mage.filter.common.FilterCreaturePermanent;
import mage.filter.predicate.Predicates;
import mage.filter.predicate.permanent.ControlledFromStartOfControllerTurnPredicate;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.TargetPermanent;
import mage.target.targetpointer.FixedTarget;

import java.util.UUID;

/**
 * @author Claude
 */
public final class ArcumsWhistle extends CardImpl {

    private static final FilterCreaturePermanent filter = new FilterCreaturePermanent(
            "non-Wall creature the active player has controlled continuously since the beginning of the turn"
    );

    static {
        filter.add(Predicates.not(SubType.WALL.getPredicate()));
        filter.add(new ControlledFromStartOfControllerTurnPredicate());
        filter.add(TargetController.ACTIVE.getControllerPredicate());
    }

    public ArcumsWhistle(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ARTIFACT}, "{3}");

        // {3}, {T}: Choose target non-Wall creature the active player has controlled continuously since the beginning of the turn. That player may pay {X}, where X is that creature's mana value. If they don't pay, the creature attacks this turn if able, and at the beginning of the next end step, destroy it if it didn't attack this turn. Activate only before attackers are declared.
        Ability ability = new ActivateIfConditionActivatedAbility(
                new ArcumsWhistleEffect(), new GenericManaCost(3), BeforeAttackersAreDeclaredCondition.instance
        );
        ability.addCost(new TapSourceCost());
        ability.addTarget(new TargetPermanent(filter));
        this.addAbility(ability);
    }

    private ArcumsWhistle(final ArcumsWhistle card) {
        super(card);
    }

    @Override
    public ArcumsWhistle copy() {
        return new ArcumsWhistle(this);
    }
}

class ArcumsWhistleEffect extends OneShotEffect {

    ArcumsWhistleEffect() {
        super(Outcome.Detriment);
        staticText = "choose target non-Wall creature the active player has controlled continuously since the " +
                "beginning of the turn. That player may pay {X}, where X is that creature's mana value. If they " +
                "don't pay, the creature attacks this turn if able, and at the beginning of the next end step, " +
                "destroy it if it didn't attack this turn";
    }

    private ArcumsWhistleEffect(final ArcumsWhistleEffect effect) {
        super(effect);
    }

    @Override
    public ArcumsWhistleEffect copy() {
        return new ArcumsWhistleEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent creature = game.getPermanent(getTargetPointer().getFirst(game, source));
        if (creature == null) {
            return false;
        }
        Player player = game.getPlayer(game.getActivePlayerId());
        if (player == null) {
            return false;
        }
        int manaValue = creature.getManaValue();
        Cost cost = new GenericManaCost(manaValue);
        if (player.chooseUse(Outcome.Benefit, "Pay {" + manaValue + "}? If you don't, "
                        + creature.getIdName() + " attacks this turn if able.", source, game)
                && cost.pay(source, game, source, player.getId(), false, null)) {
            return true;
        }
        ContinuousEffect effect = new AttacksIfAbleTargetEffect(Duration.EndOfTurn);
        effect.setTargetPointer(new FixedTarget(creature, game));
        game.addEffect(effect, source);
        DestroyTargetEffect destroyEffect = new DestroyTargetEffect();
        destroyEffect.setTargetPointer(new FixedTarget(creature, game));
        AtTheBeginOfNextEndStepDelayedTriggeredAbility delayedAbility = new AtTheBeginOfNextEndStepDelayedTriggeredAbility(
                destroyEffect, TargetController.ANY, new InvertCondition(TargetAttackedThisTurnCondition.instance)
        );
        delayedAbility.getTargets().addAll(source.getTargets());
        game.addDelayedTriggeredAbility(delayedAbility, source);
        return true;
    }
}

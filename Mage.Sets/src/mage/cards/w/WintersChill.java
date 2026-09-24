package mage.cards.w;

import mage.abilities.Ability;
import mage.abilities.common.CastOnlyIfConditionIsTrueAbility;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.common.delayed.AtTheEndOfCombatDelayedTriggeredAbility;
import mage.abilities.condition.Condition;
import mage.abilities.costs.Cost;
import mage.abilities.costs.CostAdjuster;
import mage.abilities.costs.mana.GenericManaCost;
import mage.abilities.effects.ContinuousEffect;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.DestroyTargetEffect;
import mage.abilities.effects.common.InfoEffect;
import mage.abilities.effects.common.PreventDamageByTargetEffect;
import mage.abilities.effects.common.PreventDamageToTargetEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.PhaseStep;
import mage.constants.SuperType;
import mage.constants.TurnPhase;
import mage.constants.Zone;
import mage.filter.StaticFilters;
import mage.filter.common.FilterControlledLandPermanent;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.TargetPermanent;
import mage.target.targetadjustment.XTargetsCountAdjuster;
import mage.target.targetpointer.FixedTarget;

import java.util.UUID;

/**
 * @author Claude
 */
public final class WintersChill extends CardImpl {

    public WintersChill(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.INSTANT}, "{X}{U}");

        // Cast this spell only during combat before blockers are declared.
        this.addAbility(new CastOnlyIfConditionIsTrueAbility(
                WintersChillCondition.instance, "cast this spell only during combat before blockers are declared"
        ));

        // X can't be greater than the number of snow lands you control.
        this.addAbility(new SimpleStaticAbility(
                Zone.ALL, new InfoEffect("X can't be greater than the number of snow lands you control")
        ).setRuleAtTheTop(true));
        this.getSpellAbility().setCostAdjuster(WintersChillCostAdjuster.instance);

        // Choose X target attacking creatures. For each of those creatures, its controller may pay {1} or {2}. If that player doesn't, destroy that creature at end of combat. If that player pays only {1}, prevent all combat damage that would be dealt to and dealt by that creature this combat.
        this.getSpellAbility().addEffect(new WintersChillEffect());
        this.getSpellAbility().addTarget(new TargetPermanent(StaticFilters.FILTER_ATTACKING_CREATURES));
        this.getSpellAbility().setTargetAdjuster(new XTargetsCountAdjuster());
    }

    private WintersChill(final WintersChill card) {
        super(card);
    }

    @Override
    public WintersChill copy() {
        return new WintersChill(this);
    }
}

enum WintersChillCondition implements Condition {
    instance;

    @Override
    public boolean apply(Game game, Ability source) {
        if (game.getTurnPhaseType() != TurnPhase.COMBAT || game.getStep() == null) {
            return false;
        }
        PhaseStep step = game.getStep().getType();
        return step == PhaseStep.BEGIN_COMBAT || step == PhaseStep.DECLARE_ATTACKERS;
    }

    @Override
    public String toString() {
        return "during combat before blockers are declared";
    }
}

enum WintersChillCostAdjuster implements CostAdjuster {
    instance;

    private static final FilterControlledLandPermanent filter = new FilterControlledLandPermanent("snow lands you control");

    static {
        filter.add(SuperType.SNOW.getPredicate());
    }

    @Override
    public void prepareX(Ability ability, Game game) {
        ability.setVariableCostsMinMax(0, game.getBattlefield().count(filter, ability.getControllerId(), ability, game));
    }
}

class WintersChillEffect extends OneShotEffect {

    WintersChillEffect() {
        super(Outcome.Detriment);
        staticText = "choose X target attacking creatures. For each of those creatures, its controller may pay {1} or {2}. " +
                "If that player doesn't, destroy that creature at end of combat. If that player pays only {1}, " +
                "prevent all combat damage that would be dealt to and dealt by that creature this combat";
    }

    private WintersChillEffect(final WintersChillEffect effect) {
        super(effect);
    }

    @Override
    public WintersChillEffect copy() {
        return new WintersChillEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        for (UUID targetId : getTargetPointer().getTargets(game, source)) {
            Permanent creature = game.getPermanent(targetId);
            if (creature == null) {
                continue;
            }
            Player player = game.getPlayer(creature.getControllerId());
            if (player == null) {
                continue;
            }
            if (pay(player, 2, creature, source, game)) {
                continue;
            }
            if (pay(player, 1, creature, source, game)) {
                ContinuousEffect effect = new PreventDamageToTargetEffect(Duration.EndOfCombat, true);
                effect.setTargetPointer(new FixedTarget(creature, game));
                game.addEffect(effect, source);
                effect = new PreventDamageByTargetEffect(Duration.EndOfCombat, true);
                effect.setTargetPointer(new FixedTarget(creature, game));
                game.addEffect(effect, source);
                continue;
            }
            AtTheEndOfCombatDelayedTriggeredAbility delayed = new AtTheEndOfCombatDelayedTriggeredAbility(
                    new DestroyTargetEffect().setTargetPointer(new FixedTarget(creature, game))
            );
            game.addDelayedTriggeredAbility(delayed, source);
        }
        return true;
    }

    private static boolean pay(Player player, int amount, Permanent creature, Ability source, Game game) {
        Cost cost = new GenericManaCost(amount);
        return cost.canPay(source, source, player.getId(), game)
                && player.chooseUse(Outcome.Benefit, "Pay {" + amount + "} for " + creature.getIdName() + '?'
                + (amount == 2 ? "" : " (otherwise it's destroyed at end of combat)"), source, game)
                && cost.pay(source, game, source, player.getId(), false, null);
    }
}

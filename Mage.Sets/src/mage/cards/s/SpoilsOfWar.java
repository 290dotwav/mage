package mage.cards.s;

import mage.abilities.Ability;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.costs.CostAdjuster;
import mage.abilities.costs.CostImpl;
import mage.abilities.costs.EarlyTargetCost;
import mage.abilities.dynamicvalue.common.GetXValue;
import mage.abilities.effects.common.InfoEffect;
import mage.abilities.effects.common.counter.DistributeCountersEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.Zone;
import mage.filter.FilterCard;
import mage.filter.predicate.Predicates;
import mage.game.Game;
import mage.players.Player;
import mage.target.Target;
import mage.target.common.TargetCreaturePermanentAmount;
import mage.target.common.TargetOpponent;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Claude
 */
public final class SpoilsOfWar extends CardImpl {

    public SpoilsOfWar(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.SORCERY}, "{X}{B}");

        // X is the number of artifact and/or creature cards in an opponent's graveyard as you cast this spell.
        this.addAbility(new SimpleStaticAbility(Zone.ALL, new InfoEffect(
                "X is the number of artifact and/or creature cards in an opponent's graveyard as you cast this spell"
        )).setRuleAtTheTop(true));
        this.getSpellAbility().addCost(new SpoilsOfWarChooseOpponentCost());
        this.getSpellAbility().setAdditionalCostsRuleVisible(false);
        this.getSpellAbility().setCostAdjuster(SpoilsOfWarCostAdjuster.instance);

        // Distribute X +1/+1 counters among any number of target creatures.
        this.getSpellAbility().addEffect(new DistributeCountersEffect());
        this.getSpellAbility().addTarget(new TargetCreaturePermanentAmount(GetXValue.instance));
    }

    private SpoilsOfWar(final SpoilsOfWar card) {
        super(card);
    }

    @Override
    public SpoilsOfWar copy() {
        return new SpoilsOfWar(this);
    }

    static final FilterCard filter = new FilterCard();

    static {
        filter.add(Predicates.or(
                CardType.ARTIFACT.getPredicate(),
                CardType.CREATURE.getPredicate()
        ));
    }

    static int countFor(Player opponent, Game game) {
        return opponent == null ? 0 : opponent.getGraveyard().count(filter, game);
    }
}

/**
 * The opponent is chosen as the spell is cast, before X is determined (and it's not a target).
 */
class SpoilsOfWarChooseOpponentCost extends CostImpl implements EarlyTargetCost {

    SpoilsOfWarChooseOpponentCost() {
        super();
        this.text = "";
    }

    private SpoilsOfWarChooseOpponentCost(final SpoilsOfWarChooseOpponentCost cost) {
        super(cost);
    }

    @Override
    public void chooseTarget(Game game, Ability source, Player controller) {
        Target target = new TargetOpponent(true);
        target.withChooseHint("X is the number of artifact and creature cards in their graveyard");
        controller.choose(Outcome.Benefit, target, source, game);
        addTarget(target);
    }

    @Override
    public boolean canPay(Ability ability, Ability source, UUID controllerId, Game game) {
        return true;
    }

    @Override
    public boolean pay(Ability ability, Game game, Ability source, UUID controllerId, boolean noMana, mage.abilities.costs.Cost costToPay) {
        paid = true;
        return true;
    }

    @Override
    public SpoilsOfWarChooseOpponentCost copy() {
        return new SpoilsOfWarChooseOpponentCost(this);
    }
}

enum SpoilsOfWarCostAdjuster implements CostAdjuster {
    instance;

    @Override
    public void prepareX(Ability ability, Game game) {
        SpoilsOfWarChooseOpponentCost cost = ability.getCosts().stream()
                .filter(SpoilsOfWarChooseOpponentCost.class::isInstance)
                .map(SpoilsOfWarChooseOpponentCost.class::cast)
                .findFirst()
                .orElse(null);
        if (cost == null || cost.getTargets().getFirstTarget() == null) {
            // possible X
            int min = game.getOpponents(ability.getControllerId(), true).stream()
                    .map(game::getPlayer)
                    .filter(Objects::nonNull)
                    .mapToInt(p -> SpoilsOfWar.countFor(p, game))
                    .min()
                    .orElse(0);
            int max = game.getOpponents(ability.getControllerId(), true).stream()
                    .map(game::getPlayer)
                    .filter(Objects::nonNull)
                    .mapToInt(p -> SpoilsOfWar.countFor(p, game))
                    .max()
                    .orElse(0);
            ability.setVariableCostsMinMax(min, max);
            return;
        }
        // real X
        ability.setVariableCostsValue(SpoilsOfWar.countFor(game.getPlayer(cost.getTargets().getFirstTarget()), game));
    }
}

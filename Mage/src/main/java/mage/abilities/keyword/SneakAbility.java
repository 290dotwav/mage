package mage.abilities.keyword;

import mage.MageIdentifier;
import mage.abilities.SpellAbility;
import mage.abilities.costs.common.ReturnToHandChosenControlledPermanentCost;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.cards.Card;
import mage.constants.PhaseStep;
import mage.constants.SpellAbilityType;
import mage.constants.TimingRule;
import mage.constants.Zone;
import mage.filter.common.FilterControlledPermanent;
import mage.filter.predicate.permanent.AttackingPredicate;
import mage.filter.predicate.permanent.UnblockedPredicate;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.target.common.TargetControlledPermanent;
import mage.util.CardUtil;

import java.util.Set;
import java.util.UUID;

/**
 * @author TheElk801
 */
public class SneakAbility extends SpellAbility {

    public static final String SNEAK_ACTIVATION_VALUE_KEY = "sneakActivation";
    private static final FilterControlledPermanent filter = new FilterControlledPermanent("unblocked attacker you control");

    static {
        filter.add(UnblockedPredicate.instance);
        filter.add(AttackingPredicate.instance);
    }

    public SneakAbility(Card card, String manaString) {
        super(card.getSpellAbility());
        this.newId();
        this.setCardName(card.getName() + " with Sneak");
        timing = TimingRule.INSTANT;
        zone = Zone.HAND;
        spellAbilityType = SpellAbilityType.BASE_ALTERNATE;

        this.clearManaCosts();
        this.clearManaCostsToPay();
        this.addCost(new ManaCostsImpl<>(manaString));
        this.addCost(new SneakReturnCost(new TargetControlledPermanent(filter)).setText(""));

        this.setRuleAtTheTop(true);
    }

    protected SneakAbility(final SneakAbility ability) {
        super(ability);
    }

    @Override
    public boolean activate(Game game, Set<MageIdentifier> allowedIdentifiers, boolean noMana) {
        if (!super.activate(game, allowedIdentifiers, noMana)
                || game.getStep().getType() != PhaseStep.DECLARE_BLOCKERS) {
            return false;
        }
        // 702.190b: the permanent it becomes enters attacking what the returned creature attacked,
        // so the tag keeps that defender (a player, a planeswalker or a battle); read by Spell.resolve.
        UUID defender = getCosts().stream()
                .filter(SneakReturnCost.class::isInstance)
                .map(c -> ((SneakReturnCost) c).getDefenderId())
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
        CardUtil.setSourceCostsTag(game, this, SNEAK_ACTIVATION_VALUE_KEY, defender);
        return true;
    }

    /**
     * 702.190b: the defender the sneaked permanent attacks, from the spell's costs tag; null when the spell
     * was not sneaked, or when the returned creature's defender was not known.
     */
    public static UUID getSneakDefender(Game game, mage.abilities.Ability spellAbility) {
        java.util.Map<String, Object> tags = CardUtil.getSourceCostsTagsMap(game, spellAbility);
        Object value = tags == null ? null : tags.get(SNEAK_ACTIVATION_VALUE_KEY);
        return value instanceof UUID ? (UUID) value : null;
    }

    @Override
    public SneakAbility copy() {
        return new SneakAbility(this);
    }

    @Override
    public String getRule() {
        StringBuilder sb = new StringBuilder("Sneak ");
        sb.append(getManaCosts().getText());
        sb.append(" <i>(You may cast this spell for ");
        sb.append(getManaCosts().getText());
        sb.append(" if you also return an unblocked attacker you control to hand during the declare blockers step.)</i>");
        return sb.toString();
    }
}

/**
 * Sneak's "return an unblocked attacker you control" (702.190a), keeping what that attacker attacked
 * before it leaves combat, for 702.190b.
 */
class SneakReturnCost extends ReturnToHandChosenControlledPermanentCost {

    private UUID defenderId;

    SneakReturnCost(TargetControlledPermanent target) {
        super(target);
    }

    private SneakReturnCost(final SneakReturnCost cost) {
        super(cost);
        this.defenderId = cost.defenderId;
    }

    @Override
    protected void addReturnTarget(Game game, Permanent permanent) {
        super.addReturnTarget(game, permanent);
        if (defenderId == null && game.getCombat() != null) {
            defenderId = game.getCombat().getDefenderId(permanent.getId());
        }
    }

    UUID getDefenderId() {
        return defenderId;
    }

    @Override
    public SneakReturnCost copy() {
        return new SneakReturnCost(this);
    }
}

package mage.abilities.common;

import mage.abilities.SpellAbility;
import mage.abilities.costs.common.TeamworkCost;
import mage.abilities.effects.Effect;
import mage.cards.Card;
import mage.constants.SpellAbilityType;
import mage.constants.TimingRule;
import mage.constants.Zone;
import mage.game.Game;
import mage.target.Target;

import java.util.UUID;

/**
 * "You may cast this spell as though it had flash if it's cast using teamwork."
 * <p>
 * An alternate way to cast the card with instant timing. When a spell is cast this way,
 * {@link mage.abilities.keyword.TeamworkAbility} makes its teamwork cost mandatory (no choice is offered),
 * so the spell is always cast using teamwork. The ability can only be activated if the teamwork cost can be paid.
 * <p>
 * Add it at the end of the card's constructor: it copies the effects and targets of the card's spell ability
 * (for an Aura: its enchant target and attach effect).
 *
 * @author Claude
 */
public class CastWithTeamworkAsThoughItHadFlashAbility extends SpellAbility {

    private final int teamworkAmount;

    public CastWithTeamworkAsThoughItHadFlashAbility(Card card, int teamworkAmount) {
        super(card.getSpellAbility().getManaCosts().copy(), card.getName(), Zone.HAND, SpellAbilityType.BASE_ALTERNATE);
        this.teamworkAmount = teamworkAmount;
        this.timing = TimingRule.INSTANT;
        for (Effect effect : card.getSpellAbility().getEffects()) {
            this.addEffect(effect.copy());
        }
        for (Target target : card.getSpellAbility().getTargets()) {
            this.addTarget(target.copy());
        }
    }

    protected CastWithTeamworkAsThoughItHadFlashAbility(final CastWithTeamworkAsThoughItHadFlashAbility ability) {
        super(ability);
        this.teamworkAmount = ability.teamworkAmount;
    }

    @Override
    public CastWithTeamworkAsThoughItHadFlashAbility copy() {
        return new CastWithTeamworkAsThoughItHadFlashAbility(this);
    }

    @Override
    public ActivationStatus canActivate(UUID playerId, Game game) {
        if (!new TeamworkCost(teamworkAmount).canPay(this, this, playerId, game)) {
            return ActivationStatus.getFalse();
        }
        return super.canActivate(playerId, game);
    }

    @Override
    public String getRule(boolean all) {
        return getRule();
    }

    @Override
    public String getRule() {
        return "You may cast this spell as though it had flash if it's cast using teamwork.";
    }
}

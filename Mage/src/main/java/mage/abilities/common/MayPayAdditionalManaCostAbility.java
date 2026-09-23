package mage.abilities.common;

import mage.abilities.Ability;
import mage.abilities.SpellAbility;
import mage.abilities.StaticAbility;
import mage.abilities.costs.OptionalAdditionalSourceCosts;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.constants.Outcome;
import mage.constants.Zone;
import mage.game.Game;
import mage.players.Player;
import mage.util.CardUtil;

/**
 * "As an additional cost to cast this spell, you may pay {1}{G} [any number of times]."
 * <p>
 * Not a kicker: the spell is never "kicked". The number of times the cost was paid
 * is kept as a costs tag on the spell and read with {@link #getTimesPaid}.
 *
 * @author Claude
 */
public class MayPayAdditionalManaCostAbility extends StaticAbility implements OptionalAdditionalSourceCosts {

    private final String manaCost;
    private final boolean repeatable;
    private final String costsTag;
    private final String rule;

    /**
     * @param manaCost   the additional mana cost, e.g. "{1}{G}"
     * @param repeatable true for "any number of times"
     * @param costsTag   unique key to store how many times it was paid
     * @param rule       rule text, or null to hide it (e.g. when another ability shows a combined text)
     */
    public MayPayAdditionalManaCostAbility(String manaCost, boolean repeatable, String costsTag, String rule) {
        super(Zone.STACK, null);
        this.manaCost = manaCost;
        this.repeatable = repeatable;
        this.costsTag = costsTag;
        this.rule = rule;
        this.setRuleAtTheTop(true);
        if (rule == null) {
            this.setRuleVisible(false);
        }
    }

    public MayPayAdditionalManaCostAbility(String manaCost, boolean repeatable, String costsTag) {
        this(manaCost, repeatable, costsTag, "As an additional cost to cast this spell, you may pay "
                + manaCost + (repeatable ? " any number of times" : "") + '.');
    }

    protected MayPayAdditionalManaCostAbility(final MayPayAdditionalManaCostAbility ability) {
        super(ability);
        this.manaCost = ability.manaCost;
        this.repeatable = ability.repeatable;
        this.costsTag = ability.costsTag;
        this.rule = ability.rule;
    }

    @Override
    public MayPayAdditionalManaCostAbility copy() {
        return new MayPayAdditionalManaCostAbility(this);
    }

    @Override
    public void addOptionalAdditionalCosts(Ability ability, Game game) {
        if (!(ability instanceof SpellAbility)) {
            return;
        }
        Player player = game.getPlayer(ability.getControllerId());
        if (player == null) {
            return;
        }
        int times = 0;
        while (player.canRespond()) {
            String message = "Pay " + manaCost + " as an additional cost"
                    + (repeatable ? " (" + (times + 1) + (times == 0 ? " time" : " times") + ")" : "") + '?';
            if (!new ManaCostsImpl<>(manaCost).canPay(ability, this, player.getId(), game)
                    || !player.chooseUse(Outcome.AIDontUseIt, message, ability, game)) {
                break;
            }
            ability.addManaCostsToPay(new ManaCostsImpl<>(manaCost));
            times++;
            if (!repeatable) {
                break;
            }
        }
        ability.setCostsTag(costsTag, times);
    }

    @Override
    public String getCastMessageSuffix() {
        return "";
    }

    @Override
    public String getRule() {
        return rule == null ? "" : rule;
    }

    /**
     * How many times the additional cost was paid for the spell of this ability (0 if never)
     */
    public static int getTimesPaid(Game game, Ability source, String costsTag) {
        return CardUtil.getSourceCostsTag(game, source, costsTag, 0);
    }
}

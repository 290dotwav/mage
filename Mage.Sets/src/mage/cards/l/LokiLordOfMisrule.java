package mage.cards.l;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.ActivateAsSorceryActivatedAbility;
import mage.abilities.costs.common.TapSourceCost;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.OneShotEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.target.common.TargetControlledCreaturePermanent;
import mage.util.functions.RemoveTypeCopyApplier;

import java.util.UUID;

/**
 * @author Claude
 */
public final class LokiLordOfMisrule extends CardImpl {

    public LokiLordOfMisrule(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{3}{U}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.GOD);
        this.subtype.add(SubType.SORCERER);
        this.subtype.add(SubType.VILLAIN);
        this.power = new MageInt(3);
        this.toughness = new MageInt(4);

        // {U}, {T}: Choose target creature you control. Each creature you control other than the chosen creature becomes a copy of that creature until end of turn, except it isn't legendary. Activate only as a sorcery.
        Ability ability = new ActivateAsSorceryActivatedAbility(new LokiLordOfMisruleEffect(), new ManaCostsImpl<>("{U}"));
        ability.addCost(new TapSourceCost());
        ability.addTarget(new TargetControlledCreaturePermanent());
        this.addAbility(ability);
    }

    private LokiLordOfMisrule(final LokiLordOfMisrule card) {
        super(card);
    }

    @Override
    public LokiLordOfMisrule copy() {
        return new LokiLordOfMisrule(this);
    }
}

class LokiLordOfMisruleEffect extends OneShotEffect {

    LokiLordOfMisruleEffect() {
        super(Outcome.Copy);
        staticText = "choose target creature you control. Each creature you control other than the chosen creature " +
                "becomes a copy of that creature until end of turn, except it isn't legendary";
    }

    private LokiLordOfMisruleEffect(final LokiLordOfMisruleEffect effect) {
        super(effect);
    }

    @Override
    public LokiLordOfMisruleEffect copy() {
        return new LokiLordOfMisruleEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent chosen = game.getPermanent(getTargetPointer().getFirst(game, source));
        if (chosen == null) {
            return false;
        }
        for (Permanent permanent : game.getBattlefield().getActivePermanents(
                StaticFilters.FILTER_CONTROLLED_CREATURE, source.getControllerId(), source, game)) {
            if (permanent.getId().equals(chosen.getId())) {
                continue;
            }
            game.copyPermanent(Duration.EndOfTurn, chosen, permanent.getId(), source,
                    new RemoveTypeCopyApplier(SuperType.LEGENDARY));
        }
        return true;
    }
}

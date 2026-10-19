package mage.cards.s;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.LoyaltyAbility;
import mage.abilities.common.EntersBattlefieldTriggeredAbility;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.effects.AsThoughEffectImpl;
import mage.abilities.effects.common.DamagePlayersEffect;
import mage.abilities.effects.common.GainLifeEffect;
import mage.abilities.effects.common.continuous.GainAbilityControlledEffect;
import mage.abilities.effects.keyword.EmpowerJaceEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.AsThoughEffectType;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.TargetController;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.permanent.Permanent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class SanctumLurker extends CardImpl {

    public SanctumLurker(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{B}");

        this.subtype.add(SubType.HORROR);
        this.power = new MageInt(3);
        this.toughness = new MageInt(2);

        // When this creature enters, empower Jace 1.
        this.addAbility(new EntersBattlefieldTriggeredAbility(new EmpowerJaceEffect(1)));

        // Planeswalkers you control aren't put into their owners' graveyards for having 0 loyalty.
        this.addAbility(new SimpleStaticAbility(new SanctumLurkerEffect()));

        // Planeswalkers you control have "[+2]: This planeswalker deals 1 damage to each opponent and you gain 1 life."
        LoyaltyAbility loyaltyAbility = new LoyaltyAbility(
                new DamagePlayersEffect(1, TargetController.OPPONENT)
                        .setText("this planeswalker deals 1 damage to each opponent"), 2
        );
        loyaltyAbility.addEffect(new GainLifeEffect(1).concatBy("and"));
        this.addAbility(new SimpleStaticAbility(new GainAbilityControlledEffect(
                loyaltyAbility, Duration.WhileOnBattlefield,
                StaticFilters.FILTER_CONTROLLED_PERMANENT_PLANESWALKER
        ).setText("planeswalkers you control have \"+2: This planeswalker deals 1 damage to each opponent and you gain 1 life.\"")));
    }

    private SanctumLurker(final SanctumLurker card) {
        super(card);
    }

    @Override
    public SanctumLurker copy() {
        return new SanctumLurker(this);
    }
}

class SanctumLurkerEffect extends AsThoughEffectImpl {

    SanctumLurkerEffect() {
        super(AsThoughEffectType.STAY_AT_ZERO_LOYALTY, Duration.WhileOnBattlefield, Outcome.Benefit);
        staticText = "planeswalkers you control aren't put into their owners' graveyards for having 0 loyalty";
    }

    private SanctumLurkerEffect(final SanctumLurkerEffect effect) {
        super(effect);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        return true;
    }

    @Override
    public SanctumLurkerEffect copy() {
        return new SanctumLurkerEffect(this);
    }

    @Override
    public boolean applies(UUID objectId, Ability source, UUID affectedControllerId, Game game) {
        Permanent permanent = game.getPermanent(objectId);
        return permanent != null
                && permanent.isPlaneswalker(game)
                && permanent.isControlledBy(source.getControllerId());
    }
}

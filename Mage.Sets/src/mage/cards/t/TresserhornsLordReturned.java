package mage.cards.t;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldTriggeredAbility;
import mage.abilities.common.SacrificePermanentTriggeredAbility;
import mage.abilities.costs.Cost;
import mage.abilities.costs.common.PayLifeCost;
import mage.abilities.costs.common.SacrificeTargetCost;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.DrawCardSourceControllerEffect;
import mage.abilities.keyword.IndestructibleAbility;
import mage.abilities.keyword.TrampleAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.common.TargetOpponent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class TresserhornsLordReturned extends CardImpl {

    public TresserhornsLordReturned(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{U}{B}{R}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.ZOMBIE);
        this.subtype.add(SubType.NOBLE);
        this.power = new MageInt(10);
        this.toughness = new MageInt(4);

        // Trample, indestructible
        this.addAbility(TrampleAbility.getInstance());
        this.addAbility(IndestructibleAbility.getInstance());

        // When Tresserhorn's Lord enters, sacrifice him unless you sacrifice three creatures, pay 3 life, and have target opponent draw three cards.
        Ability ability = new EntersBattlefieldTriggeredAbility(new TresserhornsLordReturnedEffect());
        ability.addTarget(new TargetOpponent());
        this.addAbility(ability);

        // Whenever you sacrifice a creature, draw a card.
        this.addAbility(new SacrificePermanentTriggeredAbility(
                new DrawCardSourceControllerEffect(1), StaticFilters.FILTER_PERMANENT_CREATURE
        ));
    }

    private TresserhornsLordReturned(final TresserhornsLordReturned card) {
        super(card);
    }

    @Override
    public TresserhornsLordReturned copy() {
        return new TresserhornsLordReturned(this);
    }
}

class TresserhornsLordReturnedEffect extends OneShotEffect {

    TresserhornsLordReturnedEffect() {
        super(Outcome.Sacrifice);
        staticText = "sacrifice him unless you sacrifice three creatures, pay 3 life, "
                + "and have target opponent draw three cards";
    }

    private TresserhornsLordReturnedEffect(final TresserhornsLordReturnedEffect effect) {
        super(effect);
    }

    @Override
    public TresserhornsLordReturnedEffect copy() {
        return new TresserhornsLordReturnedEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        Player opponent = game.getPlayer(getTargetPointer().getFirst(game, source));
        Cost sacrifice = new SacrificeTargetCost(3, StaticFilters.FILTER_PERMANENT_CREATURES);
        Cost life = new PayLifeCost(3);
        if (opponent != null
                && sacrifice.canPay(source, source, controller.getId(), game)
                && life.canPay(source, source, controller.getId(), game)
                && controller.chooseUse(Outcome.Benefit, "Sacrifice three creatures, pay 3 life and have "
                + opponent.getName() + " draw three cards? If you don't, sacrifice Tresserhorn's Lord.", source, game)) {
            sacrifice.clearPaid();
            life.clearPaid();
            if (sacrifice.pay(source, game, source, controller.getId(), false, null)
                    && life.pay(source, game, source, controller.getId(), false, null)) {
                opponent.drawCards(3, source, game);
                return true;
            }
        }
        Permanent permanent = source.getSourcePermanentIfItStillExists(game);
        if (permanent != null) {
            permanent.sacrifice(source, game);
        }
        return true;
    }
}

package mage.cards.s;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.common.delayed.ReflexiveTriggeredAbility;
import mage.abilities.costs.common.TapSourceCost;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.DrawCardSourceControllerEffect;
import mage.abilities.effects.common.counter.AddCountersTargetEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.counters.CounterType;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.players.Player;
import mage.target.TargetPermanent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class SevenOfNineTertiaryAdjunct extends CardImpl {

    public SevenOfNineTertiaryAdjunct(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ARTIFACT, CardType.CREATURE}, "{2}{U}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.BORG);
        this.power = new MageInt(1);
        this.toughness = new MageInt(4);

        // That Is Irrelevant -- {T}: Draw a card, then discard a card. When you discard a nonland card this way, put two charge counters on up to one target artifact.
        Ability ability = new SimpleActivatedAbility(new DrawCardSourceControllerEffect(1), new TapSourceCost());
        ability.addEffect(new SevenOfNineTertiaryAdjunctEffect());
        this.addAbility(ability.withFlavorWord("That Is Irrelevant"));
    }

    private SevenOfNineTertiaryAdjunct(final SevenOfNineTertiaryAdjunct card) {
        super(card);
    }

    @Override
    public SevenOfNineTertiaryAdjunct copy() {
        return new SevenOfNineTertiaryAdjunct(this);
    }
}

class SevenOfNineTertiaryAdjunctEffect extends OneShotEffect {

    SevenOfNineTertiaryAdjunctEffect() {
        super(Outcome.Benefit);
        staticText = ", then discard a card. When you discard a nonland card this way, "
                + "put two charge counters on up to one target artifact";
    }

    private SevenOfNineTertiaryAdjunctEffect(final SevenOfNineTertiaryAdjunctEffect effect) {
        super(effect);
    }

    @Override
    public SevenOfNineTertiaryAdjunctEffect copy() {
        return new SevenOfNineTertiaryAdjunctEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(source.getControllerId());
        if (player == null) {
            return false;
        }
        Card card = player.discardOne(false, false, source, game);
        if (card == null || card.isLand(game)) {
            return true;
        }
        ReflexiveTriggeredAbility ability = new ReflexiveTriggeredAbility(
                new AddCountersTargetEffect(CounterType.CHARGE.createInstance(2)), false
        );
        ability.addTarget(new TargetPermanent(0, 1, StaticFilters.FILTER_PERMANENT_ARTIFACT));
        game.fireReflexiveTriggeredAbility(ability, source);
        return true;
    }
}

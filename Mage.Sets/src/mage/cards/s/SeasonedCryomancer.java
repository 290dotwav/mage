package mage.cards.s;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldTriggeredAbility;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.common.delayed.ReflexiveTriggeredAbility;
import mage.abilities.costs.common.ExileSourceFromGraveCost;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.DrawCardSourceControllerEffect;
import mage.abilities.effects.common.TapTargetEffect;
import mage.abilities.effects.common.counter.AddCountersTargetEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.game.Game;
import mage.players.Player;
import mage.target.common.TargetCreaturePermanent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class SeasonedCryomancer extends CardImpl {

    public SeasonedCryomancer(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{1}{U}{U}");

        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.WIZARD);
        this.power = new MageInt(2);
        this.toughness = new MageInt(2);

        // When this creature enters, draw two cards, then discard two cards. When you discard one or more nonland cards this way, tap up to that many target creatures and put a stun counter on each of them.
        this.addAbility(new EntersBattlefieldTriggeredAbility(new SeasonedCryomancerEffect()));

        // {3}{U}{U}, Exile this card from your graveyard: Draw two cards.
        Ability ability = new SimpleActivatedAbility(
                Zone.GRAVEYARD, new DrawCardSourceControllerEffect(2), new ManaCostsImpl<>("{3}{U}{U}")
        );
        ability.addCost(new ExileSourceFromGraveCost());
        this.addAbility(ability);
    }

    private SeasonedCryomancer(final SeasonedCryomancer card) {
        super(card);
    }

    @Override
    public SeasonedCryomancer copy() {
        return new SeasonedCryomancer(this);
    }
}

class SeasonedCryomancerEffect extends OneShotEffect {

    SeasonedCryomancerEffect() {
        super(Outcome.Benefit);
        staticText = "draw two cards, then discard two cards. When you discard one or more nonland cards this way, "
                + "tap up to that many target creatures and put a stun counter on each of them";
    }

    private SeasonedCryomancerEffect(final SeasonedCryomancerEffect effect) {
        super(effect);
    }

    @Override
    public SeasonedCryomancerEffect copy() {
        return new SeasonedCryomancerEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(source.getControllerId());
        if (player == null) {
            return false;
        }
        player.drawCards(2, source, game);
        Cards discarded = player.discard(2, false, false, source, game);
        int nonland = discarded.count(mage.filter.StaticFilters.FILTER_CARD_NON_LAND, game);
        if (nonland < 1) {
            return true;
        }
        ReflexiveTriggeredAbility ability = new ReflexiveTriggeredAbility(
                new TapTargetEffect(), false,
                "tap up to that many target creatures and put a stun counter on each of them"
        );
        ability.addEffect(new AddCountersTargetEffect(CounterType.STUN.createInstance()));
        ability.addTarget(new TargetCreaturePermanent(0, nonland));
        game.fireReflexiveTriggeredAbility(ability, source);
        return true;
    }
}

package mage.cards.k;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.keyword.MenaceAbility;
import mage.abilities.triggers.BeginningOfEndStepTriggeredAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.counters.CounterType;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.common.TargetOpponent;
import mage.util.CardUtil;

import java.util.UUID;

/**
 * @author Claude
 */
public final class KurokiThiefOfTalents extends CardImpl {

    public KurokiThiefOfTalents(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{4}{B}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.SPIRIT);
        this.power = new MageInt(4);
        this.toughness = new MageInt(4);

        // Menace
        this.addAbility(new MenaceAbility());

        // At the beginning of your end step, target opponent may draw four cards. If they do, look at that player's hand and you may cast a spell from their hand without paying its mana cost. If they don't, put two +1/+1 counters on Kuroki.
        Ability ability = new BeginningOfEndStepTriggeredAbility(new KurokiThiefOfTalentsEffect());
        ability.addTarget(new TargetOpponent());
        this.addAbility(ability);
    }

    private KurokiThiefOfTalents(final KurokiThiefOfTalents card) {
        super(card);
    }

    @Override
    public KurokiThiefOfTalents copy() {
        return new KurokiThiefOfTalents(this);
    }
}

class KurokiThiefOfTalentsEffect extends OneShotEffect {

    KurokiThiefOfTalentsEffect() {
        super(Outcome.Benefit);
        staticText = "target opponent may draw four cards. If they do, look at that player's hand and you may cast "
                + "a spell from their hand without paying its mana cost. If they don't, put two +1/+1 counters on {this}";
    }

    private KurokiThiefOfTalentsEffect(final KurokiThiefOfTalentsEffect effect) {
        super(effect);
    }

    @Override
    public KurokiThiefOfTalentsEffect copy() {
        return new KurokiThiefOfTalentsEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        Player opponent = game.getPlayer(getTargetPointer().getFirst(game, source));
        if (controller == null || opponent == null) {
            return false;
        }
        if (opponent.chooseUse(Outcome.DrawCard, "Draw four cards? If you do, "
                + controller.getName() + " looks at your hand and may cast a spell from it for free. "
                + "If you don't, " + CardUtil.getSourceName(game, source) + " gets two +1/+1 counters.", source, game)) {
            opponent.drawCards(4, source, game);
            Cards hand = new CardsImpl(opponent.getHand());
            controller.lookAtCards(source, opponent.getName() + "'s hand", hand, game);
            CardUtil.castSpellWithAttributesForFree(controller, source, game, hand, StaticFilters.FILTER_CARD);
            return true;
        }
        Permanent permanent = source.getSourcePermanentIfItStillExists(game);
        if (permanent != null) {
            permanent.addCounters(CounterType.P1P1.createInstance(2), source, game);
        }
        return true;
    }
}

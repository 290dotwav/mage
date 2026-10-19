package mage.cards.n;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldTriggeredAbility;
import mage.abilities.condition.common.CastFromEverywhereSourceCondition;
import mage.abilities.condition.common.ThresholdCondition;
import mage.abilities.effects.OneShotEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.players.Player;
import mage.target.TargetCard;
import mage.target.common.TargetOpponent;
import mage.util.CardUtil;

import java.util.UUID;

/**
 * @author Claude
 */
public final class NullSummoner extends CardImpl {

    public NullSummoner(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{U}{B}");

        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.WARLOCK);
        this.power = new MageInt(4);
        this.toughness = new MageInt(2);

        // When this creature enters, if you cast it, target opponent reveals their hand. You choose a nonland card from it. Exile that card.
        // Threshold -- As long as there are seven or more cards in your graveyard, you may cast the exiled card, and mana of any type can be spent to cast that spell.
        Ability ability = new EntersBattlefieldTriggeredAbility(new NullSummonerEffect())
                .withInterveningIf(CastFromEverywhereSourceCondition.instance);
        ability.addTarget(new TargetOpponent());
        this.addAbility(ability);
    }

    private NullSummoner(final NullSummoner card) {
        super(card);
    }

    @Override
    public NullSummoner copy() {
        return new NullSummoner(this);
    }
}

class NullSummonerEffect extends OneShotEffect {

    NullSummonerEffect() {
        super(Outcome.Exile);
        staticText = "target opponent reveals their hand. You choose a nonland card from it. Exile that card.<br>"
                + "<i>Threshold</i> &mdash; As long as there are seven or more cards in your graveyard, "
                + "you may cast the exiled card, and mana of any type can be spent to cast that spell";
    }

    private NullSummonerEffect(final NullSummonerEffect effect) {
        super(effect);
    }

    @Override
    public NullSummonerEffect copy() {
        return new NullSummonerEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        Player opponent = game.getPlayer(getTargetPointer().getFirst(game, source));
        if (controller == null || opponent == null) {
            return false;
        }
        opponent.revealCards(source, opponent.getHand(), game);
        if (opponent.getHand().count(StaticFilters.FILTER_CARD_NON_LAND, game) < 1) {
            return true;
        }
        TargetCard target = new TargetCard(Zone.HAND, StaticFilters.FILTER_CARD_NON_LAND);
        controller.choose(Outcome.Exile, opponent.getHand(), target, source, game);
        Card card = opponent.getHand().get(target.getFirstTarget(), game);
        if (card == null) {
            return true;
        }
        controller.moveCardsToExile(card, source, game, true, CardUtil.getExileZoneId(game, source),
                CardUtil.getSourceName(game, source));
        if (game.getState().getZone(card.getId()) != Zone.EXILED) {
            return true;
        }
        CardUtil.makeCardPlayable(game, source, card, true, Duration.EndOfGame, true,
                controller.getId(), ThresholdCondition.instance);
        return true;
    }
}

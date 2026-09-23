package mage.cards.i;

import mage.MageIdentifier;
import mage.abilities.Ability;
import mage.abilities.costs.Cost;
import mage.abilities.costs.Costs;
import mage.abilities.costs.CostsImpl;
import mage.abilities.costs.common.PayLifeCost;
import mage.abilities.dynamicvalue.common.GetXValue;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.asthought.CanPlayCardControllerEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.Zone;
import mage.game.Game;
import mage.players.Player;
import mage.target.common.TargetOpponent;
import mage.util.CardUtil;

import java.util.UUID;

/**
 * @author Claude
 */
public final class InsideInformation extends CardImpl {

    public InsideInformation(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.SORCERY}, "{X}{B}{B}");

        // Exile the top X cards of target opponent's library. You may play those cards this turn. If you cast a spell this way, pay life equal to its mana value rather than pay its mana cost.
        this.getSpellAbility().addEffect(new InsideInformationEffect());
        this.getSpellAbility().addTarget(new TargetOpponent());
        this.getSpellAbility().setIdentifier(MageIdentifier.InsideInformationAlternateCast);
    }

    private InsideInformation(final InsideInformation card) {
        super(card);
    }

    @Override
    public InsideInformation copy() {
        return new InsideInformation(this);
    }
}

class InsideInformationEffect extends OneShotEffect {

    InsideInformationEffect() {
        super(Outcome.Benefit);
        staticText = "exile the top X cards of target opponent's library. You may play those cards this turn. " +
                "If you cast a spell this way, pay life equal to its mana value rather than pay its mana cost";
    }

    private InsideInformationEffect(final InsideInformationEffect effect) {
        super(effect);
    }

    @Override
    public InsideInformationEffect copy() {
        return new InsideInformationEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        Player opponent = game.getPlayer(getTargetPointer().getFirst(game, source));
        int xValue = GetXValue.instance.calculate(game, source, this);
        if (controller == null || opponent == null || xValue < 1) {
            return false;
        }
        Cards cards = new CardsImpl(opponent.getLibrary().getTopCards(game, xValue));
        if (cards.isEmpty()) {
            return false;
        }
        controller.moveCardsToExile(
                cards.getCards(game), source, game, true,
                CardUtil.getExileZoneId(game, source),
                CardUtil.getSourceName(game, source)
        );
        cards.retainZone(Zone.EXILED, game);
        for (Card card : cards.getCards(game)) {
            game.addEffect(new InsideInformationPlayEffect(game, card), source);
        }
        return true;
    }
}

class InsideInformationPlayEffect extends CanPlayCardControllerEffect {

    InsideInformationPlayEffect(Game game, Card card) {
        super(game, card.getMainCard().getId(), card.getZoneChangeCounter(game), false, Duration.EndOfTurn);
    }

    private InsideInformationPlayEffect(final InsideInformationPlayEffect effect) {
        super(effect);
    }

    @Override
    public InsideInformationPlayEffect copy() {
        return new InsideInformationPlayEffect(this);
    }

    @Override
    public boolean applies(UUID objectId, Ability source, UUID affectedControllerId, Game game) {
        if (!super.applies(objectId, source, affectedControllerId, game)) {
            return false;
        }
        Card cardToCheck = game.getCard(objectId);
        if (cardToCheck == null) {
            return false;
        }
        if (cardToCheck.isLand(game)) {
            // lands are played normally
            return true;
        }
        // allows to cast with the alternative life cost
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        Costs<Cost> newCosts = new CostsImpl<>();
        newCosts.add(new PayLifeCost(cardToCheck.getManaValue()));
        newCosts.addAll(cardToCheck.getSpellAbility().getCosts());
        controller.setCastSourceIdWithAlternateMana(
                cardToCheck.getId(), null, newCosts,
                MageIdentifier.InsideInformationAlternateCast
        );
        return true;
    }
}

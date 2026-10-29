package mage.cards.g;

import mage.abilities.Ability;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.AssimilateTargetEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.Zone;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.players.Player;
import mage.target.TargetCard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Claude
 */
public final class GiftOfPerfection extends CardImpl {

    public GiftOfPerfection(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.SORCERY}, "{4}{U}");

        // Each opponent reveals cards from the top of their library until they reveal a creature card. Assimilate one of the creature cards revealed this way. Then each opponent puts the other cards they revealed this way on the bottom of their library in a random order.
        this.getSpellAbility().addEffect(new GiftOfPerfectionEffect());
    }

    private GiftOfPerfection(final GiftOfPerfection card) {
        super(card);
    }

    @Override
    public GiftOfPerfection copy() {
        return new GiftOfPerfection(this);
    }
}

class GiftOfPerfectionEffect extends OneShotEffect {

    GiftOfPerfectionEffect() {
        super(Outcome.PutCreatureInPlay);
        staticText = "each opponent reveals cards from the top of their library until they reveal a creature card. "
                + "Assimilate one of the creature cards revealed this way. Then each opponent puts the other cards "
                + "they revealed this way on the bottom of their library in a random order. "
                + AssimilateTargetEffect.CARD_REMINDER;
    }

    private GiftOfPerfectionEffect(final GiftOfPerfectionEffect effect) {
        super(effect);
    }

    @Override
    public GiftOfPerfectionEffect copy() {
        return new GiftOfPerfectionEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        Map<UUID, Cards> revealedBy = new HashMap<>();
        Cards creatures = new CardsImpl();
        for (UUID opponentId : game.getOpponents(source.getControllerId())) {
            Player opponent = game.getPlayer(opponentId);
            if (opponent == null) {
                continue;
            }
            Cards revealed = new CardsImpl();
            for (Card card : opponent.getLibrary().getCards(game)) {
                revealed.add(card);
                if (card.isCreature(game)) {
                    creatures.add(card);
                    break;
                }
            }
            if (!revealed.isEmpty()) {
                opponent.revealCards(source, " (" + opponent.getName() + ")", revealed, game);
                revealedBy.put(opponentId, revealed);
            }
        }
        if (!creatures.isEmpty()) {
            TargetCard target = new TargetCard(Zone.LIBRARY, StaticFilters.FILTER_CARD_CREATURE);
            target.withNotTarget(true);
            target.withChooseHint("to assimilate");
            controller.choose(Outcome.PutCreatureInPlay, creatures, target, source, game);
            Card chosen = game.getCard(target.getFirstTarget());
            if (chosen != null) {
                AssimilateTargetEffect.assimilateCard(chosen, controller, source, game);
            }
        }
        for (Map.Entry<UUID, Cards> entry : revealedBy.entrySet()) {
            Player opponent = game.getPlayer(entry.getKey());
            Cards rest = entry.getValue();
            rest.retainZone(Zone.LIBRARY, game);
            if (opponent != null && !rest.isEmpty()) {
                opponent.putCardsOnBottomOfLibrary(rest, game, source, false);
            }
        }
        return true;
    }
}

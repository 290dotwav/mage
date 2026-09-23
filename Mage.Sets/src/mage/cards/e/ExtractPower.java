package mage.cards.e;

import mage.abilities.Ability;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.ExileFaceDownYouMayPlayAsLongAsExiledTargetEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.CardType;
import mage.constants.CastManaAdjustment;
import mage.constants.Outcome;
import mage.game.Game;
import mage.players.Player;
import mage.target.targetpointer.FixedTargets;

import java.util.UUID;

/**
 * @author Claude
 */
public final class ExtractPower extends CardImpl {

    public ExtractPower(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.SORCERY}, "{5}{U}");

        // Look at the top card of each player's library, then exile those cards face down. You may play them without paying their mana costs for as long as they remain exiled.
        this.getSpellAbility().addEffect(new ExtractPowerEffect());
    }

    private ExtractPower(final ExtractPower card) {
        super(card);
    }

    @Override
    public ExtractPower copy() {
        return new ExtractPower(this);
    }
}

class ExtractPowerEffect extends OneShotEffect {

    ExtractPowerEffect() {
        super(Outcome.PlayForFree);
        staticText = "look at the top card of each player's library, then exile those cards face down. " +
                "You may play them without paying their mana costs for as long as they remain exiled";
    }

    private ExtractPowerEffect(final ExtractPowerEffect effect) {
        super(effect);
    }

    @Override
    public ExtractPowerEffect copy() {
        return new ExtractPowerEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        Cards cards = new CardsImpl();
        for (UUID playerId : game.getState().getPlayersInRange(controller.getId(), game)) {
            Player player = game.getPlayer(playerId);
            if (player == null) {
                continue;
            }
            Card card = player.getLibrary().getFromTop(game);
            if (card != null) {
                cards.add(card);
            }
        }
        if (cards.isEmpty()) {
            return true;
        }
        controller.lookAtCards(source, null, cards, game);
        new ExileFaceDownYouMayPlayAsLongAsExiledTargetEffect(false, CastManaAdjustment.WITHOUT_PAYING_MANA_COST)
                .setTargetPointer(new FixedTargets(cards, game))
                .apply(game, source);
        return true;
    }
}

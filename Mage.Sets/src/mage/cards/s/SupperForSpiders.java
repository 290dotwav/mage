package mage.cards.s;

import mage.abilities.Ability;
import mage.abilities.effects.ContinuousEffectImpl;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.token.FoodAbility;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Layer;
import mage.constants.Outcome;
import mage.constants.SubLayer;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.targetpointer.FixedTarget;
import mage.watchers.common.CardsPutIntoGraveyardWatcher;

import java.util.Set;
import java.util.UUID;

/**
 * @author Claude
 */
public final class SupperForSpiders extends CardImpl {

    public SupperForSpiders(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.INSTANT}, "{1}{B}");

        // Put onto the battlefield under your control all creature cards in your opponents' graveyards that were put there from the battlefield this turn. They are Food artifacts with "{2}, {T}, Sacrifice this artifact: You gain 3 life." (They lose all other types and subtypes.)
        this.getSpellAbility().addEffect(new SupperForSpidersEffect());
        this.getSpellAbility().addWatcher(new CardsPutIntoGraveyardWatcher());
    }

    private SupperForSpiders(final SupperForSpiders card) {
        super(card);
    }

    @Override
    public SupperForSpiders copy() {
        return new SupperForSpiders(this);
    }
}

class SupperForSpidersEffect extends OneShotEffect {

    SupperForSpidersEffect() {
        super(Outcome.PutCreatureInPlay);
        staticText = "put onto the battlefield under your control all creature cards in your opponents' graveyards " +
                "that were put there from the battlefield this turn. They are Food artifacts with " +
                "\"{2}, {T}, Sacrifice this artifact: You gain 3 life.\" <i>(They lose all other types and subtypes.)</i>";
    }

    private SupperForSpidersEffect(final SupperForSpidersEffect effect) {
        super(effect);
    }

    @Override
    public SupperForSpidersEffect copy() {
        return new SupperForSpidersEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        CardsPutIntoGraveyardWatcher watcher = game.getState().getWatcher(CardsPutIntoGraveyardWatcher.class);
        if (controller == null || watcher == null) {
            return false;
        }
        Set<UUID> opponents = game.getOpponents(controller.getId());
        Cards cards = new CardsImpl();
        for (Card card : watcher.getCardsPutIntoGraveyardFromBattlefield(game)) {
            if (card != null
                    && opponents.contains(card.getOwnerId())
                    && game.getState().getZone(card.getId()) == Zone.GRAVEYARD
                    && card.isCreature(game)) {
                cards.add(card);
            }
        }
        if (cards.isEmpty()) {
            return false;
        }
        // they are Food artifacts as they enter
        for (Card card : cards.getCards(game)) {
            game.addEffect(new SupperForSpidersFoodEffect().setTargetPointer(
                    new FixedTarget(card.getId(), card.getZoneChangeCounter(game) + 1)
            ), source);
        }
        controller.moveCards(cards.getCards(game), Zone.BATTLEFIELD, source, game, false, false, false, null);
        return true;
    }
}

class SupperForSpidersFoodEffect extends ContinuousEffectImpl {

    SupperForSpidersFoodEffect() {
        super(Duration.Custom, Outcome.Neutral);
    }

    private SupperForSpidersFoodEffect(final SupperForSpidersFoodEffect effect) {
        super(effect);
    }

    @Override
    public SupperForSpidersFoodEffect copy() {
        return new SupperForSpidersFoodEffect(this);
    }

    @Override
    public boolean apply(Layer layer, SubLayer sublayer, Ability source, Game game) {
        Permanent permanent = game.getPermanent(getTargetPointer().getFirst(game, source));
        if (permanent == null) {
            discard();
            return false;
        }
        switch (layer) {
            case TypeChangingEffects_4:
                permanent.removeAllCardTypes(game);
                permanent.addCardType(game, CardType.ARTIFACT);
                permanent.removeAllSubTypes(game);
                permanent.addSubType(game, SubType.FOOD);
                return true;
            case AbilityAddingRemovingEffects_6:
                permanent.addAbility(new FoodAbility(), source.getSourceId(), game);
                return true;
        }
        return false;
    }

    @Override
    public boolean apply(Game game, Ability source) {
        return false;
    }

    @Override
    public boolean hasLayer(Layer layer) {
        return layer == Layer.TypeChangingEffects_4 || layer == Layer.AbilityAddingRemovingEffects_6;
    }
}

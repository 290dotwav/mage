package mage.cards.s;

import mage.abilities.Ability;
import mage.abilities.PlayLandAbility;
import mage.abilities.SpellAbility;
import mage.abilities.effects.AsThoughEffectImpl;
import mage.abilities.effects.ContinuousRuleModifyingEffectImpl;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.AsThoughEffectType;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.events.GameEvent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class ShamansTrance extends CardImpl {

    public ShamansTrance(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.INSTANT}, "{2}{R}");

        // Other players can't play lands or cast spells from their graveyards this turn.
        this.getSpellAbility().addEffect(new ShamansTranceRestrictionEffect());

        // You may play lands and cast spells from other players' graveyards this turn as though those cards were in your graveyard.
        this.getSpellAbility().addEffect(new ShamansTrancePlayEffect());
    }

    private ShamansTrance(final ShamansTrance card) {
        super(card);
    }

    @Override
    public ShamansTrance copy() {
        return new ShamansTrance(this);
    }
}

class ShamansTranceRestrictionEffect extends ContinuousRuleModifyingEffectImpl {

    ShamansTranceRestrictionEffect() {
        super(Duration.EndOfTurn, Outcome.Detriment);
        staticText = "other players can't play lands or cast spells from their graveyards this turn";
    }

    private ShamansTranceRestrictionEffect(final ShamansTranceRestrictionEffect effect) {
        super(effect);
    }

    @Override
    public ShamansTranceRestrictionEffect copy() {
        return new ShamansTranceRestrictionEffect(this);
    }

    @Override
    public boolean checksEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.PLAY_LAND
                || event.getType() == GameEvent.EventType.CAST_SPELL;
    }

    @Override
    public boolean applies(GameEvent event, Ability source, Game game) {
        if (source.isControlledBy(event.getPlayerId())) {
            return false;
        }
        Card card = game.getCard(event.getSourceId());
        if (card == null || !card.getMainCard().isOwnedBy(event.getPlayerId())) {
            return false;
        }
        Zone zone = event.getType() == GameEvent.EventType.CAST_SPELL && event.getZone() != null
                ? event.getZone()
                : game.getState().getZone(card.getMainCard().getId());
        return zone == Zone.GRAVEYARD;
    }
}

/**
 * The cards count as in your graveyard: they can also be cast with flashback and the like.
 */
class ShamansTrancePlayEffect extends AsThoughEffectImpl {

    ShamansTrancePlayEffect() {
        super(AsThoughEffectType.PLAY_FROM_NOT_OWN_HAND_ZONE, Duration.EndOfTurn, Outcome.PutCardInPlay);
        staticText = "you may play lands and cast spells from other players' graveyards this turn " +
                "as though those cards were in your graveyard";
    }

    private ShamansTrancePlayEffect(final ShamansTrancePlayEffect effect) {
        super(effect);
    }

    @Override
    public ShamansTrancePlayEffect copy() {
        return new ShamansTrancePlayEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        return true;
    }

    @Override
    public boolean applies(UUID objectId, Ability source, UUID affectedControllerId, Game game) {
        return false;
    }

    @Override
    public boolean applies(UUID objectId, Ability affectedAbility, Ability source, Game game, UUID playerId) {
        if (!source.isControlledBy(playerId)
                || !(affectedAbility instanceof SpellAbility || affectedAbility instanceof PlayLandAbility)) {
            return false;
        }
        Card card = game.getCard(objectId);
        if (card == null) {
            return false;
        }
        card = card.getMainCard();
        return game.getState().getZone(card.getId()) == Zone.GRAVEYARD
                && !card.isOwnedBy(playerId);
    }
}

package mage.cards.n;

import mage.abilities.Ability;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.condition.Condition;
import mage.abilities.costs.common.TapSourceCost;
import mage.abilities.costs.mana.GenericManaCost;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.triggers.BeginningOfUpkeepTriggeredAbility;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.TargetController;
import mage.constants.Zone;
import mage.filter.StaticFilters;
import mage.game.ExileZone;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.common.TargetCardInOpponentsGraveyard;
import mage.util.CardUtil;
import mage.util.RandomUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Claude
 */
public final class NegativeZonePortal extends CardImpl {

    public NegativeZonePortal(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ARTIFACT}, "{2}");

        // {2}, {T}: Exile target card from an opponent's graveyard. If it's a creature card, draw a card.
        Ability ability = new SimpleActivatedAbility(new NegativeZonePortalExileEffect(), new GenericManaCost(2));
        ability.addCost(new TapSourceCost());
        ability.addTarget(new TargetCardInOpponentsGraveyard(StaticFilters.FILTER_CARD));
        this.addAbility(ability);

        // At the beginning of your upkeep, if there are four or more creature cards exiled with this artifact, flip a coin. If you lose the flip, sacrifice this artifact and return a card exiled with it at random to its owner's hand.
        this.addAbility(new BeginningOfUpkeepTriggeredAbility(
                TargetController.YOU, new NegativeZonePortalFlipEffect(), false
        ).withInterveningIf(NegativeZonePortalCondition.instance));
    }

    private NegativeZonePortal(final NegativeZonePortal card) {
        super(card);
    }

    @Override
    public NegativeZonePortal copy() {
        return new NegativeZonePortal(this);
    }

    static ExileZone getExileZone(Game game, Ability source) {
        Permanent permanent = source.getSourcePermanentOrLKI(game);
        if (permanent == null) {
            return null;
        }
        return game.getExile().getExileZone(CardUtil.getExileZoneId(game, permanent.getId(), permanent.getZoneChangeCounter(game)));
    }
}

enum NegativeZonePortalCondition implements Condition {
    instance;

    @Override
    public boolean apply(Game game, Ability source) {
        ExileZone exileZone = NegativeZonePortal.getExileZone(game, source);
        return exileZone != null && exileZone.count(StaticFilters.FILTER_CARD_CREATURE, game) >= 4;
    }

    @Override
    public String toString() {
        return "there are four or more creature cards exiled with this artifact";
    }
}

class NegativeZonePortalExileEffect extends OneShotEffect {

    NegativeZonePortalExileEffect() {
        super(Outcome.Exile);
        staticText = "exile target card from an opponent's graveyard. If it's a creature card, draw a card";
    }

    private NegativeZonePortalExileEffect(final NegativeZonePortalExileEffect effect) {
        super(effect);
    }

    @Override
    public NegativeZonePortalExileEffect copy() {
        return new NegativeZonePortalExileEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        Card card = game.getCard(getTargetPointer().getFirst(game, source));
        Permanent permanent = source.getSourcePermanentOrLKI(game);
        if (controller == null || card == null || permanent == null) {
            return false;
        }
        // "If it's a creature card": the card as it was in the graveyard
        boolean creature = card.isCreature(game);
        controller.moveCardsToExile(
                card, source, game, true,
                CardUtil.getExileZoneId(game, permanent.getId(), permanent.getZoneChangeCounter(game)),
                CardUtil.getSourceName(game, source)
        );
        if (creature) {
            controller.drawCards(1, source, game);
        }
        return true;
    }
}

class NegativeZonePortalFlipEffect extends OneShotEffect {

    NegativeZonePortalFlipEffect() {
        super(Outcome.Benefit);
        staticText = "flip a coin. If you lose the flip, sacrifice this artifact and return a card exiled with it at random to its owner's hand";
    }

    private NegativeZonePortalFlipEffect(final NegativeZonePortalFlipEffect effect) {
        super(effect);
    }

    @Override
    public NegativeZonePortalFlipEffect copy() {
        return new NegativeZonePortalFlipEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null || controller.flipCoin(source, game, true)) {
            return true;
        }
        ExileZone exileZone = NegativeZonePortal.getExileZone(game, source);
        Permanent permanent = source.getSourcePermanentIfItStillExists(game);
        if (permanent != null) {
            permanent.sacrifice(source, game);
        }
        if (exileZone == null || exileZone.isEmpty()) {
            return true;
        }
        List<Card> cards = new ArrayList<>(exileZone.getCards(game));
        Card card = cards.get(RandomUtil.nextInt(cards.size()));
        Player owner = game.getPlayer(card.getOwnerId());
        if (owner != null) {
            owner.moveCards(card, Zone.HAND, source, game);
        }
        return true;
    }
}

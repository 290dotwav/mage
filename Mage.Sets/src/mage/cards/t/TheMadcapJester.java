package mage.cards.t;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.BatchTriggeredAbility;
import mage.abilities.TriggeredAbilityImpl;
import mage.abilities.common.AttacksTriggeredAbility;
import mage.abilities.effects.OneShotEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.events.ZoneChangeBatchEvent;
import mage.game.events.ZoneChangeEvent;
import mage.players.Player;
import mage.util.CardUtil;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Claude
 */
public final class TheMadcapJester extends CardImpl {

    public TheMadcapJester(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{1}{U}{B}{R}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.CLOWN);
        this.subtype.add(SubType.WIZARD);
        this.power = new MageInt(3);
        this.toughness = new MageInt(2);

        // Whenever The Madcap Jester attacks, each opponent exiles the top card of their library.
        this.addAbility(new AttacksTriggeredAbility(new TheMadcapJesterExileEffect()));

        // Whenever a spell or ability you control causes one or more cards to be exiled from an opponent's library, you may play those cards until the end of your next turn. Mana of any type can be spent to cast spells this way.
        this.addAbility(new TheMadcapJesterTriggeredAbility());
    }

    private TheMadcapJester(final TheMadcapJester card) {
        super(card);
    }

    @Override
    public TheMadcapJester copy() {
        return new TheMadcapJester(this);
    }
}

class TheMadcapJesterExileEffect extends OneShotEffect {

    TheMadcapJesterExileEffect() {
        super(Outcome.Exile);
        staticText = "each opponent exiles the top card of their library";
    }

    private TheMadcapJesterExileEffect(final TheMadcapJesterExileEffect effect) {
        super(effect);
    }

    @Override
    public TheMadcapJesterExileEffect copy() {
        return new TheMadcapJesterExileEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        for (UUID opponentId : game.getOpponents(source.getControllerId())) {
            Player opponent = game.getPlayer(opponentId);
            if (opponent == null) {
                continue;
            }
            Card card = opponent.getLibrary().getFromTop(game);
            if (card != null) {
                opponent.moveCards(card, Zone.EXILED, source, game);
            }
        }
        return true;
    }
}

class TheMadcapJesterTriggeredAbility extends TriggeredAbilityImpl implements BatchTriggeredAbility<ZoneChangeEvent> {

    TheMadcapJesterTriggeredAbility() {
        super(Zone.BATTLEFIELD, new TheMadcapJesterPlayEffect());
        setTriggerPhrase("Whenever a spell or ability you control causes one or more cards to be exiled "
                + "from an opponent's library, ");
    }

    private TheMadcapJesterTriggeredAbility(final TheMadcapJesterTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public TheMadcapJesterTriggeredAbility copy() {
        return new TheMadcapJesterTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.ZONE_CHANGE_BATCH;
    }

    @Override
    public boolean checkEvent(ZoneChangeEvent event, Game game) {
        if (event.getFromZone() != Zone.LIBRARY || event.getToZone() != Zone.EXILED) {
            return false;
        }
        Ability cause = event.getSource();
        if (cause == null || !cause.isControlledBy(getControllerId())) {
            return false;
        }
        Card card = game.getCard(event.getTargetId());
        return card != null && game.getOpponents(getControllerId()).contains(card.getOwnerId());
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        List<UUID> cardIds = getFilteredEvents((ZoneChangeBatchEvent) event, game)
                .stream()
                .map(ZoneChangeEvent::getTargetId)
                .collect(Collectors.toList());
        if (cardIds.isEmpty()) {
            return false;
        }
        getEffects().setValue("exiledCards", new CardsImpl(cardIds));
        return true;
    }
}

class TheMadcapJesterPlayEffect extends OneShotEffect {

    TheMadcapJesterPlayEffect() {
        super(Outcome.PlayForFree);
        staticText = "you may play those cards until the end of your next turn. "
                + "Mana of any type can be spent to cast spells this way";
    }

    private TheMadcapJesterPlayEffect(final TheMadcapJesterPlayEffect effect) {
        super(effect);
    }

    @Override
    public TheMadcapJesterPlayEffect copy() {
        return new TheMadcapJesterPlayEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Cards cards = (Cards) getValue("exiledCards");
        if (cards == null) {
            return false;
        }
        for (Card card : cards.getCards(game)) {
            if (game.getState().getZone(card.getId()) == Zone.EXILED) {
                CardUtil.makeCardPlayable(game, source, card, false, Duration.UntilEndOfYourNextTurn, true,
                        source.getControllerId(), null);
            }
        }
        return true;
    }
}

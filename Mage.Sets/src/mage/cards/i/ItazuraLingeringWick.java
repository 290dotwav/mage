package mage.cards.i;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.triggers.BeginningOfUpkeepTriggeredAbility;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.choices.Choice;
import mage.choices.ChoiceImpl;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.filter.FilterCard;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.players.Player;
import mage.target.TargetCard;
import mage.util.CardUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Claude
 */
public final class ItazuraLingeringWick extends CardImpl {

    public ItazuraLingeringWick(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{U}{R}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.SPIRIT);
        this.power = new MageInt(1);
        this.toughness = new MageInt(5);

        // At the beginning of your upkeep, exile the top three cards of your library. Each opponent secretly chooses a number 0 or greater. Then those numbers are revealed. Choose an opponent with the highest number. Itazura deals that much damage to them, then they may cast a spell from among those cards without paying its mana cost. You put a card from among them that wasn't cast this way into your hand.
        this.addAbility(new BeginningOfUpkeepTriggeredAbility(new ItazuraLingeringWickEffect()));
    }

    private ItazuraLingeringWick(final ItazuraLingeringWick card) {
        super(card);
    }

    @Override
    public ItazuraLingeringWick copy() {
        return new ItazuraLingeringWick(this);
    }
}

class ItazuraLingeringWickEffect extends OneShotEffect {

    ItazuraLingeringWickEffect() {
        super(Outcome.Benefit);
        staticText = "exile the top three cards of your library. Each opponent secretly chooses a number 0 or greater. "
                + "Then those numbers are revealed. Choose an opponent with the highest number. {this} deals that much "
                + "damage to them, then they may cast a spell from among those cards without paying its mana cost. "
                + "You put a card from among them that wasn't cast this way into your hand";
    }

    private ItazuraLingeringWickEffect(final ItazuraLingeringWickEffect effect) {
        super(effect);
    }

    @Override
    public ItazuraLingeringWickEffect copy() {
        return new ItazuraLingeringWickEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        Cards cards = new CardsImpl(controller.getLibrary().getTopCards(game, 3));
        controller.moveCardsToExile(cards.getCards(game), source, game, true,
                CardUtil.getExileZoneId(game, source), CardUtil.getSourceName(game, source));
        cards.retainZone(Zone.EXILED, game);

        // each opponent secretly chooses a number, then they are revealed
        Map<UUID, Integer> numbers = new LinkedHashMap<>();
        for (UUID opponentId : game.getOpponents(controller.getId())) {
            Player opponent = game.getPlayer(opponentId);
            if (opponent != null) {
                numbers.put(opponentId, opponent.getAmount(0, 1000, "Secretly choose a number (0 or greater)", source, game));
            }
        }
        int highest = -1;
        for (Map.Entry<UUID, Integer> entry : numbers.entrySet()) {
            game.informPlayers(game.getPlayer(entry.getKey()).getLogName() + " chose " + entry.getValue());
            highest = Math.max(highest, entry.getValue());
        }
        Player chosen = null;
        if (highest >= 0) {
            List<UUID> tied = new ArrayList<>();
            for (Map.Entry<UUID, Integer> entry : numbers.entrySet()) {
                if (entry.getValue() == highest) {
                    tied.add(entry.getKey());
                }
            }
            UUID chosenId = tied.get(0);
            if (tied.size() > 1) {
                Map<String, UUID> byName = new LinkedHashMap<>();
                for (UUID id : tied) {
                    byName.put(game.getPlayer(id).getName(), id);
                }
                Choice choice = new ChoiceImpl(true);
                choice.setMessage("Choose an opponent with the highest number");
                choice.setChoices(new LinkedHashSet<>(byName.keySet()));
                controller.choose(Outcome.Damage, choice, game);
                chosenId = byName.getOrDefault(choice.getChoice(), chosenId);
            }
            chosen = game.getPlayer(chosenId);
        }
        if (chosen != null) {
            game.informPlayers(controller.getLogName() + " chooses " + chosen.getLogName());
            if (highest > 0) {
                chosen.damage(highest, source.getSourceId(), source, game);
            }
            if (!cards.isEmpty()) {
                CardUtil.castSpellWithAttributesForFree(chosen, source, game, cards, StaticFilters.FILTER_CARD);
            }
        }
        cards.retainZone(Zone.EXILED, game);
        if (cards.isEmpty()) {
            return true;
        }
        TargetCard target = new TargetCard(Zone.EXILED, new FilterCard("card to put into your hand"));
        target.withNotTarget(true);
        target.setRequired(true);
        Card card;
        if (cards.size() == 1) {
            card = cards.getRandom(game);
        } else {
            controller.choose(Outcome.DrawCard, cards, target, source, game);
            card = game.getCard(target.getFirstTarget());
            if (card == null) {
                card = cards.getRandom(game);
            }
        }
        if (card != null) {
            controller.moveCards(card, Zone.HAND, source, game);
        }
        return true;
    }
}

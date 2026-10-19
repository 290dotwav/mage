package mage.cards.t;

import mage.MageInt;
import mage.MageObjectReference;
import mage.abilities.Ability;
import mage.abilities.common.DealsCombatDamageToAPlayerTriggeredAbility;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.keyword.ShadowAbility;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.WatcherScope;
import mage.constants.Zone;
import mage.filter.FilterCard;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.events.ZoneChangeEvent;
import mage.players.Player;
import mage.target.TargetCard;
import mage.watchers.Watcher;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author Claude
 */
public final class TheWeaverKing extends CardImpl {

    public TheWeaverKing(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{4}{U}{B}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HORROR);
        this.subtype.add(SubType.WIZARD);
        this.power = new MageInt(3);
        this.toughness = new MageInt(6);

        // Shadow
        this.addAbility(ShadowAbility.getInstance());

        // Whenever The Weaver King deals combat damage to a player, they mill that many cards. Then for each opponent, put a creature card from that player's graveyard that was put there from their library this turn onto the battlefield under your control.
        Ability ability = new DealsCombatDamageToAPlayerTriggeredAbility(new TheWeaverKingEffect(), false, true);
        ability.addWatcher(new TheWeaverKingWatcher());
        this.addAbility(ability);
    }

    private TheWeaverKing(final TheWeaverKing card) {
        super(card);
    }

    @Override
    public TheWeaverKing copy() {
        return new TheWeaverKing(this);
    }
}

class TheWeaverKingEffect extends OneShotEffect {

    TheWeaverKingEffect() {
        super(Outcome.PutCreatureInPlay);
        staticText = "they mill that many cards. Then for each opponent, put a creature card from that player's "
                + "graveyard that was put there from their library this turn onto the battlefield under your control";
    }

    private TheWeaverKingEffect(final TheWeaverKingEffect effect) {
        super(effect);
    }

    @Override
    public TheWeaverKingEffect copy() {
        return new TheWeaverKingEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        Player damaged = game.getPlayer(getTargetPointer().getFirst(game, source));
        Integer damage = (Integer) getValue("damage");
        // the zone change events of this very mill reach the watcher only after this effect, so they are kept here
        Cards milledNow = new CardsImpl();
        if (damaged != null && damage != null && damage > 0) {
            milledNow.addAll(damaged.millCards(damage, source, game));
        }
        milledNow.retainZone(Zone.GRAVEYARD, game);
        TheWeaverKingWatcher watcher = game.getState().getWatcher(TheWeaverKingWatcher.class);
        for (UUID opponentId : game.getOpponents(controller.getId())) {
            Player opponent = game.getPlayer(opponentId);
            if (opponent == null) {
                continue;
            }
            Cards choices = new CardsImpl();
            for (Card card : opponent.getGraveyard().getCards(game)) {
                if (card.isCreature(game) && (milledNow.contains(card.getId())
                        || (watcher != null && watcher.cameFromLibraryThisTurn(card, game)))) {
                    choices.add(card);
                }
            }
            if (choices.isEmpty()) {
                continue;
            }
            TargetCard target = new TargetCard(Zone.GRAVEYARD,
                    new FilterCard("creature card from " + opponent.getName() + "'s graveyard"));
            target.withNotTarget(true);
            target.setRequired(true);
            controller.choose(Outcome.PutCreatureInPlay, choices, target, source, game);
            Card card = game.getCard(target.getFirstTarget());
            if (card == null) {
                card = choices.getRandom(game);
            }
            if (card != null) {
                controller.moveCards(card, Zone.BATTLEFIELD, source, game);
            }
        }
        return true;
    }
}

/**
 * Cards put into a graveyard from a library this turn
 */
class TheWeaverKingWatcher extends Watcher {

    private final Set<MageObjectReference> fromLibrary = new HashSet<>();

    TheWeaverKingWatcher() {
        super(WatcherScope.GAME);
    }

    @Override
    public void watch(GameEvent event, Game game) {
        if (event.getType() != GameEvent.EventType.ZONE_CHANGE) {
            return;
        }
        ZoneChangeEvent zEvent = (ZoneChangeEvent) event;
        if (zEvent.getFromZone() != Zone.LIBRARY || zEvent.getToZone() != Zone.GRAVEYARD) {
            return;
        }
        Card card = game.getCard(event.getTargetId());
        if (card != null) {
            fromLibrary.add(new MageObjectReference(card, game, 0));
        }
    }

    boolean cameFromLibraryThisTurn(Card card, Game game) {
        return fromLibrary.contains(new MageObjectReference(card, game));
    }

    @Override
    public void reset() {
        super.reset();
        fromLibrary.clear();
    }
}

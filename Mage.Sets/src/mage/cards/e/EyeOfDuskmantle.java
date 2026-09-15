package mage.cards.e;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.costs.Cost;
import mage.abilities.costs.Costs;
import mage.abilities.costs.CostsImpl;
import mage.abilities.costs.common.PayLifeCost;
import mage.abilities.effects.AsThoughEffectImpl;
import mage.abilities.keyword.FlyingAbility;
import mage.abilities.keyword.LifelinkAbility;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.*;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.events.ZoneChangeEvent;
import mage.players.Player;
import mage.watchers.Watcher;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author ClaudeMTG
 */
public final class EyeOfDuskmantle extends CardImpl {

    public EyeOfDuskmantle(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{5}{B}{B}");

        this.subtype.add(SubType.EYE);
        this.power = new MageInt(3);
        this.toughness = new MageInt(8);

        // Flying
        this.addAbility(FlyingAbility.getInstance());

        // Lifelink
        this.addAbility(LifelinkAbility.getInstance());

        // You may play lands and cast spells from among cards in your graveyard you've surveilled this turn.
        // If you cast a spell this way, you pay life equal to its mana value rather than paying its mana cost.
        this.addAbility(new SimpleStaticAbility(new EyeOfDuskmantleEffect()), new EyeOfDuskmantleWatcher());
    }

    private EyeOfDuskmantle(final EyeOfDuskmantle card) {
        super(card);
    }

    @Override
    public EyeOfDuskmantle copy() {
        return new EyeOfDuskmantle(this);
    }
}

class EyeOfDuskmantleEffect extends AsThoughEffectImpl {

    EyeOfDuskmantleEffect() {
        super(AsThoughEffectType.PLAY_FROM_NOT_OWN_HAND_ZONE,
                Duration.WhileOnBattlefield, Outcome.AIDontUseIt); // AI will need help with this
        staticText = "You may play lands and cast spells from among cards in your graveyard "
                + "you've surveilled this turn. If you cast a spell this way, you pay life equal "
                + "to its mana value rather than paying its mana cost.";
    }

    private EyeOfDuskmantleEffect(final EyeOfDuskmantleEffect effect) {
        super(effect);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        return true;
    }

    @Override
    public EyeOfDuskmantleEffect copy() {
        return new EyeOfDuskmantleEffect(this);
    }

    @Override
    public boolean applies(UUID objectId, Ability source, UUID affectedControllerId, Game game) {
        throw new IllegalArgumentException("Wrong code usage: can't call applies method on empty affectedAbility");
    }

    @Override
    public boolean applies(UUID objectId, Ability affectedAbility, Ability source, Game game, UUID playerId) {
        Card cardToCheck = game.getCard(objectId);
        if (cardToCheck == null) {
            return false;
        }

        // must be you
        if (!playerId.equals(source.getControllerId())) {
            return false;
        }

        // must be a card in your graveyard
        Card mainCard = cardToCheck.getMainCard();
        Player player = game.getPlayer(mainCard.getOwnerId());
        if (player == null || !player.getId().equals(playerId)
                || player.getGraveyard().getCards(game).stream().noneMatch(c -> c.getId().equals(mainCard.getId()))) {
            return false;
        }

        // must have been surveilled into the graveyard this turn
        EyeOfDuskmantleWatcher watcher = game.getState().getWatcher(EyeOfDuskmantleWatcher.class);
        if (watcher == null || !watcher.wasSurveilledThisTurn(playerId, mainCard.getId())) {
            return false;
        }

        // allows to cast with the alternative life cost
        if (!cardToCheck.isLand(game)) {
            if (cardToCheck.getManaCost().isEmpty()) {
                return false;
            }
            Costs<Cost> newCosts = new CostsImpl<>();
            newCosts.add(new PayLifeCost(cardToCheck.getSpellAbility().getManaCosts().manaValue()));
            newCosts.addAll(cardToCheck.getSpellAbility().getCosts());
            player.setCastSourceIdWithAlternateMana(cardToCheck.getId(), null, newCosts);
        }
        return true;
    }
}

/**
 * Remembers which cards were put into a graveyard by surveil this turn. The SURVEILED event does not
 * carry the moved cards and is handled before the zone changes it made are handed to the watchers, so
 * a surveil opens a budget of moves for its source ability and the library to graveyard moves of that
 * ability are counted against it.
 */
class EyeOfDuskmantleWatcher extends Watcher {

    private final Map<UUID, Set<UUID>> surveilledCards = new HashMap<>();
    private final Map<String, Integer> openSurveils = new HashMap<>();

    EyeOfDuskmantleWatcher() {
        super(WatcherScope.GAME);
    }

    @Override
    public void watch(GameEvent event, Game game) {
        switch (event.getType()) {
            case SURVEILED:
                openSurveils.merge(makeKey(event.getPlayerId(), event.getSourceId()), event.getAmount(), Integer::sum);
                return;
            case ZONE_CHANGE:
                ZoneChangeEvent zEvent = (ZoneChangeEvent) event;
                if (zEvent.getFromZone() != Zone.LIBRARY || zEvent.getToZone() != Zone.GRAVEYARD) {
                    return;
                }
                String key = makeKey(event.getPlayerId(), event.getSourceId());
                int left = openSurveils.getOrDefault(key, 0);
                if (left < 1) {
                    return;
                }
                openSurveils.put(key, left - 1);
                surveilledCards
                        .computeIfAbsent(event.getPlayerId(), k -> new HashSet<>())
                        .add(zEvent.getTargetId());
                return;
            default:
        }
    }

    boolean wasSurveilledThisTurn(UUID playerId, UUID cardId) {
        return surveilledCards.getOrDefault(playerId, Collections.emptySet()).contains(cardId);
    }

    private static String makeKey(UUID playerId, UUID sourceId) {
        return playerId + "_" + sourceId;
    }

    @Override
    public void reset() {
        super.reset();
        surveilledCards.clear();
        openSurveils.clear();
    }
}

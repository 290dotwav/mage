package mage.cards.m;

import mage.ApprovingObject;
import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.SpellAbility;
import mage.abilities.TriggeredAbilityImpl;
import mage.abilities.costs.mana.ManaCost;
import mage.abilities.costs.mana.ManaCosts;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.keyword.MiracleAbility;
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
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.players.Player;
import mage.target.targetpointer.FixedTarget;
import mage.watchers.Watcher;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "Nonland cards in your hand have miracle {0}."
 * <p>
 * XMage clears an ability granted to a card as the card changes zones and only grants it again at the next
 * applyEffects, which comes after the draw event the miracle reveal is tied to (702.94a). So a granted
 * MiracleAbility is never there when MiracleWatcher looks. Instead:
 * - MoleculeManWatcher offers the reveal for the first card drawn each turn by a player who controls
 * Molecule Man, when that card has no printed miracle (MiracleWatcher already offers those), and fires the
 * usual MIRACLE_CARD_REVEALED event;
 * - Molecule Man's ability triggers on that event for any nonland card its controller revealed that way,
 * printed miracle or not, and casts it by paying {0}. A printed miracle card revealed with Molecule Man out
 * gets both triggers, its own cost and {0}, as a card with two miracle abilities would.
 *
 * @author Claude
 */
public final class MoleculeMan extends CardImpl {

    public MoleculeMan(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{6}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.VILLAIN);
        this.power = new MageInt(5);
        this.toughness = new MageInt(5);

        // Nonland cards in your hand have miracle {0}.
        this.addAbility(new MoleculeManTriggeredAbility(), new MoleculeManWatcher());
    }

    private MoleculeMan(final MoleculeMan card) {
        super(card);
    }

    @Override
    public MoleculeMan copy() {
        return new MoleculeMan(this);
    }
}

class MoleculeManTriggeredAbility extends TriggeredAbilityImpl {

    MoleculeManTriggeredAbility() {
        super(Zone.BATTLEFIELD, new MoleculeManEffect(), true);
    }

    private MoleculeManTriggeredAbility(final MoleculeManTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public MoleculeManTriggeredAbility copy() {
        return new MoleculeManTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.MIRACLE_CARD_REVEALED;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        Card card = game.getCard(event.getTargetId());
        if (card == null
                || card.isLand(game)
                || !isControlledBy(event.getPlayerId())
                || !card.isOwnedBy(getControllerId())
                || game.getState().getZone(card.getId()) != Zone.HAND) {
            return false;
        }
        getEffects().setTargetPointer(new FixedTarget(card, game));
        return true;
    }

    @Override
    public String getRule() {
        return "Nonland cards in your hand have miracle {0}. <i>(You may cast a card for its miracle cost "
                + "when you draw it if it's the first card you drew this turn.)</i>";
    }
}

class MoleculeManEffect extends OneShotEffect {

    private static final ManaCosts<ManaCost> miracleCosts = new ManaCostsImpl<>("{0}");

    MoleculeManEffect() {
        super(Outcome.Benefit);
        staticText = "cast that card by paying {0}";
    }

    private MoleculeManEffect(final MoleculeManEffect effect) {
        super(effect);
    }

    @Override
    public MoleculeManEffect copy() {
        return new MoleculeManEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        // the target pointer keeps the card that was revealed: gone from the hand, it's gone for good
        Card card = game.getCard(getTargetPointer().getFirst(game, source));
        if (controller == null || card == null || game.getState().getZone(card.getId()) != Zone.HAND) {
            return false;
        }
        SpellAbility abilityToCast = card.getSpellAbility().copy();
        ManaCosts<ManaCost> costRef = abilityToCast.getManaCostsToPay();
        costRef.clear();
        costRef.add(miracleCosts.copy());
        controller.cast(abilityToCast, game, false, new ApprovingObject(source, game));
        return true;
    }
}

class MoleculeManWatcher extends Watcher {

    private final Map<UUID, Integer> drawnThisTurn = new HashMap<>();

    MoleculeManWatcher() {
        super(WatcherScope.GAME);
    }

    @Override
    public void watch(GameEvent event, Game game) {
        if (event.getType() == GameEvent.EventType.UNTAP_STEP_PRE) {
            drawnThisTurn.clear();
            return;
        }
        // opening hands can't be revealed for miracle: same guard as MiracleWatcher
        if (game.getPhase() == null || event.getType() != GameEvent.EventType.DREW_CARD || event.getPlayerId() == null) {
            return;
        }
        int amount = drawnThisTurn.merge(event.getPlayerId(), 1, Integer::sum);
        if (amount != 1) {
            return;
        }
        Player player = game.getPlayer(event.getPlayerId());
        Card card = game.getCard(event.getTargetId());
        if (player == null || card == null || card.isLand(game)
                || game.getState().getZone(card.getId()) != Zone.HAND
                || card.getAbilities(game).containsClass(MiracleAbility.class)
                || game.getBattlefield().getAllActivePermanents(player.getId()).stream()
                .noneMatch(p -> p.getAbilities(game).containsClass(MoleculeManTriggeredAbility.class))) {
            return;
        }
        Cards cards = new CardsImpl(card);
        player.lookAtCards("Miracle", cards, game);
        if (player.chooseUse(Outcome.Benefit, "Reveal " + card.getLogName() + " to be able to use Miracle {0}?", null, game)) {
            player.revealCards("Miracle", cards, game);
            game.fireEvent(GameEvent.getEvent(GameEvent.EventType.MIRACLE_CARD_REVEALED, card.getId(), null, player.getId()));
        }
    }

    @Override
    public void reset() {
        super.reset();
        drawnThisTurn.clear();
    }
}

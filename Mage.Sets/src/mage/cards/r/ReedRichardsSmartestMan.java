package mage.cards.r;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.effects.ReplacementEffectImpl;
import mage.abilities.effects.common.continuous.MaximumHandSizeControllerEffect;
import mage.abilities.keyword.ReachAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.PhaseStep;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.WatcherScope;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.players.Player;
import mage.watchers.Watcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Claude
 */
public final class ReedRichardsSmartestMan extends CardImpl {

    public ReedRichardsSmartestMan(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{5}{U}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.SCIENTIST);
        this.subtype.add(SubType.HERO);
        this.power = new MageInt(2);
        this.toughness = new MageInt(4);

        // Reach
        this.addAbility(ReachAbility.getInstance());

        // You have no maximum hand size.
        this.addAbility(new SimpleStaticAbility(new MaximumHandSizeControllerEffect(
                Integer.MAX_VALUE, Duration.WhileOnBattlefield, MaximumHandSizeControllerEffect.HandSizeModification.SET
        )));

        // The first time you would draw a card each turn except the first card you draw during each of your draw steps, you draw four cards instead.
        this.addAbility(new SimpleStaticAbility(new ReedRichardsSmartestManEffect()), new ReedRichardsSmartestManWatcher());
    }

    private ReedRichardsSmartestMan(final ReedRichardsSmartestMan card) {
        super(card);
    }

    @Override
    public ReedRichardsSmartestMan copy() {
        return new ReedRichardsSmartestMan(this);
    }
}

class ReedRichardsSmartestManEffect extends ReplacementEffectImpl {

    ReedRichardsSmartestManEffect() {
        super(Duration.WhileOnBattlefield, Outcome.DrawCard);
        staticText = "the first time you would draw a card each turn except the first card you draw " +
                "during each of your draw steps, you draw four cards instead";
    }

    private ReedRichardsSmartestManEffect(final ReedRichardsSmartestManEffect effect) {
        super(effect);
    }

    @Override
    public ReedRichardsSmartestManEffect copy() {
        return new ReedRichardsSmartestManEffect(this);
    }

    @Override
    public boolean checksEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.DRAW_CARD;
    }

    @Override
    public boolean applies(GameEvent event, Ability source, Game game) {
        return event.getPlayerId().equals(source.getControllerId())
                && ReedRichardsSmartestManWatcher.isFirstCountedDraw(event.getPlayerId(), game);
    }

    @Override
    public boolean replaceEvent(GameEvent event, Ability source, Game game) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        ReedRichardsSmartestManWatcher.markReplaced(controller.getId(), game);
        controller.drawCards(4, source, game, event);
        return true;
    }
}

/**
 * Tracks, for each player and turn, the draws that are not the first card drawn during that player's draw step.
 */
class ReedRichardsSmartestManWatcher extends Watcher {

    private final Map<UUID, Integer> drawStepDraws = new HashMap<>();
    private final Map<UUID, Integer> countedDraws = new HashMap<>();
    private final Set<UUID> replaced = new HashSet<>();

    ReedRichardsSmartestManWatcher() {
        super(WatcherScope.GAME);
    }

    @Override
    public void watch(GameEvent event, Game game) {
        if (event.getType() != GameEvent.EventType.DREW_CARD || event.getPlayerId() == null) {
            return;
        }
        UUID playerId = event.getPlayerId();
        if (isOwnDrawStep(playerId, game)) {
            int before = drawStepDraws.getOrDefault(playerId, 0);
            drawStepDraws.put(playerId, before + 1);
            if (before == 0) {
                // the first card drawn during that player's draw step doesn't count
                return;
            }
        }
        countedDraws.merge(playerId, 1, Integer::sum);
    }

    @Override
    public void reset() {
        super.reset();
        drawStepDraws.clear();
        countedDraws.clear();
        replaced.clear();
    }

    private static boolean isOwnDrawStep(UUID playerId, Game game) {
        return game.isActivePlayer(playerId)
                && game.getPhase() != null
                && game.getPhase().getStep() != null
                && game.getPhase().getStep().getType() == PhaseStep.DRAW;
    }

    /**
     * @return true if the draw the player would make now is the first one this turn
     * that isn't the first card drawn during their draw step
     */
    static boolean isFirstCountedDraw(UUID playerId, Game game) {
        ReedRichardsSmartestManWatcher watcher = game.getState().getWatcher(ReedRichardsSmartestManWatcher.class);
        if (watcher == null || watcher.replaced.contains(playerId)
                || watcher.countedDraws.getOrDefault(playerId, 0) > 0) {
            return false;
        }
        return !(isOwnDrawStep(playerId, game) && watcher.drawStepDraws.getOrDefault(playerId, 0) == 0);
    }

    static void markReplaced(UUID playerId, Game game) {
        ReedRichardsSmartestManWatcher watcher = game.getState().getWatcher(ReedRichardsSmartestManWatcher.class);
        if (watcher != null) {
            watcher.replaced.add(playerId);
        }
    }
}

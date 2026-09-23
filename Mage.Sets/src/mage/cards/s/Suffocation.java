package mage.cards.s;

import mage.ObjectColor;
import mage.abilities.Ability;
import mage.abilities.common.CastOnlyIfConditionIsTrueAbility;
import mage.abilities.common.delayed.AtTheBeginOfNextUpkeepDelayedTriggeredAbility;
import mage.abilities.condition.Condition;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.CreateDelayedTriggeredAbilityEffect;
import mage.abilities.effects.common.DrawCardSourceControllerEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.WatcherScope;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.stack.Spell;
import mage.players.Player;
import mage.watchers.Watcher;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Claude
 */
public final class Suffocation extends CardImpl {

    public Suffocation(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.INSTANT}, "{1}{U}");

        // Cast this spell only if you were dealt damage this turn by a red instant or sorcery spell.
        this.addAbility(new CastOnlyIfConditionIsTrueAbility(SuffocationCondition.instance));

        // Suffocation deals 4 damage to the controller of the last red instant or sorcery spell that dealt damage to you this turn.
        this.getSpellAbility().addEffect(new SuffocationEffect());

        // Draw a card at the beginning of the next turn's upkeep.
        this.getSpellAbility().addEffect(new CreateDelayedTriggeredAbilityEffect(
                new AtTheBeginOfNextUpkeepDelayedTriggeredAbility(new DrawCardSourceControllerEffect(1)), false
        ).concatBy("<br>"));
        this.getSpellAbility().addWatcher(new SuffocationWatcher());
    }

    private Suffocation(final Suffocation card) {
        super(card);
    }

    @Override
    public Suffocation copy() {
        return new Suffocation(this);
    }
}

enum SuffocationCondition implements Condition {
    instance;

    @Override
    public boolean apply(Game game, Ability source) {
        return SuffocationWatcher.getLastController(source.getControllerId(), game) != null;
    }

    @Override
    public String toString() {
        return "you were dealt damage this turn by a red instant or sorcery spell";
    }
}

class SuffocationEffect extends OneShotEffect {

    SuffocationEffect() {
        super(Outcome.Damage);
        staticText = "{this} deals 4 damage to the controller of the last red instant or sorcery spell " +
                "that dealt damage to you this turn";
    }

    private SuffocationEffect(final SuffocationEffect effect) {
        super(effect);
    }

    @Override
    public SuffocationEffect copy() {
        return new SuffocationEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        UUID playerId = SuffocationWatcher.getLastController(source.getControllerId(), game);
        Player player = game.getPlayer(playerId);
        if (player == null) {
            return false;
        }
        player.damage(4, source.getSourceId(), source, game);
        return true;
    }
}

/**
 * For each player, the controller of the last red instant or sorcery spell that dealt damage to them this turn.
 */
class SuffocationWatcher extends Watcher {

    private final Map<UUID, UUID> lastController = new HashMap<>();

    SuffocationWatcher() {
        super(WatcherScope.GAME);
    }

    @Override
    public void watch(GameEvent event, Game game) {
        if (event.getType() != GameEvent.EventType.DAMAGED_PLAYER) {
            return;
        }
        Spell spell = game.getSpellOrLKIStack(event.getSourceId());
        if (spell == null
                || !spell.isInstantOrSorcery(game)
                || !spell.getColor(game).contains(ObjectColor.RED)) {
            return;
        }
        lastController.put(event.getTargetId(), spell.getControllerId());
    }

    @Override
    public void reset() {
        super.reset();
        lastController.clear();
    }

    static UUID getLastController(UUID playerId, Game game) {
        SuffocationWatcher watcher = game.getState().getWatcher(SuffocationWatcher.class);
        return watcher == null ? null : watcher.lastController.get(playerId);
    }
}

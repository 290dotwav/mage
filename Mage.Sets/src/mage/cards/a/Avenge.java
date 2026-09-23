package mage.cards.a;

import mage.abilities.Ability;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.condition.Condition;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.cost.SpellCostReductionSourceEffect;
import mage.abilities.hint.ConditionHint;
import mage.abilities.hint.Hint;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.WatcherScope;
import mage.constants.Zone;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
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
public final class Avenge extends CardImpl {

    public Avenge(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.SORCERY}, "{4}{W}{W}");

        // This spell costs {2} less to cast if a player attacked you during their last turn.
        Ability ability = new SimpleStaticAbility(
                Zone.ALL, new SpellCostReductionSourceEffect(2, AvengeCondition.instance)
        ).setRuleAtTheTop(true);
        ability.addHint(AvengeCondition.getHint());
        this.addAbility(ability, new AvengeWatcher());

        // Destroy all creatures. You gain 1 life for each creature destroyed this way.
        this.getSpellAbility().addEffect(new AvengeEffect());
    }

    private Avenge(final Avenge card) {
        super(card);
    }

    @Override
    public Avenge copy() {
        return new Avenge(this);
    }
}

enum AvengeCondition implements Condition {
    instance;
    private static final Hint hint = new ConditionHint(instance);

    public static Hint getHint() {
        return hint;
    }

    @Override
    public boolean apply(Game game, Ability source) {
        return AvengeWatcher.wasAttackedLastTurn(source.getControllerId(), game);
    }

    @Override
    public String toString() {
        return "a player attacked you during their last turn";
    }
}

/**
 * For each player, the players attacked during that player's most recent turn.
 */
class AvengeWatcher extends Watcher {

    private final Map<UUID, Set<UUID>> attackedLastTurn = new HashMap<>();

    AvengeWatcher() {
        super(WatcherScope.GAME);
    }

    @Override
    public void watch(GameEvent event, Game game) {
        switch (event.getType()) {
            case BEGINNING_PHASE_PRE:
                // a new turn of that player starts: their previous turn is no longer their last turn
                attackedLastTurn.remove(game.getActivePlayerId());
                return;
            case ATTACKER_DECLARED:
                if (game.getPlayer(event.getTargetId()) != null) {
                    attackedLastTurn
                            .computeIfAbsent(event.getPlayerId(), x -> new HashSet<>())
                            .add(event.getTargetId());
                }
        }
    }

    static boolean wasAttackedLastTurn(UUID playerId, Game game) {
        AvengeWatcher watcher = game.getState().getWatcher(AvengeWatcher.class);
        if (watcher == null) {
            return false;
        }
        return watcher.attackedLastTurn
                .entrySet()
                .stream()
                .anyMatch(entry -> !entry.getKey().equals(playerId) && entry.getValue().contains(playerId));
    }
}

class AvengeEffect extends OneShotEffect {

    AvengeEffect() {
        super(Outcome.DestroyPermanent);
        this.staticText = "destroy all creatures. You gain 1 life for each creature destroyed this way";
    }

    private AvengeEffect(final AvengeEffect effect) {
        super(effect);
    }

    @Override
    public AvengeEffect copy() {
        return new AvengeEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        int destroyed = 0;
        for (Permanent creature : game.getBattlefield().getActivePermanents(
                StaticFilters.FILTER_PERMANENT_CREATURE, controller.getId(), game)) {
            if (creature.destroy(source, game, false)) {
                destroyed++;
            }
        }
        if (destroyed > 0) {
            game.processAction();
            controller.gainLife(destroyed, game, source);
        }
        return true;
    }
}

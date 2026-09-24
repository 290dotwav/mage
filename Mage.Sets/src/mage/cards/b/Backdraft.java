package mage.cards.b;

import mage.abilities.Ability;
import mage.abilities.effects.OneShotEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.choices.Choice;
import mage.choices.ChoiceImpl;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.WatcherScope;
import mage.filter.FilterPlayer;
import mage.filter.predicate.ObjectSourcePlayer;
import mage.filter.predicate.ObjectSourcePlayerPredicate;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.stack.Spell;
import mage.players.Player;
import mage.target.TargetPlayer;
import mage.watchers.Watcher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Claude
 */
public final class Backdraft extends CardImpl {

    public Backdraft(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.INSTANT}, "{1}{R}");

        // Choose a player who cast one or more sorcery spells this turn. Backdraft deals damage to that player equal to half the damage dealt by one of those sorcery spells this turn, rounded down.
        this.getSpellAbility().addEffect(new BackdraftEffect());
        this.getSpellAbility().addWatcher(new BackdraftWatcher());
    }

    private Backdraft(final Backdraft card) {
        super(card);
    }

    @Override
    public Backdraft copy() {
        return new Backdraft(this);
    }
}

class BackdraftEffect extends OneShotEffect {

    private static final FilterPlayer filter = new FilterPlayer("player who cast one or more sorcery spells this turn");

    static {
        filter.add(BackdraftPredicate.instance);
    }

    BackdraftEffect() {
        super(Outcome.Damage);
        staticText = "choose a player who cast one or more sorcery spells this turn. {this} deals damage to that " +
                "player equal to half the damage dealt by one of those sorcery spells this turn, rounded down";
    }

    private BackdraftEffect(final BackdraftEffect effect) {
        super(effect);
    }

    @Override
    public BackdraftEffect copy() {
        return new BackdraftEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        TargetPlayer target = new TargetPlayer(1, 1, true, filter);
        if (!target.canChoose(controller.getId(), source, game)) {
            return false;
        }
        controller.choose(outcome, target, source, game);
        Player player = game.getPlayer(target.getFirstTarget());
        if (player == null) {
            return false;
        }
        List<BackdraftWatcher.SorcerySpell> spells = BackdraftWatcher.getSpells(player.getId(), game);
        if (spells.isEmpty()) {
            return false;
        }
        BackdraftWatcher.SorcerySpell chosen = spells.get(0);
        if (spells.size() > 1) {
            Map<String, BackdraftWatcher.SorcerySpell> choices = new LinkedHashMap<>();
            for (int i = 0; i < spells.size(); i++) {
                BackdraftWatcher.SorcerySpell spell = spells.get(i);
                choices.put((i + 1) + ". " + spell.name + " (dealt " + spell.damage + " damage)", spell);
            }
            Choice choice = new ChoiceImpl(true);
            choice.setMessage("Choose one of those sorcery spells");
            choice.setChoices(new LinkedHashSet<>(choices.keySet()));
            controller.choose(outcome, choice, game);
            if (choice.getChoice() != null) {
                chosen = choices.get(choice.getChoice());
            }
        }
        int amount = chosen.damage / 2;
        if (amount > 0) {
            player.damage(amount, source.getSourceId(), source, game);
        }
        return true;
    }
}

enum BackdraftPredicate implements ObjectSourcePlayerPredicate<Player> {
    instance;

    @Override
    public boolean apply(ObjectSourcePlayer<Player> input, Game game) {
        return !BackdraftWatcher.getSpells(input.getObject().getId(), game).isEmpty();
    }
}

/**
 * The sorcery spells cast this turn, and the damage each one dealt.
 */
class BackdraftWatcher extends Watcher {

    static class SorcerySpell {
        final String name;
        final int damage;

        SorcerySpell(String name, int damage) {
            this.name = name;
            this.damage = damage;
        }
    }

    // by spell id (immutable values only: watchers are copied with the game state)
    private final Map<UUID, UUID> casters = new LinkedHashMap<>();
    private final Map<UUID, String> names = new LinkedHashMap<>();
    private final Map<UUID, Integer> damages = new LinkedHashMap<>();

    BackdraftWatcher() {
        super(WatcherScope.GAME);
    }

    @Override
    public void watch(GameEvent event, Game game) {
        switch (event.getType()) {
            case SPELL_CAST: {
                Spell spell = game.getSpell(event.getTargetId());
                if (spell != null && spell.isSorcery(game)) {
                    casters.put(spell.getId(), spell.getControllerId());
                    names.put(spell.getId(), spell.getName());
                }
                return;
            }
            case DAMAGED_PLAYER:
            case DAMAGED_PERMANENT: {
                Spell spell = game.getSpellOrLKIStack(event.getSourceId());
                if (spell != null && casters.containsKey(spell.getId())) {
                    damages.merge(spell.getId(), event.getAmount(), Integer::sum);
                }
            }
        }
    }

    @Override
    public void reset() {
        super.reset();
        casters.clear();
        names.clear();
        damages.clear();
    }

    static List<SorcerySpell> getSpells(UUID playerId, Game game) {
        List<SorcerySpell> result = new ArrayList<>();
        BackdraftWatcher watcher = game.getState().getWatcher(BackdraftWatcher.class);
        if (watcher != null) {
            for (Map.Entry<UUID, UUID> entry : watcher.casters.entrySet()) {
                if (entry.getValue().equals(playerId)) {
                    result.add(new SorcerySpell(
                            watcher.names.get(entry.getKey()),
                            watcher.damages.getOrDefault(entry.getKey(), 0)
                    ));
                }
            }
        }
        return result;
    }
}

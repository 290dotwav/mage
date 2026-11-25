package mage.collectors.services;

import mage.constants.CommanderCardType;
import mage.constants.WatcherScope;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.events.DamagedEvent;
import mage.game.events.GameEvent;
import mage.game.events.ZoneChangeEvent;
import mage.game.permanent.Permanent;
import mage.game.stack.StackObject;
import mage.players.Player;
import mage.watchers.Watcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/**
 * The numbers of the leaderboard, counted per player during one game
 * ({@link GameStatsDataCollector} adds it at the game's start and reads it at its end).
 * <p>
 * Everything is credited to the controller of the source, read at the moment of the event:
 * <ul>
 * <li>damage dealt to another player, and to a creature another player controls;</li>
 * <li>creatures of another player killed: a creature that leaves the battlefield for the
 * graveyard (or would have, before a replacement such as a commander's), credited to the
 * controller of the ability that moved it (destroy, sacrifice, ...) or, for a state-based
 * death with damage marked on it (704.5g, 704.5h), to the controller of the last damage it
 * took; a creature put there for 0 toughness (704.5f) or by nothing known is counted as
 * {@link #getUnknownKills() unknown}, never guessed;</li>
 * <li>its own creatures that died, spells cast, life gained;</li>
 * <li>players it eliminated: a player who loses at 0 life, with 10 poison counters, or
 * right after a commander's damage, is credited to the controller of the last damage or life
 * loss he took (none when that was himself). Whether the loss was a concession is the
 * collector's to say (it hears the game log), and a concession is credited to nobody.</li>
 * </ul>
 * A watcher lives in the game state: a rollback or an undo restores the state, and these
 * numbers with it, so nothing undone stays counted. Simulated games (the AI's) hold their
 * own copies and never reach the real one.
 */
public class GameStatsWatcher extends Watcher {

    /** The mark of a creature last damaged by two controllers at once: its death is nobody's. */
    static final UUID SEVERAL = new UUID(0L, 0L);

    private final HashMap<UUID, Integer> damageToPlayers = new HashMap<>();
    private final HashMap<UUID, Integer> damageToCreatures = new HashMap<>();
    private final HashMap<UUID, Integer> creaturesKilled = new HashMap<>();
    private final HashMap<UUID, Integer> creaturesLost = new HashMap<>();
    private final HashMap<UUID, Integer> spellsCast = new HashMap<>();
    private final HashMap<UUID, Integer> lifeGained = new HashMap<>();
    private final HashMap<UUID, Integer> eliminatedTurn = new HashMap<>();
    /** Who eliminated whom: the loser's id to the controller credited (absent: unknown). */
    private final HashMap<UUID, UUID> eliminatedBy = new HashMap<>();
    private Integer unknownKills = 0;

    /** A permanent to the controllers of the damage it took since its last batch of damage. */
    private final HashMap<UUID, HashSet<UUID>> damagedSinceBatch = new HashMap<>();
    /** A permanent to the controller of its last batch of damage ({@link #SEVERAL} when not one). */
    private final HashMap<UUID, UUID> lastDamagedBy = new HashMap<>();
    /** A player to the controller of the last damage or life loss he took. */
    private final HashMap<UUID, UUID> lastHitBy = new HashMap<>();
    /** A player to whether that last hit was a commander's damage. */
    private final HashMap<UUID, Boolean> lastHitByCommander = new HashMap<>();

    public GameStatsWatcher() {
        super(WatcherScope.GAME);
    }

    @Override
    public void watch(GameEvent event, Game game) {
        switch (event.getType()) {
            case DAMAGED_PLAYER:
                onDamagedPlayer((DamagedEvent) event, game);
                return;
            case DAMAGED_PERMANENT:
                onDamagedPermanent((DamagedEvent) event, game);
                return;
            case DAMAGED_BATCH_FOR_ONE_PERMANENT:
                onPermanentBatch(event.getTargetId());
                return;
            case LOST_LIFE:
                if (event.getSourceId() != null) {
                    UUID by = controllerOf(event.getSourceId(), game);
                    if (by != null) {
                        lastHitBy.put(event.getTargetId(), by);
                        lastHitByCommander.put(event.getTargetId(), isCommander(by, event.getSourceId(), game));
                    }
                }
                return;
            case ZONE_CHANGE:
                onZoneChange((ZoneChangeEvent) event, game);
                return;
            case SPELL_CAST:
                add(spellsCast, event.getPlayerId(), 1);
                return;
            case GAINED_LIFE:
                add(lifeGained, event.getPlayerId(), event.getAmount());
                return;
            case LOST:
                onLost(event.getPlayerId(), game);
                return;
            default:
        }
    }

    private void onDamagedPlayer(DamagedEvent event, Game game) {
        UUID by = controllerOf(event.getSourceId(), game);
        UUID damaged = event.getTargetId();
        if (by == null) {
            lastHitBy.remove(damaged);
            lastHitByCommander.remove(damaged);
            return;
        }
        lastHitBy.put(damaged, by);
        lastHitByCommander.put(damaged, isCommander(by, event.getSourceId(), game));
        if (!by.equals(damaged)) {
            add(damageToPlayers, by, event.getAmount());
        }
    }

    private void onDamagedPermanent(DamagedEvent event, Game game) {
        UUID by = controllerOf(event.getSourceId(), game);
        UUID permanentId = event.getTargetId();
        damagedSinceBatch.computeIfAbsent(permanentId, k -> new HashSet<>()).add(by == null ? SEVERAL : by);
        Permanent permanent = game.getPermanentOrLKIBattlefield(permanentId);
        if (by != null && permanent != null && permanent.isCreature(game) && !by.equals(permanent.getControllerId())) {
            add(damageToCreatures, by, event.getAmount());
        }
    }

    /** Damage dealt at the same time to one permanent is one batch: one controller, or nobody's. */
    private void onPermanentBatch(UUID permanentId) {
        HashSet<UUID> by = damagedSinceBatch.remove(permanentId);
        if (by == null || by.isEmpty()) {
            return;
        }
        lastDamagedBy.put(permanentId, by.size() == 1 ? by.iterator().next() : SEVERAL);
    }

    private void onZoneChange(ZoneChangeEvent event, Game game) {
        if (event.getFromZone() != Zone.BATTLEFIELD
                || (event.getToZone() != Zone.GRAVEYARD && event.getOriginalToZone() != Zone.GRAVEYARD)) {
            return;
        }
        Permanent dead = event.getTarget();
        if (dead == null || !dead.isCreature(game)) {
            return;
        }
        UUID owner = dead.getControllerId();
        add(creaturesLost, owner, 1);
        UUID killer = killerOf(event, dead, game);
        if (killer == null) {
            unknownKills++;
        } else if (!killer.equals(owner)) {
            add(creaturesKilled, killer, 1);
        }
        damagedSinceBatch.remove(dead.getId());
        lastDamagedBy.remove(dead.getId());
    }

    /** The controller the death is credited to, or null when it is not known. */
    private UUID killerOf(ZoneChangeEvent event, Permanent dead, Game game) {
        if (event.getSourceId() != null) {
            // an ability moved it: destroy, sacrifice, "put into its owner's graveyard"
            return controllerOf(event.getSourceId(), game);
        }
        // a state-based action: lethal damage or deathtouch, never 0 toughness (nobody dealt that)
        if (dead.getToughness().getValue() > 0 && (dead.getDamage() > 0 || dead.isDeathtouched())) {
            UUID by = lastDamagedBy.get(dead.getId());
            if (by == null) {
                HashSet<UUID> pending = damagedSinceBatch.get(dead.getId());
                by = pending != null && pending.size() == 1 ? pending.iterator().next() : null;
            }
            return by == null || SEVERAL.equals(by) ? null : by;
        }
        return null;
    }

    private void onLost(UUID loser, Game game) {
        if (loser == null || eliminatedTurn.containsKey(loser)) {
            return;
        }
        eliminatedTurn.put(loser, game.getTurnNum());
        Player player = game.getPlayer(loser);
        UUID by = lastHitBy.get(loser);
        if (player == null || by == null || by.equals(loser)) {
            return;
        }
        boolean killedByHit = player.getLife() <= 0
                || player.getCountersCount(mage.counters.CounterType.POISON) >= 10
                || Boolean.TRUE.equals(lastHitByCommander.get(loser));
        if (killedByHit) {
            eliminatedBy.put(loser, by);
        }
    }

    /** The controller of a damage or effect source: a permanent (or its last known state), a spell, an emblem. */
    static UUID controllerOf(UUID sourceId, Game game) {
        if (sourceId == null) {
            return null;
        }
        Permanent permanent = game.getPermanentOrLKIBattlefield(sourceId);
        if (permanent != null) {
            return permanent.getControllerId();
        }
        StackObject stackObject = game.getStack().getStackObject(sourceId);
        if (stackObject != null) {
            return stackObject.getControllerId();
        }
        return game.getControllerId(sourceId);
    }

    private static boolean isCommander(UUID controllerId, UUID sourceId, Game game) {
        Player player = game.getPlayer(controllerId);
        return player != null && game.getCommandersIds(player, CommanderCardType.COMMANDER_OR_OATHBREAKER, false).contains(sourceId);
    }

    @Override
    public void reset() {
        super.reset();
        // damage wears off in the cleanup step (514.2): what a creature took this turn says nothing next turn
        damagedSinceBatch.clear();
        lastDamagedBy.clear();
    }

    private static void add(Map<UUID, Integer> map, UUID playerId, int amount) {
        if (playerId != null && amount > 0) {
            map.merge(playerId, amount, Integer::sum);
        }
    }

    public int getDamageToPlayers(UUID playerId) {
        return damageToPlayers.getOrDefault(playerId, 0);
    }

    public int getDamageToCreatures(UUID playerId) {
        return damageToCreatures.getOrDefault(playerId, 0);
    }

    public int getCreaturesKilled(UUID playerId) {
        return creaturesKilled.getOrDefault(playerId, 0);
    }

    public int getCreaturesLost(UUID playerId) {
        return creaturesLost.getOrDefault(playerId, 0);
    }

    public int getSpellsCast(UUID playerId) {
        return spellsCast.getOrDefault(playerId, 0);
    }

    public int getLifeGained(UUID playerId) {
        return lifeGained.getOrDefault(playerId, 0);
    }

    /** The turn he lost the game, or null while he has not. */
    public Integer getEliminatedTurn(UUID playerId) {
        return eliminatedTurn.get(playerId);
    }

    /** Who is credited with his elimination, or null when nobody is known to be. */
    public UUID getEliminatedBy(UUID playerId) {
        return eliminatedBy.get(playerId);
    }

    /** How many players this one eliminated, his concessions excepted ({@code quit}). */
    public int getPlayersEliminated(UUID playerId, java.util.Set<UUID> quit) {
        int count = 0;
        for (Map.Entry<UUID, UUID> entry : eliminatedBy.entrySet()) {
            if (entry.getValue().equals(playerId) && !quit.contains(entry.getKey())) {
                count++;
            }
        }
        return count;
    }

    /** Creatures that died with no killer known. */
    public int getUnknownKills() {
        return unknownKills;
    }
}

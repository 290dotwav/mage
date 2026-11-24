package mage.abilities.effects;

import mage.MageObject;
import mage.abilities.Abilities;
import mage.abilities.Ability;
import mage.abilities.PlayLandAbility;
import mage.abilities.SpellAbility;
import mage.constants.EmptyNames;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.permanent.Permanent;

import java.io.Serializable;
import java.util.*;

/**
 * ClaudeMTG fork: where each change of a permanent comes from, as {@link ContinuousEffects#apply}
 * made it. Around each effect it applies in layer 6 (abilities added and removed, 613.1f) and in
 * layer 7b, 7c and 7e (power and toughness set, modified, switched, 613.4), the battlefield is
 * read before and after; what moved is put on the name of the object the effect comes from
 * (an Equipment, an anthem, the resolved spell). A +1/+1 on a creature then says it is Glorious
 * Anthem's; a 6/6 that Humility makes 1/1 says -5/-5, Humility, and every ability it lost.
 * <p>
 * Counters are not in it (7d, and abilities given by counters): a screen reads them off the
 * permanent. Neither is a characteristic-defining ability (7a), the card's own.
 * <p>
 * Recorded only for a game that asks for it ({@link Game#isRecordingChangeSources}: the web door
 * turns it on for the game it serves); never for the copies the AI simulates on, which do not
 * carry the switch. The record is rebuilt on each application and replaces the previous one whole.
 */
public final class ChangeSources {

    /** What one source did to one permanent: power and toughness added (negative when taken), abilities given and taken. */
    public static final class Change implements Serializable {

        private final String from;
        private int power;
        private int toughness;
        private final List<String> gained = new ArrayList<>(1);
        private final List<String> lost = new ArrayList<>(1);

        Change(String from) {
            this.from = from;
        }

        /** A change as it is said (the door's tests). */
        public Change(String from, int power, int toughness, List<String> gained, List<String> lost) {
            this.from = from;
            this.power = power;
            this.toughness = toughness;
            this.gained.addAll(gained);
            this.lost.addAll(lost);
        }

        /** The English name of the object the effect comes from. */
        public String getFrom() {
            return from;
        }

        public int getPower() {
            return power;
        }

        public int getToughness() {
            return toughness;
        }

        /** The abilities it gave, in the words of the permanent's rules. */
        public List<String> getGained() {
            return Collections.unmodifiableList(gained);
        }

        /** The abilities it took away, in the words they had. */
        public List<String> getLost() {
            return Collections.unmodifiableList(lost);
        }

        boolean isEmpty() {
            return power == 0 && toughness == 0 && gained.isEmpty() && lost.isEmpty();
        }

        @Override
        public String toString() {
            return from + " " + power + "/" + toughness + " +" + gained + " -" + lost;
        }
    }

    // permanent id -> source name -> what it did, in the order they first did something
    private final Map<UUID, Map<String, Change>> changes = new HashMap<>();

    // the battlefield before the effect being applied
    private Permanent[] permanents = new Permanent[0];
    private int[] power = new int[0];
    private int[] toughness = new int[0];
    private Ability[][] abilities = null;

    /** Read the battlefield before an effect applies; its abilities too when the effect is a layer 6 one. */
    /** Set once a reading failed: what was recorded is dropped, and the game goes on untouched. */
    private boolean broken;

    /**
     * Never lets a failure of its own reach the game: this is only a reading for the screen, run inside the
     * application of the layers (613), where an exception would stop the game itself.
     */
    void before(Game game, boolean withAbilities) {
        if (broken) {
            return;
        }
        try {
            readBefore(game, withAbilities);
        } catch (RuntimeException e) {
            broken = true;
        }
    }

    void after(Ability source, Game game) {
        if (broken) {
            return;
        }
        try {
            readAfter(source, game);
        } catch (RuntimeException e) {
            broken = true;
        }
    }

    private void readBefore(Game game, boolean withAbilities) {
        Collection<Permanent> all = game.getBattlefield().getAllPermanents();
        int n = all.size();
        if (permanents.length != n) {
            permanents = new Permanent[n];
            power = new int[n];
            toughness = new int[n];
        }
        abilities = withAbilities ? new Ability[n][] : null;
        int i = 0;
        for (Permanent permanent : all) {
            if (i == n) {
                break;
            }
            permanents[i] = permanent;
            power[i] = permanent.getPower().getValue();
            toughness[i] = permanent.getToughness().getValue();
            if (abilities != null) {
                abilities[i] = permanent.getAbilities().toArray(new Ability[0]);
            }
            i++;
        }
        for (; i < n; i++) {
            permanents[i] = null;
        }
    }

    /** After the effect applied: what moved, on the name of the object its ability comes from. */
    private void readAfter(Ability source, Game game) {
        String from = null;
        boolean named = false;
        for (int i = 0; i < permanents.length; i++) {
            Permanent permanent = permanents[i];
            if (permanent == null) {
                continue;
            }
            int dp = permanent.getPower().getValue() - power[i];
            int dt = permanent.getToughness().getValue() - toughness[i];
            List<String> gained = null;
            List<String> lost = null;
            if (abilities != null) {
                Abilities<Ability> now = permanent.getAbilities();
                Ability[] was = abilities[i];
                if (!same(was, now)) {
                    Set<UUID> wasIds = new HashSet<>();
                    for (Ability a : was) {
                        wasIds.add(a.getId());
                    }
                    Set<UUID> nowIds = new HashSet<>();
                    for (Ability a : now) {
                        nowIds.add(a.getId());
                        if (!wasIds.contains(a.getId())) {
                            gained = add(gained, ruleOf(a));
                        }
                    }
                    for (Ability a : was) {
                        if (!nowIds.contains(a.getId())) {
                            lost = add(lost, ruleOf(a));
                        }
                    }
                }
            }
            if (dp == 0 && dt == 0 && gained == null && lost == null) {
                continue;
            }
            if (!named) {
                from = nameOf(source, game);
                named = true;
            }
            if (from == null) {
                return;
            }
            Change change = changes
                    .computeIfAbsent(permanent.getId(), k -> new LinkedHashMap<>(2))
                    .computeIfAbsent(from, Change::new);
            change.power += dp;
            change.toughness += dt;
            addAll(change.gained, gained);
            addAll(change.lost, lost);
        }
    }

    /** The record: permanent id -> its changes, each source once, those that came to nothing left out. */
    Map<UUID, List<Change>> result() {
        if (broken) {
            return new HashMap<>();
        }
        Map<UUID, List<Change>> result = new HashMap<>(changes.size() * 2);
        for (Map.Entry<UUID, Map<String, Change>> entry : changes.entrySet()) {
            List<Change> list = new ArrayList<>(entry.getValue().size());
            for (Change change : entry.getValue().values()) {
                if (!change.isEmpty()) {
                    list.add(change);
                }
            }
            if (!list.isEmpty()) {
                result.put(entry.getKey(), Collections.unmodifiableList(list));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * The name of the object an ability comes from, null when it is not to be said: unknown, a
     * face-down object (its name is hidden), a card in a hand or a library.
     */
    static String nameOf(Ability source, Game game) {
        if (source == null || source.getSourceId() == null) {
            return null;
        }
        MageObject object = game.getObject(source.getSourceId());
        if (object == null) {
            return null;
        }
        Zone zone = game.getState().getZone(source.getSourceId());
        if (zone == Zone.HAND || zone == Zone.LIBRARY) {
            return null;
        }
        String name = object.getName();
        if (name == null || name.isEmpty() || EmptyNames.isEmptyName(name)) {
            return null;
        }
        return name;
    }

    /** An ability's line as the permanent's rules say it ({@code AbilitiesImpl.getRules}), null for one they do not show. */
    static String ruleOf(Ability ability) {
        if (!ability.getRuleVisible() || ability instanceof SpellAbility || ability instanceof PlayLandAbility) {
            return null;
        }
        String rule = ability.getRule();
        if (rule == null || rule.length() <= 3) {
            return null;
        }
        return Character.toUpperCase(rule.charAt(0)) + rule.substring(1);
    }

    private static boolean same(Ability[] was, Abilities<Ability> now) {
        if (was.length != now.size()) {
            return false;
        }
        int i = 0;
        for (Ability a : now) {
            if (a != was[i++]) {
                return false;
            }
        }
        return true;
    }

    private static List<String> add(List<String> list, String rule) {
        if (rule == null) {
            return list;
        }
        if (list == null) {
            list = new ArrayList<>(2);
        }
        if (!list.contains(rule)) {
            list.add(rule);
        }
        return list;
    }

    private static void addAll(List<String> into, List<String> rules) {
        if (rules == null) {
            return;
        }
        for (String rule : rules) {
            if (!into.contains(rule)) {
                into.add(rule);
            }
        }
    }
}

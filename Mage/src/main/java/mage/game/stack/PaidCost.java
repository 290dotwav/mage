package mage.game.stack;

import mage.MageObject;
import mage.Mana;
import mage.cards.Card;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.target.Target;
import mage.abilities.Ability;
import mage.abilities.costs.Cost;
import mage.abilities.costs.Costs;
import mage.abilities.costs.SacrificeCost;
import mage.abilities.costs.VariableCost;
import mage.abilities.costs.common.DiscardTargetCost;
import mage.abilities.costs.common.ExileFromGraveCost;
import mage.abilities.costs.common.ExileFromHandCost;
import mage.abilities.costs.common.ExileTargetCost;
import mage.abilities.costs.common.PayLifeCost;
import mage.abilities.costs.common.RemoveCounterCost;
import mage.abilities.costs.common.RemoveCountersSourceCost;
import mage.abilities.costs.common.ReturnToHandChosenControlledPermanentCost;
import mage.abilities.costs.common.RevealTargetFromHandCost;
import mage.abilities.costs.common.SacrificeTargetCost;
import mage.abilities.costs.mana.GenericManaCost;
import mage.abilities.costs.mana.ManaCost;
import mage.abilities.costs.mana.ManaCosts;
import mage.abilities.costs.mana.VariableManaCost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The mana a spell or an activated ability on the stack was put there for, read off the
 * object itself: the cost as it was paid (601.2f: the printed cost with every increase,
 * reduction, alternative or additional cost applied, commander tax included, X replaced
 * by its value), the value chosen for X, and the mana actually spent on it, by type.
 * <p>
 * It is the engine's own record, the one "mana spent to cast" reads (sunburst, adamant,
 * {@code ManaSpentToCastWatcher}): {@code getManaCostsToPay()} after payment, each part's
 * {@code getUsedManaToPay()}. Nothing is reconstructed.
 * <p>
 * {@code null} for what was not paid for as it went on the stack: a triggered ability, a
 * copy (707.10: a copy is not cast), and an activated ability whose cost holds no mana; and
 * for a spell or an ability whose mana is still being paid (it is on the stack from 601.2a).
 * A spell cast without paying its mana cost (Dauthi Voidwalker, an alternative cost of no
 * mana) has an empty {@link #getCost()} and nothing spent.
 * <p>
 * The life paid for it is told too ({@link #getLifePaid()}): every "pay N life" cost paid among
 * its costs. Each Phyrexian symbol paid with life becomes one (107.4f,
 * {@code AbilityImpl.handlePhyrexianCosts}), so does each {B} K'rrik let be paid with life
 * ({@code handlePhyrexianLikeEffects}), and an additional or alternative cost is one (Snuff Out,
 * Toxic Deluge's X as the fixed cost its announcement became).
 * An activated ability whose only cost was life ("Pay 1 life: ...") is told for its life alone.
 * <p>
 * Every other cost paid for it is told as well ({@link #getOthers()}): « et ça vaut pour tous les
 * coûts, même les sacrifices etc. ». A sacrifice, a discard, an exile, a reveal, a return to hand,
 * a tap or an untap, and counters removed, each with the names of what it took (read off the
 * cost's own record when it keeps one, else off its chosen targets and the game), or marked as the
 * source itself ({T}, "Sacrifice {this}", cycling's discard). A cost of another kind (put
 * counters, behold, mill, energy...) is not told.
 */
public final class PaidCost {

    private static final String SPENT_ORDER = "WUBRGC";

    private final String cost;
    private final Integer x;
    private final String spent;
    private final int lifePaid;
    private final List<Part> others;

    /** One cost other than mana and life, as paid ({@link #getOthers()}). */
    public static final class Part {
        private final String kind;
        private final List<String> names;
        private final boolean source;
        private final int amount;
        private final String counter;

        Part(String kind, List<String> names, boolean source, int amount, String counter) {
            this.kind = kind;
            this.names = names;
            this.source = source;
            this.amount = amount;
            this.counter = counter;
        }

        /** sacrifice, discard, exile, reveal, return, tap, untap or counters. */
        public String getKind() {
            return kind;
        }

        /** The names of what it took, in the order taken; empty when the cost does not say (or it is the source). */
        public List<String> getNames() {
            return names;
        }

        /** The source itself was the price: {T}, "Sacrifice {this}", cycling's discard. */
        public boolean isSource() {
            return source;
        }

        /** How many (counters removed, or the count of a variable cost); 0 when not told. */
        public int getAmount() {
            return amount;
        }

        /** The kind of counter removed ("+1/+1", "loyalty"); empty when not told or any kind. */
        public String getCounter() {
            return counter;
        }
    }

    private static final Set<String> SOURCE_COSTS = new HashSet<>(Arrays.asList(
            "SacrificeSourceCost", "DiscardSourceCost", "CyclingDiscardCost", "ExileSourceCost",
            "ExileSourceFromGraveCost", "ExileSourceFromHandCost", "ReturnToHandFromBattlefieldSourceCost",
            "RevealSourceFromYourHandCost", "RemoveCountersSourceCost", "RemoveAllCountersSourceCost",
            "RemoveVariableCountersSourceCost", "TapSourceCost", "UntapSourceCost"));

    private PaidCost(String cost, Integer x, String spent, int lifePaid, List<Part> others) {
        this.cost = cost;
        this.x = x;
        this.spent = spent;
        this.lifePaid = lifePaid;
        this.others = others;
    }

    /** What {@code object} cost as it went on the stack, or null when nothing was paid for it (see the class). */
    public static PaidCost of(StackObject object) {
        return of(object, null);
    }

    /**
     * What {@code object} cost as it went on the stack, the names of what its other costs took read
     * off {@code game} when the cost keeps only their ids (null: only the names a cost keeps itself).
     */
    public static PaidCost of(StackObject object, Game game) {
        if (object == null || object.isCopy()) {
            return null;
        }
        Ability ability;
        boolean spell;
        if (object instanceof Spell) {
            ability = ((Spell) object).getSpellAbility();
            spell = true;
        } else if (object instanceof StackAbility) {
            ability = ((StackAbility) object).getStackAbility();
            spell = false;
            if (ability == null || !ability.getAbilityType().isActivatedAbility()) {
                return null;
            }
        } else {
            return null;
        }
        if (ability == null) {
            return null;
        }
        ManaCosts<ManaCost> paid = ability.getManaCostsToPay();
        if (!paid.isPaid()) {
            // 601.2a puts the spell on the stack before its costs are paid (601.2g-h): while the
            // mana is still being asked for, nothing is paid yet, and a part-paid cost is no answer
            return null;
        }
        int generic = 0;
        boolean anyGeneric = false;
        Integer x = null;
        StringBuilder rest = new StringBuilder();
        Mana used = new Mana();
        for (ManaCost part : paid) {
            used.add(part.getUsedManaToPay());
            if (part instanceof VariableManaCost) {
                // 107.3: its value was added to the cost as generic (or coloured) mana when it was
                // announced (AbilityImpl.handleManaXCosts), so the {X} itself is not drawn again
                VariableManaCost variable = (VariableManaCost) part;
                if (variable.wasAnnounced()) {
                    x = variable.getAmount();
                }
                continue;
            }
            if (part instanceof GenericManaCost) {
                generic += part.manaValue();
                anyGeneric = true;
                continue;
            }
            rest.append(part.getText());
        }
        int life = lifePaid(ability.getCosts());
        List<Part> others = new ArrayList<>();
        others(ability.getCosts(), game, others);
        if (!spell && !anyGeneric && rest.length() == 0 && used.count() == 0 && life == 0 && others.isEmpty()) {
            // an activated ability with nothing in its cost that it tells: nothing to say
            return null;
        }
        String text = (anyGeneric && (generic > 0 || rest.length() == 0) ? "{" + generic + "}" : "") + rest;
        return new PaidCost(text, x, symbols(used), life, Collections.unmodifiableList(others));
    }

    /** The life paid among {@code costs}: each paid {@link PayLifeCost}, nested cost lists included. */
    private static int lifePaid(Costs<? extends Cost> costs) {
        int life = 0;
        if (costs == null) {
            return life;
        }
        for (Cost cost : costs) {
            if (cost instanceof PayLifeCost && cost.isPaid()) {
                life += Math.max(0, ((PayLifeCost) cost).getLifePaid());
            } else if (cost instanceof Costs) {
                life += lifePaid((Costs<?>) cost);
            }
        }
        return life;
    }

    /** Every paid cost of {@code costs} other than mana and life, nested cost lists included, as {@link Part}s. */
    private static void others(Costs<? extends Cost> costs, Game game, List<Part> out) {
        if (costs == null) {
            return;
        }
        for (Cost cost : costs) {
            if (cost instanceof Costs) {
                others((Costs<?>) cost, game, out);
                continue;
            }
            if (cost == null || cost instanceof ManaCost || cost instanceof PayLifeCost || !cost.isPaid()) {
                continue;
            }
            Part part = part(cost, game);
            if (part != null) {
                out.add(part);
            }
        }
    }

    private static Part part(Cost cost, Game game) {
        String type = cost.getClass().getSimpleName();
        String kind;
        if (cost instanceof SacrificeCost || type.startsWith("Sacrifice")) {
            kind = "sacrifice";
        } else if (type.contains("Discard")) {
            kind = "discard";
        } else if (type.startsWith("Exile")) {
            kind = "exile";
        } else if (type.startsWith("Reveal")) {
            kind = "reveal";
        } else if (type.startsWith("ReturnToHand")) {
            kind = "return";
        } else if (type.startsWith("Remove") && type.contains("Counter")) {
            kind = "counters";
        } else if (type.startsWith("Tap") || type.equals("CrewCost")) {
            kind = "tap";
        } else if (type.startsWith("Untap")) {
            kind = "untap";
        } else {
            return null;
        }
        boolean source = SOURCE_COSTS.contains(type);
        List<String> names = new ArrayList<>();
        if (!source) {
            names(cost, game, names);
        }
        int amount = 0;
        String counter = "";
        if (cost instanceof RemoveCountersSourceCost) {
            amount = ((RemoveCountersSourceCost) cost).getAmount();
            counter = ((RemoveCountersSourceCost) cost).getCounterName();
        } else if (cost instanceof RemoveCounterCost) {
            amount = ((RemoveCounterCost) cost).getCountersToRemove();
            counter = ((RemoveCounterCost) cost).getCounterTypeName();
        } else if (cost instanceof VariableCost) {
            amount = ((VariableCost) cost).getAmount();
        }
        return new Part(kind, Collections.unmodifiableList(names), source, Math.max(0, amount), counter == null ? "" : counter);
    }

    /** The names of what {@code cost} took: its own record when it keeps one, else its chosen targets. */
    private static void names(Cost cost, Game game, List<String> out) {
        List<? extends MageObject> kept = null;
        if (cost instanceof SacrificeTargetCost) {
            kept = ((SacrificeTargetCost) cost).getPermanents();
        } else if (cost instanceof ReturnToHandChosenControlledPermanentCost) {
            kept = ((ReturnToHandChosenControlledPermanentCost) cost).getPermanents();
        } else if (cost instanceof ExileTargetCost) {
            kept = ((ExileTargetCost) cost).getPermanents();
        } else if (cost instanceof DiscardTargetCost) {
            kept = ((DiscardTargetCost) cost).getCards();
        } else if (cost instanceof ExileFromHandCost) {
            kept = ((ExileFromHandCost) cost).getCards();
        } else if (cost instanceof ExileFromGraveCost) {
            kept = ((ExileFromGraveCost) cost).getExiledCards();
        } else if (cost instanceof RevealTargetFromHandCost) {
            kept = ((RevealTargetFromHandCost) cost).getRevealedCards();
        }
        if (kept != null && !kept.isEmpty()) {
            for (MageObject object : kept) {
                if (object != null && !object.getName().isEmpty()) {
                    out.add(object.getName());
                }
            }
            return;
        }
        if (game == null || cost.getTargets() == null) {
            return;
        }
        for (Target target : cost.getTargets()) {
            for (UUID id : target.getTargets()) {
                String name = nameOf(id, game);
                if (!name.isEmpty()) {
                    out.add(name);
                }
            }
        }
    }

    private static String nameOf(UUID id, Game game) {
        Permanent permanent = game.getPermanentOrLKIBattlefield(id);
        if (permanent != null && !permanent.isFaceDown(game)) {
            return permanent.getName();
        }
        Card card = game.getCard(id);
        return card == null ? "" : card.getName();
    }

    private static String symbols(Mana mana) {
        StringBuilder out = new StringBuilder();
        for (char type : SPENT_ORDER.toCharArray()) {
            int n;
            switch (type) {
                case 'W': n = mana.getWhite(); break;
                case 'U': n = mana.getBlue(); break;
                case 'B': n = mana.getBlack(); break;
                case 'R': n = mana.getRed(); break;
                case 'G': n = mana.getGreen(); break;
                default: n = mana.getColorless(); break;
            }
            for (int i = 0; i < n; i++) {
                out.append('{').append(type).append('}');
            }
        }
        return out.toString();
    }

    /** The cost as paid, in mana symbols ("{4}{G}{G}"); empty when no mana cost was paid. */
    public String getCost() {
        return cost;
    }

    /** The value announced for X, or null when the cost had no {X}. */
    public Integer getX() {
        return x;
    }

    /** The mana spent on it, one symbol per mana, W U B R G then colourless ("{R}{U}{G}{G}"); empty when none. */
    public String getSpent() {
        return spent;
    }

    /** The life paid for it (Phyrexian mana, "pay N life" costs, K'rrik); 0 when none. */
    public int getLifePaid() {
        return lifePaid;
    }

    /** Every other cost paid for it, in the order of its costs; empty when none (see the class). */
    public List<Part> getOthers() {
        return others;
    }
}

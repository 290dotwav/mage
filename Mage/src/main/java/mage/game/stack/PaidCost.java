package mage.game.stack;

import mage.Mana;
import mage.abilities.Ability;
import mage.abilities.costs.mana.GenericManaCost;
import mage.abilities.costs.mana.ManaCost;
import mage.abilities.costs.mana.ManaCosts;
import mage.abilities.costs.mana.VariableManaCost;

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
 */
public final class PaidCost {

    private static final String SPENT_ORDER = "WUBRGC";

    private final String cost;
    private final Integer x;
    private final String spent;

    private PaidCost(String cost, Integer x, String spent) {
        this.cost = cost;
        this.x = x;
        this.spent = spent;
    }

    /** What {@code object} cost as it went on the stack, or null when nothing was paid for it (see the class). */
    public static PaidCost of(StackObject object) {
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
        if (!spell && !anyGeneric && rest.length() == 0 && used.count() == 0) {
            // an activated ability with no mana in its cost ({T}, a sacrifice): nothing to say
            return null;
        }
        String text = (anyGeneric && (generic > 0 || rest.length() == 0) ? "{" + generic + "}" : "") + rest;
        return new PaidCost(text, x, symbols(used));
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
}

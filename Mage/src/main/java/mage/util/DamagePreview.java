package mage.util;

import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.Mode;
import mage.abilities.dynamicvalue.DynamicValue;
import mage.abilities.dynamicvalue.common.GetXValue;
import mage.abilities.dynamicvalue.common.SourcePermanentPowerValue;
import mage.abilities.dynamicvalue.common.StaticValue;
import mage.abilities.effects.Effect;
import mage.abilities.effects.common.DamageTargetEffect;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.target.Target;
import mage.target.TargetAmount;
import mage.target.Targets;
import mage.target.targetpointer.FirstTargetPointer;
import mage.target.targetpointer.SecondTargetPointer;
import mage.target.targetpointer.ThirdTargetPointer;
import mage.target.targetpointer.TargetPointer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ClaudeMTG fork: the damage a spell or ability will deal to the target a
 * human player is choosing, put on the question sent to the client
 * (options "damagePreview", "damageBy", "damageMayChange").
 * <p>
 * The amount as written: prevention, doubling and other replacement effects
 * are not applied. Only what is known for sure is told, from a white list:
 * a plain DamageTargetEffect whose amount is a fixed number, X (announced
 * before targets, 601.2b) or the source permanent's power, and the effects
 * that implement {@link DamagePreviewing}. Anything else (a cost paid after
 * targets, a value read from another target, divided damage, two damage
 * effects on the same target) sends nothing. A failure of the reading never
 * reaches the game: nothing is sent.
 */
public final class DamagePreview implements Serializable {

    public static final String KEY_AMOUNT = "damagePreview";
    public static final String KEY_BY = "damageBy";
    public static final String KEY_MAY_CHANGE = "damageMayChange";

    private final int amount;
    private final String by; // name of the creature whose power gives the amount, or null
    private final boolean mayChange;

    public DamagePreview(int amount, String by, boolean mayChange) {
        this.amount = amount;
        this.by = by;
        this.mayChange = mayChange;
    }

    public int getAmount() {
        return amount;
    }

    public String getBy() {
        return by;
    }

    public boolean isMayChange() {
        return mayChange;
    }

    /**
     * Writes the preview into the question's options, or removes a stale one.
     * Never throws.
     */
    public static void putInto(Map<String, Serializable> options, Target target, Ability source, Game game) {
        if (options == null) {
            return;
        }
        try {
            options.remove(KEY_AMOUNT);
            options.remove(KEY_BY);
            options.remove(KEY_MAY_CHANGE);
            DamagePreview preview = compute(target, source, game);
            if (preview == null) {
                return;
            }
            options.put(KEY_AMOUNT, preview.getAmount());
            if (preview.getBy() != null && !preview.getBy().isEmpty()) {
                options.put(KEY_BY, preview.getBy());
            }
            if (preview.isMayChange()) {
                options.put(KEY_MAY_CHANGE, Boolean.TRUE);
            }
        } catch (RuntimeException e) {
            options.remove(KEY_AMOUNT);
            options.remove(KEY_BY);
            options.remove(KEY_MAY_CHANGE);
        }
    }

    /**
     * The damage the source will deal to a target chosen with this target, or
     * null when it cannot be told for sure. Never throws.
     */
    public static DamagePreview compute(Target target, Ability source, Game game) {
        try {
            if (target == null || source == null || game == null || game.isSimulation()) {
                return null;
            }
            if (target instanceof TargetAmount) {
                return null; // divided damage: the split is chosen with the targets
            }
            Mode mode = null;
            int targetIndex = -1;
            for (UUID modeId : source.getModes().getSelectedModes()) {
                Mode candidate = source.getModes().get(modeId);
                if (candidate == null) {
                    continue;
                }
                int index = indexOf(candidate.getTargets(), target);
                if (index >= 0) {
                    if (mode != null) {
                        return null; // the same target in two modes: not told
                    }
                    mode = candidate;
                    targetIndex = index;
                }
            }
            if (mode == null) {
                return null;
            }
            List<Effect> aimed = new ArrayList<>();
            for (Effect effect : mode.getEffects()) {
                if (pointerIndex(effect.getTargetPointer()) == targetIndex
                        && (effect instanceof DamagePreviewing || effect instanceof DamageTargetEffect)) {
                    aimed.add(effect);
                }
            }
            if (aimed.size() != 1) {
                return null;
            }
            Effect effect = aimed.get(0);
            if (effect instanceof DamagePreviewing) {
                return ((DamagePreviewing) effect).previewDamage(target, source, game);
            }
            if (effect.getClass() != DamageTargetEffect.class) {
                return null; // a subclass may deal another amount
            }
            return fromValue(((DamageTargetEffect) effect).getAmount(), effect, source, game);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static DamagePreview fromValue(DynamicValue value, Effect effect, Ability source, Game game) {
        if (value == null) {
            return null;
        }
        if (value instanceof StaticValue || value instanceof GetXValue) {
            return new DamagePreview(Math.max(0, value.calculate(game, source, effect)), null, false);
        }
        if (value instanceof SourcePermanentPowerValue) {
            Permanent permanent = source.getSourcePermanentOrLKI(game);
            if (permanent == null) {
                return null;
            }
            return new DamagePreview(Math.max(0, value.calculate(game, source, effect)), nameOf(permanent), true);
        }
        return null;
    }

    /**
     * The name to show for the creature whose power gives the amount, or null
     * (a face-down creature has none).
     */
    public static String nameOf(MageObject object) {
        if (object == null) {
            return null;
        }
        String name = object.getName();
        return name == null || name.isEmpty() ? null : name;
    }

    private static int indexOf(Targets targets, Target target) {
        if (targets == null) {
            return -1;
        }
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i) == target) {
                return i;
            }
        }
        return -1;
    }

    private static int pointerIndex(TargetPointer pointer) {
        if (pointer == null) {
            return -1;
        }
        Class<?> c = pointer.getClass();
        if (c == FirstTargetPointer.class) {
            return 0;
        }
        if (c == SecondTargetPointer.class) {
            return 1;
        }
        if (c == ThirdTargetPointer.class) {
            return 2;
        }
        return -1;
    }
}

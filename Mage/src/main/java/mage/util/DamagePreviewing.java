package mage.util;

import mage.abilities.Ability;
import mage.game.Game;
import mage.target.Target;

/**
 * ClaudeMTG fork: a hand-written damage effect that can tell, while its
 * target is being chosen, how much damage it will deal (see
 * {@link DamagePreview}). Return null when the amount is not known for sure.
 * The amount as written: no prevention, no doubling.
 */
public interface DamagePreviewing {

    DamagePreview previewDamage(Target target, Ability source, Game game);
}

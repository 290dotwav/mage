package mage.abilities.effects.common.continuous;

import mage.MageObjectReference;
import mage.abilities.Ability;
import mage.abilities.effects.ContinuousEffectImpl;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Layer;
import mage.constants.Outcome;
import mage.constants.SubLayer;
import mage.constants.SubType;
import mage.game.Game;
import mage.game.permanent.Permanent;

/**
 * The lasting half of assimilate (Star Trek): "It's a Borg artifact creature
 * and loses all other creature types." Follows one object: a permanent, or a
 * card about to be put onto the battlefield (reference at its next zone change
 * counter), so the creature is already a Borg artifact as it enters and an
 * "artifact enters" trigger sees it. Ends once that object has left.
 *
 * @author Claude
 */
public class BecomesBorgEffect extends ContinuousEffectImpl {

    private final MageObjectReference mor;

    public BecomesBorgEffect(MageObjectReference mor) {
        super(Duration.Custom, Layer.TypeChangingEffects_4, SubLayer.NA, Outcome.Benefit);
        this.mor = mor;
        staticText = "it's a Borg artifact creature and loses all other creature types";
    }

    protected BecomesBorgEffect(final BecomesBorgEffect effect) {
        super(effect);
        this.mor = effect.mor;
    }

    @Override
    public BecomesBorgEffect copy() {
        return new BecomesBorgEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent permanent = mor.getPermanent(game);
        if (permanent == null) {
            Permanent entering = game.getPermanentEntering(mor.getSourceId());
            if (entering != null && entering.getZoneChangeCounter(game) == mor.getZoneChangeCounter()) {
                permanent = entering;
            }
        }
        if (permanent == null) {
            // Not there yet (the card is still on its way) or gone for good.
            if (game.getState().getZoneChangeCounter(mor.getSourceId()) >= mor.getZoneChangeCounter()) {
                discard();
            }
            return false;
        }
        permanent.addCardType(game, CardType.ARTIFACT);
        permanent.addCardType(game, CardType.CREATURE);
        permanent.removeAllCreatureTypes(game);
        permanent.addSubType(game, SubType.BORG);
        return true;
    }
}

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

import java.util.UUID;

/**
 * The lasting half of assimilate (Star Trek): "It's a Borg artifact creature
 * and loses all other creature types." Follows one object: a permanent, or a
 * card about to be put onto the battlefield (reference at its next zone change
 * counter), so the creature is already a Borg artifact as it enters and an
 * "artifact enters" trigger sees it. Ends once that object has left, or when
 * the card it waited for never arrived ({@link #cancelArrival}): left alone,
 * it would wait for that zone change counter and turn the card into a Borg
 * the next time anyone put it onto the battlefield.
 *
 * @author Claude
 */
public class BecomesBorgEffect extends ContinuousEffectImpl {

    private final MageObjectReference mor;
    private final UUID arrivalId; // same in every copy of this effect, names it in the game state

    public BecomesBorgEffect(MageObjectReference mor) {
        super(Duration.Custom, Layer.TypeChangingEffects_4, SubLayer.NA, Outcome.Benefit);
        this.mor = mor;
        this.arrivalId = UUID.randomUUID();
        staticText = "it's a Borg artifact creature and loses all other creature types";
    }

    protected BecomesBorgEffect(final BecomesBorgEffect effect) {
        super(effect);
        this.mor = effect.mor;
        this.arrivalId = effect.arrivalId;
    }

    /**
     * The card this effect waited for did not reach the battlefield: the copy
     * of this effect in the game ends at its next application.
     */
    public void cancelArrival(Game game) {
        game.getState().setValue(cancelKey(), Boolean.TRUE);
    }

    private String cancelKey() {
        return "BecomesBorgEffect|cancelled|" + arrivalId;
    }

    @Override
    public BecomesBorgEffect copy() {
        return new BecomesBorgEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        if (Boolean.TRUE.equals(game.getState().getValue(cancelKey()))) {
            discard();
            return false;
        }
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

package mage.cards.p;

import mage.abilities.Ability;
import mage.abilities.common.MayPayAdditionalManaCostAbility;
import mage.abilities.dynamicvalue.DynamicValue;
import mage.abilities.effects.Effect;
import mage.abilities.effects.OneShotEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.TargetPermanent;
import mage.target.targetadjustment.TargetsCountAdjuster;

import java.util.UUID;

/**
 * @author Claude
 */
public final class PrimitiveJustice extends CardImpl {

    static final String RED_TAG = "PrimitiveJusticeRedPaid";
    static final String GREEN_TAG = "PrimitiveJusticeGreenPaid";

    public PrimitiveJustice(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.SORCERY}, "{1}{R}");

        // As an additional cost to cast this spell, you may pay {1}{R} and/or {1}{G} any number of times.
        this.addAbility(new MayPayAdditionalManaCostAbility("{1}{R}", true, RED_TAG,
                "As an additional cost to cast this spell, you may pay {1}{R} and/or {1}{G} any number of times."));
        this.addAbility(new MayPayAdditionalManaCostAbility("{1}{G}", true, GREEN_TAG, null));

        // Destroy target artifact. For each additional {1}{R} you paid, destroy another target artifact. For each additional {1}{G} you paid, destroy another target artifact, and you gain 1 life.
        this.getSpellAbility().addEffect(new PrimitiveJusticeEffect());
        this.getSpellAbility().addTarget(new TargetPermanent(StaticFilters.FILTER_PERMANENT_ARTIFACT));
        this.getSpellAbility().setTargetAdjuster(new TargetsCountAdjuster(PrimitiveJusticeValue.instance));
    }

    private PrimitiveJustice(final PrimitiveJustice card) {
        super(card);
    }

    @Override
    public PrimitiveJustice copy() {
        return new PrimitiveJustice(this);
    }
}

enum PrimitiveJusticeValue implements DynamicValue {
    instance;

    @Override
    public int calculate(Game game, Ability sourceAbility, Effect effect) {
        return 1 + MayPayAdditionalManaCostAbility.getTimesPaid(game, sourceAbility, PrimitiveJustice.RED_TAG)
                + MayPayAdditionalManaCostAbility.getTimesPaid(game, sourceAbility, PrimitiveJustice.GREEN_TAG);
    }

    @Override
    public PrimitiveJusticeValue copy() {
        return this;
    }

    @Override
    public String getMessage() {
        return "";
    }

    @Override
    public String toString() {
        return "1";
    }
}

class PrimitiveJusticeEffect extends OneShotEffect {

    PrimitiveJusticeEffect() {
        super(Outcome.DestroyPermanent);
        staticText = "destroy target artifact. For each additional {1}{R} you paid, destroy another target artifact. " +
                "For each additional {1}{G} you paid, destroy another target artifact, and you gain 1 life";
    }

    private PrimitiveJusticeEffect(final PrimitiveJusticeEffect effect) {
        super(effect);
    }

    @Override
    public PrimitiveJusticeEffect copy() {
        return new PrimitiveJusticeEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        for (UUID targetId : getTargetPointer().getTargets(game, source)) {
            Permanent permanent = game.getPermanent(targetId);
            if (permanent != null) {
                permanent.destroy(source, game, false);
            }
        }
        int green = MayPayAdditionalManaCostAbility.getTimesPaid(game, source, PrimitiveJustice.GREEN_TAG);
        Player player = game.getPlayer(source.getControllerId());
        if (player != null && green > 0) {
            player.gainLife(green, game, source);
        }
        return true;
    }
}

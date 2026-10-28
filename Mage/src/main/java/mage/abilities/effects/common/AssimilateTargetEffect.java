package mage.abilities.effects.common;

import mage.MageObjectReference;
import mage.abilities.Ability;
import mage.abilities.Mode;
import mage.abilities.effects.ContinuousEffect;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.continuous.BecomesBorgEffect;
import mage.abilities.effects.common.continuous.GainControlTargetEffect;
import mage.cards.Card;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.counters.Counters;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.Target;
import mage.target.TargetPermanent;
import mage.target.targetpointer.FixedTarget;

import java.util.UUID;

/**
 * Assimilate (Star Trek keyword action).
 * <p>
 * A card: "Put it onto the battlefield under your control with a +1/+1
 * counter. It's a Borg artifact creature and loses all other creature types."
 * A permanent: "Gain control of it and put a +1/+1 counter on it. It's a Borg
 * artifact creature and loses all other creature types."
 * <p>
 * The effect assimilates every object its target pointer names; the static
 * helpers serve the cards that find what they assimilate some other way
 * (revealed, exiled with the source).
 *
 * @author Claude
 */
public class AssimilateTargetEffect extends OneShotEffect {

    public static final String CARD_REMINDER = "<i>(Put it onto the battlefield under your control with a +1/+1 counter. "
            + "It's a Borg artifact creature and loses all other creature types.)</i>";
    public static final String PERMANENT_REMINDER = "<i>(Gain control of it and put a +1/+1 counter on it. "
            + "It's a Borg artifact creature and loses all other creature types.)</i>";

    public AssimilateTargetEffect() {
        super(Outcome.PutCreatureInPlay);
    }

    protected AssimilateTargetEffect(final AssimilateTargetEffect effect) {
        super(effect);
    }

    @Override
    public AssimilateTargetEffect copy() {
        return new AssimilateTargetEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        boolean done = false;
        for (UUID id : getTargetPointer().getTargets(game, source)) {
            Permanent permanent = game.getPermanent(id);
            if (permanent != null) {
                done |= assimilatePermanent(permanent, controller, source, game);
                continue;
            }
            Card card = game.getCard(id);
            if (card != null) {
                done |= assimilateCard(card, controller, source, game) != null;
            }
        }
        return done;
    }

    /**
     * Puts the card onto the battlefield under the player's control with a
     * +1/+1 counter, a Borg artifact creature as it enters.
     *
     * @return the permanent it became, or null when it did not arrive
     */
    public static Permanent assimilateCard(Card card, Player player, Ability source, Game game) {
        if (card == null || player == null) {
            return null;
        }
        MageObjectReference arriving = new MageObjectReference(card, game, 1);
        game.addEffect(new BecomesBorgEffect(arriving), source);
        game.setEnterWithCounters(card.getId(), new Counters().addCounter(CounterType.P1P1.createInstance()));
        player.moveCards(card, Zone.BATTLEFIELD, source, game);
        Permanent permanent = arriving.getPermanent(game);
        if (permanent == null) {
            game.setEnterWithCounters(card.getId(), null);
        }
        return permanent;
    }

    /**
     * The player gains control of the permanent for good and puts a +1/+1
     * counter on it; it's a Borg artifact creature.
     */
    public static boolean assimilatePermanent(Permanent permanent, Player player, Ability source, Game game) {
        if (permanent == null || player == null) {
            return false;
        }
        ContinuousEffect control = new GainControlTargetEffect(Duration.Custom, true, player.getId());
        control.setTargetPointer(new FixedTarget(permanent, game));
        game.addEffect(control, source);
        permanent.addCounters(CounterType.P1P1.createInstance(), player.getId(), source, game);
        game.addEffect(new BecomesBorgEffect(new MageObjectReference(permanent, game)), source);
        return true;
    }

    @Override
    public String getText(Mode mode) {
        if (staticText != null && !staticText.isEmpty()) {
            return staticText;
        }
        Target target = mode.getTargets().isEmpty() ? null : mode.getTargets().get(0);
        boolean onBattlefield = target instanceof TargetPermanent;
        return "assimilate " + getTargetPointer().describeTargets(mode.getTargets(), "it")
                + ". " + (onBattlefield ? PERMANENT_REMINDER : CARD_REMINDER);
    }
}

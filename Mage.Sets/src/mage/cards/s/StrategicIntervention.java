package mage.cards.s;

import mage.abilities.common.AttacksAloneControlledTriggeredAbility;
import mage.abilities.effects.common.TapTargetEffect;
import mage.abilities.effects.common.continuous.BoostTargetEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.filter.common.FilterCreaturePermanent;
import mage.filter.predicate.permanent.ControllerIdPredicate;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.target.TargetPermanent;
import mage.target.targetpointer.FixedTarget;

import java.util.UUID;

/**
 * @author Claude
 */
public final class StrategicIntervention extends CardImpl {

    public StrategicIntervention(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ENCHANTMENT}, "{1}{W}");

        // Whenever a creature you control attacks alone, it gets +1/+1 until end of turn. Tap up to one target creature defending player controls.
        this.addAbility(new StrategicInterventionTriggeredAbility());
    }

    private StrategicIntervention(final StrategicIntervention card) {
        super(card);
    }

    @Override
    public StrategicIntervention copy() {
        return new StrategicIntervention(this);
    }
}

class StrategicInterventionTriggeredAbility extends AttacksAloneControlledTriggeredAbility {

    StrategicInterventionTriggeredAbility() {
        super(new BoostTargetEffect(1, 1).setText("it gets +1/+1 until end of turn"), false, false);
        this.addEffect(new TapTargetEffect("tap up to one target creature defending player controls"));
        this.addTarget(new TargetPermanent(0, 1, new FilterCreaturePermanent("creature defending player controls")));
    }

    private StrategicInterventionTriggeredAbility(final StrategicInterventionTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public StrategicInterventionTriggeredAbility copy() {
        return new StrategicInterventionTriggeredAbility(this);
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        if (!super.checkTrigger(event, game)) {
            return false;
        }
        Permanent attacker = game.getPermanent(event.getSourceId());
        UUID defendingPlayerId = game.getCombat().getDefendingPlayerId(event.getSourceId(), game);
        if (attacker == null || defendingPlayerId == null) {
            return false;
        }
        // the boost goes to the attacker, the tap to the chosen target
        this.getEffects().get(0).setTargetPointer(new FixedTarget(attacker, game));
        FilterCreaturePermanent filter = new FilterCreaturePermanent("creature defending player controls");
        filter.add(new ControllerIdPredicate(defendingPlayerId));
        this.getTargets().clear();
        this.addTarget(new TargetPermanent(0, 1, filter));
        return true;
    }
}

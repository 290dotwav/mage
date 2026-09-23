package mage.cards.m;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.TriggeredAbilityImpl;
import mage.abilities.condition.common.MonarchIsNotSetCondition;
import mage.abilities.effects.common.BecomesMonarchTargetEffect;
import mage.abilities.effects.common.continuous.BoostTargetEffect;
import mage.abilities.effects.common.continuous.GainAbilityTargetEffect;
import mage.abilities.hint.common.MonarchHint;
import mage.abilities.keyword.TrampleAbility;
import mage.abilities.triggers.BeginningOfEndStepTriggeredAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.TargetController;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.target.common.TargetOpponent;
import mage.target.targetpointer.FixedTarget;

import java.util.UUID;

/**
 * @author Claude
 */
public final class MBakuJabariChieftain extends CardImpl {

    public MBakuJabariChieftain(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{1}{G}{G}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.NOBLE);
        this.subtype.add(SubType.WARRIOR);
        this.power = new MageInt(4);
        this.toughness = new MageInt(3);

        // At the beginning of your end step, if there is no monarch, target opponent becomes the monarch.
        Ability ability = new BeginningOfEndStepTriggeredAbility(
                TargetController.YOU, new BecomesMonarchTargetEffect(), false, MonarchIsNotSetCondition.instance
        );
        ability.addTarget(new TargetOpponent());
        ability.addHint(MonarchHint.instance);
        this.addAbility(ability);

        // Whenever a creature attacks one of your opponents, if that player is the monarch, that creature gets +1/+1 and gains trample until end of turn.
        this.addAbility(new MBakuJabariChieftainTriggeredAbility());
    }

    private MBakuJabariChieftain(final MBakuJabariChieftain card) {
        super(card);
    }

    @Override
    public MBakuJabariChieftain copy() {
        return new MBakuJabariChieftain(this);
    }
}

class MBakuJabariChieftainTriggeredAbility extends TriggeredAbilityImpl {

    MBakuJabariChieftainTriggeredAbility() {
        super(Zone.BATTLEFIELD, new BoostTargetEffect(1, 1).setText("that creature gets +1/+1"));
        this.addEffect(new GainAbilityTargetEffect(TrampleAbility.getInstance()).setText("and gains trample until end of turn"));
        setTriggerPhrase("Whenever a creature attacks one of your opponents, if that player is the monarch, ");
    }

    private MBakuJabariChieftainTriggeredAbility(final MBakuJabariChieftainTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public MBakuJabariChieftainTriggeredAbility copy() {
        return new MBakuJabariChieftainTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.ATTACKER_DECLARED;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        UUID defenderId = event.getTargetId();
        if (!game.getOpponents(getControllerId()).contains(defenderId)
                || !defenderId.equals(game.getMonarchId())) {
            return false;
        }
        this.getEffects().setTargetPointer(new FixedTarget(event.getSourceId(), game));
        this.getEffects().setValue("defenderId", defenderId);
        return true;
    }

    @Override
    public boolean checkInterveningIfClause(Game game) {
        Object defenderId = getEffects().get(0).getValue("defenderId");
        return defenderId != null && defenderId.equals(game.getMonarchId());
    }
}

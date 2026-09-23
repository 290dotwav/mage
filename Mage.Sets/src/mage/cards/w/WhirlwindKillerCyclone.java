package mage.cards.w;

import mage.MageInt;
import mage.abilities.TriggeredAbilityImpl;
import mage.abilities.effects.common.combat.CantBlockTargetEffect;
import mage.abilities.keyword.HasteAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.filter.common.FilterCreaturePermanent;
import mage.filter.predicate.permanent.ControllerIdPredicate;
import mage.game.Game;
import mage.game.events.DefenderAttackedEvent;
import mage.game.events.GameEvent;
import mage.target.TargetPermanent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class WhirlwindKillerCyclone extends CardImpl {

    public WhirlwindKillerCyclone(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{R}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.MUTANT);
        this.subtype.add(SubType.VILLAIN);
        this.power = new MageInt(2);
        this.toughness = new MageInt(3);

        // Haste
        this.addAbility(HasteAbility.getInstance());

        // Whenever one or more creatures you control that entered this turn attack a player, target creature that player controls can't block this turn.
        this.addAbility(new WhirlwindKillerCycloneTriggeredAbility());
    }

    private WhirlwindKillerCyclone(final WhirlwindKillerCyclone card) {
        super(card);
    }

    @Override
    public WhirlwindKillerCyclone copy() {
        return new WhirlwindKillerCyclone(this);
    }
}

class WhirlwindKillerCycloneTriggeredAbility extends TriggeredAbilityImpl {

    WhirlwindKillerCycloneTriggeredAbility() {
        super(Zone.BATTLEFIELD, new CantBlockTargetEffect(Duration.EndOfTurn)
                .setText("target creature that player controls can't block this turn"));
        setTriggerPhrase("Whenever one or more creatures you control that entered this turn attack a player, ");
    }

    private WhirlwindKillerCycloneTriggeredAbility(final WhirlwindKillerCycloneTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public WhirlwindKillerCycloneTriggeredAbility copy() {
        return new WhirlwindKillerCycloneTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.DEFENDER_ATTACKED;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        UUID defenderId = event.getTargetId();
        if (game.getPlayer(defenderId) == null
                || ((DefenderAttackedEvent) event)
                .getAttackers(game)
                .stream()
                .noneMatch(p -> p.isControlledBy(getControllerId()) && p.getTurnsOnBattlefield() == 0)) {
            return false;
        }
        FilterCreaturePermanent filter = new FilterCreaturePermanent("creature that player controls");
        filter.add(new ControllerIdPredicate(defenderId));
        this.getTargets().clear();
        this.addTarget(new TargetPermanent(filter));
        return true;
    }
}

package mage.cards.r;

import mage.abilities.Ability;
import mage.abilities.BatchTriggeredAbility;
import mage.abilities.TriggeredAbilityImpl;
import mage.abilities.common.PlanCounterThresholdTriggeredAbility;
import mage.abilities.effects.common.CreateTokenEffect;
import mage.abilities.effects.common.DrawCardSourceControllerEffect;
import mage.abilities.effects.common.LoseLifeSourceControllerEffect;
import mage.abilities.effects.common.SacrificeSourceEffect;
import mage.abilities.effects.common.counter.AddCountersSourceEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.events.ZoneChangeBatchEvent;
import mage.game.events.ZoneChangeEvent;
import mage.game.permanent.token.RobotVillainToken;

import java.util.UUID;

/**
 * @author Claude
 */
public final class RobotDomination extends CardImpl {

    public RobotDomination(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ENCHANTMENT}, "{3}{B}");

        this.subtype.add(SubType.PLAN);

        // Whenever one or more creature cards are put into your graveyard from anywhere, you draw a card, lose 1 life, and put a plan counter on this enchantment.
        this.addAbility(new RobotDominationTriggeredAbility());

        // When the third plan counter is put on this enchantment, sacrifice it and create three 2/2 colorless Robot Villain artifact creature tokens.
        Ability planAbility = new PlanCounterThresholdTriggeredAbility(
                3, new SacrificeSourceEffect().setText("sacrifice it")
        );
        planAbility.addEffect(new CreateTokenEffect(new RobotVillainToken(), 3).concatBy("and"));
        this.addAbility(planAbility);
    }

    private RobotDomination(final RobotDomination card) {
        super(card);
    }

    @Override
    public RobotDomination copy() {
        return new RobotDomination(this);
    }
}

class RobotDominationTriggeredAbility extends TriggeredAbilityImpl implements BatchTriggeredAbility<ZoneChangeEvent> {

    RobotDominationTriggeredAbility() {
        super(Zone.BATTLEFIELD, new DrawCardSourceControllerEffect(1, true));
        this.addEffect(new LoseLifeSourceControllerEffect(1).setText(", lose 1 life"));
        this.addEffect(new AddCountersSourceEffect(CounterType.PLAN.createInstance())
                .setText(", and put a plan counter on this enchantment"));
        setTriggerPhrase("Whenever one or more creature cards are put into your graveyard from anywhere, ");
    }

    private RobotDominationTriggeredAbility(final RobotDominationTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public RobotDominationTriggeredAbility copy() {
        return new RobotDominationTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.ZONE_CHANGE_BATCH;
    }

    @Override
    public boolean checkEvent(ZoneChangeEvent event, Game game) {
        if (!Zone.GRAVEYARD.match(event.getToZone())) {
            return false;
        }
        Card card = game.getCard(event.getTargetId());
        return card != null
                && isControlledBy(card.getOwnerId())
                && card.isCreature(game);
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        return !getFilteredEvents((ZoneChangeBatchEvent) event, game).isEmpty();
    }
}

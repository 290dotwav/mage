package mage.cards.h;

import mage.MageInt;
import mage.abilities.TriggeredAbilityImpl;
import mage.abilities.common.AttacksTriggeredAbility;
import mage.abilities.dynamicvalue.common.SavedDamageValue;
import mage.abilities.effects.common.continuous.GainAbilitySourceEffect;
import mage.abilities.effects.common.counter.AddCountersSourceEffect;
import mage.abilities.keyword.IndestructibleAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.watchers.common.DamageDoneWatcher;

import java.util.UUID;

/**
 * @author Claude
 */
public final class HerculesOlympianHero extends CardImpl {

    public HerculesOlympianHero(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{3}{W}{W}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.DEMIGOD);
        this.subtype.add(SubType.WARRIOR);
        this.subtype.add(SubType.HERO);
        this.power = new MageInt(3);
        this.toughness = new MageInt(3);

        // Whenever Hercules attacks, put a +1/+1 counter on him. He gains indestructible until end of turn.
        AttacksTriggeredAbility ability = new AttacksTriggeredAbility(
                new AddCountersSourceEffect(CounterType.P1P1.createInstance()).setText("put a +1/+1 counter on him")
        );
        ability.addEffect(new GainAbilitySourceEffect(IndestructibleAbility.getInstance(), Duration.EndOfTurn)
                .setText("He gains indestructible until end of turn"));
        this.addAbility(ability);

        // Whenever Hercules is dealt damage for the first time each turn, put that many +1/+1 counters on him.
        this.addAbility(new HerculesOlympianHeroTriggeredAbility(), new DamageDoneWatcher());
    }

    private HerculesOlympianHero(final HerculesOlympianHero card) {
        super(card);
    }

    @Override
    public HerculesOlympianHero copy() {
        return new HerculesOlympianHero(this);
    }
}

class HerculesOlympianHeroTriggeredAbility extends TriggeredAbilityImpl {

    HerculesOlympianHeroTriggeredAbility() {
        super(Zone.BATTLEFIELD, new AddCountersSourceEffect(CounterType.P1P1.createInstance(), SavedDamageValue.MANY)
                .setText("put that many +1/+1 counters on him. <i>(He must survive the damage to get the counters.)</i>"));
        setTriggerPhrase("Whenever {this} is dealt damage for the first time each turn, ");
    }

    private HerculesOlympianHeroTriggeredAbility(final HerculesOlympianHeroTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public HerculesOlympianHeroTriggeredAbility copy() {
        return new HerculesOlympianHeroTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.DAMAGED_BATCH_FOR_ONE_PERMANENT;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        if (!getSourceId().equals(event.getTargetId())) {
            return false;
        }
        Permanent permanent = game.getPermanentOrLKIBattlefield(getSourceId());
        DamageDoneWatcher watcher = game.getState().getWatcher(DamageDoneWatcher.class);
        if (permanent == null || watcher == null) {
            return false;
        }
        // the watcher already counts this batch: it is the first time only if it is all the damage dealt to him this turn
        if (watcher.damageDoneTo(permanent.getId(), permanent.getZoneChangeCounter(game), game) != event.getAmount()) {
            return false;
        }
        this.getEffects().setValue("damage", event.getAmount());
        return true;
    }
}

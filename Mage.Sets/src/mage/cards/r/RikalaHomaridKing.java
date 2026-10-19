package mage.cards.r;

import mage.MageInt;
import mage.Mana;
import mage.abilities.Ability;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.dynamicvalue.common.CountersSourceCount;
import mage.abilities.dynamicvalue.common.StaticValue;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.continuous.BoostControlledEffect;
import mage.abilities.effects.mana.BasicManaEffect;
import mage.abilities.mana.TriggeredManaAbility;
import mage.abilities.triggers.BeginningOfUpkeepTriggeredAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.events.TappedForManaEvent;
import mage.game.permanent.Permanent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class RikalaHomaridKing extends CardImpl {

    public RikalaHomaridKing(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{1}{G}{U}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.LOBSTER);
        this.subtype.add(SubType.NOBLE);
        this.power = new MageInt(0);
        this.toughness = new MageInt(4);

        // At the beginning of your upkeep, put a tide counter on Rikala. Then if there are four or more tide counters on Rikala, remove them.
        this.addAbility(new BeginningOfUpkeepTriggeredAbility(new RikalaHomaridKingEffect()));

        // Creatures you control get +1/+0 for each tide counter on Rikala.
        this.addAbility(new SimpleStaticAbility(new BoostControlledEffect(
                new CountersSourceCount(CounterType.TIDE), StaticValue.get(0), Duration.WhileOnBattlefield
        ).setText("creatures you control get +1/+0 for each tide counter on {this}")));

        // Whenever you tap an Island for mana, if there are three or more tide counters on Rikala, add an additional {U}.
        this.addAbility(new RikalaHomaridKingManaAbility());
    }

    private RikalaHomaridKing(final RikalaHomaridKing card) {
        super(card);
    }

    @Override
    public RikalaHomaridKing copy() {
        return new RikalaHomaridKing(this);
    }
}

class RikalaHomaridKingEffect extends OneShotEffect {

    RikalaHomaridKingEffect() {
        super(Outcome.Benefit);
        staticText = "put a tide counter on {this}. Then if there are four or more tide counters on {this}, remove them";
    }

    private RikalaHomaridKingEffect(final RikalaHomaridKingEffect effect) {
        super(effect);
    }

    @Override
    public RikalaHomaridKingEffect copy() {
        return new RikalaHomaridKingEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent permanent = source.getSourcePermanentIfItStillExists(game);
        if (permanent == null) {
            return false;
        }
        permanent.addCounters(CounterType.TIDE.createInstance(), source, game);
        int tide = permanent.getCounters(game).getCount(CounterType.TIDE);
        if (tide >= 4) {
            permanent.removeCounters(CounterType.TIDE.getName(), tide, source, game);
        }
        return true;
    }
}

class RikalaHomaridKingManaAbility extends TriggeredManaAbility {

    RikalaHomaridKingManaAbility() {
        super(Zone.BATTLEFIELD, new BasicManaEffect(Mana.BlueMana(1)), false);
    }

    private RikalaHomaridKingManaAbility(final RikalaHomaridKingManaAbility ability) {
        super(ability);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.TAPPED_FOR_MANA;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        if (!isControlledBy(event.getPlayerId())) {
            return false;
        }
        Permanent rikala = getSourcePermanentIfItStillExists(game);
        if (rikala == null || rikala.getCounters(game).getCount(CounterType.TIDE) < 3) {
            return false;
        }
        Permanent permanent = ((TappedForManaEvent) event).getPermanent();
        return permanent != null && permanent.hasSubtype(SubType.ISLAND, game);
    }

    @Override
    public RikalaHomaridKingManaAbility copy() {
        return new RikalaHomaridKingManaAbility(this);
    }

    @Override
    public String getRule() {
        return "Whenever you tap an Island for mana, if there are three or more tide counters on {this}, add an additional {U}.";
    }
}

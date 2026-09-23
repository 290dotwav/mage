package mage.cards.m;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.Mode;
import mage.abilities.triggers.BeginningOfUpkeepTriggeredAbility;
import mage.abilities.costs.common.RemoveCounterCost;
import mage.abilities.effects.common.DoIfCostPaid;
import mage.abilities.effects.common.DrawCardSourceControllerEffect;
import mage.abilities.effects.common.counter.AddCountersSourceEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.counters.CounterType;
import mage.target.common.TargetControlledCreaturePermanent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class MisterHydeMonsterWithin extends CardImpl {

    public MisterHydeMonsterWithin(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{G}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.VILLAIN);
        this.power = new MageInt(2);
        this.toughness = new MageInt(2);

        // At the beginning of your upkeep, choose one --
        // * Put a +1/+1 counter on Mister Hyde.
        Ability ability = new BeginningOfUpkeepTriggeredAbility(
                new AddCountersSourceEffect(CounterType.P1P1.createInstance())
        );

        // * Remove a counter from a creature you control. If you do, draw a card.
        ability.addMode(new Mode(new DoIfCostPaid(
                new DrawCardSourceControllerEffect(1),
                new RemoveCounterCost(new TargetControlledCreaturePermanent().withTargetName("a creature you control")),
                null, false
        )));
        this.addAbility(ability);
    }

    private MisterHydeMonsterWithin(final MisterHydeMonsterWithin card) {
        super(card);
    }

    @Override
    public MisterHydeMonsterWithin copy() {
        return new MisterHydeMonsterWithin(this);
    }
}

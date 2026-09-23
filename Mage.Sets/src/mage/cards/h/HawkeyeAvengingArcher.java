package mage.cards.h;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.DealtDamageAndDiedTriggeredAbility;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.costs.common.TapSourceCost;
import mage.abilities.effects.common.DamageTargetEffect;
import mage.abilities.effects.common.DrawCardSourceControllerEffect;
import mage.abilities.keyword.ReachAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.SetTargetPointer;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.filter.StaticFilters;
import mage.target.common.TargetAnyTarget;

import java.util.UUID;

/**
 * @author Claude
 */
public final class HawkeyeAvengingArcher extends CardImpl {

    public HawkeyeAvengingArcher(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{U}{R}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.ARCHER);
        this.subtype.add(SubType.HERO);
        this.power = new MageInt(3);
        this.toughness = new MageInt(4);

        // Reach
        this.addAbility(ReachAbility.getInstance());

        // Whenever a creature an opponent controls dies, if Hawkeye dealt damage to it this turn, draw a card.
        this.addAbility(new DealtDamageAndDiedTriggeredAbility(
                new DrawCardSourceControllerEffect(1), false,
                StaticFilters.FILTER_OPPONENTS_PERMANENT_CREATURE, SetTargetPointer.NONE
        ).setTriggerPhrase("Whenever a creature an opponent controls dies, if {this} dealt damage to it this turn, "));

        // {T}: Hawkeye deals 1 damage to any target.
        Ability ability = new SimpleActivatedAbility(new DamageTargetEffect(1), new TapSourceCost());
        ability.addTarget(new TargetAnyTarget());
        this.addAbility(ability);
    }

    private HawkeyeAvengingArcher(final HawkeyeAvengingArcher card) {
        super(card);
    }

    @Override
    public HawkeyeAvengingArcher copy() {
        return new HawkeyeAvengingArcher(this);
    }
}

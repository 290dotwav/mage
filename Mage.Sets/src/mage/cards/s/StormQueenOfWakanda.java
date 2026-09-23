package mage.cards.s;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.AttackedByCreatureTriggeredAbility;
import mage.abilities.common.AttacksTriggeredAbility;
import mage.abilities.dynamicvalue.common.SourcePermanentPowerValue;
import mage.abilities.dynamicvalue.common.StaticValue;
import mage.abilities.effects.common.DamageTargetEffect;
import mage.abilities.effects.common.continuous.BoostTargetEffect;
import mage.abilities.effects.common.continuous.GainAbilityTargetEffect;
import mage.abilities.keyword.FlyingAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.SetTargetPointer;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.filter.common.FilterAttackingCreature;
import mage.filter.common.FilterCreaturePermanent;
import mage.filter.predicate.mageobject.AbilityPredicate;
import mage.filter.predicate.mageobject.AnotherPredicate;
import mage.target.TargetPermanent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class StormQueenOfWakanda extends CardImpl {

    private static final FilterAttackingCreature filterAttacking = new FilterAttackingCreature("another target attacking creature");
    private static final FilterCreaturePermanent filterFlying = new FilterCreaturePermanent("creature with flying");

    static {
        filterAttacking.add(AnotherPredicate.instance);
        filterFlying.add(new AbilityPredicate(FlyingAbility.class));
    }

    public StormQueenOfWakanda(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{3}{G}{W}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.MUTANT);
        this.subtype.add(SubType.NOBLE);
        this.subtype.add(SubType.HERO);
        this.power = new MageInt(4);
        this.toughness = new MageInt(5);

        // Flying
        this.addAbility(FlyingAbility.getInstance());

        // Whenever Storm attacks, until end of turn, another target attacking creature gains flying and gets +X/+0, where X is Storm's power.
        Ability ability = new AttacksTriggeredAbility(new GainAbilityTargetEffect(FlyingAbility.getInstance(), Duration.EndOfTurn)
                .setText("until end of turn, another target attacking creature gains flying"));
        ability.addEffect(new BoostTargetEffect(SourcePermanentPowerValue.NOT_NEGATIVE, StaticValue.get(0), Duration.EndOfTurn)
                .setText("and gets +X/+0, where X is {this}'s power"));
        ability.addTarget(new TargetPermanent(filterAttacking));
        this.addAbility(ability);

        // Whenever a creature with flying attacks you, Storm deals damage equal to her power to that creature.
        this.addAbility(new AttackedByCreatureTriggeredAbility(
                Zone.BATTLEFIELD,
                new DamageTargetEffect(SourcePermanentPowerValue.NOT_NEGATIVE)
                        .setText("{this} deals damage equal to her power to that creature"),
                false, SetTargetPointer.PERMANENT, filterFlying
        ));
    }

    private StormQueenOfWakanda(final StormQueenOfWakanda card) {
        super(card);
    }

    @Override
    public StormQueenOfWakanda copy() {
        return new StormQueenOfWakanda(this);
    }
}

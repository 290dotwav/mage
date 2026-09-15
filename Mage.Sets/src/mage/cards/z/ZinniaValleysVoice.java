package mage.cards.z;

import mage.MageInt;
import mage.MageObjectReference;
import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldControlledTriggeredAbility;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.dynamicvalue.common.PermanentsOnBattlefieldCount;
import mage.abilities.dynamicvalue.common.StaticValue;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.CreateTokenCopyTargetEffect;
import mage.abilities.effects.common.continuous.BoostSourceEffect;
import mage.abilities.effects.common.continuous.GainAbilityControlledSpellsEffect;
import mage.abilities.keyword.FlyingAbility;
import mage.abilities.keyword.OffspringAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.*;
import mage.filter.FilterPermanent;
import mage.filter.StaticFilters;
import mage.filter.common.FilterNonlandCard;
import mage.filter.predicate.mageobject.AnotherPredicate;
import mage.filter.predicate.mageobject.BasePowerPredicate;
import mage.game.Game;
import mage.game.permanent.Permanent;

import java.util.Collections;
import java.util.UUID;

/**
 * @author ClaudeMTG
 */
public final class ZinniaValleysVoice extends CardImpl {

    private static final FilterPermanent filter
            = new FilterPermanent("other creatures you control with base power 1");
    private static final FilterNonlandCard spellFilter = new FilterNonlandCard("Creature spells");

    static {
        filter.add(CardType.CREATURE.getPredicate());
        filter.add(TargetController.YOU.getControllerPredicate());
        filter.add(AnotherPredicate.instance);
        filter.add(new BasePowerPredicate(ComparisonType.EQUAL_TO, 1));

        spellFilter.add(CardType.CREATURE.getPredicate());
    }

    public ZinniaValleysVoice(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{U}{R}{W}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.BIRD);
        this.subtype.add(SubType.BARD);
        this.power = new MageInt(1);
        this.toughness = new MageInt(3);

        // Flying
        this.addAbility(FlyingAbility.getInstance());

        // Zinnia gets +X/+0, where X is the number of other creatures you control with base power 1.
        this.addAbility(new SimpleStaticAbility(new BoostSourceEffect(
                new PermanentsOnBattlefieldCount(filter), StaticValue.get(0), Duration.WhileOnBattlefield
        ).setText("{this} gets +X/+0, where X is the number of other creatures you control with base power 1")));

        // Creature spells you cast gain offspring {2} as you cast them.
        this.addAbility(new SimpleStaticAbility(new GainAbilityControlledSpellsEffect(
                new OffspringAbility("{2}"), spellFilter
        ).setText("creature spells you cast gain offspring {2} as you cast them. "
                + "<i>(You may pay an additional {2} as you cast a creature spell. If you do, "
                + "when that creature enters, create a 1/1 token copy of it.)</i>")));

        // A granted keyword is lost when the card changes zone, so the offspring token part is
        // made here instead of by the offspring ability of the entering creature itself.
        this.addAbility(new EntersBattlefieldControlledTriggeredAbility(
                Zone.BATTLEFIELD, new ZinniaValleysVoiceOffspringEffect(),
                StaticFilters.FILTER_PERMANENT_CREATURE, false, SetTargetPointer.PERMANENT
        ).setRuleVisible(false));
    }

    private ZinniaValleysVoice(final ZinniaValleysVoice card) {
        super(card);
    }

    @Override
    public ZinniaValleysVoice copy() {
        return new ZinniaValleysVoice(this);
    }
}

class ZinniaValleysVoiceOffspringEffect extends OneShotEffect {

    ZinniaValleysVoiceOffspringEffect() {
        super(Outcome.PutCreatureInPlay);
        staticText = "create a 1/1 token copy of it";
    }

    private ZinniaValleysVoiceOffspringEffect(final ZinniaValleysVoiceOffspringEffect effect) {
        super(effect);
    }

    @Override
    public ZinniaValleysVoiceOffspringEffect copy() {
        return new ZinniaValleysVoiceOffspringEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent permanent = getTargetPointer().getFirstTargetPermanentOrLKI(game, source);
        if (permanent == null
                || !permanent.isControlledBy(source.getControllerId())) {
            return false;
        }
        // a creature that has offspring of its own makes its own token
        if (permanent.getMainCard().getAbilities().stream().anyMatch(OffspringAbility.class::isInstance)) {
            return false;
        }
        if (!game.getPermanentCostsTags()
                .getOrDefault(new MageObjectReference(permanent, game, -1), Collections.emptyMap())
                .containsKey(OffspringAbility.OFFSPRING_ACTIVATION_VALUE_KEY)) {
            return false;
        }
        return new CreateTokenCopyTargetEffect(
                null, null, false, 1, false,
                false, null, 1, 1, false
        ).setSavedPermanent(permanent).apply(game, source);
    }
}

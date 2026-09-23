package mage.cards.h;

import mage.MageInt;
import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.common.SpellCastControllerTriggeredAbility;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.keyword.FlyingAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.target.TargetPermanent;
import mage.util.functions.CopyApplier;

import java.util.UUID;

/**
 * @author Claude
 */
public final class HulklingYoungAvenger extends CardImpl {

    public HulklingYoungAvenger(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{R}{R}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.KREE);
        this.subtype.add(SubType.SKRULL);
        this.subtype.add(SubType.HERO);
        this.power = new MageInt(4);
        this.toughness = new MageInt(4);

        // Flying
        this.addAbility(FlyingAbility.getInstance());

        // Whenever you cast a noncreature spell, Hulkling becomes a copy of up to one other target creature until end of turn, except his name is Hulkling, Young Avenger, he's 4/4, and he has flying and this ability.
        this.addAbility(makeAbility());
    }

    private HulklingYoungAvenger(final HulklingYoungAvenger card) {
        super(card);
    }

    @Override
    public HulklingYoungAvenger copy() {
        return new HulklingYoungAvenger(this);
    }

    static Ability makeAbility() {
        Ability ability = new SpellCastControllerTriggeredAbility(
                new HulklingYoungAvengerEffect(), StaticFilters.FILTER_SPELL_A_NON_CREATURE, false
        );
        ability.addTarget(new TargetPermanent(0, 1, StaticFilters.FILTER_ANOTHER_CREATURE));
        return ability;
    }
}

class HulklingYoungAvengerEffect extends OneShotEffect {

    private static final CopyApplier applier = new CopyApplier() {
        @Override
        public boolean apply(Game game, MageObject blueprint, Ability source, UUID copyToObjectId) {
            blueprint.setName("Hulkling, Young Avenger");
            // 707.9d: a copied power/toughness characteristic-defining ability would override the 4/4
            blueprint.removePTCDA();
            blueprint.getPower().setModifiedBaseValue(4);
            blueprint.getToughness().setModifiedBaseValue(4);
            blueprint.getAbilities().add(FlyingAbility.getInstance());
            blueprint.getAbilities().add(HulklingYoungAvenger.makeAbility());
            return true;
        }
    };

    HulklingYoungAvengerEffect() {
        super(Outcome.Copy);
        staticText = "{this} becomes a copy of up to one other target creature until end of turn, "
                + "except his name is Hulkling, Young Avenger, he's 4/4, and he has flying and this ability";
    }

    private HulklingYoungAvengerEffect(final HulklingYoungAvengerEffect effect) {
        super(effect);
    }

    @Override
    public HulklingYoungAvengerEffect copy() {
        return new HulklingYoungAvengerEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent permanent = source.getSourcePermanentIfItStillExists(game);
        Permanent target = game.getPermanent(getTargetPointer().getFirst(game, source));
        if (permanent == null || target == null) {
            return false;
        }
        game.copyPermanent(Duration.EndOfTurn, target, permanent.getId(), source, applier);
        return true;
    }
}

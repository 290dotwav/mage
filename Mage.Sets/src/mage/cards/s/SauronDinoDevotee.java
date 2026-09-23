package mage.cards.s;

import mage.MageInt;
import mage.ObjectColor;
import mage.abilities.Ability;
import mage.abilities.Mode;
import mage.abilities.common.EntersBattlefieldOrAttacksSourceTriggeredAbility;
import mage.abilities.effects.ContinuousEffectImpl;
import mage.abilities.effects.common.GainLifeEffect;
import mage.abilities.effects.common.counter.AddCountersTargetEffect;
import mage.abilities.keyword.FlyingAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Layer;
import mage.constants.Outcome;
import mage.constants.SubLayer;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.counters.CounterType;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.target.TargetPermanent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class SauronDinoDevotee extends CardImpl {

    public SauronDinoDevotee(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{3}{G}{G}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.DINOSAUR);
        this.subtype.add(SubType.VAMPIRE);
        this.subtype.add(SubType.VILLAIN);
        this.power = new MageInt(4);
        this.toughness = new MageInt(4);

        // Flying
        this.addAbility(FlyingAbility.getInstance());

        // Whenever Sauron enters or attacks, choose one --
        // * Cure Cancer -- You gain 3 life.
        Ability ability = new EntersBattlefieldOrAttacksSourceTriggeredAbility(new GainLifeEffect(3));
        ability.withFirstModeFlavorWord("Cure Cancer");

        // * Turn People into Dinosaurs -- Put a saurian counter on another target creature. It's a green Dinosaur with base power and toughness 5/5 for as long as it has a saurian counter on it.
        Mode mode = new Mode(new AddCountersTargetEffect(CounterType.SAURIAN.createInstance()));
        mode.addEffect(new SauronDinoDevoteeEffect());
        mode.addTarget(new TargetPermanent(StaticFilters.FILTER_ANOTHER_TARGET_CREATURE));
        ability.addMode(mode.withFlavorWord("Turn People into Dinosaurs"));
        this.addAbility(ability);
    }

    private SauronDinoDevotee(final SauronDinoDevotee card) {
        super(card);
    }

    @Override
    public SauronDinoDevotee copy() {
        return new SauronDinoDevotee(this);
    }
}

class SauronDinoDevoteeEffect extends ContinuousEffectImpl {

    SauronDinoDevoteeEffect() {
        super(Duration.Custom, Outcome.Neutral);
        staticText = "It's a green Dinosaur with base power and toughness 5/5 for as long as it has a saurian counter on it";
    }

    private SauronDinoDevoteeEffect(final SauronDinoDevoteeEffect effect) {
        super(effect);
    }

    @Override
    public SauronDinoDevoteeEffect copy() {
        return new SauronDinoDevoteeEffect(this);
    }

    @Override
    public boolean isInactive(Ability source, Game game) {
        Permanent permanent = game.getPermanent(getTargetPointer().getFirst(game, source));
        return permanent == null || permanent.getCounters(game).getCount(CounterType.SAURIAN) < 1;
    }

    @Override
    public boolean apply(Layer layer, SubLayer sublayer, Ability source, Game game) {
        Permanent permanent = game.getPermanent(getTargetPointer().getFirst(game, source));
        if (permanent == null || permanent.getCounters(game).getCount(CounterType.SAURIAN) < 1) {
            return false;
        }
        switch (layer) {
            case TypeChangingEffects_4:
                // "It's a Dinosaur": the creature type replaces its other creature types (205.1b)
                permanent.removeAllCreatureTypes(game);
                permanent.addSubType(game, SubType.DINOSAUR);
                break;
            case ColorChangingEffects_5:
                permanent.getColor(game).setColor(ObjectColor.GREEN);
                break;
            case PTChangingEffects_7:
                if (sublayer == SubLayer.SetPT_7b) {
                    permanent.getPower().setModifiedBaseValue(5);
                    permanent.getToughness().setModifiedBaseValue(5);
                }
                break;
        }
        return true;
    }

    @Override
    public boolean apply(Game game, Ability source) {
        return false;
    }

    @Override
    public boolean hasLayer(Layer layer) {
        return layer == Layer.TypeChangingEffects_4
                || layer == Layer.ColorChangingEffects_5
                || layer == Layer.PTChangingEffects_7;
    }
}

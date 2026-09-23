package mage.cards.g;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.AttacksTriggeredAbility;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.continuous.SetBasePowerToughnessTargetEffect;
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
import mage.target.targetpointer.FixedTarget;

import java.util.UUID;

/**
 * @author Claude
 */
public final class GalionElvenkingsButler extends CardImpl {

    public GalionElvenkingsButler(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{G}{G}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.ELF);
        this.subtype.add(SubType.ADVISOR);
        this.power = new MageInt(4);
        this.toughness = new MageInt(4);

        // Whenever Galion attacks, choose up to one other target creature you control. Its base power and toughness become equal to Galion's power and toughness until end of turn.
        Ability ability = new AttacksTriggeredAbility(new GalionElvenkingsButlerEffect());
        ability.addTarget(new TargetPermanent(0, 1, StaticFilters.FILTER_ANOTHER_TARGET_CREATURE_YOU_CONTROL));
        this.addAbility(ability);
    }

    private GalionElvenkingsButler(final GalionElvenkingsButler card) {
        super(card);
    }

    @Override
    public GalionElvenkingsButler copy() {
        return new GalionElvenkingsButler(this);
    }
}

class GalionElvenkingsButlerEffect extends OneShotEffect {

    GalionElvenkingsButlerEffect() {
        super(Outcome.BoostCreature);
        staticText = "choose up to one other target creature you control. Its base power and toughness " +
                "become equal to {this}'s power and toughness until end of turn";
    }

    private GalionElvenkingsButlerEffect(final GalionElvenkingsButlerEffect effect) {
        super(effect);
    }

    @Override
    public GalionElvenkingsButlerEffect copy() {
        return new GalionElvenkingsButlerEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent target = game.getPermanent(getTargetPointer().getFirst(game, source));
        Permanent galion = source.getSourcePermanentOrLKI(game);
        if (target == null || galion == null) {
            return false;
        }
        // the values are locked in as the ability resolves
        game.addEffect(new SetBasePowerToughnessTargetEffect(
                galion.getPower().getValue(), galion.getToughness().getValue(), Duration.EndOfTurn
        ).setTargetPointer(new FixedTarget(target, game)), source);
        return true;
    }
}

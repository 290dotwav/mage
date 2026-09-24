package mage.cards.b;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.common.delayed.AtTheBeginOfNextEndStepDelayedTriggeredAbility;
import mage.abilities.costs.common.TapSourceCost;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.ContinuousEffect;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.ReturnToHandTargetEffect;
import mage.abilities.effects.common.continuous.GainAbilityTargetEffect;
import mage.abilities.keyword.LandwalkAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.choices.Choice;
import mage.choices.ChoiceLandType;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.filter.common.FilterControlledLandPermanent;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.common.TargetControlledCreaturePermanent;
import mage.target.targetpointer.FixedTarget;

import java.util.UUID;

/**
 * @author Claude
 */
public final class BarbarianGuides extends CardImpl {

    public BarbarianGuides(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{R}");

        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.BARBARIAN);
        this.power = new MageInt(1);
        this.toughness = new MageInt(2);

        // {2}{R}, {T}: Choose a land type. Target creature you control gains snow landwalk of the chosen type until end of turn. Return that creature to its owner's hand at the beginning of the next end step.
        Ability ability = new SimpleActivatedAbility(new BarbarianGuidesEffect(), new ManaCostsImpl<>("{2}{R}"));
        ability.addCost(new TapSourceCost());
        ability.addTarget(new TargetControlledCreaturePermanent());
        this.addAbility(ability);
    }

    private BarbarianGuides(final BarbarianGuides card) {
        super(card);
    }

    @Override
    public BarbarianGuides copy() {
        return new BarbarianGuides(this);
    }
}

class BarbarianGuidesEffect extends OneShotEffect {

    BarbarianGuidesEffect() {
        super(Outcome.AddAbility);
        staticText = "choose a land type. Target creature you control gains snow landwalk of the chosen type " +
                "until end of turn. Return that creature to its owner's hand at the beginning of the next end step. " +
                "<i>(It can't be blocked as long as defending player controls a snow land of that type.)</i>";
    }

    private BarbarianGuidesEffect(final BarbarianGuidesEffect effect) {
        super(effect);
    }

    @Override
    public BarbarianGuidesEffect copy() {
        return new BarbarianGuidesEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        Permanent creature = game.getPermanent(getTargetPointer().getFirst(game, source));
        if (controller == null) {
            return false;
        }
        Choice choice = new ChoiceLandType();
        if (!controller.choose(outcome, choice, game)) {
            return false;
        }
        SubType landType = SubType.byDescription(choice.getChoice());
        if (landType == null) {
            return false;
        }
        game.informPlayers(controller.getLogName() + " chooses " + landType);
        if (creature != null) {
            FilterControlledLandPermanent filter = new FilterControlledLandPermanent("snow " + landType);
            filter.add(SuperType.SNOW.getPredicate());
            filter.add(landType.getPredicate());
            ContinuousEffect effect = new GainAbilityTargetEffect(new LandwalkAbility(filter), Duration.EndOfTurn);
            effect.setTargetPointer(new FixedTarget(creature, game));
            game.addEffect(effect, source);
            game.addDelayedTriggeredAbility(new AtTheBeginOfNextEndStepDelayedTriggeredAbility(
                    new ReturnToHandTargetEffect().setTargetPointer(new FixedTarget(creature, game))
                            .setText("return that creature to its owner's hand")
            ), source);
        }
        return true;
    }
}

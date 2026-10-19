package mage.cards.v;

import mage.abilities.Ability;
import mage.abilities.LoyaltyAbility;
import mage.abilities.common.CanBeYourCommanderAbility;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.effects.ReplacementEffectImpl;
import mage.abilities.effects.common.ExileReturnBattlefieldNextEndStepTargetEffect;
import mage.abilities.effects.common.ReturnToHandTargetEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.filter.common.FilterControlledPermanent;
import mage.filter.predicate.mageobject.AnotherPredicate;
import mage.game.Game;
import mage.game.events.EntersTheBattlefieldEvent;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.game.permanent.PermanentToken;
import mage.game.stack.Spell;
import mage.target.TargetPermanent;
import mage.target.common.TargetNonlandPermanent;
import mage.target.targetadjustment.ForEachPlayerTargetsAdjuster;
import mage.target.targetpointer.EachTargetPointer;

import java.util.UUID;

/**
 * @author Claude
 */
public final class VenserVisionaryTraveler extends CardImpl {

    private static final FilterControlledPermanent filter = new FilterControlledPermanent("other target permanent you control");

    static {
        filter.add(AnotherPredicate.instance);
    }

    public VenserVisionaryTraveler(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.PLANESWALKER}, "{3}{W}{U}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.VENSER);
        this.setStartingLoyalty(4);

        // Each nontoken creature you control that wasn't cast from your hand enters with two additional +1/+1 counters on it.
        this.addAbility(new SimpleStaticAbility(new VenserVisionaryTravelerEffect()));

        // +1: Exile up to one other target permanent you control. At the beginning of the next end step, return that card to the battlefield under its owner's control.
        Ability ability = new LoyaltyAbility(new ExileReturnBattlefieldNextEndStepTargetEffect(), 1);
        ability.addTarget(new TargetPermanent(0, 1, filter));
        this.addAbility(ability);

        // -2: For each opponent, return up to one target nonland permanent that player controls to its owner's hand.
        ability = new LoyaltyAbility(new ReturnToHandTargetEffect()
                .setTargetPointer(new EachTargetPointer())
                .setText("for each opponent, return up to one target nonland permanent that player controls to its owner's hand"), -2);
        ability.addTarget(new TargetNonlandPermanent(0, 1));
        ability.setTargetAdjuster(new ForEachPlayerTargetsAdjuster(false, true));
        this.addAbility(ability);

        // Venser, Visionary Traveler can be your commander.
        this.addAbility(CanBeYourCommanderAbility.getInstance());
    }

    private VenserVisionaryTraveler(final VenserVisionaryTraveler card) {
        super(card);
    }

    @Override
    public VenserVisionaryTraveler copy() {
        return new VenserVisionaryTraveler(this);
    }
}

class VenserVisionaryTravelerEffect extends ReplacementEffectImpl {

    VenserVisionaryTravelerEffect() {
        super(Duration.WhileOnBattlefield, Outcome.BoostCreature);
        staticText = "each nontoken creature you control that wasn't cast from your hand enters with two additional +1/+1 counters on it";
    }

    private VenserVisionaryTravelerEffect(final VenserVisionaryTravelerEffect effect) {
        super(effect);
    }

    @Override
    public VenserVisionaryTravelerEffect copy() {
        return new VenserVisionaryTravelerEffect(this);
    }

    @Override
    public boolean checksEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.ENTERS_THE_BATTLEFIELD;
    }

    @Override
    public boolean applies(GameEvent event, Ability source, Game game) {
        EntersTheBattlefieldEvent etbEvent = (EntersTheBattlefieldEvent) event;
        Permanent permanent = etbEvent.getTarget();
        if (permanent == null
                || permanent instanceof PermanentToken
                || !permanent.isCreature(game)
                || !permanent.isControlledBy(source.getControllerId())) {
            return false;
        }
        if (etbEvent.getFromZone() != Zone.STACK) {
            return true; // not cast at all
        }
        Spell spell = game.getSpellOrLKIStack(permanent.getId());
        return spell == null
                || spell.getFromZone() != Zone.HAND
                || !spell.isControlledBy(source.getControllerId());
    }

    @Override
    public boolean replaceEvent(GameEvent event, Ability source, Game game) {
        Permanent permanent = ((EntersTheBattlefieldEvent) event).getTarget();
        if (permanent != null) {
            permanent.addCounters(CounterType.P1P1.createInstance(2), source.getControllerId(), source, game, event.getAppliedEffects());
        }
        return false;
    }
}

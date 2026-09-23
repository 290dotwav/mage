package mage.cards.h;

import mage.MageObjectReference;
import mage.abilities.Ability;
import mage.abilities.DelayedTriggeredAbility;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.ReplacementEffectImpl;
import mage.abilities.effects.common.DrawCardSourceControllerEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.counters.Counter;
import mage.counters.Counters;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.events.DamageEvent;
import mage.game.events.GameEvent;
import mage.game.events.ZoneChangeEvent;
import mage.game.permanent.Permanent;
import mage.target.TargetPermanent;
import mage.target.common.TargetControlledCreaturePermanent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class HeroicSacrifice extends CardImpl {

    public HeroicSacrifice(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.INSTANT}, "{1}{W}");

        // Choose target creature you control. Until end of turn, all damage that would be dealt to you and creatures you control is dealt to the chosen creature instead (if it's still on the battlefield). When that creature dies this turn, put its counters on up to one target creature you control and draw a card.
        this.getSpellAbility().addEffect(new HeroicSacrificeEffect());
        this.getSpellAbility().addTarget(new TargetControlledCreaturePermanent());
    }

    private HeroicSacrifice(final HeroicSacrifice card) {
        super(card);
    }

    @Override
    public HeroicSacrifice copy() {
        return new HeroicSacrifice(this);
    }
}

class HeroicSacrificeEffect extends OneShotEffect {

    HeroicSacrificeEffect() {
        super(Outcome.RedirectDamage);
        staticText = "choose target creature you control. Until end of turn, all damage that would be dealt to you " +
                "and creatures you control is dealt to the chosen creature instead (if it's still on the battlefield). " +
                "When that creature dies this turn, put its counters on up to one target creature you control and draw a card";
    }

    private HeroicSacrificeEffect(final HeroicSacrificeEffect effect) {
        super(effect);
    }

    @Override
    public HeroicSacrificeEffect copy() {
        return new HeroicSacrificeEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent permanent = game.getPermanent(getTargetPointer().getFirst(game, source));
        if (permanent == null) {
            return false;
        }
        MageObjectReference mor = new MageObjectReference(permanent, game);
        game.addEffect(new HeroicSacrificeReplacementEffect(mor), source);
        game.addDelayedTriggeredAbility(new HeroicSacrificeDelayedTriggeredAbility(mor), source);
        return true;
    }
}

class HeroicSacrificeReplacementEffect extends ReplacementEffectImpl {

    private final MageObjectReference mor;

    HeroicSacrificeReplacementEffect(MageObjectReference mor) {
        super(Duration.EndOfTurn, Outcome.RedirectDamage);
        this.mor = mor;
    }

    private HeroicSacrificeReplacementEffect(final HeroicSacrificeReplacementEffect effect) {
        super(effect);
        this.mor = effect.mor;
    }

    @Override
    public HeroicSacrificeReplacementEffect copy() {
        return new HeroicSacrificeReplacementEffect(this);
    }

    @Override
    public boolean checksEventType(GameEvent event, Game game) {
        switch (event.getType()) {
            case DAMAGE_PERMANENT:
            case DAMAGE_PLAYER:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean applies(GameEvent event, Ability source, Game game) {
        if (mor.getPermanent(game) == null) {
            return false;
        }
        if (event.getType() == GameEvent.EventType.DAMAGE_PLAYER) {
            return event.getTargetId().equals(source.getControllerId());
        }
        if (mor.refersTo(event.getTargetId(), game)) {
            return false;
        }
        Permanent permanent = game.getPermanent(event.getTargetId());
        return permanent != null
                && permanent.isCreature(game)
                && permanent.isControlledBy(source.getControllerId());
    }

    @Override
    public boolean replaceEvent(GameEvent event, Ability source, Game game) {
        Permanent chosen = mor.getPermanent(game);
        if (chosen == null) {
            return false;
        }
        DamageEvent damageEvent = (DamageEvent) event;
        chosen.damage(
                damageEvent.getAmount(), damageEvent.getSourceId(), source, game,
                damageEvent.isCombatDamage(), damageEvent.isPreventable(), event.getAppliedEffects()
        );
        return true;
    }
}

class HeroicSacrificeDelayedTriggeredAbility extends DelayedTriggeredAbility {

    private final MageObjectReference mor;

    HeroicSacrificeDelayedTriggeredAbility(MageObjectReference mor) {
        super(new HeroicSacrificeCountersEffect(), Duration.EndOfTurn, true, false);
        this.addEffect(new DrawCardSourceControllerEffect(1).concatBy("and"));
        this.addTarget(new TargetPermanent(0, 1, StaticFilters.FILTER_CONTROLLED_CREATURE));
        this.mor = mor;
        setTriggerPhrase("When that creature dies this turn, ");
    }

    private HeroicSacrificeDelayedTriggeredAbility(final HeroicSacrificeDelayedTriggeredAbility ability) {
        super(ability);
        this.mor = ability.mor;
    }

    @Override
    public HeroicSacrificeDelayedTriggeredAbility copy() {
        return new HeroicSacrificeDelayedTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.ZONE_CHANGE;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        ZoneChangeEvent zEvent = (ZoneChangeEvent) event;
        if (!zEvent.isDiesEvent() || !mor.refersTo(zEvent.getTarget(), game)) {
            return false;
        }
        Counters counters = zEvent.getTarget().getCounters(game);
        getEffects().setValue("heroicSacrificeCounters", counters == null ? new Counters() : counters.copy());
        return true;
    }
}

class HeroicSacrificeCountersEffect extends OneShotEffect {

    HeroicSacrificeCountersEffect() {
        super(Outcome.Benefit);
        staticText = "put its counters on up to one target creature you control";
    }

    private HeroicSacrificeCountersEffect(final HeroicSacrificeCountersEffect effect) {
        super(effect);
    }

    @Override
    public HeroicSacrificeCountersEffect copy() {
        return new HeroicSacrificeCountersEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent permanent = game.getPermanent(getTargetPointer().getFirst(game, source));
        Object value = getValue("heroicSacrificeCounters");
        if (permanent == null || !(value instanceof Counters)) {
            return true;
        }
        for (Counter counter : ((Counters) value).values()) {
            permanent.addCounters(counter.copy(), source.getControllerId(), source, game);
        }
        return true;
    }
}

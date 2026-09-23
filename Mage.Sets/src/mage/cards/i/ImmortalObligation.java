package mage.cards.i;

import mage.abilities.Ability;
import mage.abilities.effects.ContinuousEffectImpl;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.RestrictionEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Layer;
import mage.constants.Outcome;
import mage.constants.SubLayer;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.counters.Counters;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.common.TargetCardInOpponentsGraveyard;
import mage.target.targetpointer.FixedTarget;
import mage.util.CardUtil;

import java.util.UUID;

/**
 * @author Claude
 */
public final class ImmortalObligation extends CardImpl {

    public ImmortalObligation(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.INSTANT}, "{1}{W}");

        // Return target creature card from an opponent's graveyard to the battlefield under their control with a duty counter on it. For as long as that creature has a duty counter on it, it is goaded, can't attack you or a permanent you control, and can't block creatures you control.
        this.getSpellAbility().addEffect(new ImmortalObligationEffect());
        this.getSpellAbility().addTarget(new TargetCardInOpponentsGraveyard(StaticFilters.FILTER_CARD_CREATURE));
    }

    private ImmortalObligation(final ImmortalObligation card) {
        super(card);
    }

    @Override
    public ImmortalObligation copy() {
        return new ImmortalObligation(this);
    }
}

class ImmortalObligationEffect extends OneShotEffect {

    ImmortalObligationEffect() {
        super(Outcome.Benefit);
        staticText = "return target creature card from an opponent's graveyard to the battlefield under their control " +
                "with a duty counter on it. For as long as that creature has a duty counter on it, it is goaded, " +
                "can't attack you or a permanent you control, and can't block creatures you control";
    }

    private ImmortalObligationEffect(final ImmortalObligationEffect effect) {
        super(effect);
    }

    @Override
    public ImmortalObligationEffect copy() {
        return new ImmortalObligationEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Card card = game.getCard(getTargetPointer().getFirst(game, source));
        if (card == null) {
            return false;
        }
        Player owner = game.getPlayer(card.getOwnerId());
        if (owner == null) {
            return false;
        }
        Counters counters = new Counters();
        counters.addCounter(CounterType.DUTY.createInstance());
        game.setEnterWithCounters(card.getId(), counters);
        owner.moveCards(card, Zone.BATTLEFIELD, source, game);
        Permanent permanent = CardUtil.getPermanentFromCardPutToBattlefield(card, game);
        if (permanent == null) {
            return true;
        }
        game.addEffect(new ImmortalObligationGoadEffect()
                .setTargetPointer(new FixedTarget(permanent, game)), source);
        game.addEffect(new ImmortalObligationRestrictionEffect()
                .setTargetPointer(new FixedTarget(permanent, game)), source);
        return true;
    }
}

class ImmortalObligationGoadEffect extends ContinuousEffectImpl {

    ImmortalObligationGoadEffect() {
        super(Duration.Custom, Layer.RulesEffects, SubLayer.NA, Outcome.Detriment);
    }

    private ImmortalObligationGoadEffect(final ImmortalObligationGoadEffect effect) {
        super(effect);
    }

    @Override
    public ImmortalObligationGoadEffect copy() {
        return new ImmortalObligationGoadEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent permanent = game.getPermanent(getTargetPointer().getFirst(game, source));
        if (permanent == null) {
            discard();
            return false;
        }
        if (permanent.getCounters(game).getCount(CounterType.DUTY) > 0) {
            permanent.addGoadingPlayer(source.getControllerId());
        }
        return true;
    }
}

class ImmortalObligationRestrictionEffect extends RestrictionEffect {

    ImmortalObligationRestrictionEffect() {
        super(Duration.Custom, Outcome.Detriment);
    }

    private ImmortalObligationRestrictionEffect(final ImmortalObligationRestrictionEffect effect) {
        super(effect);
    }

    @Override
    public ImmortalObligationRestrictionEffect copy() {
        return new ImmortalObligationRestrictionEffect(this);
    }

    @Override
    public boolean applies(Permanent permanent, Ability source, Game game) {
        Permanent creature = game.getPermanent(getTargetPointer().getFirst(game, source));
        if (creature == null) {
            discard();
            return false;
        }
        return creature.getId().equals(permanent.getId())
                && permanent.getCounters(game).getCount(CounterType.DUTY) > 0;
    }

    @Override
    public boolean canAttack(Permanent attacker, UUID defenderId, Ability source, Game game, boolean canUseChooseDialogs) {
        if (source.isControlledBy(defenderId)) {
            return false;
        }
        Permanent permanent = game.getPermanent(defenderId);
        return permanent == null || !permanent.isControlledBy(source.getControllerId());
    }

    @Override
    public boolean canBlock(Permanent attacker, Permanent blocker, Ability source, Game game, boolean canUseChooseDialogs) {
        return attacker == null || !attacker.isControlledBy(source.getControllerId());
    }
}

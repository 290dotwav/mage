package mage.cards.h;

import mage.abilities.Ability;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.costs.common.SacrificeSourceCost;
import mage.abilities.costs.common.TapSourceCost;
import mage.abilities.costs.mana.GenericManaCost;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.PreventionEffectImpl;
import mage.abilities.hint.common.MonarchHint;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.counters.Counters;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.game.permanent.PermanentToken;
import mage.players.Player;
import mage.target.TargetPermanent;

import java.util.UUID;

/**
 * @author Claude
 */
public final class HeartShapedHerb extends CardImpl {

    public HeartShapedHerb(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ARTIFACT}, "{4}");

        // If a source an opponent controls would deal damage to you, prevent 1 of that damage.
        this.addAbility(new SimpleStaticAbility(new HeartShapedHerbPreventionEffect()));

        // {2}, {T}, Sacrifice this artifact: You may sacrifice a creature. If you do, return that card to the battlefield under its owner's control with three +1/+1 counters on it and you become the monarch.
        Ability ability = new SimpleActivatedAbility(new HeartShapedHerbEffect(), new GenericManaCost(2));
        ability.addCost(new TapSourceCost());
        ability.addCost(new SacrificeSourceCost());
        ability.addHint(MonarchHint.instance);
        this.addAbility(ability);
    }

    private HeartShapedHerb(final HeartShapedHerb card) {
        super(card);
    }

    @Override
    public HeartShapedHerb copy() {
        return new HeartShapedHerb(this);
    }
}

class HeartShapedHerbPreventionEffect extends PreventionEffectImpl {

    HeartShapedHerbPreventionEffect() {
        super(Duration.WhileOnBattlefield, 1, false, false);
        this.staticText = "If a source an opponent controls would deal damage to you, prevent 1 of that damage";
    }

    private HeartShapedHerbPreventionEffect(final HeartShapedHerbPreventionEffect effect) {
        super(effect);
    }

    @Override
    public boolean checksEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.DAMAGE_PLAYER;
    }

    @Override
    public boolean applies(GameEvent event, Ability source, Game game) {
        if (!event.getTargetId().equals(source.getControllerId())) {
            return false;
        }
        UUID sourceControllerId = game.getControllerId(event.getSourceId());
        return sourceControllerId != null
                && game.getOpponents(source.getControllerId()).contains(sourceControllerId)
                && super.applies(event, source, game);
    }

    @Override
    public HeartShapedHerbPreventionEffect copy() {
        return new HeartShapedHerbPreventionEffect(this);
    }
}

class HeartShapedHerbEffect extends OneShotEffect {

    HeartShapedHerbEffect() {
        super(Outcome.Benefit);
        staticText = "you may sacrifice a creature. If you do, return that card to the battlefield under its owner's " +
                "control with three +1/+1 counters on it and you become the monarch";
    }

    private HeartShapedHerbEffect(final HeartShapedHerbEffect effect) {
        super(effect);
    }

    @Override
    public HeartShapedHerbEffect copy() {
        return new HeartShapedHerbEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        TargetPermanent target = new TargetPermanent(0, 1, StaticFilters.FILTER_CONTROLLED_CREATURE, true);
        if (!target.canChoose(controller.getId(), source, game)
                || !controller.chooseUse(Outcome.Benefit, "Sacrifice a creature?", source, game)) {
            return false;
        }
        controller.choose(Outcome.Sacrifice, target, source, game);
        Permanent permanent = game.getPermanent(target.getFirstTarget());
        if (permanent == null) {
            return false;
        }
        boolean isToken = permanent instanceof PermanentToken;
        if (!permanent.sacrifice(source, game)) {
            return false;
        }
        // "that card": a token or a card that isn't in the graveyard (e.g. a commander put into the command zone) isn't returned
        Card card = isToken ? null : game.getCard(permanent.getId());
        if (card != null && game.getState().getZone(card.getId()) == Zone.GRAVEYARD) {
            Player owner = game.getPlayer(card.getOwnerId());
            if (owner != null) {
                Counters counters = new Counters();
                counters.addCounter(CounterType.P1P1.createInstance(3));
                game.setEnterWithCounters(card.getId(), counters);
                owner.moveCards(card, Zone.BATTLEFIELD, source, game);
            }
        }
        game.setMonarchId(source, controller.getId());
        return true;
    }
}

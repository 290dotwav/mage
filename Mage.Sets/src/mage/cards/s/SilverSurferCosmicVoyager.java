package mage.cards.s;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldTriggeredAbility;
import mage.abilities.common.delayed.AtTheBeginOfNextEndStepDelayedTriggeredAbility;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.ReplacementEffectImpl;
import mage.abilities.keyword.FlashAbility;
import mage.abilities.keyword.FlyingAbility;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.events.EntersTheBattlefieldEvent;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.TargetPermanent;
import mage.target.targetpointer.FixedTargets;
import mage.util.CardUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author Claude
 */
public final class SilverSurferCosmicVoyager extends CardImpl {

    public SilverSurferCosmicVoyager(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{4}{U}{U}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.ALIEN);
        this.subtype.add(SubType.HERO);
        this.power = new MageInt(5);
        this.toughness = new MageInt(5);

        // Flash
        this.addAbility(FlashAbility.getInstance());

        // Flying
        this.addAbility(FlyingAbility.getInstance());

        // When Silver Surfer enters, exile any number of other target permanents you control. Return those cards to the battlefield under their owner's control at the beginning of the next end step. If a land enters this way, it enters tapped.
        Ability ability = new EntersBattlefieldTriggeredAbility(new SilverSurferCosmicVoyagerExileEffect());
        ability.addTarget(new TargetPermanent(0, Integer.MAX_VALUE, StaticFilters.FILTER_OTHER_CONTROLLED_PERMANENTS));
        this.addAbility(ability);
    }

    private SilverSurferCosmicVoyager(final SilverSurferCosmicVoyager card) {
        super(card);
    }

    @Override
    public SilverSurferCosmicVoyager copy() {
        return new SilverSurferCosmicVoyager(this);
    }
}

class SilverSurferCosmicVoyagerExileEffect extends OneShotEffect {

    SilverSurferCosmicVoyagerExileEffect() {
        super(Outcome.Benefit);
        staticText = "exile any number of other target permanents you control. Return those cards to the battlefield " +
                "under their owner's control at the beginning of the next end step. If a land enters this way, it enters tapped";
    }

    private SilverSurferCosmicVoyagerExileEffect(final SilverSurferCosmicVoyagerExileEffect effect) {
        super(effect);
    }

    @Override
    public SilverSurferCosmicVoyagerExileEffect copy() {
        return new SilverSurferCosmicVoyagerExileEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        Cards cards = new CardsImpl();
        for (UUID targetId : getTargetPointer().getTargets(game, source)) {
            Permanent permanent = game.getPermanent(targetId);
            if (permanent != null) {
                cards.add(permanent);
            }
        }
        if (cards.isEmpty()) {
            return false;
        }
        controller.moveCardsToExile(cards.getCards(game), source, game, true,
                CardUtil.getExileZoneId(game, source), CardUtil.getSourceName(game, source));
        cards.retainZone(Zone.EXILED, game);
        if (cards.isEmpty()) {
            return true;
        }
        game.addDelayedTriggeredAbility(new AtTheBeginOfNextEndStepDelayedTriggeredAbility(
                new SilverSurferCosmicVoyagerReturnEffect().setTargetPointer(new FixedTargets(cards, game))
        ), source);
        return true;
    }
}

class SilverSurferCosmicVoyagerReturnEffect extends OneShotEffect {

    SilverSurferCosmicVoyagerReturnEffect() {
        super(Outcome.PutCardInPlay);
        staticText = "return those cards to the battlefield under their owner's control. If a land enters this way, it enters tapped";
    }

    private SilverSurferCosmicVoyagerReturnEffect(final SilverSurferCosmicVoyagerReturnEffect effect) {
        super(effect);
    }

    @Override
    public SilverSurferCosmicVoyagerReturnEffect copy() {
        return new SilverSurferCosmicVoyagerReturnEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        if (controller == null) {
            return false;
        }
        Set<Card> cards = new HashSet<>();
        for (UUID id : getTargetPointer().getTargets(game, source)) {
            Card card = game.getCard(id);
            if (card != null) {
                cards.add(card);
            }
        }
        if (cards.isEmpty()) {
            return false;
        }
        Set<UUID> ids = new HashSet<>();
        cards.forEach(card -> ids.add(card.getId()));
        // the lands among them enter tapped; all of them enter at the same time
        SilverSurferCosmicVoyagerTappedEffect tappedEffect = new SilverSurferCosmicVoyagerTappedEffect(ids);
        game.addEffect(tappedEffect, source);
        controller.moveCards(cards, Zone.BATTLEFIELD, source, game, false, false, true, null);
        tappedEffect.discard();
        return true;
    }
}

class SilverSurferCosmicVoyagerTappedEffect extends ReplacementEffectImpl {

    private final Set<UUID> ids = new HashSet<>();

    SilverSurferCosmicVoyagerTappedEffect(Set<UUID> ids) {
        super(Duration.Custom, Outcome.Tap);
        this.ids.addAll(ids);
    }

    private SilverSurferCosmicVoyagerTappedEffect(final SilverSurferCosmicVoyagerTappedEffect effect) {
        super(effect);
        this.ids.addAll(effect.ids);
    }

    @Override
    public SilverSurferCosmicVoyagerTappedEffect copy() {
        return new SilverSurferCosmicVoyagerTappedEffect(this);
    }

    @Override
    public boolean checksEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.ENTERS_THE_BATTLEFIELD;
    }

    @Override
    public boolean applies(GameEvent event, Ability source, Game game) {
        Permanent permanent = ((EntersTheBattlefieldEvent) event).getTarget();
        return permanent != null
                && ids.contains(permanent.getId())
                && permanent.isLand(game);
    }

    @Override
    public boolean replaceEvent(GameEvent event, Ability source, Game game) {
        Permanent permanent = ((EntersTheBattlefieldEvent) event).getTarget();
        if (permanent != null) {
            permanent.setTapped(true);
        }
        return false;
    }
}

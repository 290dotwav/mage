package mage.cards.d;

import mage.abilities.Ability;
import mage.abilities.TriggeredAbilityImpl;
import mage.abilities.common.delayed.AtTheBeginOfNextEndStepDelayedTriggeredAbility;
import mage.abilities.condition.common.PermanentsOnTheBattlefieldCondition;
import mage.abilities.dynamicvalue.common.PermanentsOnBattlefieldCount;
import mage.abilities.effects.Effect;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.CreateTokenEffect;
import mage.abilities.effects.common.ReturnToBattlefieldUnderYourControlTargetEffect;
import mage.abilities.effects.common.continuous.GainAbilityTargetEffect;
import mage.abilities.keyword.HasteAbility;
import mage.abilities.triggers.BeginningOfCombatTriggeredAbility;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.*;
import mage.filter.FilterPermanent;
import mage.filter.common.FilterControlledLandPermanent;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.events.ZoneChangeEvent;
import mage.game.permanent.token.SandWarriorToken;
import mage.game.permanent.token.Token;
import mage.target.targetpointer.FixedTarget;
import mage.target.targetpointer.FixedTargets;

import java.util.UUID;

/**
 * @author ClaudeMTG
 */
public final class DesertWarfare extends CardImpl {

    private static final FilterPermanent filter = new FilterControlledLandPermanent("Deserts");

    static {
        filter.add(SubType.DESERT.getPredicate());
    }

    public DesertWarfare(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ENCHANTMENT}, "{3}{G}");

        // Whenever you sacrifice a Desert and whenever a Desert card is put into your graveyard from
        // your hand or library, put that card onto the battlefield under your control at the beginning
        // of your next end step.
        this.addAbility(new DesertWarfareTriggeredAbility());

        // At the beginning of combat on your turn, if you control five or more Deserts, create that many
        // 1/1 red, green, and white Sand Warrior creature tokens. They gain haste.
        this.addAbility(new BeginningOfCombatTriggeredAbility(new DesertWarfareTokenEffect())
                .withInterveningIf(new PermanentsOnTheBattlefieldCondition(
                        filter, ComparisonType.MORE_THAN, 4, true
                )));
    }

    private DesertWarfare(final DesertWarfare card) {
        super(card);
    }

    @Override
    public DesertWarfare copy() {
        return new DesertWarfare(this);
    }
}

class DesertWarfareTriggeredAbility extends TriggeredAbilityImpl {

    DesertWarfareTriggeredAbility() {
        super(Zone.BATTLEFIELD, new DesertWarfareReturnEffect(), false);
        setTriggerPhrase("Whenever you sacrifice a Desert and whenever a Desert card is put into "
                + "your graveyard from your hand or library, ");
    }

    private DesertWarfareTriggeredAbility(final DesertWarfareTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public DesertWarfareTriggeredAbility copy() {
        return new DesertWarfareTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.SACRIFICED_PERMANENT
                || event.getType() == GameEvent.EventType.ZONE_CHANGE;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        if (event.getType() == GameEvent.EventType.ZONE_CHANGE) {
            ZoneChangeEvent zEvent = (ZoneChangeEvent) event;
            if (zEvent.getToZone() != Zone.GRAVEYARD
                    || (zEvent.getFromZone() != Zone.HAND && zEvent.getFromZone() != Zone.LIBRARY)) {
                return false;
            }
        } else if (!isControlledBy(event.getPlayerId())) {
            return false;
        }
        Card card = game.getCard(event.getTargetId());
        if (card == null
                || !isControlledBy(card.getOwnerId())
                || !card.hasSubtype(SubType.DESERT, game)) {
            return false;
        }
        getEffects().setTargetPointer(new FixedTarget(card.getId()));
        return true;
    }
}

class DesertWarfareReturnEffect extends OneShotEffect {

    DesertWarfareReturnEffect() {
        super(Outcome.PutCardInPlay);
        staticText = "put that card onto the battlefield under your control "
                + "at the beginning of your next end step";
    }

    private DesertWarfareReturnEffect(final DesertWarfareReturnEffect effect) {
        super(effect);
    }

    @Override
    public DesertWarfareReturnEffect copy() {
        return new DesertWarfareReturnEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Card card = game.getCard(getTargetPointer().getFirst(game, source));
        if (card == null || Zone.GRAVEYARD != game.getState().getZone(card.getId())) {
            return false;
        }
        Effect effect = new ReturnToBattlefieldUnderYourControlTargetEffect();
        effect.setTargetPointer(new FixedTarget(card, game));
        game.addDelayedTriggeredAbility(new AtTheBeginOfNextEndStepDelayedTriggeredAbility(effect), source);
        return true;
    }
}

class DesertWarfareTokenEffect extends OneShotEffect {

    private static final FilterPermanent filter = new FilterControlledLandPermanent("Deserts");

    static {
        filter.add(SubType.DESERT.getPredicate());
    }

    DesertWarfareTokenEffect() {
        super(Outcome.PutCreatureInPlay);
        staticText = "create that many 1/1 red, green, and white Sand Warrior creature tokens. They gain haste";
    }

    private DesertWarfareTokenEffect(final DesertWarfareTokenEffect effect) {
        super(effect);
    }

    @Override
    public DesertWarfareTokenEffect copy() {
        return new DesertWarfareTokenEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        int amount = new PermanentsOnBattlefieldCount(filter).calculate(game, source, this);
        if (amount < 1) {
            return false;
        }
        Token token = new SandWarriorToken();
        token.putOntoBattlefield(amount, game, source, source.getControllerId());
        game.addEffect(new GainAbilityTargetEffect(
                HasteAbility.getInstance(), Duration.EndOfGame
        ).setTargetPointer(new FixedTargets(token, game)), source);
        return true;
    }
}

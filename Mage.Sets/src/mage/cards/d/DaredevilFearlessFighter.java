package mage.cards.d;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.TriggeredAbilityImpl;
import mage.abilities.common.AttacksTriggeredAbility;
import mage.abilities.dynamicvalue.common.SavedDamageValue;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.DamageTargetEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.players.Player;
import mage.target.common.TargetOpponent;
import mage.util.CardUtil;

import java.util.UUID;

/**
 * @author Claude
 */
public final class DaredevilFearlessFighter extends CardImpl {

    public DaredevilFearlessFighter(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{R}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.HERO);
        this.power = new MageInt(3);
        this.toughness = new MageInt(3);

        // Whenever a source you control deals damage to you, Daredevil deals that much damage to target opponent.
        this.addAbility(new DaredevilFearlessFighterTriggeredAbility());

        // Whenever Daredevil attacks, exile the top card of your library. Daredevil deals damage to you equal to that card's mana value. You may play it this turn.
        this.addAbility(new AttacksTriggeredAbility(new DaredevilFearlessFighterEffect()));
    }

    private DaredevilFearlessFighter(final DaredevilFearlessFighter card) {
        super(card);
    }

    @Override
    public DaredevilFearlessFighter copy() {
        return new DaredevilFearlessFighter(this);
    }
}

class DaredevilFearlessFighterTriggeredAbility extends TriggeredAbilityImpl {

    DaredevilFearlessFighterTriggeredAbility() {
        super(Zone.BATTLEFIELD, new DamageTargetEffect(SavedDamageValue.MUCH));
        this.addTarget(new TargetOpponent());
        setTriggerPhrase("Whenever a source you control deals damage to you, ");
    }

    private DaredevilFearlessFighterTriggeredAbility(final DaredevilFearlessFighterTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public DaredevilFearlessFighterTriggeredAbility copy() {
        return new DaredevilFearlessFighterTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.DAMAGED_PLAYER;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        if (!isControlledBy(event.getTargetId())
                || !isControlledBy(game.getControllerId(event.getSourceId()))) {
            return false;
        }
        this.getEffects().setValue("damage", event.getAmount());
        return true;
    }
}

class DaredevilFearlessFighterEffect extends OneShotEffect {

    DaredevilFearlessFighterEffect() {
        super(Outcome.Benefit);
        staticText = "exile the top card of your library. {this} deals damage to you equal to that card's mana value. You may play it this turn";
    }

    private DaredevilFearlessFighterEffect(final DaredevilFearlessFighterEffect effect) {
        super(effect);
    }

    @Override
    public DaredevilFearlessFighterEffect copy() {
        return new DaredevilFearlessFighterEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(source.getControllerId());
        if (player == null) {
            return false;
        }
        Card card = player.getLibrary().getFromTop(game);
        if (card == null) {
            return false;
        }
        player.moveCards(card, Zone.EXILED, source, game);
        int manaValue = card.getManaValue();
        if (manaValue > 0) {
            player.damage(manaValue, source.getSourceId(), source, game);
        }
        if (game.getState().getZone(card.getId()) == Zone.EXILED) {
            CardUtil.makeCardPlayable(game, source, card, false, Duration.EndOfTurn, false);
        }
        return true;
    }
}

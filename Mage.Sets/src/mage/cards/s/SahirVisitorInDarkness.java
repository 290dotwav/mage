package mage.cards.s;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.TriggeredAbilityImpl;
import mage.abilities.condition.common.SourceOnBattlefieldOrCommandZoneCondition;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.DamageControllerEffect;
import mage.abilities.triggers.BeginningOfUpkeepTriggeredAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.AbilityWord;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.TargetController;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.events.LifeLostEvent;
import mage.game.permanent.Permanent;
import mage.players.Player;

import java.util.UUID;

/**
 * @author Claude
 */
public final class SahirVisitorInDarkness extends CardImpl {

    public SahirVisitorInDarkness(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{1}{B}{B}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.WIZARD);
        this.power = new MageInt(3);
        this.toughness = new MageInt(3);

        // Eminence -- At the beginning of your upkeep, Sahir deals 1 damage to you. This ability triggers only if Sahir is in the command zone or on the battlefield.
        this.addAbility(new BeginningOfUpkeepTriggeredAbility(
                Zone.ALL, TargetController.YOU, new DamageControllerEffect(1), false
        ).withInterveningIf(SourceOnBattlefieldOrCommandZoneCondition.instance).setAbilityWord(AbilityWord.EMINENCE));

        // Whenever a source you control deals exactly 1 damage to you and whenever a spell or ability you control causes you to lose exactly 1 life, put a +1/+1 counter on Sahir. If this is the second time this ability has resolved this turn, draw a card.
        this.addAbility(new SahirVisitorInDarknessTriggeredAbility());
    }

    private SahirVisitorInDarkness(final SahirVisitorInDarkness card) {
        super(card);
    }

    @Override
    public SahirVisitorInDarkness copy() {
        return new SahirVisitorInDarkness(this);
    }
}

class SahirVisitorInDarknessTriggeredAbility extends TriggeredAbilityImpl {

    SahirVisitorInDarknessTriggeredAbility() {
        super(Zone.BATTLEFIELD, new SahirVisitorInDarknessEffect());
        setTriggerPhrase("Whenever a source you control deals exactly 1 damage to you and whenever a spell or "
                + "ability you control causes you to lose exactly 1 life, ");
    }

    private SahirVisitorInDarknessTriggeredAbility(final SahirVisitorInDarknessTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public SahirVisitorInDarknessTriggeredAbility copy() {
        return new SahirVisitorInDarknessTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.DAMAGED_PLAYER
                || event.getType() == GameEvent.EventType.LOST_LIFE;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        if (event.getAmount() != 1) {
            return false;
        }
        if (event.getType() == GameEvent.EventType.DAMAGED_PLAYER) {
            return isControlledBy(event.getTargetId())
                    && isControlledBy(game.getControllerId(event.getSourceId()));
        }
        // the life lost to damage is the damage case above, not a second trigger
        return isControlledBy(event.getPlayerId())
                && !((LifeLostEvent) event).isFromDamage()
                && isControlledBy(game.getControllerId(event.getSourceId()));
    }
}

class SahirVisitorInDarknessEffect extends OneShotEffect {

    SahirVisitorInDarknessEffect() {
        super(Outcome.BoostCreature);
        staticText = "put a +1/+1 counter on {this}. If this is the second time this ability has resolved this turn, draw a card";
    }

    private SahirVisitorInDarknessEffect(final SahirVisitorInDarknessEffect effect) {
        super(effect);
    }

    @Override
    public SahirVisitorInDarknessEffect copy() {
        return new SahirVisitorInDarknessEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent permanent = source.getSourcePermanentIfItStillExists(game);
        if (permanent != null) {
            permanent.addCounters(CounterType.P1P1.createInstance(), source, game);
        }
        String key = "SahirResolved" + source.getSourceId() + "-" + source.getOriginalId() + "-" + game.getTurnNum();
        Integer resolved = (Integer) game.getState().getValue(key);
        int count = resolved == null ? 1 : resolved + 1;
        game.getState().setValue(key, count);
        if (count == 2) {
            Player controller = game.getPlayer(source.getControllerId());
            if (controller != null) {
                controller.drawCards(1, source, game);
            }
        }
        return true;
    }
}

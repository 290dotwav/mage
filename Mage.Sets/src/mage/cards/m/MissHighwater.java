package mage.cards.m;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.DelayedTriggeredAbility;
import mage.abilities.common.DealsCombatDamageToAPlayerTriggeredAbility;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.keyword.MenaceAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.counters.CounterType;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.game.permanent.token.Token;
import mage.players.Player;
import mage.util.functions.CopyTokenFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Claude
 */
public final class MissHighwater extends CardImpl {

    public MissHighwater(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{B}{B}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.DEMON);
        this.subtype.add(SubType.ADVISOR);
        this.power = new MageInt(5);
        this.toughness = new MageInt(3);

        // Menace
        this.addAbility(new MenaceAbility());

        // Whenever Miss Highwater deals combat damage to a player who doesn't have a contract counter, they may discard their hand. If they do, they draw seven cards and get a contract counter. For as long as they have a contract counter, when they lose the game, for each artifact and creature they controlled, create a token that's a copy of it.
        this.addAbility(new MissHighwaterTriggeredAbility());
    }

    private MissHighwater(final MissHighwater card) {
        super(card);
    }

    @Override
    public MissHighwater copy() {
        return new MissHighwater(this);
    }
}

class MissHighwaterTriggeredAbility extends DealsCombatDamageToAPlayerTriggeredAbility {

    MissHighwaterTriggeredAbility() {
        super(new MissHighwaterEffect(), false, true);
        setTriggerPhrase("Whenever {this} deals combat damage to a player who doesn't have a contract counter, ");
    }

    private MissHighwaterTriggeredAbility(final MissHighwaterTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public MissHighwaterTriggeredAbility copy() {
        return new MissHighwaterTriggeredAbility(this);
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        if (!super.checkTrigger(event, game)) {
            return false;
        }
        Player player = game.getPlayer(event.getPlayerId());
        return player != null && player.getCountersCount(CounterType.CONTRACT) < 1;
    }
}

class MissHighwaterEffect extends OneShotEffect {

    MissHighwaterEffect() {
        super(Outcome.Benefit);
        staticText = "they may discard their hand. If they do, they draw seven cards and get a contract counter. "
                + "For as long as they have a contract counter, when they lose the game, for each artifact "
                + "and creature they controlled, create a token that's a copy of it";
    }

    private MissHighwaterEffect(final MissHighwaterEffect effect) {
        super(effect);
    }

    @Override
    public MissHighwaterEffect copy() {
        return new MissHighwaterEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(getTargetPointer().getFirst(game, source));
        if (player == null || player.getCountersCount(CounterType.CONTRACT) > 0) {
            return false;
        }
        if (!player.chooseUse(Outcome.DrawCard, "Discard your hand, draw seven cards and get a contract counter?", source, game)) {
            return true;
        }
        player.discard(player.getHand(), false, source, game);
        player.drawCards(7, source, game);
        player.addCounters(CounterType.CONTRACT.createInstance(), source.getControllerId(), source, game);
        game.addDelayedTriggeredAbility(new MissHighwaterDelayedTriggeredAbility(player.getId()), source);
        return true;
    }
}

class MissHighwaterDelayedTriggeredAbility extends DelayedTriggeredAbility {

    private final UUID contractPlayerId;

    MissHighwaterDelayedTriggeredAbility(UUID contractPlayerId) {
        super(new MissHighwaterCopyEffect(), Duration.EndOfGame, true, false);
        this.contractPlayerId = contractPlayerId;
    }

    private MissHighwaterDelayedTriggeredAbility(final MissHighwaterDelayedTriggeredAbility ability) {
        super(ability);
        this.contractPlayerId = ability.contractPlayerId;
    }

    @Override
    public MissHighwaterDelayedTriggeredAbility copy() {
        return new MissHighwaterDelayedTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.LOST;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        if (!contractPlayerId.equals(event.getPlayerId())) {
            return false;
        }
        Player player = game.getPlayer(contractPlayerId);
        if (player == null || player.getCountersCount(CounterType.CONTRACT) < 1) {
            return false;
        }
        // what they controlled, kept now: their permanents leave the game with them
        List<Permanent> controlled = new ArrayList<>();
        for (Permanent permanent : game.getBattlefield().getAllActivePermanents(contractPlayerId)) {
            if (permanent.isArtifact(game) || permanent.isCreature(game)) {
                controlled.add(permanent.copy());
            }
        }
        getEffects().setValue("controlled", controlled);
        return true;
    }

    @Override
    public String getRule() {
        return "When that player loses the game while they have a contract counter, for each artifact and creature "
                + "they controlled, create a token that's a copy of it.";
    }
}

class MissHighwaterCopyEffect extends OneShotEffect {

    MissHighwaterCopyEffect() {
        super(Outcome.PutCreatureInPlay);
        staticText = "for each artifact and creature they controlled, create a token that's a copy of it";
    }

    private MissHighwaterCopyEffect(final MissHighwaterCopyEffect effect) {
        super(effect);
    }

    @Override
    public MissHighwaterCopyEffect copy() {
        return new MissHighwaterCopyEffect(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean apply(Game game, Ability source) {
        Object value = getValue("controlled");
        if (!(value instanceof List)) {
            return false;
        }
        for (Permanent permanent : (List<Permanent>) value) {
            Token token = CopyTokenFunction.createTokenCopy(permanent, game);
            token.putOntoBattlefield(1, game, source, source.getControllerId());
        }
        return true;
    }
}

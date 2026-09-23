package mage.cards.a;

import mage.abilities.Ability;
import mage.abilities.TriggeredAbilityImpl;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.AttachEffect;
import mage.abilities.effects.common.DrawCardSourceControllerEffect;
import mage.abilities.effects.common.GainLifeEffect;
import mage.abilities.effects.common.LoseLifeTargetEffect;
import mage.abilities.keyword.EnchantAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.TargetPlayer;
import mage.target.common.TargetOpponent;
import mage.target.targetpointer.FixedTarget;

import java.util.UUID;

/**
 * @author Claude
 */
public final class Archnemesis extends CardImpl {

    public Archnemesis(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ENCHANTMENT}, "{1}{U}{B}");

        this.subtype.add(SubType.AURA);

        // Enchant opponent
        TargetPlayer auraTarget = new TargetOpponent();
        this.getSpellAbility().addTarget(auraTarget);
        this.getSpellAbility().addEffect(new AttachEffect(Outcome.Damage));
        this.addAbility(new EnchantAbility(auraTarget));

        // Whenever you attack enchanted player, that player loses 2 life. You draw a card and gain 2 life.
        this.addAbility(new ArchnemesisAttackTriggeredAbility());

        // Whenever a player attacks you, you may attach this Aura to that player.
        this.addAbility(new ArchnemesisAttackedTriggeredAbility());
    }

    private Archnemesis(final Archnemesis card) {
        super(card);
    }

    @Override
    public Archnemesis copy() {
        return new Archnemesis(this);
    }
}

class ArchnemesisAttackTriggeredAbility extends TriggeredAbilityImpl {

    ArchnemesisAttackTriggeredAbility() {
        super(Zone.BATTLEFIELD, new LoseLifeTargetEffect(2).setText("that player loses 2 life"));
        this.addEffect(new DrawCardSourceControllerEffect(1, true).setText("You draw a card"));
        this.addEffect(new GainLifeEffect(2).setText("and gain 2 life"));
        setTriggerPhrase("Whenever you attack enchanted player, ");
    }

    private ArchnemesisAttackTriggeredAbility(final ArchnemesisAttackTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public ArchnemesisAttackTriggeredAbility copy() {
        return new ArchnemesisAttackTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.DEFENDER_ATTACKED;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        Permanent aura = game.getPermanentOrLKIBattlefield(getSourceId());
        if (aura == null
                || !isControlledBy(event.getPlayerId())
                || !event.getTargetId().equals(aura.getAttachedTo())) {
            return false;
        }
        this.getEffects().setTargetPointer(new FixedTarget(event.getTargetId()));
        return true;
    }
}

class ArchnemesisAttackedTriggeredAbility extends TriggeredAbilityImpl {

    ArchnemesisAttackedTriggeredAbility() {
        super(Zone.BATTLEFIELD, new ArchnemesisAttachEffect(), true);
        setTriggerPhrase("Whenever a player attacks you, ");
    }

    private ArchnemesisAttackedTriggeredAbility(final ArchnemesisAttackedTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public ArchnemesisAttackedTriggeredAbility copy() {
        return new ArchnemesisAttackedTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.DEFENDER_ATTACKED;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        if (!isControlledBy(event.getTargetId())) {
            return false;
        }
        this.getEffects().setTargetPointer(new FixedTarget(event.getPlayerId()));
        return true;
    }
}

class ArchnemesisAttachEffect extends OneShotEffect {

    ArchnemesisAttachEffect() {
        super(Outcome.Benefit);
        staticText = "you may attach this Aura to that player";
    }

    private ArchnemesisAttachEffect(final ArchnemesisAttachEffect effect) {
        super(effect);
    }

    @Override
    public ArchnemesisAttachEffect copy() {
        return new ArchnemesisAttachEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent aura = source.getSourcePermanentIfItStillExists(game);
        Player player = game.getPlayer(getTargetPointer().getFirst(game, source));
        // Enchant opponent: it can only be attached to an opponent of its controller (303.4)
        if (aura == null || player == null
                || !game.getOpponents(aura.getControllerId()).contains(player.getId())
                || player.getId().equals(aura.getAttachedTo())) {
            return false;
        }
        return player.addAttachment(aura.getId(), source, game);
    }
}

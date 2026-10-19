package mage.cards.g;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.TriggeredAbilityImpl;
import mage.abilities.effects.OneShotEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.stack.Spell;
import mage.game.stack.StackAbility;
import mage.game.stack.StackObject;
import mage.target.targetpointer.FixedTarget;

import java.util.UUID;

/**
 * @author Claude
 */
public final class GlavaFiveAdventsMage extends CardImpl {

    public GlavaFiveAdventsMage(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{5}{G}{G}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.HUMAN);
        this.subtype.add(SubType.WIZARD);
        this.power = new MageInt(5);
        this.toughness = new MageInt(5);

        // Whenever you cast a permanent spell with {X} in its mana cost or activate an ability with {X} in its activation cost that isn't a mana ability, you may have the value of X become 5. Do this only once each turn.
        this.addAbility(new GlavaFiveAdventsMageTriggeredAbility());
    }

    private GlavaFiveAdventsMage(final GlavaFiveAdventsMage card) {
        super(card);
    }

    @Override
    public GlavaFiveAdventsMage copy() {
        return new GlavaFiveAdventsMage(this);
    }
}

class GlavaFiveAdventsMageTriggeredAbility extends TriggeredAbilityImpl {

    GlavaFiveAdventsMageTriggeredAbility() {
        super(Zone.BATTLEFIELD, new GlavaFiveAdventsMageEffect(), true);
        setDoOnlyOnceEachTurn(true);
        setTriggerPhrase("Whenever you cast a permanent spell with {X} in its mana cost or activate an ability "
                + "with {X} in its activation cost that isn't a mana ability, ");
    }

    private GlavaFiveAdventsMageTriggeredAbility(final GlavaFiveAdventsMageTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public GlavaFiveAdventsMageTriggeredAbility copy() {
        return new GlavaFiveAdventsMageTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.SPELL_CAST
                || event.getType() == GameEvent.EventType.ACTIVATED_ABILITY;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        if (!isControlledBy(event.getPlayerId())) {
            return false;
        }
        StackObject stackObject = game.getStack().getStackObject(event.getTargetId());
        if (stackObject instanceof Spell) {
            Spell spell = (Spell) stackObject;
            if (!spell.isPermanent(game) || !spell.getCard().getManaCost().containsX()) {
                return false;
            }
        } else if (stackObject instanceof StackAbility) {
            if (!stackObject.getStackAbility().getManaCosts().containsX()) {
                return false;
            }
        } else {
            return false;
        }
        getEffects().setTargetPointer(new FixedTarget(stackObject.getId()));
        return true;
    }
}

class GlavaFiveAdventsMageEffect extends OneShotEffect {

    GlavaFiveAdventsMageEffect() {
        super(Outcome.Benefit);
        staticText = "you may have the value of X become 5";
    }

    private GlavaFiveAdventsMageEffect(final GlavaFiveAdventsMageEffect effect) {
        super(effect);
    }

    @Override
    public GlavaFiveAdventsMageEffect copy() {
        return new GlavaFiveAdventsMageEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        StackObject stackObject = game.getStack().getStackObject(getTargetPointer().getFirst(game, source));
        if (stackObject == null) {
            return false;
        }
        stackObject.getStackAbility().setCostsTag("X", 5);
        game.informPlayers(stackObject.getLogName() + ": X is now 5");
        return true;
    }
}

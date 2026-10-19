package mage.abilities.keyword;

import mage.abilities.Ability;
import mage.abilities.DelayedTriggeredAbility;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.ReplacementEffectImpl;
import mage.cards.Card;
import mage.cards.CardsImpl;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.Zone;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.events.ZoneChangeEvent;
import mage.game.stack.Spell;
import mage.players.Player;
import mage.util.CardUtil;

import java.util.UUID;

/**
 * 702.192a Paradigm represents two spell abilities, one of which creates a delayed triggered ability. Paradigm
 * means "If this is the first time a spell you control with this spell's name has resolved this game, at the
 * beginning of each of your precombat main phases for the rest of the game, create a copy of this object in
 * exile. You may cast the copy without paying its mana cost" and "Exile this spell." See rule 707.10.
 * <p>
 * Done as rebound is (a static ability of the spell on the stack, a replacement of its move to the graveyard as
 * it resolves): a spell that does not resolve (countered, every target gone) goes to the graveyard and does
 * nothing more. The copy is made from the card as it is each time, cast at once or not at all: a copy left in
 * exile would cease to exist anyway (704.5e).
 *
 * @author TheElk801, 290dotwav
 */
public class ParadigmAbility extends SimpleStaticAbility {

    public ParadigmAbility() {
        super(Zone.STACK, new ParadigmReplacementEffect());
    }

    protected ParadigmAbility(final ParadigmAbility ability) {
        super(ability);
    }

    @Override
    public ParadigmAbility copy() {
        return new ParadigmAbility(this);
    }

    @Override
    public String getRule() {
        return "Paradigm <i>(Then exile this spell. After you first resolve a spell with this name, " +
                "you may cast a copy of it from exile without paying its mana cost " +
                "at the beginning of each of your first main phases.)</i>";
    }

    /**
     * The game's record that a spell of this name, controlled by this player, has resolved: only the first one
     * creates the delayed trigger.
     */
    static String resolvedKey(UUID controllerId, String name) {
        return "paradigmResolved|" + controllerId + '|' + name;
    }
}

class ParadigmReplacementEffect extends ReplacementEffectImpl {

    ParadigmReplacementEffect() {
        super(Duration.WhileOnStack, Outcome.Benefit);
    }

    private ParadigmReplacementEffect(final ParadigmReplacementEffect effect) {
        super(effect);
    }

    @Override
    public ParadigmReplacementEffect copy() {
        return new ParadigmReplacementEffect(this);
    }

    @Override
    public boolean checksEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.ZONE_CHANGE;
    }

    @Override
    public boolean applies(GameEvent event, Ability source, Game game) {
        // As rebound: a spell countered or fizzled has no source on its move (or another one), so only a spell
        // that resolves gets here.
        ZoneChangeEvent zEvent = (ZoneChangeEvent) event;
        return zEvent.getFromZone() == Zone.STACK
                && zEvent.getToZone() == Zone.GRAVEYARD
                && event.getSourceId() != null
                && event.getSourceId().equals(source.getSourceId());
    }

    @Override
    public boolean replaceEvent(GameEvent event, Ability source, Game game) {
        Spell spell = game.getStack().getSpell(source.getSourceId());
        if (spell != null && spell.isCopy()) {
            return false;
        }
        Card card = game.getCard(source.getSourceId());
        Player controller = game.getPlayer(source.getControllerId());
        if (card == null || controller == null) {
            return false;
        }
        String key = ParadigmAbility.resolvedKey(controller.getId(), card.getName());
        if (game.getState().getValue(key) == null) {
            game.getState().setValue(key, Boolean.TRUE);
            game.addDelayedTriggeredAbility(new ParadigmDelayedTriggeredAbility(card.getId(), card.getName()), source);
        }
        // "Exile this spell."
        controller.moveCardsToExile(card, source, game, true, null, "");
        return true;
    }
}

class ParadigmDelayedTriggeredAbility extends DelayedTriggeredAbility {

    private final String cardName;

    ParadigmDelayedTriggeredAbility(UUID cardId, String cardName) {
        super(new ParadigmCastCopyEffect(cardId), Duration.EndOfGame, false, false);
        this.cardName = cardName;
    }

    private ParadigmDelayedTriggeredAbility(final ParadigmDelayedTriggeredAbility ability) {
        super(ability);
        this.cardName = ability.cardName;
    }

    @Override
    public ParadigmDelayedTriggeredAbility copy() {
        return new ParadigmDelayedTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.PRECOMBAT_MAIN_PHASE_PRE;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        return game.isActivePlayer(getControllerId());
    }

    @Override
    public String getRule() {
        return "Paradigm - At the beginning of your precombat main phase, create a copy of " + cardName
                + " in exile. You may cast the copy without paying its mana cost.";
    }
}

class ParadigmCastCopyEffect extends OneShotEffect {

    private final UUID cardId;

    ParadigmCastCopyEffect(UUID cardId) {
        super(Outcome.PlayForFree);
        this.cardId = cardId;
        staticText = "create a copy of this card in exile. You may cast the copy without paying its mana cost";
    }

    private ParadigmCastCopyEffect(final ParadigmCastCopyEffect effect) {
        super(effect);
        this.cardId = effect.cardId;
    }

    @Override
    public ParadigmCastCopyEffect copy() {
        return new ParadigmCastCopyEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        Card card = game.getCard(cardId);
        if (controller == null || card == null) {
            return false;
        }
        // "This object" is the card the spell was, wherever it has gone since (707.10); the copy is made in the
        // card's zone, exile, as Massimo the Magician's is.
        Card copy = game.copyCard(card, source, controller.getId());
        if (copy == null) {
            return false;
        }
        CardUtil.castSpellWithAttributesForFree(controller, source, game, new CardsImpl(copy), StaticFilters.FILTER_CARD);
        return true;
    }
}

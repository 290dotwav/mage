package mage.cards.l;

import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.TriggeredAbilityImpl;
import mage.abilities.effects.OneShotEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SubType;
import mage.constants.SuperType;
import mage.constants.Zone;
import mage.filter.FilterSpell;
import mage.filter.predicate.Predicates;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.stack.Spell;
import mage.players.Player;
import mage.util.CardUtil;
import mage.watchers.common.SpellsCastWatcher;

import java.util.UUID;

/**
 * @author Claude
 */
public final class LadyLokiAgentOfChaos extends CardImpl {

    public LadyLokiAgentOfChaos(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{5}{R}");

        this.supertype.add(SuperType.LEGENDARY);
        this.subtype.add(SubType.GOD);
        this.subtype.add(SubType.SORCERER);
        this.subtype.add(SubType.VILLAIN);
        this.power = new MageInt(5);
        this.toughness = new MageInt(5);

        // Whenever you cast your first instant, sorcery, or Villain spell each turn, exile it, then exile cards from the top of your library until you exile a nonland card. Lady Loki deals damage to each opponent equal to the difference between that spell's mana value and that nonland card's mana value. You may cast that card without paying its mana cost.
        this.addAbility(new LadyLokiAgentOfChaosTriggeredAbility());
    }

    private LadyLokiAgentOfChaos(final LadyLokiAgentOfChaos card) {
        super(card);
    }

    @Override
    public LadyLokiAgentOfChaos copy() {
        return new LadyLokiAgentOfChaos(this);
    }
}

class LadyLokiAgentOfChaosTriggeredAbility extends TriggeredAbilityImpl {

    static final FilterSpell filter = new FilterSpell("instant, sorcery, or Villain spell");

    static {
        filter.add(Predicates.or(
                CardType.INSTANT.getPredicate(),
                CardType.SORCERY.getPredicate(),
                SubType.VILLAIN.getPredicate()
        ));
    }

    LadyLokiAgentOfChaosTriggeredAbility() {
        super(Zone.BATTLEFIELD, new LadyLokiAgentOfChaosEffect());
        setTriggerPhrase("Whenever you cast your first instant, sorcery, or Villain spell each turn, ");
    }

    private LadyLokiAgentOfChaosTriggeredAbility(final LadyLokiAgentOfChaosTriggeredAbility ability) {
        super(ability);
    }

    @Override
    public LadyLokiAgentOfChaosTriggeredAbility copy() {
        return new LadyLokiAgentOfChaosTriggeredAbility(this);
    }

    @Override
    public boolean checkEventType(GameEvent event, Game game) {
        return event.getType() == GameEvent.EventType.SPELL_CAST;
    }

    @Override
    public boolean checkTrigger(GameEvent event, Game game) {
        if (!isControlledBy(event.getPlayerId())) {
            return false;
        }
        Spell spell = game.getSpell(event.getTargetId());
        SpellsCastWatcher watcher = game.getState().getWatcher(SpellsCastWatcher.class);
        if (spell == null || watcher == null || !filter.match(spell, getControllerId(), this, game)) {
            return false;
        }
        // the watcher already holds this spell: it is the first if it is the only matching one
        long matching = watcher
                .getSpellsCastThisTurn(getControllerId())
                .stream()
                .filter(s -> filter.match(s, getControllerId(), this, game))
                .count();
        if (matching != 1) {
            return false;
        }
        getEffects().setValue("spellCast", spell);
        return true;
    }
}

class LadyLokiAgentOfChaosEffect extends OneShotEffect {

    LadyLokiAgentOfChaosEffect() {
        super(Outcome.Benefit);
        staticText = "exile it, then exile cards from the top of your library until you exile a nonland card. "
                + "{this} deals damage to each opponent equal to the difference between that spell's mana value "
                + "and that nonland card's mana value. You may cast that card without paying its mana cost";
    }

    private LadyLokiAgentOfChaosEffect(final LadyLokiAgentOfChaosEffect effect) {
        super(effect);
    }

    @Override
    public LadyLokiAgentOfChaosEffect copy() {
        return new LadyLokiAgentOfChaosEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player controller = game.getPlayer(source.getControllerId());
        Spell spellLKI = (Spell) getValue("spellCast");
        if (controller == null || spellLKI == null) {
            return false;
        }
        int spellManaValue = spellLKI.getManaValue();
        // exile it: only if it is still that spell on the stack
        Spell spell = game.getSpell(spellLKI.getId());
        if (spell != null && spell.getZoneChangeCounter(game) == spellLKI.getZoneChangeCounter(game)) {
            spellManaValue = spell.getManaValue();
            controller.moveCards(spell, Zone.EXILED, source, game);
        }
        Card nonland = null;
        for (Card card : controller.getLibrary().getCards(game)) {
            controller.moveCards(card, Zone.EXILED, source, game);
            if (!card.isLand(game)) {
                nonland = card;
                break;
            }
        }
        if (nonland == null) {
            return true;
        }
        int damage = Math.abs(spellManaValue - nonland.getManaValue());
        if (damage > 0) {
            for (UUID opponentId : game.getOpponents(controller.getId())) {
                Player opponent = game.getPlayer(opponentId);
                if (opponent != null) {
                    opponent.damage(damage, source.getSourceId(), source, game);
                }
            }
        }
        CardUtil.castSpellWithAttributesForFree(controller, source, game, nonland);
        return true;
    }
}

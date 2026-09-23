package mage.cards.u;

import mage.abilities.Ability;
import mage.abilities.common.SpellCastControllerTriggeredAbility;
import mage.abilities.effects.OneShotEffect;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.SetTargetPointer;
import mage.filter.StaticFilters;
import mage.game.Game;
import mage.game.stack.Spell;
import mage.players.Player;
import mage.watchers.common.ManaPaidSourceWatcher;

import java.util.UUID;

/**
 * @author Claude
 */
public final class UncoverTheMoonLetters extends CardImpl {

    public UncoverTheMoonLetters(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ENCHANTMENT}, "{3}{U}");

        // Whenever you cast a noncreature spell, you may draw X cards, where X is the amount of mana spent to cast that spell. If you do, discard two cards.
        this.addAbility(new SpellCastControllerTriggeredAbility(
                new UncoverTheMoonLettersEffect(), StaticFilters.FILTER_SPELL_A_NON_CREATURE,
                false, SetTargetPointer.SPELL
        ));
    }

    private UncoverTheMoonLetters(final UncoverTheMoonLetters card) {
        super(card);
    }

    @Override
    public UncoverTheMoonLetters copy() {
        return new UncoverTheMoonLetters(this);
    }
}

class UncoverTheMoonLettersEffect extends OneShotEffect {

    UncoverTheMoonLettersEffect() {
        super(Outcome.DrawCard);
        staticText = "you may draw X cards, where X is the amount of mana spent to cast that spell. " +
                "If you do, discard two cards";
    }

    private UncoverTheMoonLettersEffect(final UncoverTheMoonLettersEffect effect) {
        super(effect);
    }

    @Override
    public UncoverTheMoonLettersEffect copy() {
        return new UncoverTheMoonLettersEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(source.getControllerId());
        Spell spell = (Spell) getValue("spellCast");
        if (player == null || spell == null) {
            return false;
        }
        int amount = ManaPaidSourceWatcher.getTotalPaid(spell.getId(), game);
        if (amount < 1 || !player.chooseUse(
                outcome, "Draw " + amount + " card" + (amount > 1 ? "s" : "") + ", then discard two cards?", source, game
        )) {
            return false;
        }
        if (player.drawCards(amount, source, game) < 1) {
            return true;
        }
        player.discard(2, false, false, source, game);
        return true;
    }
}

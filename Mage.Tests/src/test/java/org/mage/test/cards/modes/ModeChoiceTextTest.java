package org.mage.test.cards.modes;

import mage.abilities.Ability;
import mage.abilities.Mode;
import mage.abilities.Modes;
import mage.cards.Card;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link Modes#getChoiceText} and {@link Modes#getChoiceMessage}: the mode dialog as a player
 * reads it (HumanPlayer.chooseMode). A spree mode said nothing of its own cost (702.172a), a mode
 * chosen twice read "(selected 2x) counter target spell..." with its first letter down, and spree's
 * question said "of 2147483647".
 */
public class ModeChoiceTextTest extends CardTestPlayerBase {

    private Ability spellOf(String cardName) {
        Card card = currentGame.getCards().stream()
                .filter(c -> c.getName().equals(cardName) && c.getOwnerId().equals(playerA.getId()))
                .findFirst().orElse(null);
        Assert.assertNotNull(cardName, card);
        return card.getSpellAbility();
    }

    private List<String> texts(Ability spell) {
        Modes modes = spell.getModes();
        return modes.values().stream().map(m -> modes.getChoiceText(m, currentGame.getObject(spell))).collect(Collectors.toList());
    }

    @Test
    public void spree_eachModeSaysItsCost_andTheQuestionNoCeiling() {
        addCard(Zone.HAND, playerA, "Explosive Derailment");
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        Ability spell = spellOf("Explosive Derailment");
        spell.getModes().clearSelectedModes();
        List<String> texts = texts(spell);
        Assert.assertTrue(texts.toString(), texts.contains("+ {2}: Explosive Derailment deals 4 damage to target creature."));
        Assert.assertTrue(texts.toString(), texts.contains("+ {2}: Destroy target artifact."));
        Assert.assertEquals("Choose mode (selected 0, min 1)", spell.getModes().getChoiceMessage(currentGame, spell));
    }

    @Test
    public void sameModeTwice_counted_andUpperCase() {
        addCard(Zone.HAND, playerA, "Mystic Confluence");
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        Ability spell = spellOf("Mystic Confluence");
        Modes modes = spell.getModes();
        // A spell's modes come with the first one selected: cleared, as a cast starts its choice.
        modes.clearSelectedModes();
        Mode counter = modes.values().stream().filter(m -> modes.getChoiceText(m, null).startsWith("Counter")).findFirst().orElse(null);
        Assert.assertNotNull(texts(spell).toString(), counter);
        modes.addSelectedMode(counter.getId());
        modes.addSelectedMode(counter.getId());
        Assert.assertTrue(modes.getChoiceText(counter, null), modes.getChoiceText(counter, null).startsWith("(selected 2x) Counter target spell unless its controller pays {3}"));
        Assert.assertEquals("Choose mode (selected 2 of 3, min 3)", modes.getChoiceMessage(currentGame, spell));
    }

    @Test
    public void plainModes_unchanged() {
        addCard(Zone.HAND, playerA, "Kolaghan's Command");
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        for (String text : texts(spellOf("Kolaghan's Command"))) {
            Assert.assertFalse(text, text.startsWith("+") || text.startsWith("("));
            Assert.assertTrue(text, Character.isUpperCase(text.charAt(0)));
        }
    }
}

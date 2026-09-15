package org.mage.test.cards.single.dsc;

import mage.abilities.Ability;
import mage.abilities.keyword.MiracleAbility;
import mage.cards.Card;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.player.TestPlayer;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author ClaudeMTG
 */
public class AminatouVeilPiercerTest extends CardTestPlayerBase {

    private static final String aminatou = "Aminatou, Veil Piercer";
    // {1}{W}{U}{B} Legendary Creature - Human Wizard 2/4
    // At the beginning of your upkeep, surveil 2.
    // Each enchantment card in your hand has miracle. Its miracle cost is equal to its mana cost reduced by {4}.

    private String miracleRuleOf(String cardName) {
        for (Card card : playerA.getHand().getCards(currentGame)) {
            if (!card.getName().equals(cardName)) {
                continue;
            }
            for (Ability ability : card.getAbilities(currentGame)) {
                if (ability instanceof MiracleAbility) {
                    return ability.getRule();
                }
            }
        }
        return null;
    }

    @Test
    public void testUpkeepSurveil() {
        skipInitShuffling();
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Grizzly Bears"); // bottom
        addCard(Zone.LIBRARY, playerA, "Alaborn Trooper");
        addCard(Zone.LIBRARY, playerA, "Hill Giant"); // top
        addCard(Zone.BATTLEFIELD, playerA, aminatou);

        addTarget(playerA, "Hill Giant^Alaborn Trooper"); // surveil both into the graveyard

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, "Hill Giant", 1);
        assertGraveyardCount(playerA, "Alaborn Trooper", 1);
        assertGraveyardCount(playerA, 2);
    }

    /**
     * Ghostly Prison costs {2}{W}, so the miracle cost Aminatou gives it is {W}.
     * A creature card in hand gets nothing.
     */
    @Test
    public void testEnchantmentCardsInHandGetMiracle() {
        skipInitShuffling();
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.BATTLEFIELD, playerA, aminatou);
        addCard(Zone.HAND, playerA, "Ghostly Prison"); // {2}{W} enchantment
        addCard(Zone.HAND, playerA, "Grizzly Bears"); // {1}{G} creature

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        Assert.assertEquals(
                "Ghostly Prison must have miracle {W}",
                "Miracle {W} <i>(You may cast this card for its miracle cost when you draw it "
                        + "if it's the first card you drew this turn.)</i>",
                miracleRuleOf("Ghostly Prison")
        );
        Assert.assertNull("a creature card must not get miracle", miracleRuleOf("Grizzly Bears"));
    }

    /**
     * The miracle cost tracks the real mana cost, Sigil of the Empty Throne is {5}{W}, so {1}{W}.
     */
    @Test
    public void testMiracleCostIsTheManaCostMinusFour() {
        skipInitShuffling();
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.BATTLEFIELD, playerA, aminatou);
        addCard(Zone.HAND, playerA, "Omniscience"); // {7}{U}{U}{U} enchantment

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        Assert.assertEquals(
                "Omniscience must have miracle {3}{U}{U}{U}",
                "Miracle {3}{U}{U}{U} <i>(You may cast this card for its miracle cost when you draw it "
                        + "if it's the first card you drew this turn.)</i>",
                miracleRuleOf("Omniscience")
        );
    }

    /**
     * Without Aminatou there is no miracle at all.
     */
    @Test
    public void testNoMiracleWithoutAminatou() {
        skipInitShuffling();
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.HAND, playerA, "Ghostly Prison");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        Assert.assertNull(miracleRuleOf("Ghostly Prison"));
    }
}

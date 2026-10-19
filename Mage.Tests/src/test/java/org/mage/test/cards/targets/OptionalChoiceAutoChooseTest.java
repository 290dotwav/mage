package org.mage.test.cards.targets;

import mage.abilities.Ability;
import mage.cards.Card;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.filter.FilterCard;
import mage.filter.predicate.mageobject.NamePredicate;
import mage.target.TargetCard;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

import java.util.Collections;
import java.util.UUID;

/**
 * A "you may" pick of a card from among several (ripple's "you may cast any of
 * those cards with the same name", 702.60a) is asked even when a single card
 * fits: taking none is an answer. Auto-choice (`TargetImpl.tryToAutoChoose`,
 * used by `HumanPlayer` for every pick, and for a chair at the door) used to
 * take that sole card for the player, as it does for a pick that must be made.
 * A required pick with a single candidate is still taken without asking.
 *
 * @author ClaudeMTG
 */
public class OptionalChoiceAutoChooseTest extends CardTestPlayerBase {

    private Card rippleCard() {
        return playerA.getLibrary().getCards(currentGame).stream()
                .filter(card -> card.getName().equals("Surging Flame"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no Surging Flame in the library"));
    }

    private TargetCard sameName() {
        FilterCard filter = new FilterCard("card(s) with the name: \"Surging Flame\" to cast without paying their mana cost");
        filter.add(new NamePredicate("Surging Flame"));
        return new TargetCard(Zone.LIBRARY, filter);
    }

    @Test
    public void test_OptionalSingleCandidate_IsAsked_RequiredSingleCandidate_IsAuto() {
        addCard(Zone.LIBRARY, playerA, "Surging Flame", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);

        // auto-choice is off in strict mode: the test needs it on
        setStrictChooseMode(false);
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        Card card = rippleCard();
        Ability source = card.getSpellAbility();
        UUID me = playerA.getId();

        // required: the sole card is chosen for the player
        TargetCard required = sameName();
        Assert.assertEquals("a required pick of one from one is still made for the player",
                card.getId(), required.tryToAutoChoose(me, source, currentGame, Collections.singleton(card.getId())));

        // cancelable only (a cost, a cast's targets): unchanged, still chosen for the player
        TargetCard cancelable = sameName();
        cancelable.setRequired(false);
        Assert.assertEquals("a cancelable pick (setRequired(false) alone) keeps its auto-choice",
                card.getId(), cancelable.tryToAutoChoose(me, source, currentGame, Collections.singleton(card.getId())));

        // a "you may" pick: never chosen for the player, even with one card
        TargetCard optional = sameName();
        optional.withOptionalChoice();
        Assert.assertTrue(optional.isOptionalChoice());
        Assert.assertFalse("a you-may pick offers the Done that takes none", optional.isRequired());
        Assert.assertNull("a you-may pick of one card is asked, not made",
                optional.tryToAutoChoose(me, source, currentGame, Collections.singleton(card.getId())));
        // and a copy of it (targets are copied with their ability) stays optional
        Assert.assertNull("a copy keeps the you-may",
                optional.copy().tryToAutoChoose(me, source, currentGame, Collections.singleton(card.getId())));
    }
}

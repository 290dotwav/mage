package org.mage.test.cards.abilities.keywords;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.permanent.Permanent;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * 702.190a "Sneak [cost]" means "Any time you could cast an instant during your declare blockers step, you may
 * cast this spell by paying [cost] and returning an unblocked creature you control to its owner's hand rather
 * than paying this spell's mana cost."
 * <p>
 * 702.190b A permanent spell whose sneak cost was paid enters the battlefield tapped and attacking (see rule
 * 506.3a). It will be attacking the same player, planeswalker, or battle as the creature that was returned to its
 * owner's hand to pay the sneak cost of the spell that became that permanent.
 */
public class SneakTest extends CardTestPlayerBase {

    private static final String leonardo = "Leonardo, Big Brother"; // 1/3, sneak {W}, +1/+0 for each other creature you control
    private static final String bears = "Grizzly Bears";
    private static final String lion = "Silvercoat Lion"; // 2/2
    private static final String technique = "Donatello's Technique"; // sorcery, sneak {U}, draw two cards

    @Test
    public void testCreatureEntersTappedAndAttacking() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 1);
        addCard(Zone.BATTLEFIELD, playerA, bears);
        addCard(Zone.BATTLEFIELD, playerA, lion);
        addCard(Zone.HAND, playerA, leonardo);

        attack(1, playerA, bears, playerB);
        castSpell(1, PhaseStep.DECLARE_BLOCKERS, playerA, leonardo + " with Sneak");
        setChoice(playerA, bears); // the unblocked attacker returned

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertHandCount(playerA, bears, 1);
        Permanent leo = getPermanent(leonardo, playerA);
        Assert.assertTrue("entered tapped", leo.isTapped());
        // 1/3, +1/+0 for the Lion: 2 damage, as the bear it replaced would have dealt
        assertLife(playerB, 20 - 2);
        // the Lion never attacked
        assertTapped(lion, false);
    }

    @Test
    public void testAttacksThePlaneswalkerTheReturnedCreatureAttacked() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 1);
        addCard(Zone.BATTLEFIELD, playerA, bears);
        addCard(Zone.BATTLEFIELD, playerB, "Ajani Goldmane"); // loyalty 4
        addCard(Zone.HAND, playerA, leonardo);

        attack(1, playerA, bears, "Ajani Goldmane");
        castSpell(1, PhaseStep.DECLARE_BLOCKERS, playerA, leonardo + " with Sneak");
        setChoice(playerA, bears);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertLife(playerB, 20);
        assertCounterCount(playerB, "Ajani Goldmane", mage.counters.CounterType.LOYALTY, 4 - 1);
    }

    @Test
    public void testSorceryIsCastWithoutAttacking() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 1);
        addCard(Zone.BATTLEFIELD, playerA, bears);
        addCard(Zone.HAND, playerA, technique);
        addCard(Zone.LIBRARY, playerA, "Swamp", 2);

        attack(1, playerA, bears, playerB);
        castSpell(1, PhaseStep.DECLARE_BLOCKERS, playerA, technique + " with Sneak");
        setChoice(playerA, bears);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertLife(playerB, 20);
        assertHandCount(playerA, bears, 1);
        assertGraveyardCount(playerA, technique, 1);
        assertHandCount(playerA, 3); // the bear and two cards
    }

    @Test
    public void testInstantResolvesItsEffect() {
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);
        addCard(Zone.BATTLEFIELD, playerA, bears);
        addCard(Zone.BATTLEFIELD, playerB, lion);
        addCard(Zone.HAND, playerA, "Jennika's Technique"); // instant, sneak {R}, 2 damage to each creature

        attack(1, playerA, bears, playerB);
        castSpell(1, PhaseStep.DECLARE_BLOCKERS, playerA, "Jennika's Technique with Sneak");
        setChoice(playerA, bears);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertHandCount(playerA, bears, 1);
        assertGraveyardCount(playerB, lion, 1);
    }
}

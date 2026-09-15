package org.mage.test.cards.single.clb;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author ClaudeMTG
 */
public class DurnanOfTheYawningPortalTest extends CardTestPlayerBase {

    private static final String durnan = "Durnan of the Yawning Portal";
    // {3}{G} Legendary Creature - Human Warrior 3/3
    // Whenever Durnan attacks, look at the top four cards of your library. You may exile a creature
    //   card from among them. Put the rest on the bottom of your library in any order. For as long as
    //   that card remains exiled, you may cast it. That spell has undaunted.
    // Choose a Background

    private void initLibrary() {
        skipInitShuffling();
        removeAllCardsFromLibrary(playerA);
        // bottom : Forest Hill Giant : top
        addCard(Zone.LIBRARY, playerA, "Forest");
        addCard(Zone.LIBRARY, playerA, "Hill Giant"); // {3}{R} 3/3
    }

    @Test
    public void testExileAndCastWithUndaunted() {
        initLibrary();
        addCard(Zone.BATTLEFIELD, playerA, durnan);
        // Hill Giant costs {3}{R}, undaunted makes it {2}{R} with one opponent
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 3);

        attack(1, playerA, durnan);
        setChoice(playerA, "Hill Giant"); // exile it

        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Hill Giant");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 3);
        assertPermanentCount(playerA, "Hill Giant", 1);
        assertExileCount(playerA, 0);
        assertTappedCount("Mountain", true, 3);
    }

    @Test
    public void testCantCastWithoutTheUndauntedDiscount() {
        initLibrary();
        addCard(Zone.BATTLEFIELD, playerA, durnan);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);

        attack(1, playerA, durnan);
        setChoice(playerA, "Hill Giant"); // exile it

        // {3}{R} reduced to {2}{R} by undaunted is still one mana more than available
        checkPlayableAbility("can't cast yet", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Cast Hill Giant", false);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Hill Giant", 0);
        assertExileCount(playerA, 1);
    }
}

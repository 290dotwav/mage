package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class TheSpearOfBashengaTest extends CardTestPlayerBase {

    // When The Spear of Bashenga enters, if there is no monarch, you become the monarch.
    // Equipped creature gets +2/+2 and has vigilance.
    // Whenever equipped creature attacks the monarch, destroy target tapped nonland permanent that player controls.
    // Equip {2}
    private static final String SPEAR = "The Spear of Bashenga";

    @Test
    public void test_EntersMonarch() {
        addCard(Zone.HAND, playerA, SPEAR);
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 5);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, SPEAR);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        Assert.assertEquals("monarch", playerA.getId(), currentGame.getMonarchId());
    }

    @Test
    public void test_AttackMonarchDestroysTapped() {
        addCard(Zone.BATTLEFIELD, playerA, SPEAR);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 2);
        // B becomes the monarch with Okoye, tapping its Mind Stone to pay
        addCard(Zone.HAND, playerB, "Okoye, Mighty and Adored");
        addCard(Zone.BATTLEFIELD, playerB, "Savannah", 3);
        addCard(Zone.BATTLEFIELD, playerB, "Mind Stone");

        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Okoye, Mighty and Adored");
        addTarget(playerB, "Okoye, Mighty and Adored"); // its combat trigger
        checkMonarch("B is the monarch", 2, PhaseStep.END_TURN, playerB, playerB);

        activateAbility(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Equip", "Grizzly Bears");
        attack(3, playerA, "Grizzly Bears", playerB);
        addTarget(playerA, "Mind Stone");

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_COMBAT);
        execute();

        assertGraveyardCount(playerB, "Mind Stone", 1);
        assertTapped("Grizzly Bears", false); // vigilance
        assertLife(playerB, 20 - 4);
    }
}

package org.mage.test.cards.single.frc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class TeferisReproachTest extends CardTestPlayerBase {

    private static final String reproach = "Teferi's Reproach";

    @Test
    public void test_UntilThatPlayersNextTurn() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 4);
        addCard(Zone.HAND, playerA, reproach);
        addCard(Zone.HAND, playerA, "Blood Tithe"); // each opponent loses 3 life
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Forest");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);
        addCard(Zone.HAND, playerA, "Lightning Bolt");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, reproach, playerB);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPermanentCount("bears phased out", 1, PhaseStep.PRECOMBAT_MAIN, playerB, "Grizzly Bears", 0);
        checkPermanentCount("land stays", 1, PhaseStep.PRECOMBAT_MAIN, playerB, "Forest", 1);
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Blood Tithe");
        waitStackResolved(1, PhaseStep.POSTCOMBAT_MAIN);
        checkLife("life can't change", 1, PhaseStep.POSTCOMBAT_MAIN, playerB, 20);

        // on playerB's turn it is over (the Bolt waits for playerA's lands to untap)
        checkPermanentCount("bears phased in", 2, PhaseStep.PRECOMBAT_MAIN, playerB, "Grizzly Bears", 1);
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertExileCount(playerA, reproach, 1);
        assertLife(playerB, 17);
        Assert.assertTrue(playerA.getLife() == 20);
    }
}

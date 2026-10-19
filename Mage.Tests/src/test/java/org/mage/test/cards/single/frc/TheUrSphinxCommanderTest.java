package org.mage.test.cards.single.frc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestCommanderDuelBase;

/**
 * @author Claude
 */
public class TheUrSphinxCommanderTest extends CardTestCommanderDuelBase {

    // eminence from the command zone
    @Test
    public void test_EminenceFromCommandZone() {
        addCard(Zone.COMMAND, playerA, "The Ur-Sphinx");
        addCard(Zone.BATTLEFIELD, playerA, "Island", 5);
        addCard(Zone.HAND, playerA, "Sphinx of Jwar Isle"); // {4}{U}{U}

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Sphinx of Jwar Isle");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Sphinx of Jwar Isle", 1);
    }
}

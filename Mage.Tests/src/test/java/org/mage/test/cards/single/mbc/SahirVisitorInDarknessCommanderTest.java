package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestCommanderDuelBase;

/**
 * @author Claude
 */
public class SahirVisitorInDarknessCommanderTest extends CardTestCommanderDuelBase {

    // eminence from the command zone
    @Test
    public void test_EminenceFromCommandZone() {
        addCard(Zone.COMMAND, playerA, "Sahir, Visitor in Darkness");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertLife(playerA, 40 - 1);
    }
}

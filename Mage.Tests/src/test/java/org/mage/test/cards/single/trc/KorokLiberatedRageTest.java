package org.mage.test.cards.single.trc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class KorokLiberatedRageTest extends CardTestPlayerBase {

    private static final String korok = "Korok, Liberated Rage";

    // one opponent attacked: creatures he controls get +1/+1 until end of turn
    @Test
    public void test_Battlefront() {
        addCard(Zone.BATTLEFIELD, playerA, korok);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        attack(1, playerA, korok);
        checkPT("korok", 1, PhaseStep.END_COMBAT, playerA, korok, 3, 3);
        checkPT("bears", 1, PhaseStep.END_COMBAT, playerA, "Grizzly Bears", 3, 3);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 3);
        assertPowerToughness(playerA, "Grizzly Bears", 2, 2);
    }
}

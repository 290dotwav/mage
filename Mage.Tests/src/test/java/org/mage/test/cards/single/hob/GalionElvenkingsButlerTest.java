package org.mage.test.cards.single.hob;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class GalionElvenkingsButlerTest extends CardTestPlayerBase {

    private static final String galion = "Galion, Elvenking's Butler"; // 4/4

    @Test
    public void test_BasePTLockedIn() {
        addCard(Zone.BATTLEFIELD, playerA, galion);
        addCard(Zone.BATTLEFIELD, playerA, "Llanowar Elves"); // 1/1
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 1);
        addCard(Zone.HAND, playerA, "Giant Growth");

        attack(1, playerA, galion);
        addTarget(playerA, "Llanowar Elves");
        // pump Galion afterwards: the elves stay 4/4
        castSpell(1, PhaseStep.DECLARE_BLOCKERS, playerA, "Giant Growth", galion);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertPowerToughness(playerA, "Llanowar Elves", 4, 4);
        assertPowerToughness(playerA, galion, 7, 7);
    }
}

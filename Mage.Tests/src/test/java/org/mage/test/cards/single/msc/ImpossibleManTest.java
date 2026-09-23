package org.mage.test.cards.single.msc;

import mage.abilities.keyword.VigilanceAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class ImpossibleManTest extends CardTestPlayerBase {

    // Flying
    // {2}{U}: Impossible Man becomes a copy of another target permanent until end of turn, except his name is Impossible Man.
    private static final String IMPOSSIBLE = "Impossible Man";

    @Test
    public void test_CopyUntilEndOfTurn() {
        addCard(Zone.BATTLEFIELD, playerA, IMPOSSIBLE);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.BATTLEFIELD, playerB, "Serra Angel");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}{U}: ", "Serra Angel");
        checkPT("copy", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, IMPOSSIBLE, 4, 4);
        checkAbility("copy has vigilance", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, IMPOSSIBLE, VigilanceAbility.class, true);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.UPKEEP);
        execute();

        assertPowerToughness(playerA, IMPOSSIBLE, 1, 4);
        assertAbility(playerA, IMPOSSIBLE, VigilanceAbility.getInstance(), false);
    }
}

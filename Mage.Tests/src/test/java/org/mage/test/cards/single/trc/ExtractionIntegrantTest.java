package org.mage.test.cards.single.trc;

import mage.abilities.keyword.MenaceAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class ExtractionIntegrantTest extends CardTestPlayerBase {

    private static final String integrant = "Extraction Integrant";
    private static final String myr = "Alpha Myr"; // 2/1 artifact creature
    private static final String bears = "Grizzly Bears";
    private static final String runeclaw = "Runeclaw Bear";

    // an artifact creature has menace, and each creature blocking it gets -1/-1
    @Test
    public void test_MenaceAndBlockersShrink() {
        addCard(Zone.BATTLEFIELD, playerA, integrant);
        addCard(Zone.BATTLEFIELD, playerA, myr);
        addCard(Zone.BATTLEFIELD, playerB, bears);
        addCard(Zone.BATTLEFIELD, playerB, runeclaw);

        attack(1, playerA, myr);
        block(1, playerB, bears, myr);
        block(1, playerB, runeclaw, myr);
        checkPT("bears shrunk", 1, PhaseStep.COMBAT_DAMAGE, playerB, bears, 1, 1);
        checkPT("runeclaw shrunk", 1, PhaseStep.COMBAT_DAMAGE, playerB, runeclaw, 1, 1);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertAbilityCount(playerA, myr, MenaceAbility.class, 1);
        assertAbilityCount(playerA, integrant, MenaceAbility.class, 1);
    }
}

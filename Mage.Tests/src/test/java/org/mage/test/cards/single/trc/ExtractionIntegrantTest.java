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

    // an artifact creature has menace (two blockers needed), and each creature blocking it gets -1/-1
    @Test
    public void test_MenaceAndBlockersShrink() {
        addCard(Zone.BATTLEFIELD, playerA, integrant);
        addCard(Zone.BATTLEFIELD, playerA, myr);
        addCard(Zone.BATTLEFIELD, playerB, bears);
        addCard(Zone.BATTLEFIELD, playerB, runeclaw);

        checkAbility("myr has menace", 1, PhaseStep.PRECOMBAT_MAIN, playerA, myr, MenaceAbility.class, true);
        attack(1, playerA, myr);
        block(1, playerB, bears, myr);
        block(1, playerB, runeclaw, myr);
        setChoice(playerA, "Whenever a creature blocks this creature"); // order of the two triggers
        setChoiceAmount(playerA, 1, 1); // the Myr's 2 damage, 1 to each blocker

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        // 1 damage kills each 2/2 only because it is a 1/1 now
        assertGraveyardCount(playerB, bears, 1);
        assertGraveyardCount(playerB, runeclaw, 1);
        assertGraveyardCount(playerA, myr, 1);
        assertAbilityCount(playerA, integrant, MenaceAbility.class, 1);
    }
}

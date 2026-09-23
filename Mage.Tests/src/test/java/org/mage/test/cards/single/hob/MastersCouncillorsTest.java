package org.mage.test.cards.single.hob;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class MastersCouncillorsTest extends CardTestPlayerBase {

    private static final String councillors = "Master's Councillors"; // 1/3

    @Test
    public void test_BoostPerFullGraveyard() {
        addCard(Zone.BATTLEFIELD, playerA, councillors);
        addCard(Zone.GRAVEYARD, playerA, "Grizzly Bears", 7);
        addCard(Zone.GRAVEYARD, playerB, "Grizzly Bears", 6);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertPowerToughness(playerA, councillors, 3, 3);
    }

    @Test
    public void test_SecondDrawMills() {
        addCard(Zone.BATTLEFIELD, playerA, councillors);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.HAND, playerA, "Think Twice");
        addCard(Zone.GRAVEYARD, playerB, "Grizzly Bears", 4);

        // turn 3: draw step is the first card, Think Twice the second
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Think Twice");
        addTarget(playerA, playerB);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerB, 7);
        assertPowerToughness(playerA, councillors, 3, 3);
    }
}

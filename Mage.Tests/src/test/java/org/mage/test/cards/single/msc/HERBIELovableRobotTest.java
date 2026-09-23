package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class HERBIELovableRobotTest extends CardTestPlayerBase {

    private static final String herbie = "H.E.R.B.I.E., Lovable Robot";

    @Test
    public void test_NoncreatureSpellCast_Surveil() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, herbie);
        addCard(Zone.HAND, playerA, "Shock");
        addCard(Zone.LIBRARY, playerA, "Forest");

        addCard(Zone.BATTLEFIELD, playerA, "Mountain");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerB);
        addTarget(playerA, "Forest"); // surveil: put into graveyard

        setStopAt(1, PhaseStep.DECLARE_ATTACKERS);
        setStrictChooseMode(true);
        execute();

        assertLife(playerB, 18);
        assertGraveyardCount(playerA, "Forest", 1);
    }

    @Test
    public void test_NoNoncreatureSpell_NoTrigger() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, herbie);
        addCard(Zone.LIBRARY, playerA, "Forest");

        setStopAt(1, PhaseStep.DECLARE_ATTACKERS);
        setStrictChooseMode(true);
        execute();

        assertGraveyardCount(playerA, "Forest", 0);
    }
}

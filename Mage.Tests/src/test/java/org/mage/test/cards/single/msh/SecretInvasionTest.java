package org.mage.test.cards.single.msh;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class SecretInvasionTest extends CardTestPlayerBase {

    @Test
    public void test_ExileAndCopy_ReturnWhenAuraLeaves() {
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 2);
        addCard(Zone.HAND, playerA, "Secret Invasion");
        addCard(Zone.HAND, playerA, "Disenchant");
        addCard(Zone.BATTLEFIELD, playerB, "Serra Angel");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Secret Invasion", "Grizzly Bears");
        addTarget(playerA, "Serra Angel");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPermanentCount("angel exiled", 1, PhaseStep.PRECOMBAT_MAIN, playerB, "Serra Angel", 0);
        checkPermanentCount("bears is a copy", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Serra Angel", 1);

        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Disenchant", "Secret Invasion");

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertPermanentCount(playerB, "Serra Angel", 1);
        assertPermanentCount(playerA, "Grizzly Bears", 1);
    }
}

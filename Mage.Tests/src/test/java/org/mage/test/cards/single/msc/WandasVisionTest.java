package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class WandasVisionTest extends CardTestPlayerBase {

    @Test
    public void test_SecondSpell_ExileUntilNonland_CastFree() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, "Wanda's Vision");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);
        addCard(Zone.HAND, playerA, "Shock", 2);
        addCard(Zone.LIBRARY, playerA, "Grizzly Bears");
        addCard(Zone.LIBRARY, playerA, "Forest"); // top

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerB);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerB);
        setChoice(playerA, true); // cast Grizzly Bears for free

        setStopAt(1, PhaseStep.BEGIN_COMBAT);
        setStrictChooseMode(true);
        execute();

        assertLife(playerB, 20 - 4);
        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertExileCount(playerA, "Forest", 1);
    }
}

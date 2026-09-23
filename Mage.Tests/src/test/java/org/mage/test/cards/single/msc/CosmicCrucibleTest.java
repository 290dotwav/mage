package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class CosmicCrucibleTest extends CardTestPlayerBase {

    private static final String crucible = "Cosmic Crucible";

    @Test
    public void test_FirstMainMana_CopyOnlyOnce() {
        addCard(Zone.BATTLEFIELD, playerA, crucible);
        addCard(Zone.HAND, playerA, "Lightning Bolt", 2);

        // first main phase: four mana in any combination of colors, used for both bolts
        setChoiceAmount(playerA, 0, 0, 0, 4, 0); // W U B R G

        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);
        setChoice(playerA, true); // copy it
        setChoice(playerA, false); // don't change the target
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);
        // no copy: only once each turn

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertLife(playerB, 20 - 3 - 3 - 3);
    }
}

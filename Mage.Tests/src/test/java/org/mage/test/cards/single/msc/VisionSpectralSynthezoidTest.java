package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class VisionSpectralSynthezoidTest extends CardTestPlayerBase {

    private static final String vision = "Vision, Spectral Synthezoid";

    @Test
    public void test_OnceEachTurn_NoncreatureOrRobot() {
        addCard(Zone.BATTLEFIELD, playerA, vision);
        addCard(Zone.HAND, playerA, "Lightning Bolt", 2);
        addCard(Zone.HAND, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, "Flying Drone"); // Robot creature

        checkPlayableAbility("creature not free", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Grizzly Bears", false);
        checkPlayableAbility("robot free", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Flying Drone", true);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);
        setChoice(playerA, "Without paying manacost"); // permitting object: Vision
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPlayableAbility("used this turn", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Lightning Bolt", false);
        checkPlayableAbility("not on opponent's turn", 2, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Lightning Bolt", false);
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Flying Drone");
        setChoice(playerA, "Without paying manacost");

        setStopAt(3, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertLife(playerB, 20 - 3);
        assertPermanentCount(playerA, "Flying Drone", 1);
    }
}

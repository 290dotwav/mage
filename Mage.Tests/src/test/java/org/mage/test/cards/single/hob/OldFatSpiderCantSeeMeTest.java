package org.mage.test.cards.single.hob;

import mage.abilities.keyword.HexproofAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class OldFatSpiderCantSeeMeTest extends CardTestPlayerBase {

    private static final String saga = "Old Fat Spider Can't See Me";

    @Test
    public void test_Chapters() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, saga);
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant"); // 3/3

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, saga);
        addTarget(playerA, "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkAbility("hexproof", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears", HexproofAbility.class, true);

        // II on turn 3: Hill Giant's damage is prevented
        addTarget(playerA, "Hill Giant");
        attack(4, playerB, "Hill Giant");

        // III (turn 5) and IV (turn 7): draw; then the Saga is sacrificed and hexproof ends
        checkAbility("hexproof still", 5, PhaseStep.END_TURN, playerA, "Grizzly Bears", HexproofAbility.class, true);
        checkAbility("hexproof gone", 7, PhaseStep.END_TURN, playerA, "Grizzly Bears", HexproofAbility.class, false);

        setStrictChooseMode(true);
        setStopAt(7, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20);
        assertGraveyardCount(playerA, saga, 1);
        // draws on turns 3, 5, 7 + chapters III and IV
        assertHandCount(playerA, 5);
    }
}

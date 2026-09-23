package org.mage.test.cards.single.tmc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.player.TestPlayer;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class CaseyAndRaphHotheadsTest extends CardTestPlayerBase {

    private static final String casey = "Casey & Raph, Hotheads";

    @Test
    public void test_BothModes_OpponentPlaysFreeOnTheirTurn() {
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 5);
        addCard(Zone.HAND, playerA, casey);
        removeAllCardsFromLibrary(playerB);
        addCard(Zone.LIBRARY, playerB, "Island", 3);
        addCard(Zone.LIBRARY, playerB, "Hill Giant"); // top, {3}{R}
        skipInitShuffling();

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, casey);
        setModeChoice(playerA, "1");
        setModeChoice(playerA, "2");
        addTarget(playerA, playerB); // exiles and may play
        addTarget(playerA, playerA); // treasures

        // B has no lands: casts Hill Giant for free on their turn
        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Hill Giant");

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Treasure Token", 2);
        assertPermanentCount(playerB, "Hill Giant", 1);
    }

    @Test
    public void test_OnlyUntilThatPlayersNextEndStep() {
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 5);
        addCard(Zone.HAND, playerA, casey);
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Island", 3);
        addCard(Zone.LIBRARY, playerA, "Hill Giant"); // top
        skipInitShuffling();

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, casey);
        setModeChoice(playerA, "1");
        setModeChoice(playerA, TestPlayer.MODE_SKIP);
        addTarget(playerA, playerA);

        checkPlayableAbility("this turn", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Cast Hill Giant", true);
        checkPlayableAbility("not after my end step", 3, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Hill Giant", false);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertExileCount(playerA, "Hill Giant", 1);
    }
}

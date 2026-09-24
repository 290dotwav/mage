package org.mage.test.cards.single.sos;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class LoreholdTheHistorianTest extends CardTestPlayerBase {

    private static final String lorehold = "Lorehold, the Historian";

    @Test
    public void test_MiracleOnFirstDraw() {
        addCard(Zone.BATTLEFIELD, playerA, lorehold);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Lava Axe"); // {4}{R}: 5 damage to target player
        skipInitShuffling();

        // turn 3 draw: Lava Axe has miracle {2}
        setChoice(playerA, true); // reveal for miracle
        setChoice(playerA, true); // cast it for its miracle cost
        addTarget(playerA, playerB);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertLife(playerB, 20 - 5);
        assertGraveyardCount(playerA, "Lava Axe", 1);
        assertTappedCount("Mountain", true, 2);
    }

    @Test
    public void test_OpponentUpkeepLoot() {
        addCard(Zone.BATTLEFIELD, playerA, lorehold);
        addCard(Zone.HAND, playerA, "Grizzly Bears");

        // turn 2: opponent's upkeep
        setChoice(playerA, true); // discard to draw
        setChoice(playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.DRAW);
        execute();

        assertGraveyardCount(playerA, "Grizzly Bears", 1);
        assertHandCount(playerA, 1);
    }
}

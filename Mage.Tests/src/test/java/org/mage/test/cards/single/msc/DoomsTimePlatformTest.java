package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class DoomsTimePlatformTest extends CardTestPlayerBase {

    // Whenever you attack, exile target nonland card from your graveyard with two time counters on it. If it doesn't have suspend, it gains suspend.
    private static final String PLATFORM = "Doom's Time Platform";

    @Test
    public void test_SuspendFromGraveyardAndCast() {
        addCard(Zone.BATTLEFIELD, playerA, PLATFORM);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.GRAVEYARD, playerA, "Hill Giant");

        attack(1, playerA, "Grizzly Bears", playerB);
        addTarget(playerA, "Hill Giant");

        checkExileCount("suspended", 1, PhaseStep.END_TURN, playerA, "Hill Giant", 1);

        // turn 3 upkeep: 2 -> 1; turn 5 upkeep: 1 -> 0, cast it for free
        setChoice(playerA, true); // cast the suspended card

        setStrictChooseMode(true);
        setStopAt(5, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertPermanentCount(playerA, "Hill Giant", 1);
    }

    @Test
    public void test_CountersAfterOneUpkeep() {
        addCard(Zone.BATTLEFIELD, playerA, PLATFORM);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.GRAVEYARD, playerA, "Hill Giant");

        attack(1, playerA, "Grizzly Bears", playerB);
        addTarget(playerA, "Hill Giant");

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertExileCount(playerA, "Hill Giant", 1);
        assertCounterOnExiledCardCount("Hill Giant", CounterType.TIME, 1);
    }
}

package org.mage.test.cards.single.trc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestCommanderDuelBase;

/**
 * @author Claude
 */
public class QueensDiamondTest extends CardTestCommanderDuelBase {

    private static final String diamond = "Queen's Diamond";

    // charge counters from station, then one mana of a colour per counter, for the commander
    @Test
    public void test_ManaForCommander() {
        addCard(Zone.BATTLEFIELD, playerA, diamond);
        addCard(Zone.BATTLEFIELD, playerA, "Runeclaw Bear"); // power 2, stations it
        addCard(Zone.COMMAND, playerA, "Grizzly Bears"); // {1}{G}

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Station");
        setChoice(playerA, "Runeclaw Bear");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");
        setChoice(playerA, "Green");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertCounterCount(playerA, diamond, CounterType.CHARGE, 2);
        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertTapped(diamond, true);
    }
}

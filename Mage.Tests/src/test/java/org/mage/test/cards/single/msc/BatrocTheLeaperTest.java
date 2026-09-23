package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class BatrocTheLeaperTest extends CardTestPlayerBase {

    // Multikicker {2}
    // Batroc enters with a +1/+1 counter on him for each time he was kicked.
    // When Batroc enters, he deals damage equal to his power to each of up to X targets, where X is the number of times he was kicked.
    private static final String BATROC = "Batroc the Leaper";

    @Test
    public void test_KickedTwice() {
        addCard(Zone.HAND, playerA, BATROC);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 6);
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant"); // 3/3

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, BATROC);
        setChoice(playerA, true); // kick
        setChoice(playerA, true); // kick
        setChoice(playerA, false); // stop
        addTarget(playerA, playerB);
        addTarget(playerA, "Hill Giant");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertCounterCount(playerA, BATROC, CounterType.P1P1, 2);
        assertLife(playerB, 20 - 4);
        assertGraveyardCount(playerB, "Hill Giant", 1);
    }

    @Test
    public void test_NotKicked() {
        addCard(Zone.HAND, playerA, BATROC);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, BATROC);
        setChoice(playerA, false); // no kick

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPowerToughness(playerA, BATROC, 2, 2);
        assertLife(playerB, 20);
    }
}

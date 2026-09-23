package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class HawkeyeAvengingArcherTest extends CardTestPlayerBase {

    // Reach
    // Whenever a creature an opponent controls dies, if Hawkeye dealt damage to it this turn, draw a card.
    // {T}: Hawkeye deals 1 damage to any target.
    private static final String HAWKEYE = "Hawkeye, Avenging Archer";

    @Test
    public void test_PingKillDraws() {
        addCard(Zone.BATTLEFIELD, playerA, HAWKEYE);
        addCard(Zone.BATTLEFIELD, playerB, "Memnite");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: ", "Memnite");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerB, "Memnite", 1);
        assertHandCount(playerA, 1);
    }

    @Test
    public void test_DamagedThenKilledOtherwise() {
        addCard(Zone.BATTLEFIELD, playerA, HAWKEYE);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");
        addCard(Zone.HAND, playerA, "Shock");
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: ", "Hill Giant");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", "Hill Giant"); // dealt damage by Hawkeye earlier: draws
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Lightning Bolt", "Grizzly Bears"); // never damaged by Hawkeye

        addCard(Zone.HAND, playerA, "Lightning Bolt");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerB, "Hill Giant", 1);
        assertGraveyardCount(playerB, "Grizzly Bears", 1);
        assertHandCount(playerA, 1);
    }
}

package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class FlyingDroneTest extends CardTestPlayerBase {

    private static final String drone = "Flying Drone";

    @Test
    public void test_FreeAfterFlyerEntered() {
        addCard(Zone.BATTLEFIELD, playerA, drone);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.HAND, playerA, "Storm Crow"); // {1}{U} 1/2 flying

        checkPlayableAbility("not free yet", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "{1}{U}, {T}: Draw", true);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Storm Crow");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        // no mana left: only playable because it now costs nothing
        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{1}{U}, {T}: Draw");
        // the drawn card is the only card in hand: it is discarded

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertTapped(drone, true);
        assertGraveyardCount(playerA, 1);
    }

    @Test
    public void test_DroneItselfDoesNotCount() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.HAND, playerA, drone);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, drone);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPlayableAbility("not free", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "{1}{U}, {T}: Draw", false);

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();
    }
}

package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class DamoclesBaseSwordOfKangTest extends CardTestPlayerBase {

    private static final String base = "Damocles Base, Sword of Kang";

    private void setup() {
        addCard(Zone.BATTLEFIELD, playerA, base);
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant"); // power 3
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Crew 3");
        setChoice(playerA, "Hill Giant");
        attack(1, playerA, base);
    }

    @Test
    public void test_ChooseSacrifice() {
        setup();
        setChoice(playerB, true); // sacrifice a nontoken creature
        setChoice(playerB, "Grizzly Bears");

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertLife(playerB, 20 - 5);
        assertGraveyardCount(playerB, "Grizzly Bears", 1);
        assertHandCount(playerA, 0);
    }

    @Test
    public void test_ChooseLoseLife() {
        setup();
        setChoice(playerB, false); // lose 2 life, opponent draws two

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertLife(playerB, 20 - 5 - 2);
        assertPermanentCount(playerB, "Grizzly Bears", 1);
        assertHandCount(playerA, 2);
    }
}

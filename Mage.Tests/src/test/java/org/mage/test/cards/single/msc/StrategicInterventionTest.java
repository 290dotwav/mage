package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class StrategicInterventionTest extends CardTestPlayerBase {

    // Whenever a creature you control attacks alone, it gets +1/+1 until end of turn. Tap up to one target creature defending player controls.
    private static final String INTERVENTION = "Strategic Intervention";

    @Test
    public void test_AttackAlone() {
        addCard(Zone.BATTLEFIELD, playerA, INTERVENTION);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant"); // stays home
        addCard(Zone.BATTLEFIELD, playerB, "Wall of Wood"); // 0/3 defender

        attack(1, playerA, "Grizzly Bears", playerB);
        addTarget(playerA, "Wall of Wood");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertTapped("Wall of Wood", true);
        assertTapped("Hill Giant", false);
        assertLife(playerB, 20 - 3);
    }

    @Test
    public void test_TwoAttackersNoTrigger() {
        addCard(Zone.BATTLEFIELD, playerA, INTERVENTION);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant");

        attack(1, playerA, "Grizzly Bears", playerB);
        attack(1, playerA, "Hill Giant", playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertLife(playerB, 20 - 2 - 3);
    }
}

package org.mage.test.cards.single.trc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class JuratiCordialQueenTest extends CardTestPlayerBase {

    private static final String jurati = "Jurati, Cordial Queen";

    // an artifact creature hits a player: a copy of an artifact token he controls
    @Test
    public void test_CombatDamageCopiesToken() {
        addCard(Zone.BATTLEFIELD, playerA, jurati);
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 2);
        addCard(Zone.HAND, playerA, "Servo Exhibition"); // two 1/1 Servo artifact tokens

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Servo Exhibition");
        attack(1, playerA, jurati);
        addTarget(playerA, "Servo Token");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 3);
        assertPermanentCount(playerA, "Servo Token", 3);
    }
}

package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class MoltenLavamancerTest extends CardTestPlayerBase {

    private static final String lavamancer = "Molten Lavamancer";

    @Test
    public void test_OnceEachTurn_NotOnCombatDamage() {
        addCard(Zone.BATTLEFIELD, playerA, lavamancer);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);
        addCard(Zone.HAND, playerA, "Shock", 2);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerB);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerB);

        attack(3, playerA, lavamancer); // combat damage: no token

        setStopAt(3, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertPermanentCount(playerA, "Elemental Token", 1);
        assertLife(playerB, 20 - 2 - 2 - 2);
    }

    @Test
    public void test_NotDuringOpponentsTurn() {
        addCard(Zone.BATTLEFIELD, playerA, lavamancer);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);
        addCard(Zone.HAND, playerA, "Shock");

        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerB);

        setStopAt(2, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertPermanentCount(playerA, "Elemental Token", 0);
        assertLife(playerB, 18);
    }
}

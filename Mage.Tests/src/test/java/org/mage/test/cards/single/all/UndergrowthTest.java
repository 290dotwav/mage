package org.mage.test.cards.single.all;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class UndergrowthTest extends CardTestPlayerBase {

    private static final String undergrowth = "Undergrowth";

    @Test
    public void test_PreventAllCombatDamage() {
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 1);
        addCard(Zone.HAND, playerA, undergrowth);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Raging Goblin"); // red 1/1 haste

        attack(2, playerB, "Grizzly Bears");
        attack(2, playerB, "Raging Goblin");
        castSpell(2, PhaseStep.DECLARE_BLOCKERS, playerA, undergrowth);
        setChoice(playerA, false);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20);
    }

    @Test
    public void test_PaidRedCreaturesStillDeal() {
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 3);
        addCard(Zone.HAND, playerA, undergrowth);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Raging Goblin"); // red 1/1

        attack(2, playerB, "Grizzly Bears");
        attack(2, playerB, "Raging Goblin");
        castSpell(2, PhaseStep.DECLARE_BLOCKERS, playerA, undergrowth);
        setChoice(playerA, true);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 19);
        assertTappedCount("Mountain", true, 3);
    }
}

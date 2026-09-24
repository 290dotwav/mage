package org.mage.test.cards.single.hob;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class WizardsStaffTest extends CardTestPlayerBase {

    private static final String staff = "Wizard's Staff";

    @Test
    public void test_ProwessTriggersTwice() {
        addCard(Zone.BATTLEFIELD, playerA, staff);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 4);
        addCard(Zone.HAND, playerA, "Shock");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Equip {3}", "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerB);
        setChoice(playerA, "Prowess"); // order the two prowess triggers

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertLife(playerB, 20 - 2);
        assertPowerToughness(playerA, "Grizzly Bears", 2 + 2, 2 + 2);
    }

    @Test
    public void test_EquipWizardCheap() {
        addCard(Zone.BATTLEFIELD, playerA, staff);
        addCard(Zone.BATTLEFIELD, playerA, "Prodigal Sorcerer"); // Human Wizard 1/1
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Island", 1);

        checkPlayableAbility("can't equip bears for 1", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Equip {3}", false);
        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Equip Wizard", "Prodigal Sorcerer");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertAttachedTo(playerA, staff, "Prodigal Sorcerer", true);
    }
}

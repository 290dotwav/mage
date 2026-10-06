package org.mage.test.commander;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestCommander4Players;

/**
 * Auto mana pays without losing life whenever it can: a painland's coloured ability, a Mana Confluence or an Ancient
 * Tomb is used only when nothing painless pays, and a painland pays generic with its {C}. The owner, 6 October 2026:
 * « mon pote a joué son commandant et l'auto mana a utilisé un land qui lui a enlevé un pv alors qu'il pouvait éviter
 * ça » (Kaalia of the Vast, beside a Command Tower, a Concealed Courtyard, a Battlefield Forge and an Orzhov Signet).
 */
public class PainlessManaTest extends CardTestCommander4Players {

    @Test
    public void test_CommanderWithPainlessLandsAndAPainland_NoLifeLost() {
        // Kaalia of the Vast {1}{R}{W}{B}
        addCard(Zone.COMMAND, playerA, "Kaalia of the Vast");
        addCard(Zone.BATTLEFIELD, playerA, "Command Tower");
        addCard(Zone.BATTLEFIELD, playerA, "Plains");
        addCard(Zone.BATTLEFIELD, playerA, "Swamp");
        addCard(Zone.BATTLEFIELD, playerA, "Battlefield Forge");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Kaalia of the Vast");
        setChoice(playerA, "Red"); // the Tower makes the {R}: the Forge gives its {C} to the {1}

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Kaalia of the Vast", 1);
        assertLife(playerA, 20);
    }

    @Test
    public void test_TheLiveBoard_NoLifeLost() {
        // his friend's board: Command Tower, Concealed Courtyard, Battlefield Forge, Orzhov Signet
        addCard(Zone.COMMAND, playerA, "Kaalia of the Vast");
        addCard(Zone.BATTLEFIELD, playerA, "Command Tower");
        addCard(Zone.BATTLEFIELD, playerA, "Concealed Courtyard");
        addCard(Zone.BATTLEFIELD, playerA, "Battlefield Forge");
        addCard(Zone.BATTLEFIELD, playerA, "Orzhov Signet");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Kaalia of the Vast");
        setChoice(playerA, "Red");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Kaalia of the Vast", 1);
        assertLife(playerA, 20);
    }

    @Test
    public void test_GenericPaidWithThePainlandsColorless() {
        // Hill Giant {3}{R}: a Mountain for {R}, the Forge's {C} and two Plains for the {3}
        addCard(Zone.HAND, playerA, "Hill Giant");
        addCard(Zone.BATTLEFIELD, playerA, "Battlefield Forge");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 2);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Hill Giant");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Hill Giant", 1);
        assertTappedCount("Battlefield Forge", true, 1);
        assertLife(playerA, 20);
    }

    @Test
    public void test_OnlyThePainlandPays_OneLifeLost() {
        // Raging Goblin {R}: nothing but the Forge makes red, so it pays, and it hurts
        addCard(Zone.HAND, playerA, "Raging Goblin");
        addCard(Zone.BATTLEFIELD, playerA, "Battlefield Forge");
        addCard(Zone.BATTLEFIELD, playerA, "Plains");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Raging Goblin");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Raging Goblin", 1);
        assertTappedCount("Plains", false, 1);
        assertLife(playerA, 19);
    }

    @Test
    public void test_ColouredPipPaidByABasicBeforeThePainland() {
        // Raging Goblin {R} with a Battlefield Forge and a Mountain: the Mountain
        addCard(Zone.HAND, playerA, "Raging Goblin");
        addCard(Zone.BATTLEFIELD, playerA, "Battlefield Forge");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Raging Goblin");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Raging Goblin", 1);
        assertTappedCount("Battlefield Forge", false, 1);
        assertLife(playerA, 20);
    }
}

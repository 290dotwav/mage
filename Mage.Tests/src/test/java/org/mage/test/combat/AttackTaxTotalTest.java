package org.mage.test.combat;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * CR 508.1g-h: the attacking player totals every cost to attack, may activate
 * mana abilities, then pays the total at once; if he can't or won't, the
 * declaration is illegal and nothing of it is kept.
 * <p>
 * The taxes used to be asked one by one: with two Ghostly Prisons and mana for
 * one tax only, the first {2} was paid (the Sol Ring tapped), the second could
 * not be, the creature did not attack and the first {2} was lost.
 *
 * @author ClaudeMTG
 */
public class AttackTaxTotalTest extends CardTestPlayerBase {

    // Creatures can't attack you unless their controller pays {2} for each creature they control that's attacking you.
    private static final String PRISON = "Ghostly Prison";
    // Creatures can't attack you unless their controller pays {2} for each creature they control that's attacking you.
    private static final String PROPAGANDA = "Propaganda";
    private static final String MEMNITE = "Memnite"; // {0} 1/1
    private static final String SOL_RING = "Sol Ring"; // {T}: Add {C}{C}.

    @Test
    public void twoPrisons_manaForOneTaxOnly_noAttackAndNothingSpent() {
        setStrictChooseMode(true);

        addCard(Zone.BATTLEFIELD, playerB, PRISON, 2);
        addCard(Zone.BATTLEFIELD, playerA, MEMNITE);
        addCard(Zone.BATTLEFIELD, playerA, SOL_RING);

        attack(1, playerA, MEMNITE);
        setChoice(playerA, true); // Pay {4} to attack? - one question for both Prisons

        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertLife(playerB, 20); // no attack
        assertTapped(MEMNITE, false);
        assertTapped(SOL_RING, false); // the {2} it could pay was given back
    }

    @Test
    public void twoPrisons_manaForBoth_attacksWithOnePayment() {
        setStrictChooseMode(true);

        addCard(Zone.BATTLEFIELD, playerB, PRISON, 2);
        addCard(Zone.BATTLEFIELD, playerA, MEMNITE);
        addCard(Zone.BATTLEFIELD, playerA, SOL_RING);
        addCard(Zone.BATTLEFIELD, playerA, "Wastes", 2);

        attack(1, playerA, MEMNITE);
        setChoice(playerA, true); // Pay {4} to attack? - asked once, no order of the Prisons to choose

        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertLife(playerB, 19);
        assertTapped(SOL_RING, true);
        assertTappedCount("Wastes", true, 2);
    }

    @Test
    public void twoPrisons_declined_nothingSpent() {
        setStrictChooseMode(true);

        addCard(Zone.BATTLEFIELD, playerB, PRISON, 2);
        addCard(Zone.BATTLEFIELD, playerA, MEMNITE);
        addCard(Zone.BATTLEFIELD, playerA, SOL_RING);
        addCard(Zone.BATTLEFIELD, playerA, "Wastes", 2);

        attack(1, playerA, MEMNITE);
        setChoice(playerA, false); // Pay {4} to attack? No

        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertLife(playerB, 20);
        assertTapped(SOL_RING, false);
        assertTappedCount("Wastes", false, 2);
    }

    @Test
    public void propagandaAndPrison_manaForOneTaxOnly_noAttackAndNothingSpent() {
        setStrictChooseMode(true);

        addCard(Zone.BATTLEFIELD, playerB, PRISON);
        addCard(Zone.BATTLEFIELD, playerB, PROPAGANDA);
        addCard(Zone.BATTLEFIELD, playerA, MEMNITE);
        addCard(Zone.BATTLEFIELD, playerA, "Wastes", 3);

        attack(1, playerA, MEMNITE);
        setChoice(playerA, true); // Pay {4} to attack?

        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertLife(playerB, 20);
        assertTappedCount("Wastes", false, 3);
    }

    @Test
    public void singlePrison_unchanged() {
        setStrictChooseMode(true);

        addCard(Zone.BATTLEFIELD, playerB, PRISON);
        addCard(Zone.BATTLEFIELD, playerA, MEMNITE);
        addCard(Zone.BATTLEFIELD, playerA, SOL_RING);

        attack(1, playerA, MEMNITE);
        setChoice(playerA, true); // Pay {2} to attack?

        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertLife(playerB, 19);
        assertTapped(SOL_RING, true);
    }
}

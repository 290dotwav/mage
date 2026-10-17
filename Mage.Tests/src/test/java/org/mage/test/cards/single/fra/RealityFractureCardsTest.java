package org.mage.test.cards.single.fra;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * The two Reality Fracture cards the owner's Molecule Man deck needed: they load and cast.
 */
public class RealityFractureCardsTest extends CardTestPlayerBase {

    private static final String EMRAKUL = "Emrakul, the Exigent Doom";
    private static final String OMNATH = "Omnath, Locus of the Void";

    // When you cast this spell, untap all lands you control.
    @Test
    public void emrakulCastsAndUntapsTheLands() {
        addCard(Zone.BATTLEFIELD, playerA, "Wastes", 10);
        addCard(Zone.HAND, playerA, EMRAKUL);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, EMRAKUL);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, EMRAKUL, 1);
        assertPowerToughness(playerA, EMRAKUL, 12, 12);
        assertTappedCount("Wastes", false, 10);
    }

    // Landfall: {C}{C}; +1/+1 for each unspent mana; lost mana becomes colorless instead.
    @Test
    public void omnathCastsAndGrowsOnLandfall() {
        addCard(Zone.BATTLEFIELD, playerA, "Wastes", 7);
        addCard(Zone.HAND, playerA, OMNATH);
        addCard(Zone.HAND, playerA, "Wastes");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, OMNATH);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        playLand(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Wastes");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPT("grown by the landfall mana", 1, PhaseStep.PRECOMBAT_MAIN, playerA, OMNATH, 8, 8);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertPermanentCount(playerA, OMNATH, 1);
        // the {C}{C} was not lost at the end of the main phase: it stayed, colorless
        assertPowerToughness(playerA, OMNATH, 8, 8);
    }
}

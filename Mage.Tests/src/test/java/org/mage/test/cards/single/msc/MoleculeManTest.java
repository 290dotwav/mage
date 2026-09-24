package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class MoleculeManTest extends CardTestPlayerBase {

    // Nonland cards in your hand have miracle {0}.
    private static final String MOLECULE = "Molecule Man";

    @Test
    public void test_FirstDrawIsAMiracle() {
        skipInitShuffling();
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Hill Giant");
        addCard(Zone.BATTLEFIELD, playerA, MOLECULE);

        // turn 3 draw step: Hill Giant is the first card drawn
        setChoice(playerA, true); // reveal for miracle
        setChoice(playerA, true); // use the miracle trigger: cast it for {0}

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertPermanentCount(playerA, "Hill Giant", 1);
    }

    @Test
    public void test_LandsHaveNoMiracle() {
        skipInitShuffling();
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Forest");
        addCard(Zone.BATTLEFIELD, playerA, MOLECULE);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertHandCount(playerA, "Forest", 1);
    }

    @Test
    public void test_PrintedMiracleCanUseZero() {
        // Thunderous Wrath: deals 5 damage to any target; Miracle {R}. No mana: only {0} can cast it.
        skipInitShuffling();
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Thunderous Wrath");
        addCard(Zone.BATTLEFIELD, playerA, MOLECULE);

        setChoice(playerA, true); // reveal (the card's own miracle)
        setChoice(playerA, "Miracle {0}"); // order triggers: the granted miracle {0} resolves first
        setChoice(playerA, true); // use Molecule Man's miracle {0}
        addTarget(playerA, playerB);
        setChoice(playerA, true); // the card's own trigger: tries to cast for {R}, can't pay, the cast is cancelled
        addTarget(playerA, playerB);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertLife(playerB, 20 - 5);
    }

    @Test
    public void test_OpponentsMoleculeManDoesNothing() {
        skipInitShuffling();
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Hill Giant");
        addCard(Zone.BATTLEFIELD, playerB, MOLECULE);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertHandCount(playerA, "Hill Giant", 1);
    }
}

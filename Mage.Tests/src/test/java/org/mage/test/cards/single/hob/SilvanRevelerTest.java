package org.mage.test.cards.single.hob;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class SilvanRevelerTest extends CardTestPlayerBase {

    private static final String reveler = "Silvan Reveler";

    @Test
    public void test_DiscardLandPutsItOntoBattlefieldTapped_ThenLandfallReturns() {
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Grizzly Bears", 5);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 4);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 4);
        addCard(Zone.HAND, playerA, reveler);
        addCard(Zone.HAND, playerA, "Mountain");
        addCard(Zone.HAND, playerA, "Plains");
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 1);
        addCard(Zone.HAND, playerB, "Lightning Bolt");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, reveler);
        setChoice(playerA, "Mountain"); // discard
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPermanentTapped("mountain came in tapped", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Mountain", true, 1);

        castSpell(1, PhaseStep.BEGIN_COMBAT, playerB, "Lightning Bolt", reveler);
        waitStackResolved(1, PhaseStep.BEGIN_COMBAT);

        // landfall from the graveyard
        playLand(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Plains");
        setChoice(playerA, true); // pay {1}{G}{U}

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Mountain", 1);
        assertHandCount(playerA, reveler, 1);
        assertHandCount(playerA, "Grizzly Bears", 1);
        assertGraveyardCount(playerA, 0);
    }

    @Test
    public void test_DiscardNonLand() {
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Grizzly Bears", 5);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.HAND, playerA, reveler);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, reveler);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, "Grizzly Bears", 1);
        assertPermanentCount(playerA, "Grizzly Bears", 0);
    }
}

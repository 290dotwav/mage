package org.mage.test.cards.single.tmp;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * Intuition: "Search your library for three cards and reveal them. Target opponent chooses one. Put that card into
 * your hand and the rest into your graveyard." The opponent's pick now says what it is for ("Select a card for
 * PlayerA's hand (the other two go to the graveyard)"): it read "Select a card" and nothing more.
 */
public class IntuitionTest extends CardTestPlayerBase {

    @Test
    public void testTheOpponentPicksTheCardForTheHand() {
        addCard(Zone.HAND, playerA, "Intuition");
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.LIBRARY, playerA, "Grizzly Bears");
        addCard(Zone.LIBRARY, playerA, "Hill Giant");
        addCard(Zone.LIBRARY, playerA, "Llanowar Elves");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Intuition", playerB);
        addTarget(playerA, "Grizzly Bears^Hill Giant^Llanowar Elves");
        setChoice(playerB, "Llanowar Elves");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerA, "Llanowar Elves", 1);
        assertGraveyardCount(playerA, "Grizzly Bears", 1);
        assertGraveyardCount(playerA, "Hill Giant", 1);
    }
}

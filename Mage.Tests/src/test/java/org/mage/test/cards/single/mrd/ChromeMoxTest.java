package org.mage.test.cards.single.mrd;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * Chrome Mox: "Imprint - When Chrome Mox enters, you may exile a nonartifact,
 * nonland card from your hand." The pick used to be made over every player's
 * hand (a TargetCard of the hand zone chosen with no cards given), so the
 * opponent's cards were offered too, and one taken imprinted nothing.
 */
public class ChromeMoxTest extends CardTestPlayerBase {

    private static final String mox = "Chrome Mox";

    @Test
    public void testOnlyYourOwnHandIsOffered() {
        // Nothing of A's own can be imprinted: no pick is asked (strict mode fails on an unexpected one).
        addCard(Zone.HAND, playerA, mox);
        addCard(Zone.HAND, playerA, "Mountain");
        addCard(Zone.HAND, playerB, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, mox);
        setChoice(playerA, true); // the imprint's "may"

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, mox, 1);
        assertHandCount(playerB, "Grizzly Bears", 1);
        assertExileCount(playerB, 0);
    }

    @Test
    public void testImprintedCardMakesItsColour() {
        addCard(Zone.HAND, playerA, mox);
        addCard(Zone.HAND, playerA, "Lightning Bolt");
        addCard(Zone.HAND, playerA, "Hill Giant"); // {3}{R}
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 3);
        addCard(Zone.HAND, playerB, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, mox);
        setChoice(playerA, true);
        setChoice(playerA, "Lightning Bolt");
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Hill Giant");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertExileCount(playerA, "Lightning Bolt", 1);
        assertPermanentCount(playerA, "Hill Giant", 1);
        assertHandCount(playerB, "Grizzly Bears", 1);
    }
}

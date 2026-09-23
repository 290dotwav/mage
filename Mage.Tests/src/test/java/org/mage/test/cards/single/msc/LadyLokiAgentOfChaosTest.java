package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class LadyLokiAgentOfChaosTest extends CardTestPlayerBase {

    // Whenever you cast your first instant, sorcery, or Villain spell each turn, exile it, then exile cards from the
    // top of your library until you exile a nonland card. Lady Loki deals damage to each opponent equal to the
    // difference between that spell's mana value and that nonland card's mana value. You may cast that card without paying its mana cost.
    private static final String LOKI = "Lady Loki, Agent of Chaos";

    @Test
    public void test_FirstInstant() {
        skipInitShuffling();
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Hill Giant"); // mana value 4
        addCard(Zone.LIBRARY, playerA, "Island");
        addCard(Zone.BATTLEFIELD, playerA, LOKI);
        addCard(Zone.HAND, playerA, "Lightning Bolt", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);
        setChoice(playerA, true); // cast Hill Giant for free
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        // second instant this turn: no trigger, it resolves
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Lightning Bolt", playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        // |1 - 4| = 3 from Lady Loki, then 3 from the second Bolt; the first Bolt never resolved
        assertLife(playerB, 20 - 3 - 3);
        assertExileCount(playerA, "Lightning Bolt", 1);
        assertExileCount(playerA, "Island", 1);
        assertPermanentCount(playerA, "Hill Giant", 1);
        assertGraveyardCount(playerA, "Lightning Bolt", 1);
    }

    @Test
    public void test_CreatureSpellDoesNotTrigger() {
        addCard(Zone.BATTLEFIELD, playerA, LOKI);
        addCard(Zone.HAND, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertLife(playerB, 20);
    }
}

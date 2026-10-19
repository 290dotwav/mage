package org.mage.test.cards.single.fra;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class UldarosTheorixTest extends CardTestPlayerBase {

    private static final String uldaros = "Uldaros Theorix";

    // one card of each type exiled, copies cast for free up to a total mana value of 6, permanents as tokens
    @Test
    public void test_CastCopiesUpToSix() {
        addCard(Zone.BATTLEFIELD, playerA, "Underground River", 7);
        addCard(Zone.HAND, playerA, uldaros);
        addCard(Zone.GRAVEYARD, playerA, "Lightning Bolt"); // instant, 1
        addCard(Zone.GRAVEYARD, playerA, "Grizzly Bears"); // creature, 2
        addCard(Zone.GRAVEYARD, playerA, "Divination"); // sorcery, 3

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, uldaros);
        addTarget(playerA, "Lightning Bolt^Grizzly Bears^Divination");
        setChoice(playerA, "Lightning Bolt"); // which copy
        setChoice(playerA, true); // cast it
        addTarget(playerA, playerB);
        setChoice(playerA, "Grizzly Bears");
        setChoice(playerA, true);
        setChoice(playerA, true); // the last one

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertExileCount(playerA, "Lightning Bolt", 1);
        assertExileCount(playerA, "Grizzly Bears", 1);
        assertExileCount(playerA, "Divination", 1);
        assertLife(playerB, 17);
        assertTokenCount(playerA, "Grizzly Bears", 1);
        assertHandCount(playerA, 2);
    }

    // over the total of 6, the last copy can't be cast
    @Test
    public void test_TotalManaValueLimit() {
        addCard(Zone.BATTLEFIELD, playerA, "Underground River", 7);
        addCard(Zone.HAND, playerA, uldaros);
        addCard(Zone.GRAVEYARD, playerA, "Hill Giant"); // creature, 4
        addCard(Zone.GRAVEYARD, playerA, "Divination"); // sorcery, 3

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, uldaros);
        addTarget(playerA, "Hill Giant^Divination");
        setChoice(playerA, "Hill Giant");
        setChoice(playerA, true);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertTokenCount(playerA, "Hill Giant", 1);
        assertExileCount(playerA, "Divination", 1);
        assertHandCount(playerA, 0);
    }
}

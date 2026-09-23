package org.mage.test.cards.single.hob;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class RoadsGoEverEverOnTest extends CardTestPlayerBase {

    private static final String roads = "Roads Go Ever, Ever On";

    @Test
    public void test_AllChapters() {
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Plains", 2);
        addCard(Zone.LIBRARY, playerA, "Island", 10);
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, roads);

        // I: exile two Plains, gain 2
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, roads);
        addTarget(playerA, "Plains^Plains");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkExileCount("two plains exiled", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Plains", 2);
        checkLife("gained 2", 1, PhaseStep.PRECOMBAT_MAIN, playerA, 22);

        // II: one to hand (turn 3), III: the other (turn 5)
        setChoice(playerA, "Plains");
        checkHandCardCount("II returned one", 3, PhaseStep.BEGIN_COMBAT, playerA, "Plains", 1);
        checkExileCount("one left", 3, PhaseStep.BEGIN_COMBAT, playerA, "Plains", 1);
        checkHandCardCount("III returned the other", 5, PhaseStep.BEGIN_COMBAT, playerA, "Plains", 2);
        checkExileCount("none left", 5, PhaseStep.BEGIN_COMBAT, playerA, "Plains", 0);

        // IV (turn 7): whenever you attack this turn, +1/+1 per Plains
        attack(7, playerA, "Grizzly Bears");
        addTarget(playerA, "Grizzly Bears");
        checkPT("boosted", 7, PhaseStep.DECLARE_BLOCKERS, playerA, "Grizzly Bears", 2 + 2, 2 + 2);

        setStrictChooseMode(true);
        setStopAt(7, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 4);
        assertGraveyardCount(playerA, roads, 1);
    }
}

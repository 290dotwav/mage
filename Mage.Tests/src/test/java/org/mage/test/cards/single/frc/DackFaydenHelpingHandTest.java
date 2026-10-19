package org.mage.test.cards.single.frc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.permanent.Permanent;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestMultiPlayerBase;

/**
 * @author Claude
 */
public class DackFaydenHelpingHandTest extends CardTestMultiPlayerBase {

    private static final String dack = "Dack Fayden, Helping Hand";

    // two opponents in range: two creature cards, each goaded and given to a different opponent
    @Test
    public void test_EachOpponentGetsOne() {
        skipInitShuffling();
        // Player order: A -> D -> C -> B; range one: A's opponents are D and B
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 6);
        addCard(Zone.HAND, playerA, dack);
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Island", 5);
        addCard(Zone.LIBRARY, playerA, "Grizzly Bears");
        addCard(Zone.LIBRARY, playerA, "Swamp");
        addCard(Zone.LIBRARY, playerA, "Hill Giant");
        addCard(Zone.LIBRARY, playerA, "Plains"); // drawn on turn 1

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, dack);
        setChoice(playerA, "PlayerB"); // for the first one, PlayerD gets the other

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        Permanent bears = getPermanent("Grizzly Bears");
        Permanent giant = getPermanent("Hill Giant");
        Assert.assertEquals(playerB.getId(), giant.getControllerId());
        Assert.assertEquals(playerD.getId(), bears.getControllerId());
        Assert.assertTrue(bears.getGoadingPlayers().contains(playerA.getId()));
        Assert.assertTrue(giant.getGoadingPlayers().contains(playerA.getId()));
        assertLibraryCount(playerA, 6);
        assertHandCount(playerA, "Plains", 1);
    }
}

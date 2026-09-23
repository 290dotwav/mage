package org.mage.test.cards.single.hob;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class InsideInformationTest extends CardTestPlayerBase {

    private static final String info = "Inside Information";

    @Test
    public void test_PlayExiledCardsPayingLife() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 4);
        addCard(Zone.HAND, playerA, info);
        removeAllCardsFromLibrary(playerB);
        addCard(Zone.LIBRARY, playerB, "Island", 3);
        addCard(Zone.LIBRARY, playerB, "Forest");
        addCard(Zone.LIBRARY, playerB, "Grizzly Bears"); // top card, MV 2
        skipInitShuffling();

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, info, playerB);
        setChoice(playerA, "X=2");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkExileCount("exiled", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears", 1);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        playLand(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Forest");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertPermanentCount(playerA, "Forest", 1);
        assertLife(playerA, 20 - 2);
        // the swamps paid only for Inside Information
        assertTappedCount("Swamp", true, 4);
    }

    @Test
    public void test_OnlyThisTurn() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 3);
        addCard(Zone.HAND, playerA, info);
        removeAllCardsFromLibrary(playerB);
        addCard(Zone.LIBRARY, playerB, "Island", 3);
        addCard(Zone.LIBRARY, playerB, "Grizzly Bears");
        skipInitShuffling();

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, info, playerB);
        setChoice(playerA, "X=1");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPlayableAbility("now", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Cast Grizzly Bears", true);
        checkPlayableAbility("later", 3, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Grizzly Bears", false);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertExileCount(playerB, "Grizzly Bears", 1);
    }
}

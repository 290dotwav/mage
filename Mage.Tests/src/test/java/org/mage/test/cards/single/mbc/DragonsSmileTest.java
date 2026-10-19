package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class DragonsSmileTest extends CardTestPlayerBase {

    private static final String smile = "Dragon's Smile";

    // combat damage exiles that many cards; one of them is cast for {0}, and only one each turn
    @Test
    public void test_ExileAndCastForFree() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, smile);
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant");
        removeAllCardsFromLibrary(playerB);
        addCard(Zone.LIBRARY, playerB, "Island", 2);
        addCard(Zone.LIBRARY, playerB, "Lava Axe", 2); // {4}{R}

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Crew 3");
        setChoice(playerA, "Hill Giant");
        attack(1, playerA, smile);
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Lava Axe", playerB);
        setChoice(playerA, "Cast with alternative cost: {0}"); // pay {0} rather than its mana cost
        waitStackResolved(1, PhaseStep.POSTCOMBAT_MAIN);
        checkPlayableAbility("only once each turn", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Cast Lava Axe", false);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 4 - 5);
        assertExileCount(playerB, "Island", 2);
        assertExileCount(playerB, "Lava Axe", 1);
    }
}

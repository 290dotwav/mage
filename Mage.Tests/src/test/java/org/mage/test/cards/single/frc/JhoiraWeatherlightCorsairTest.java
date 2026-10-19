package org.mage.test.cards.single.frc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class JhoiraWeatherlightCorsairTest extends CardTestPlayerBase {

    private static final String jhoira = "Jhoira, Weatherlight Corsair";

    @Test
    public void test_EntersTakesHistoric() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 6);
        addCard(Zone.HAND, playerA, jhoira);
        removeAllCardsFromLibrary(playerB);
        addCard(Zone.LIBRARY, playerB, "Island", 3);
        addCard(Zone.LIBRARY, playerB, "Mind Stone"); // artifact, mana value 2
        addCard(Zone.LIBRARY, playerB, "Grizzly Bears"); // not historic

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, jhoira);
        addTarget(playerA, playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Mind Stone", 1);
        assertLife(playerA, 18);
        assertLibraryCount(playerB, 4);
        assertLibraryCount(playerB, "Grizzly Bears", 1);
    }

    @Test
    public void test_AttacksTakesLegendary() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, jhoira);
        removeAllCardsFromLibrary(playerB);
        addCard(Zone.LIBRARY, playerB, "Isamaru, Hound of Konda"); // legendary, mana value 1
        addCard(Zone.LIBRARY, playerB, "Forest");

        attack(1, playerA, jhoira);
        addTarget(playerA, playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Isamaru, Hound of Konda", 1);
        assertLife(playerA, 19);
        assertLife(playerB, 16);
    }
}

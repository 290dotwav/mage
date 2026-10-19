package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class MatocLavamancerTest extends CardTestPlayerBase {

    // exactly 2 damage: the top card is exiled and can be played
    @Test
    public void test_ExactlyTwo() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, "Matoc, Lavamancer");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);
        addCard(Zone.HAND, playerA, "Shock");
        addCard(Zone.LIBRARY, playerA, "Lightning Bolt");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerB);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 2 - 3);
    }

    // 3 damage: nothing
    @Test
    public void test_NotTwo() {
        addCard(Zone.BATTLEFIELD, playerA, "Matoc, Lavamancer");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);
        addCard(Zone.HAND, playerA, "Lightning Bolt");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertExileCount(playerA, 0);
    }

    // {R}, {T}, exile two cards from the graveyard: 2 damage, which triggers its own exile
    @Test
    public void test_Activated() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, "Matoc, Lavamancer");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);
        addCard(Zone.GRAVEYARD, playerA, "Swamp", 2);
        addCard(Zone.LIBRARY, playerA, "Island");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{R}, {T}, Exile two", playerB);
        setChoice(playerA, "Swamp^Swamp");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 18);
        assertExileCount(playerA, "Swamp", 2);
        assertExileCount(playerA, "Island", 1);
    }
}

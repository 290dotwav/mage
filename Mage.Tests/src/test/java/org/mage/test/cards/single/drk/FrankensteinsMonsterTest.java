package org.mage.test.cards.single.drk;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class FrankensteinsMonsterTest extends CardTestPlayerBase {

    private static final String monster = "Frankenstein's Monster"; // 0/1

    @Test
    public void test_ExileTwo_ChooseCounters() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 4);
        addCard(Zone.HAND, playerA, monster);
        addCard(Zone.GRAVEYARD, playerA, "Grizzly Bears");
        addCard(Zone.GRAVEYARD, playerA, "Hill Giant");
        addCard(Zone.GRAVEYARD, playerA, "Lightning Bolt");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, monster);
        setChoice(playerA, "X=2");
        setChoice(playerA, "Grizzly Bears^Hill Giant");
        setChoice(playerA, "+2/+0");
        setChoice(playerA, "+0/+2");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPowerToughness(playerA, monster, 2, 3);
        assertExileCount(playerA, 2);
        assertGraveyardCount(playerA, "Lightning Bolt", 1);
    }

    @Test
    public void test_CantExileEnough_GoesToGraveyard() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 4);
        addCard(Zone.HAND, playerA, monster);
        addCard(Zone.GRAVEYARD, playerA, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, monster);
        setChoice(playerA, "X=2");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, monster, 0);
        assertGraveyardCount(playerA, monster, 1);
        assertGraveyardCount(playerA, "Grizzly Bears", 1);
    }

    @Test
    public void test_XZero() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 2);
        addCard(Zone.HAND, playerA, monster);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, monster);
        setChoice(playerA, "X=0");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPowerToughness(playerA, monster, 0, 1);
    }
}

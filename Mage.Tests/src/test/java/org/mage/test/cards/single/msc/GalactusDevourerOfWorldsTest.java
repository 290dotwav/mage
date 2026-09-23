package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestCommander4Players;

public class GalactusDevourerOfWorldsTest extends CardTestCommander4Players {

    private static final String galactus = "Galactus, Devourer of Worlds";

    @Test
    public void test_MustAttackOpponentWithMostLife() {
        addCard(Zone.BATTLEFIELD, playerA, galactus);
        setLife(playerB, 30);
        setLife(playerC, 25);

        // Galactus is forced to attack B (the only opponent with the most life): no choice asked
        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertLife(playerB, 30 - 12);
        assertLife(playerC, 25);
        assertLife(playerD, 20);
    }

    @Test
    public void test_TiedMostLife_ChooseAmongThem() {
        addCard(Zone.BATTLEFIELD, playerA, galactus);
        setLife(playerB, 30);
        setLife(playerC, 30);

        addTarget(playerA, playerC); // must attack B or C

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertLife(playerB, 30);
        assertLife(playerC, 30 - 12);
    }

    @Test
    public void test_EntersExile() {
        addCard(Zone.BATTLEFIELD, playerA, "Wastes", 10);
        addCard(Zone.HAND, playerA, galactus);
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, galactus);
        addTarget(playerA, "Hill Giant");

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertExileCount(playerB, "Hill Giant", 1);
    }
}

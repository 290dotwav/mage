package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.player.TestPlayer;
import org.mage.test.serverside.base.CardTestCommander4Players;

public class KingSolomonsFrogsTest extends CardTestCommander4Players {

    private static final String frogs = "King Solomon's Frogs";

    @Test
    public void test_ExileOnePerOpponent_ControllersDraw() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 4);
        addCard(Zone.HAND, playerA, frogs);
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant"); // mv 4
        addCard(Zone.BATTLEFIELD, playerC, "Serra Angel"); // mv 5
        addCard(Zone.BATTLEFIELD, playerD, "Grizzly Bears"); // mv 2: not a legal target

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, frogs);
        // targets are chosen per opponent in turn order D, C, B
        addTarget(playerA, TestPlayer.TARGET_SKIP);
        addTarget(playerA, "Serra Angel");
        addTarget(playerA, "Hill Giant");

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertExileCount(playerB, "Hill Giant", 1);
        assertExileCount(playerC, "Serra Angel", 1);
        assertPermanentCount(playerD, "Grizzly Bears", 1);
        assertHandCount(playerB, 1);
        assertHandCount(playerC, 1);
        assertHandCount(playerD, 0);
    }

    @Test
    public void test_Monarch() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 3);
        addCard(Zone.BATTLEFIELD, playerA, frogs);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{3}, {T}, Exile");

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertExileCount(playerA, frogs, 1);
        Assert.assertTrue("A must be the monarch", playerA.getId().equals(currentGame.getMonarchId()));
    }
}

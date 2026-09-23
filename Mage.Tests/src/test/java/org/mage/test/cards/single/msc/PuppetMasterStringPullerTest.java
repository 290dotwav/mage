package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestCommander4Players;

public class PuppetMasterStringPullerTest extends CardTestCommander4Players {

    private static final String puppet = "Puppet Master, String Puller";

    @Test
    public void test_GoadCantBlock_TreasureWhenGoadedHitsOpponent() {
        addCard(Zone.BATTLEFIELD, playerA, puppet);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");

        attack(1, playerA, puppet, playerB);
        addTarget(playerA, "Grizzly Bears");
        block(1, playerB, "Grizzly Bears", puppet); // can't block: ignored

        // turn order A, D, C, B: on B's turn the goaded bears must attack C or D
        addTarget(playerB, playerC);

        setStopAt(4, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertLife(playerB, 20 - 2);
        assertLife(playerC, 20 - 2);
        assertPermanentCount(playerA, "Treasure Token", 1);
    }
}

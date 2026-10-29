package org.mage.test.cards.single.trc;

import mage.constants.PhaseStep;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class TheBorgQueenTest extends CardTestPlayerBase {

    private static final String queen = "The Borg Queen";

    // enters: the opponent exiles down to a creature card, which comes over; the rest go to the bottom
    @Test
    public void test_EntersAssimilates() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 2);
        addCard(Zone.HAND, playerA, queen);
        addCard(Zone.LIBRARY, playerB, "Hill Giant");
        addCard(Zone.LIBRARY, playerB, "Lightning Bolt"); // on top of the giant
        skipInitShuffling();

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, queen);
        addTarget(playerA, playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Hill Giant", 1);
        assertCounterCount(playerA, "Hill Giant", CounterType.P1P1, 1);
        BorgAssert.isBorg(getPermanent("Hill Giant", playerA), currentGame, SubType.GIANT);
        // 3/3, +1/+1 counter, +2/+2 as an artifact creature
        assertPowerToughness(playerA, "Hill Giant", 6, 6);
        assertPowerToughness(playerA, queen, 6, 6);
        assertExileCount(playerB, 0);
        assertLibraryCount(playerB, "Lightning Bolt", 1);
    }

    // attacks: again
    @Test
    public void test_AttacksAssimilates() {
        addCard(Zone.BATTLEFIELD, playerA, queen);
        addCard(Zone.LIBRARY, playerB, "Grizzly Bears");
        skipInitShuffling();

        attack(1, playerA, queen);
        addTarget(playerA, playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertPowerToughness(playerA, "Grizzly Bears", 5, 5);
        assertLife(playerB, 20 - 6);
    }


    // the card is already a Borg artifact as it enters: Conquest Mission sees a nontoken artifact enter
    @Test
    public void test_EntersAsArtifact() {
        addCard(Zone.BATTLEFIELD, playerA, queen);
        addCard(Zone.BATTLEFIELD, playerA, "Conquest Mission");
        addCard(Zone.LIBRARY, playerB, "Grizzly Bears");
        skipInitShuffling();

        attack(1, playerA, queen);
        addTarget(playerA, playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertCounterCount(playerA, "Conquest Mission", CounterType.QUEST, 1);
        assertPermanentCount(playerA, "Borg Token", 1);
    }
}

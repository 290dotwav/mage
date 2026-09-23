package org.mage.test.cards.single.hob;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class KeyToTheSideDoorTest extends CardTestPlayerBase {

    private static final String key = "Key to the Side-Door";

    @Test
    public void test_DiscardLegendaryCopyToDraw() {
        addCard(Zone.BATTLEFIELD, playerA, key);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Isamaru, Hound of Konda");
        addCard(Zone.HAND, playerA, "Isamaru, Hound of Konda");
        addCard(Zone.HAND, playerA, "Grizzly Bears");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{1}, {T}, Discard");
        setChoice(playerA, "Isamaru, Hound of Konda");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, "Isamaru, Hound of Konda", 1);
        assertHandCount(playerA, 3); // bears + two drawn
    }

    @Test
    public void test_NoMatchingLegendary() {
        addCard(Zone.BATTLEFIELD, playerA, key);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, "Isamaru, Hound of Konda");
        addCard(Zone.HAND, playerA, "Grizzly Bears");

        checkPlayableAbility("no legendary match", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "{1}, {T}, Discard", false);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
    }

    @Test
    public void test_Unblockable() {
        addCard(Zone.BATTLEFIELD, playerA, key);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Wall of Wood");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}, {T}: Target creature", "Grizzly Bears");
        attack(1, playerA, "Grizzly Bears");
        block(1, playerB, "Wall of Wood", "Grizzly Bears"); // not allowed, ignored

        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 2);
    }
}

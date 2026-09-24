package org.mage.test.cards.single.mkc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.player.TestPlayer;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class FeatherRadiantArbiterTest extends CardTestPlayerBase {

    private static final String feather = "Feather, Radiant Arbiter"; // 4/3

    @Test
    public void test_PayForTwoCopies() {
        addCard(Zone.BATTLEFIELD, playerA, feather);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 5);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");
        addCard(Zone.HAND, playerA, "Giant Growth");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Giant Growth", feather);
        setChoice(playerA, "Grizzly Bears^Hill Giant"); // pay {4}
        setChoice(playerA, TestPlayer.CHOICE_SKIP); // skip stack order of the copies

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPowerToughness(playerA, feather, 7, 6);
        assertPowerToughness(playerA, "Grizzly Bears", 5, 5);
        assertPowerToughness(playerB, "Hill Giant", 6, 6);
        assertTappedCount("Forest", true, 5);
    }

    @Test
    public void test_ChooseNone() {
        addCard(Zone.BATTLEFIELD, playerA, feather);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 5);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, "Giant Growth");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Giant Growth", feather);
        setChoice(playerA, TestPlayer.CHOICE_SKIP);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPowerToughness(playerA, feather, 7, 6);
        assertPowerToughness(playerA, "Grizzly Bears", 2, 2);
        assertTappedCount("Forest", true, 1);
    }

    @Test
    public void test_CreatureSpellOrOtherTargetDoesNotTrigger() {
        addCard(Zone.BATTLEFIELD, playerA, feather);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 5);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, "Giant Growth");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Giant Growth", "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPowerToughness(playerA, feather, 4, 3);
        assertPowerToughness(playerA, "Grizzly Bears", 5, 5);
    }
}

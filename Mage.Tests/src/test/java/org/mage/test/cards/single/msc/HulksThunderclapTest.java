package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class HulksThunderclapTest extends CardTestPlayerBase {

    // As an additional cost to cast this spell, you may behold a Gamma creature.
    // Target creature you control deals damage equal to its power to another target creature. If this spell's
    // additional cost was paid, destroy target noncreature artifact or noncreature enchantment.
    private static final String THUNDERCLAP = "Hulk's Thunderclap";
    private static final String GAMMA = "Brawn, Amadeus Cho"; // Gamma creature

    @Test
    public void test_Beheld() {
        addCard(Zone.HAND, playerA, THUNDERCLAP);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant"); // 3 power
        addCard(Zone.HAND, playerA, GAMMA);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Mind Stone");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, THUNDERCLAP);
        setChoice(playerA, true); // behold
        setChoice(playerA, GAMMA); // reveal it from hand
        addTarget(playerA, "Hill Giant");
        addTarget(playerA, "Grizzly Bears");
        addTarget(playerA, "Mind Stone");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerB, "Grizzly Bears", 1);
        assertGraveyardCount(playerB, "Mind Stone", 1);
    }

    @Test
    public void test_NotBeheld() {
        addCard(Zone.HAND, playerA, THUNDERCLAP);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant");
        addCard(Zone.HAND, playerA, GAMMA);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Mind Stone");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, THUNDERCLAP);
        setChoice(playerA, false); // no behold
        addTarget(playerA, "Hill Giant");
        addTarget(playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerB, "Grizzly Bears", 1);
        assertPermanentCount(playerB, "Mind Stone", 1);
    }
}

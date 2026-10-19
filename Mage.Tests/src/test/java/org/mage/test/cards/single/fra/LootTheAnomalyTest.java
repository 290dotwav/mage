package org.mage.test.cards.single.fra;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class LootTheAnomalyTest extends CardTestPlayerBase {

    private static final String loot = "Loot, the Anomaly";

    // -2 power deals 2 combat damage
    @Test
    public void test_NegativePowerDealsDamage() {
        addCard(Zone.BATTLEFIELD, playerA, loot);

        attack(1, playerA, loot);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 18);
    }

    // threshold: sacrifice a creature, -4/4 deals 4
    @Test
    public void test_ThresholdSacrificeMakesMore() {
        addCard(Zone.BATTLEFIELD, playerA, loot);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.GRAVEYARD, playerA, "Swamp", 7);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "<i>Threshold</i> &mdash; Sacrifice another");
        setChoice(playerA, "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPT("-4/4", 1, PhaseStep.PRECOMBAT_MAIN, playerA, loot, -4, 4);
        attack(1, playerA, loot);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, "Grizzly Bears", 1);
        assertLife(playerB, 16);
    }

    // without threshold nothing to activate
    @Test
    public void test_NoThreshold() {
        addCard(Zone.BATTLEFIELD, playerA, loot);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.GRAVEYARD, playerA, "Swamp", 6);

        checkPlayableAbility("no threshold", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "<i>Threshold</i> &mdash; Sacrifice another", false);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
    }

    // a positive power still deals its own power: a pump to +1 power deals 1
    @Test
    public void test_PositivePowerUnchanged() {
        addCard(Zone.BATTLEFIELD, playerA, loot);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);
        addCard(Zone.HAND, playerA, "Brute Force"); // +3/+3

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Brute Force", loot);
        attack(1, playerA, loot);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 19);
    }
}

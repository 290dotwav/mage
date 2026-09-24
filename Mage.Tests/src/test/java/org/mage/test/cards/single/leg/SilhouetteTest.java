package org.mage.test.cards.single.leg;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class SilhouetteTest extends CardTestPlayerBase {

    private static final String silhouette = "Silhouette";

    @Test
    public void test_TargetedDamagePrevented() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, silhouette);
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 1);
        addCard(Zone.HAND, playerB, "Lightning Bolt");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, silhouette, "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerB, "Lightning Bolt", "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertDamageReceived(playerA, "Grizzly Bears", 0);
    }

    @Test
    public void test_UntargetedDamageNotPrevented() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, silhouette);
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 2);
        addCard(Zone.HAND, playerB, "Pyroclasm"); // 2 damage to each creature

        castSpell(2, PhaseStep.UPKEEP, playerA, silhouette, "Grizzly Bears");
        waitStackResolved(2, PhaseStep.UPKEEP);
        castSpell(2, PhaseStep.POSTCOMBAT_MAIN, playerB, "Pyroclasm");

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, "Grizzly Bears", 1);
    }

    @Test
    public void test_FightFromTargetingSpellPrevented() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, silhouette);
        addCard(Zone.BATTLEFIELD, playerB, "Forest", 1);
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");
        addCard(Zone.HAND, playerB, "Prey Upon"); // target creature you control fights target creature you don't control

        castSpell(2, PhaseStep.UPKEEP, playerA, silhouette, "Grizzly Bears");
        waitStackResolved(2, PhaseStep.UPKEEP);
        castSpell(2, PhaseStep.POSTCOMBAT_MAIN, playerB, "Prey Upon");
        addTarget(playerB, "Hill Giant");
        addTarget(playerB, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertDamageReceived(playerB, "Hill Giant", 2);
    }
}

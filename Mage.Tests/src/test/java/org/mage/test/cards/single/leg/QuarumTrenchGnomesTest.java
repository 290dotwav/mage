package org.mage.test.cards.single.leg;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class QuarumTrenchGnomesTest extends CardTestPlayerBase {

    private static final String gnomes = "Quarum Trench Gnomes";

    @Test
    public void test_PlainsMakesColorless() {
        addCard(Zone.BATTLEFIELD, playerA, gnomes);
        addCard(Zone.BATTLEFIELD, playerB, "Plains", 1);
        addCard(Zone.HAND, playerB, "Savannah Lions"); // {W}
        addCard(Zone.HAND, playerB, "Ornithopter");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: If target Plains", "Plains");

        // B's Plains now makes {C}: no Savannah Lions, but a colorless spell is fine
        checkPlayableAbility("no white", 2, PhaseStep.PRECOMBAT_MAIN, playerB, "Cast Savannah Lions", false);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerB, "Savannah Lions", 1);
    }

    @Test
    public void test_ManaProduced() {
        addCard(Zone.BATTLEFIELD, playerA, gnomes);
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 1);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: If target Plains", "Plains");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        activateManaAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: Add {W}");
        checkManaPool("colorless", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "C", 1);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();
    }
}

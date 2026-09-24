package org.mage.test.cards.single.all;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class SuffocationTest extends CardTestPlayerBase {

    private static final String suffocation = "Suffocation";

    @Test
    public void test_AfterRedSpellDamage() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.HAND, playerA, suffocation);
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 1);
        addCard(Zone.HAND, playerB, "Lightning Bolt");

        checkPlayableAbility("not yet", 2, PhaseStep.UPKEEP, playerA, "Cast " + suffocation, false);
        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Lightning Bolt", playerA);
        waitStackResolved(2, PhaseStep.PRECOMBAT_MAIN);
        castSpell(2, PhaseStep.POSTCOMBAT_MAIN, playerA, suffocation);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertLife(playerA, 17);
        assertLife(playerB, 16);
        assertHandCount(playerA, 2); // the delayed draw at turn 3 upkeep and the normal draw
    }

    @Test
    public void test_RedCreatureDamageDoesNotCount() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.HAND, playerA, suffocation);
        addCard(Zone.BATTLEFIELD, playerB, "Raging Goblin");

        attack(2, playerB, "Raging Goblin");
        checkPlayableAbility("no", 2, PhaseStep.POSTCOMBAT_MAIN, playerA, "Cast " + suffocation, false);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 19);
    }
}

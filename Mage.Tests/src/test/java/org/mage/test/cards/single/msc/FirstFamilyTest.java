package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class FirstFamilyTest extends CardTestPlayerBase {

    // You draw X cards and gain X life, where X is the number of colors among permanents you control and spells you've cast this turn.
    private static final String FAMILY = "First Family";

    @Test
    public void test_ColorsFromPermanentsAndSpells() {
        addCard(Zone.HAND, playerA, FAMILY);
        addCard(Zone.HAND, playerA, "Lightning Bolt"); // red spell cast this turn
        addCard(Zone.BATTLEFIELD, playerA, "Suntail Hawk"); // white permanent
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears"); // green, shared with First Family
        addCard(Zone.BATTLEFIELD, playerA, "Volcanic Island", 5);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, FAMILY);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 1);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        // white, green, red, blue (First Family itself) = 4
        assertHandCount(playerA, 4);
        assertLife(playerA, 24);
    }
}

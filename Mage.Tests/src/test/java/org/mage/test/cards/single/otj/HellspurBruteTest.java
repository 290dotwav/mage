package org.mage.test.cards.single.otj;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * Hellspur Brute {4}{R}: "Affinity for outlaws". A changeling has every creature type (702.73a), so it is an
 * outlaw: two Changeling Outcasts take {2} off.
 */
public class HellspurBruteTest extends CardTestPlayerBase {

    @Test
    public void testChangelingsAreOutlaws() {
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Changeling Outcast", 2);
        addCard(Zone.HAND, playerA, "Hellspur Brute");

        checkPlayableAbility("two changelings: {2}{R}", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Hellspur Brute", true);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Hellspur Brute");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertPermanentCount(playerA, "Hellspur Brute", 1);
    }
}

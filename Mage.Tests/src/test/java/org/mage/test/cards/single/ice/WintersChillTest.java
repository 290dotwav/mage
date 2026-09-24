package org.mage.test.cards.single.ice;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class WintersChillTest extends CardTestPlayerBase {

    private static final String chill = "Winter's Chill";

    @Test
    public void test_PayOneOrNothing() {
        addCard(Zone.BATTLEFIELD, playerA, "Snow-Covered Island", 3);
        addCard(Zone.HAND, playerA, chill);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 1);

        attack(2, playerB, "Grizzly Bears");
        attack(2, playerB, "Hill Giant");
        castSpell(2, PhaseStep.DECLARE_ATTACKERS, playerA, chill);
        setChoice(playerA, "X=2");
        addTarget(playerA, "Grizzly Bears^Hill Giant");
        setChoice(playerB, false); // Grizzly Bears: pay {2}? no
        setChoice(playerB, false); // Grizzly Bears: pay {1}? no -> destroyed at end of combat
        setChoice(playerB, false); // Hill Giant: pay {2}? no
        setChoice(playerB, true); // Hill Giant: pay {1} -> no combat damage

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertLife(playerA, 20 - 2);
        assertGraveyardCount(playerB, "Grizzly Bears", 1);
        assertPermanentCount(playerB, "Hill Giant", 1);
        assertTapped("Mountain", true);
    }

    @Test
    public void test_OnlyBeforeBlockers() {
        addCard(Zone.BATTLEFIELD, playerA, "Snow-Covered Island", 3);
        addCard(Zone.HAND, playerA, chill);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        attack(2, playerB, "Grizzly Bears");
        checkPlayableAbility("main", 2, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast " + chill, false);
        checkPlayableAbility("attackers", 2, PhaseStep.DECLARE_ATTACKERS, playerA, "Cast " + chill, true);
        checkPlayableAbility("blockers", 2, PhaseStep.DECLARE_BLOCKERS, playerA, "Cast " + chill, false);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_COMBAT);
        execute();
    }
}

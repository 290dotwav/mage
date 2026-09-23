package org.mage.test.cards.single.msh;

import mage.abilities.keyword.IndestructibleAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class SpiderManToTheRescueTest extends CardTestPlayerBase {

    // Flash; Reach, vigilance
    // No One Dies! -- When Spider-Man enters, you may tap him. When you do, another target nonattacking
    // creature you control gains indestructible until end of turn.
    private static final String SPIDEY = "Spider-Man, To the Rescue";

    @Test
    public void test_TapGivesIndestructible() {
        addCard(Zone.HAND, playerA, SPIDEY);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 3);
        addCard(Zone.HAND, playerB, "Murder");
        addCard(Zone.BATTLEFIELD, playerB, "Swamp", 3);

        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerA, SPIDEY);
        setChoice(playerA, true); // tap him
        addTarget(playerA, "Grizzly Bears");
        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Murder", "Grizzly Bears", SPIDEY, StackClause.WHILE_NOT_ON_STACK);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertTapped(SPIDEY, true);
        assertAbility(playerA, "Grizzly Bears", IndestructibleAbility.getInstance(), true);
        assertPermanentCount(playerA, "Grizzly Bears", 1);
    }
}

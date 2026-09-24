package org.mage.test.cards.single.leg;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class NorthStarTest extends CardTestPlayerBase {

    private static final String star = "North Star";

    @Test
    public void test_OneSpellOnly() {
        addCard(Zone.BATTLEFIELD, playerA, star);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 7);
        addCard(Zone.HAND, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, "Llanowar Elves");

        checkPlayableAbility("no green", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Grizzly Bears", false);
        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{4}, {T}: For one spell");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPlayableAbility("now", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Grizzly Bears", true);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        // used for the Bears: not for another spell
        checkPlayableAbility("used", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Cast Llanowar Elves", false);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Grizzly Bears", 1);
    }

    @Test
    public void test_NotUsedByASpellThatDidntNeedIt() {
        addCard(Zone.BATTLEFIELD, playerA, star);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 7);
        addCard(Zone.HAND, playerA, "Think Twice"); // {1}{U}
        addCard(Zone.HAND, playerA, "Llanowar Elves");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{4}, {T}: For one spell");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Think Twice");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Llanowar Elves");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Llanowar Elves", 1);
    }
}

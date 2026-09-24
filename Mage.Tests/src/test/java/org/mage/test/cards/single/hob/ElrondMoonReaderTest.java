package org.mage.test.cards.single.hob;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class ElrondMoonReaderTest extends CardTestPlayerBase {

    private static final String elrond = "Elrond, Moon-Reader";

    @Test
    public void test_ManaAbilityOfCreatureTriggersOncePerTurn() {
        addCard(Zone.BATTLEFIELD, playerA, elrond);
        addCard(Zone.BATTLEFIELD, playerA, "Llanowar Elves");
        addCard(Zone.BATTLEFIELD, playerA, "Prodigal Sorcerer");
        addCard(Zone.BATTLEFIELD, playerA, "Forest"); // land ability: no trigger

        activateManaAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: Add {G}", 1);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkHandCount("drew from mana ability", 1, PhaseStep.PRECOMBAT_MAIN, playerA, 1);

        // second creature ability this turn: no more draw
        activateAbility(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "{T}: {this} deals 1 damage", playerB);
        waitStackResolved(1, PhaseStep.POSTCOMBAT_MAIN);
        checkHandCount("only once", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, 1);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 19);
    }

    @Test
    public void test_NonManaAbility_AndNotLand_NotOpponent() {
        addCard(Zone.BATTLEFIELD, playerA, elrond);
        addCard(Zone.BATTLEFIELD, playerA, "Forest");
        addCard(Zone.BATTLEFIELD, playerB, "Prodigal Sorcerer");
        addCard(Zone.BATTLEFIELD, playerA, "Prodigal Sorcerer");

        activateManaAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: Add {G}");
        // opponent activates a creature's ability: no trigger for A
        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerB, "{T}: {this} deals 1 damage", playerA);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkHandCount("land + opponent: nothing", 1, PhaseStep.PRECOMBAT_MAIN, playerA, 0);

        activateAbility(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "{T}: {this} deals 1 damage", playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerA, 1);
        assertLife(playerB, 19);
        assertLife(playerA, 19);
    }

    @Test
    public void test_Flicker() {
        addCard(Zone.BATTLEFIELD, playerA, elrond);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 7);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Elvish Visionary"); // ETB: draw a card

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{5}{U}{U}: Exile");
        addTarget(playerA, "Grizzly Bears^Elvish Visionary");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkExileCount("bears gone", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears", 1);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.UPKEEP);
        execute();

        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertPermanentCount(playerA, "Elvish Visionary", 1);
        // Elrond drew one (activated ability of a creature), the Visionary one more on its return
        assertHandCount(playerA, 2);
    }

    @Test
    public void test_ManaAbilityUsedToPayCosts() {
        addCard(Zone.BATTLEFIELD, playerA, elrond);
        addCard(Zone.BATTLEFIELD, playerA, "Llanowar Elves");
        addCard(Zone.HAND, playerA, "Giant Growth");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Giant Growth", elrond);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertTapped("Llanowar Elves", true);
        assertPowerToughness(playerA, elrond, 6, 6);
        assertHandCount(playerA, 1);
    }
}

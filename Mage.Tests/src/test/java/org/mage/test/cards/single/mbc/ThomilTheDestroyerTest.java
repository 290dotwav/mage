package org.mage.test.cards.single.mbc;

import mage.abilities.keyword.FlyingAbility;
import mage.abilities.keyword.TrampleAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class ThomilTheDestroyerTest extends CardTestPlayerBase {

    private static final String thomil = "Thomil, the Destroyer";

    // 0: sacrifice a creature for {B}{B}{B}, spent on a Chupacabra
    @Test
    public void test_SacrificeForMana() {
        addCard(Zone.BATTLEFIELD, playerA, thomil);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 1);
        addCard(Zone.HAND, playerA, "Ravenous Chupacabra"); // {2}{B}{B}
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "0: You may sacrifice");
        setChoice(playerA, true);
        setChoice(playerA, "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Ravenous Chupacabra");
        addTarget(playerA, "Hill Giant");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, "Grizzly Bears", 1);
        assertPermanentCount(playerA, "Ravenous Chupacabra", 1);
        assertGraveyardCount(playerB, "Hill Giant", 1);
        assertCounterCount(playerA, thomil, CounterType.LOYALTY, 4);
    }

    // +2 makes a Zombie, -5 a Lord of the Pit that asks for another creature at upkeep
    @Test
    public void test_ZombieThenLordOfThePit() {
        addCard(Zone.BATTLEFIELD, playerA, thomil);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "+2: Create");
        activateAbility(3, PhaseStep.PRECOMBAT_MAIN, playerA, "-5: Create");
        waitStackResolved(3, PhaseStep.PRECOMBAT_MAIN);
        checkPT("7/7", 3, PhaseStep.PRECOMBAT_MAIN, playerA, "Lord of the Pit", 7, 7);
        checkAbility("flying", 3, PhaseStep.PRECOMBAT_MAIN, playerA, "Lord of the Pit", FlyingAbility.class, true);
        checkAbility("trample", 3, PhaseStep.PRECOMBAT_MAIN, playerA, "Lord of the Pit", TrampleAbility.class, true);
        setChoice(playerA, "Zombie Token"); // turn 5 upkeep

        setStrictChooseMode(true);
        setStopAt(5, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertPermanentCount(playerA, "Zombie Token", 0);
        assertPermanentCount(playerA, "Lord of the Pit", 1);
        assertCounterCount(playerA, thomil, CounterType.LOYALTY, 1);
        assertLife(playerA, 20);
    }
}

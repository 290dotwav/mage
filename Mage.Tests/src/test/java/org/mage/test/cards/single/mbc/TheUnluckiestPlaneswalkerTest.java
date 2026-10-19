package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.game.permanent.Permanent;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class TheUnluckiestPlaneswalkerTest extends CardTestPlayerBase {

    private static final String unlucky = "The Unluckiest Planeswalker";

    // +1: the mana spent on an Aura makes a Gremlin
    @Test
    public void test_ManaForAnAura() {
        addCard(Zone.BATTLEFIELD, playerA, unlucky);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, "Rancor"); // {G}

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "+1: Add one mana");
        setChoice(playerA, "Green");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Rancor", "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Rancor", 1);
        assertPermanentCount(playerA, "Gremlin Token", 1);
    }

    // -3: discard, draw twice the Auras you control, and the curse on you moves to an opponent
    @Test
    public void test_MinusThree() {
        addCard(Zone.BATTLEFIELD, playerA, unlucky);
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 2);
        addCard(Zone.HAND, playerA, "Pacifism");
        addCard(Zone.HAND, playerA, "Island");
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");
        addCard(Zone.BATTLEFIELD, playerB, "Island", 3);
        addCard(Zone.HAND, playerB, "Curse of the Bloody Tome"); // enchanted player mills two at upkeep

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Pacifism", "Hill Giant");
        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Curse of the Bloody Tome", playerA);
        activateAbility(3, PhaseStep.PRECOMBAT_MAIN, playerA, "-3: Discard your hand");

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, "Island", 1);
        assertHandCount(playerA, 2); // the turn 3 draw went with the hand
        Permanent curse = getPermanent("Curse of the Bloody Tome");
        Assert.assertEquals(playerB.getId(), curse.getAttachedTo());
        assertCounterCount(playerA, unlucky, CounterType.LOYALTY, 1);
    }
}

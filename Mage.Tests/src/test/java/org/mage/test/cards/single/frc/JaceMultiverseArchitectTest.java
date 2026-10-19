package org.mage.test.cards.single.frc;

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
public class JaceMultiverseArchitectTest extends CardTestPlayerBase {

    private static final String jace = "Jace, Multiverse Architect";

    // +1: draw two, then put a card from hand on the bottom
    @Test
    public void test_PlusOne() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, jace);
        addCard(Zone.LIBRARY, playerA, "Grizzly Bears");
        addCard(Zone.LIBRARY, playerA, "Hill Giant");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "+1: Draw two cards");
        setChoice(playerA, "Hill Giant"); // to the bottom

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertCounterCount(playerA, jace, CounterType.LOYALTY, 5);
        assertHandCount(playerA, 1);
        assertHandCount(playerA, "Grizzly Bears", 1);
    }

    // -3: exile another creature you control, reveal until a creature or planeswalker card, put it onto the battlefield
    @Test
    public void test_MinusThree() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, jace);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Hill Giant");
        addCard(Zone.LIBRARY, playerA, "Island", 2);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "-3: Exile another target");
        addTarget(playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertExileCount(playerA, "Grizzly Bears", 1);
        assertPermanentCount(playerA, "Hill Giant", 1);
        assertLibraryCount(playerA, "Island", 2);
        assertCounterCount(playerA, jace, CounterType.LOYALTY, 1);
    }

    // an opponent who doesn't pay {2} can't attack Jace, but can still attack the player
    @Test
    public void test_OpponentDoesNotPay() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, jace);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 2);

        setChoice(playerB, false); // don't pay {2}
        attack(2, playerB, "Grizzly Bears", playerA);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 18);
        assertCounterCount(playerA, jace, CounterType.LOYALTY, 4);
    }

    // an opponent who doesn't pay {2}: an attack on Jace is refused
    @Test
    public void test_OpponentDoesNotPayCantAttackJace() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, jace);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        setChoice(playerB, false); // don't pay {2}
        runCode("can't attack Jace, can attack the player", 2, PhaseStep.POSTCOMBAT_MAIN, playerB, (info, player, game) -> {
            Permanent bears = game.getBattlefield().getAllActivePermanents().stream()
                    .filter(p -> p.getName().equals("Grizzly Bears")).findFirst().orElse(null);
            Permanent jacePermanent = game.getBattlefield().getAllActivePermanents().stream()
                    .filter(p -> p.getName().equals(jace)).findFirst().orElse(null);
            Assert.assertNotNull(bears);
            Assert.assertNotNull(jacePermanent);
            Assert.assertFalse(bears.canAttack(jacePermanent.getId(), game));
            Assert.assertTrue(bears.canAttack(playerA.getId(), game));
        });

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertCounterCount(playerA, jace, CounterType.LOYALTY, 4);
    }

    // an opponent who pays {2} can attack Jace
    @Test
    public void test_OpponentPays() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, jace);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 2);

        setChoice(playerB, true); // pay {2}
        attack(2, playerB, "Grizzly Bears", jace);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertTappedCount("Mountain", true, 2);
        assertCounterCount(playerA, jace, CounterType.LOYALTY, 2);
        assertLife(playerA, 20);
    }
}

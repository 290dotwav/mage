package org.mage.test.game;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.stack.PaidCost;
import mage.game.stack.StackObject;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

import static org.mage.test.game.PaidCostTest.ability;
import static org.mage.test.game.PaidCostTest.spell;

/**
 * The life paid for a spell or an ability on the stack ({@link PaidCost#getLifePaid()}), which
 * the web door writes beside the mana spent: Phyrexian mana paid with life, a "pay N life"
 * additional or alternative cost, a {B} K'rrik let be paid with life.
 *
 * @author ClaudeMTG
 */
public class PaidLifeTest extends CardTestPlayerBase {

    private static void assertPaidLife(StackObject object, String spent, int life) {
        PaidCost paid = PaidCost.of(object);
        Assert.assertNotNull("paid for " + object.getName(), paid);
        Assert.assertEquals("spent on " + object.getName(), spent, paid.getSpent());
        Assert.assertEquals("life paid for " + object.getName(), life, paid.getLifePaid());
    }

    @Test
    public void test_NoLife() {
        addCard(Zone.HAND, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");
        runCode("on the stack", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            assertPaidLife(spell(game, "Grizzly Bears"), "{G}{G}", 0);
        });

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
        assertLife(playerA, 20);
    }

    @Test
    public void test_PhyrexianMana_BothPaidWithLife() {
        // Dismember {1}{B/P}{B/P}: Target creature gets -5/-5 until end of turn.
        addCard(Zone.HAND, playerA, "Dismember");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Dismember", "Grizzly Bears");
        setChoice(playerA, true); // 2 life for the first {B/P}
        setChoice(playerA, true); // 2 life for the second
        runCode("on the stack", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            assertPaidLife(spell(game, "Dismember"), "{R}", 4);
        });

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
        assertLife(playerA, 16);
        assertGraveyardCount(playerB, "Grizzly Bears", 1);
    }

    @Test
    public void test_PhyrexianMana_OnePaidWithLife() {
        addCard(Zone.HAND, playerA, "Dismember");
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 2);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Dismember", "Grizzly Bears");
        setChoice(playerA, true); // 2 life for the first {B/P}
        setChoice(playerA, false); // {B} for the second
        runCode("on the stack", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            assertPaidLife(spell(game, "Dismember"), "{B}{B}", 2);
        });

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
        assertLife(playerA, 18);
    }

    @Test
    public void test_ToxicDeluge_X3() {
        // Toxic Deluge {2}{B}: As an additional cost to cast this spell, pay X life.
        addCard(Zone.HAND, playerA, "Toxic Deluge");
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 3);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Toxic Deluge");
        setChoice(playerA, "X=3");
        runCode("on the stack", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            assertPaidLife(spell(game, "Toxic Deluge"), "{B}{B}{B}", 3);
        });

        setStopAt(1, PhaseStep.END_TURN);
        execute();
        assertLife(playerA, 17);
        assertGraveyardCount(playerB, "Grizzly Bears", 1);
    }

    @Test
    public void test_SnuffOut_AlternativeCost() {
        // Snuff Out {3}{B}: If you control a Swamp, you may pay 4 life rather than pay this spell's mana cost.
        addCard(Zone.HAND, playerA, "Snuff Out");
        addCard(Zone.BATTLEFIELD, playerA, "Swamp");
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Snuff Out", "Grizzly Bears");
        setChoice(playerA, "Cast with alternative cost: Pay 4 life (source: Snuff Out");
        runCode("on the stack", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            assertPaidLife(spell(game, "Snuff Out"), "", 4);
        });

        setStopAt(1, PhaseStep.END_TURN);
        execute();
        assertLife(playerA, 16);
        assertGraveyardCount(playerB, "Grizzly Bears", 1);
        assertTapped("Swamp", false);
    }

    @Test
    public void test_Krrik_SpellPaidWithLife() {
        // K'rrik: For each {B} in a cost, you may pay 2 life rather than pay that mana.
        addCard(Zone.BATTLEFIELD, playerA, "K'rrik, Son of Yawgmoth");
        addCard(Zone.HAND, playerA, "Banehound"); // {B}

        setChoice(playerA, true); // 2 life for {B}
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Banehound");
        runCode("on the stack", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            assertPaidLife(spell(game, "Banehound"), "", 2);
        });

        setStopAt(1, PhaseStep.END_TURN);
        execute();
        assertLife(playerA, 18);
        assertPermanentCount(playerA, "Banehound", 1);
    }

    @Test
    public void test_Krrik_ActivatedAbilityPaidWithLife() {
        // Frozen Shade {B}: Frozen Shade gets +1/+1 until end of turn.
        addCard(Zone.BATTLEFIELD, playerA, "K'rrik, Son of Yawgmoth");
        addCard(Zone.BATTLEFIELD, playerA, "Frozen Shade");

        setChoice(playerA, true); // 2 life for {B}
        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{B}: ");
        runCode("on the stack", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            assertPaidLife(ability(game, "Frozen Shade"), "", 2);
        });

        setStopAt(1, PhaseStep.END_TURN);
        execute();
        assertLife(playerA, 18);
    }

    @Test
    public void test_AbilityWithOnlyALifeCost() {
        // Necropotence: Pay 1 life: Exile the top card of your library face down. ...
        addCard(Zone.BATTLEFIELD, playerA, "Necropotence");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Pay 1 life: ");
        runCode("on the stack", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            assertPaidLife(ability(game, "Necropotence"), "", 1);
        });

        setStopAt(1, PhaseStep.END_TURN);
        execute();
        assertLife(playerA, 19);
    }
}

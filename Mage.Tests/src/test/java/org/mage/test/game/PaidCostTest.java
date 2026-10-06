package org.mage.test.game;

import mage.MageObject;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.stack.PaidCost;
import mage.game.stack.Spell;
import mage.game.stack.StackAbility;
import mage.game.stack.StackObject;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * What a spell or an ability on the stack was paid with ({@link PaidCost}), which the web door
 * writes on each stack entry so the table can show it: the cost as paid, X, the mana spent.
 *
 * @author ClaudeMTG
 */
public class PaidCostTest extends CardTestPlayerBase {

    public static StackObject spell(Game game, String name) {
        for (StackObject object : game.getStack()) {
            if (object instanceof Spell && object.getName().equals(name)) {
                return object;
            }
        }
        Assert.fail(name + " is not on the stack");
        return null;
    }

    public static StackObject ability(Game game, String sourceName) {
        for (StackObject object : game.getStack()) {
            MageObject source = game.getObject(object.getSourceId());
            if (object instanceof StackAbility && source != null && source.getName().equals(sourceName)) {
                return object;
            }
        }
        Assert.fail("no ability of " + sourceName + " on the stack");
        return null;
    }

    public static void assertPaid(StackObject object, String cost, Integer x, String spent) {
        PaidCost paid = PaidCost.of(object);
        Assert.assertNotNull("paid for " + object.getName(), paid);
        Assert.assertEquals("cost of " + object.getName(), cost, paid.getCost());
        Assert.assertEquals("X of " + object.getName(), x, paid.getX());
        Assert.assertEquals("spent on " + object.getName(), spent, paid.getSpent());
    }

    @Test
    public void test_NormalSpell_GenericPaidWithAnotherColour() {
        // Grizzly Bears {1}{G}
        addCard(Zone.HAND, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Forest");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");
        runCode("on the stack", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            assertPaid(spell(game, "Grizzly Bears"), "{1}{G}", null, "{R}{G}");
        });

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
        assertPermanentCount(playerA, "Grizzly Bears", 1);
    }

    @Test
    public void test_XSpell() {
        // Blaze {X}{R}: Blaze deals X damage to any target.
        addCard(Zone.HAND, playerA, "Blaze");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Blaze", playerB);
        setChoice(playerA, "X=3");
        runCode("on the stack", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            assertPaid(spell(game, "Blaze"), "{3}{R}", 3, "{U}{U}{R}{R}");
        });

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
        assertLife(playerB, 20 - 3);
    }

    @Test
    public void test_XZero() {
        addCard(Zone.HAND, playerA, "Blaze");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Blaze", playerB);
        setChoice(playerA, "X=0");
        runCode("on the stack", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            assertPaid(spell(game, "Blaze"), "{R}", 0, "{R}");
        });

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
    }

    @Test
    public void test_CostReduction() {
        // Blinkmoth Infusion {12}{U}{U}, affinity for artifacts: 10 Ornithopters leave {2}{U}{U}
        addCard(Zone.HAND, playerA, "Blinkmoth Infusion");
        addCard(Zone.BATTLEFIELD, playerA, "Ornithopter", 10);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 4);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Blinkmoth Infusion");
        runCode("on the stack", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            assertPaid(spell(game, "Blinkmoth Infusion"), "{2}{U}{U}", null, "{U}{U}{U}{U}");
        });

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
    }

    @Test
    public void test_ZeroCostSpell() {
        // Ornithopter {0}: paid, for nothing
        addCard(Zone.HAND, playerA, "Ornithopter");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Ornithopter");
        runCode("on the stack", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            assertPaid(spell(game, "Ornithopter"), "{0}", null, "");
        });

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
        assertPermanentCount(playerA, "Ornithopter", 1);
    }

    @Test
    public void test_FreeCast_DauthiVoidwalker() {
        // {T}, Sacrifice Dauthi Voidwalker: ... You may play it this turn without paying its mana cost.
        addCard(Zone.BATTLEFIELD, playerA, "Dauthi Voidwalker", 1);
        addCard(Zone.BATTLEFIELD, playerB, "Balduvian Bears", 1); // {1}{G}
        addCard(Zone.HAND, playerA, "Lightning Bolt");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", "Balduvian Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}, Sacrifice");
        setChoice(playerA, "Balduvian Bears");
        runCode("tap and sacrifice is no mana", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            Assert.assertNull("an ability whose cost holds no mana", PaidCost.of(ability(game, "Dauthi Voidwalker")));
        });
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Balduvian Bears");
        runCode("on the stack", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            assertPaid(spell(game, "Balduvian Bears"), "", null, "");
        });

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
        assertPermanentCount(playerA, "Balduvian Bears", 1);
    }

    @Test
    public void test_ActivatedAbilityWithManaCost() {
        // Mind Stone: {1}, {T}, Sacrifice Mind Stone: Draw a card.
        addCard(Zone.BATTLEFIELD, playerA, "Mind Stone");
        addCard(Zone.BATTLEFIELD, playerA, "Forest");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{1}, {T}, Sacrifice");
        runCode("on the stack", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            assertPaid(ability(game, "Mind Stone"), "{1}", null, "{G}");
        });

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
        assertGraveyardCount(playerA, "Mind Stone", 1);
    }

    @Test
    public void test_TriggeredAbilityHasNone() {
        // Young Pyromancer: Whenever you cast an instant or sorcery spell, create a 1/1 red Elemental creature token.
        addCard(Zone.BATTLEFIELD, playerA, "Young Pyromancer");
        addCard(Zone.HAND, playerA, "Lightning Bolt");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);
        runCode("on the stack", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            Assert.assertNull("a trigger is not paid for", PaidCost.of(ability(game, "Young Pyromancer")));
            assertPaid(spell(game, "Lightning Bolt"), "{R}", null, "{R}");
        });

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
        assertLife(playerB, 20 - 3);
    }
}

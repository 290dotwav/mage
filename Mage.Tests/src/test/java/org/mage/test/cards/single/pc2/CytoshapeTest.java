package org.mage.test.cards.single.pc2;

import mage.constants.EmptyNames;
import mage.constants.PhaseStep;
import mage.game.permanent.Permanent;
import mage.view.GameView;
import mage.view.PermanentView;
import mage.view.PlayerView;
import org.junit.Assert;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * Cytoshape: "Choose a nonlegendary creature on the battlefield. Target creature becomes a copy of that
 * creature until end of turn." Its target is a creature: a land was offered (any permanent).
 */
public class CytoshapeTest extends CardTestPlayerBase {

    private static final String cytoshape = "Cytoshape";

    @Test
    public void testNoCreatureToTargetNotCastable() {
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.HAND, playerA, cytoshape);

        checkPlayableAbility("only lands: no target creature", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast " + cytoshape, false);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();
    }

    @Test
    public void testCreatureBecomesCopy() {
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.HAND, playerA, cytoshape);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Craw Wurm");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, cytoshape, "Grizzly Bears");
        setChoice(playerA, "Craw Wurm");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertPermanentCount(playerA, "Craw Wurm", 1);
        assertPowerToughness(playerA, "Craw Wurm", 6, 4);
    }

    /**
     * 707.2, 708.2a: a copy of a face-down creature copies its face-down values, a 2/2 with no name, no
     * colour, no subtype and no text. The copy carried the morph's "{this} becomes a 2/2 face-down
     * creature..." rule, kept on the original for turning it face up.
     */
    @Test
    public void testCopyOfFaceDownIsNamelessTextless() {
        addCard(Zone.BATTLEFIELD, playerB, "Forest", 3);
        addCard(Zone.HAND, playerB, "Ainok Survivalist");
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.HAND, playerA, cytoshape);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Ainok Survivalist using Morph");
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, cytoshape, "Grizzly Bears");
        setChoice(playerA, EmptyNames.FACE_DOWN_CREATURE.getTestCommand());

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        Permanent copy = currentGame.getBattlefield().getAllActivePermanents(playerA.getId()).stream()
                .filter(p -> p.isCreature(currentGame)).findFirst().orElse(null);
        Assert.assertNotNull(copy);
        Assert.assertTrue("a copy", copy.isCopy());
        Assert.assertFalse("face up", copy.isFaceDown(currentGame));
        Assert.assertEquals("no name", "", copy.getName());
        Assert.assertTrue("colourless", copy.getColor(currentGame).isColorless());
        Assert.assertTrue("no subtype", copy.getSubtype(currentGame).isEmpty());
        Assert.assertEquals(2, copy.getPower().getValue());
        Assert.assertEquals(2, copy.getToughness().getValue());
        Assert.assertTrue("no text: " + copy.getRules(currentGame), copy.getRules(currentGame).isEmpty());

        // The original is still face down and can still be turned face up.
        Permanent down = currentGame.getBattlefield().getAllActivePermanents(playerB.getId()).stream()
                .filter(p -> p.isFaceDown(currentGame)).findFirst().orElse(null);
        Assert.assertNotNull(down);
        Assert.assertFalse("its morph kept", down.getAbilities(currentGame).isEmpty());

        // What its controller's screen is sent: no name, no text.
        GameView gv = new GameView(currentGame.getState(), currentGame, playerA.getId(), null);
        PermanentView seen = null;
        for (PlayerView pv : gv.getPlayers()) {
            if (pv.getBattlefield().containsKey(copy.getId())) {
                seen = pv.getBattlefield().get(copy.getId());
            }
        }
        Assert.assertNotNull(seen);
        Assert.assertEquals("", seen.getName());
        Assert.assertTrue("no rules: " + seen.getRules(), seen.getRules().isEmpty());
    }
}

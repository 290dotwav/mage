package org.mage.test.cards.single.mbc;

import mage.constants.EmptyNames;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class SashAndWaistcoatUnmenTest extends CardTestPlayerBase {

    private static final String sash = "Sash and Waistcoat, Unmen";

    // any player may manifest with it, from their own library, as a sorcery
    @Test
    public void test_OpponentManifests() {
        addCard(Zone.BATTLEFIELD, playerA, sash);
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 3);

        activateAbility(2, PhaseStep.PRECOMBAT_MAIN, playerB, "{3}: Manifest");

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerB, EmptyNames.FACE_DOWN_CREATURE.getTestCommand(), 1);
        assertPermanentCount(playerA, EmptyNames.FACE_DOWN_CREATURE.getTestCommand(), 0);
    }

    // a face-down creature attacking an opponent can't be blocked
    @Test
    public void test_FaceDownAttackerUnblockable() {
        addCard(Zone.BATTLEFIELD, playerA, sash);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{3}: Manifest");
        attack(3, playerA, EmptyNames.FACE_DOWN_CREATURE.getTestCommand());
        runCode("can't be blocked", 3, PhaseStep.DECLARE_BLOCKERS, playerB, (info, player, game) -> {
            mage.game.permanent.Permanent faceDown = game.getBattlefield().getAllActivePermanents(playerA.getId())
                    .stream().filter(p -> p.isFaceDown(game)).findFirst().orElse(null);
            mage.game.permanent.Permanent bears = game.getBattlefield().getAllActivePermanents(playerB.getId())
                    .stream().filter(p -> p.getName().equals("Grizzly Bears")).findFirst().orElse(null);
            org.junit.Assert.assertNotNull(faceDown);
            org.junit.Assert.assertFalse("blocked by nobody", bears.canBlock(faceDown.getId(), game));
        });

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 18);
    }
}

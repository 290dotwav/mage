package org.mage.test.cards.single.drk;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class ReflectingMirrorTest extends CardTestPlayerBase {

    private static final String mirror = "Reflecting Mirror";

    @Test
    public void test_RedirectShockToItsCaster() {
        addCard(Zone.BATTLEFIELD, playerA, mirror);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 1);
        addCard(Zone.HAND, playerB, "Shock");

        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Shock", playerA);
        activateAbility(2, PhaseStep.PRECOMBAT_MAIN, playerA, "{X}, {T}: Change", "Shock", "Shock");
        setChoice(playerA, "X=2");
        addTarget(playerA, "Shock"); // (the only other player, PlayerB, becomes the new target)

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20);
        assertLife(playerB, 18);
        assertTappedCount("Island", true, 2);
    }

    @Test
    public void test_OnlySpellsTargetingYou() {
        addCard(Zone.BATTLEFIELD, playerA, mirror);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 1);
        addCard(Zone.HAND, playerB, "Shock");

        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Shock", "Grizzly Bears");
        checkPlayableAbility("can't", 2, PhaseStep.PRECOMBAT_MAIN, playerA, "{X}, {T}: Change", false);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, "Grizzly Bears", 1);
    }
}

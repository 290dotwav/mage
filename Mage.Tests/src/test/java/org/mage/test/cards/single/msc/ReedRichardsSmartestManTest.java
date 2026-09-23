package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class ReedRichardsSmartestManTest extends CardTestPlayerBase {

    private static final String reed = "Reed Richards, Smartest Man";

    @Test
    public void test_DrawStepFirstCardNotReplaced_NextDrawIsFour_ThenNormal() {
        addCard(Zone.BATTLEFIELD, playerA, reed);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.HAND, playerA, "Opt", 2);

        // turn 3: draw step card (normal), then Opt's draw -> four cards, second Opt's draw -> one card
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Opt");
        addTarget(playerA, org.mage.test.player.TestPlayer.TARGET_SKIP); // scry: keep
        waitStackResolved(3, PhaseStep.PRECOMBAT_MAIN);
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Opt");
        addTarget(playerA, org.mage.test.player.TestPlayer.TARGET_SKIP);

        setStopAt(3, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        // 2 Opts - 2 cast + 1 (draw step) + 4 (first Opt) + 1 (second Opt)
        assertHandCount(playerA, 6);
    }

    @Test
    public void test_NoMaximumHandSize() {
        addCard(Zone.BATTLEFIELD, playerA, reed);
        addCard(Zone.HAND, playerA, "Island", 10);

        setStopAt(2, PhaseStep.UPKEEP);
        setStrictChooseMode(true);
        execute();

        assertHandCount(playerA, 10);
    }
}

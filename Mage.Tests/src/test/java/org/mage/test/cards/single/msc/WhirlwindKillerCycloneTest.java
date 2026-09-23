package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class WhirlwindKillerCycloneTest extends CardTestPlayerBase {

    // Haste
    // Whenever one or more creatures you control that entered this turn attack a player, target creature that player controls can't block this turn.
    private static final String WHIRLWIND = "Whirlwind, Killer Cyclone";

    @Test
    public void test_EnteredThisTurnAttacks() {
        addCard(Zone.HAND, playerA, WHIRLWIND);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 3);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, WHIRLWIND);
        attack(1, playerA, WHIRLWIND, playerB);
        addTarget(playerA, "Grizzly Bears");
        // bears can't block: the block must fail
        block(1, playerB, "Grizzly Bears", WHIRLWIND);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 2);
    }

    @Test
    public void test_OldCreatureDoesNotTrigger() {
        addCard(Zone.BATTLEFIELD, playerA, WHIRLWIND);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        attack(3, playerA, WHIRLWIND, playerB);
        block(3, playerB, "Grizzly Bears", WHIRLWIND);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20);
        assertDamageReceived(playerA, WHIRLWIND, 2);
    }
}

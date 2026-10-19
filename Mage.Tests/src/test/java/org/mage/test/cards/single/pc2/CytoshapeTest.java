package org.mage.test.cards.single.pc2;

import mage.constants.PhaseStep;
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
}

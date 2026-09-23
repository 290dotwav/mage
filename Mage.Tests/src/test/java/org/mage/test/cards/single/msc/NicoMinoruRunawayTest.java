package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class NicoMinoruRunawayTest extends CardTestPlayerBase {

    private static final String nico = "Nico Minoru, Runaway";

    @Test
    public void test_ActivateCastFreeFromExile_DealsDamage() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, nico);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 3);
        addCard(Zone.HAND, playerA, "Island");
        addCard(Zone.LIBRARY, playerA, "Lightning Bolt");
        addCard(Zone.LIBRARY, playerA, "Forest"); // top

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}{R}, {T}, Discard a card");
        setChoice(playerA, "Island"); // discard
        setChoice(playerA, true); // cast Lightning Bolt
        addTarget(playerA, playerB);

        setStopAt(1, PhaseStep.BEGIN_COMBAT);
        setStrictChooseMode(true);
        execute();

        // 3 from Bolt + 2 from Nico (cast from exile)
        assertLife(playerB, 20 - 3 - 2);
        assertGraveyardCount(playerA, "Island", 1);
        assertExileCount(playerA, "Forest", 1);
    }
}

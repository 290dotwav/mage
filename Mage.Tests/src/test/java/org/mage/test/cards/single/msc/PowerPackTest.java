package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class PowerPackTest extends CardTestPlayerBase {

    private static final String pack = "Power Pack";

    @Test
    public void test_ExileRandom_CastFreeNextUpkeep_ThenExile() {
        addCard(Zone.BATTLEFIELD, playerA, pack);
        addCard(Zone.GRAVEYARD, playerA, "Lava Axe"); // the only instant or sorcery card

        attack(1, playerA, pack);
        setChoice(playerA, true); // turn 3 upkeep: cast Lava Axe for free
        addTarget(playerA, playerB);

        setStopAt(3, PhaseStep.DRAW);
        setStrictChooseMode(true);
        execute();

        assertLife(playerB, 20 - 4 - 5);
        assertExileCount(playerA, "Lava Axe", 1);
    }
}

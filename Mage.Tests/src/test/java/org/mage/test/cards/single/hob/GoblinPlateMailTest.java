package org.mage.test.cards.single.hob;

import mage.abilities.keyword.MenaceAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class GoblinPlateMailTest extends CardTestPlayerBase {

    private static final String mail = "Goblin Plate Mail";

    @Test
    public void test_AmassAndAttach() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 2);
        addCard(Zone.HAND, playerA, mail);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, mail);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Goblin Army Token", 1);
        assertCounterCount(playerA, "Goblin Army Token", mage.counters.CounterType.P1P1, 1);
        assertAttachedTo(playerA, mail, "Goblin Army Token", true);
        assertPowerToughness(playerA, "Goblin Army Token", 2, 1);
        assertAbility(playerA, "Goblin Army Token", new MenaceAbility(), true);
    }
}

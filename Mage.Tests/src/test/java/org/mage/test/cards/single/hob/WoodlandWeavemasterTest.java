package org.mage.test.cards.single.hob;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class WoodlandWeavemasterTest extends CardTestPlayerBase {

    private static final String weavemaster = "Woodland Weavemaster"; // 1/2

    @Test
    public void test_BoostAndElfOnlyMana() {
        addCard(Zone.BATTLEFIELD, playerA, weavemaster);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 1);
        addCard(Zone.HAND, playerA, "Llanowar Elves");
        addCard(Zone.HAND, playerA, "Elvish Visionary"); // {1}{G} Elf
        addCard(Zone.HAND, playerA, "Grizzly Bears"); // {1}{G} Bear

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Llanowar Elves");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPT("boosted", 1, PhaseStep.PRECOMBAT_MAIN, playerA, weavemaster, 2, 3);
        // only the Weavemaster's mana is available: it can't pay for a non-Elf
        checkPlayableAbility("no bears", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Grizzly Bears", false);
        checkPlayableAbility("elf ok", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Elvish Visionary", true);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Elvish Visionary");
        setChoice(playerA, "Green");

        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Elvish Visionary", 1);
        assertTapped(weavemaster, true);
        assertHandCount(playerA, "Grizzly Bears", 1);
    }
}

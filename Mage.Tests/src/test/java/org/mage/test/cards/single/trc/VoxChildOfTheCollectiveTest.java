package org.mage.test.cards.single.trc;

import mage.cards.decks.CardNameUtil;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class VoxChildOfTheCollectiveTest extends CardTestPlayerBase {

    private static final String vox = "Vox, Child of the Collective";

    // first main phase: the next artifact creature spell and the next Spacecraft spell have undaunted (one opponent: {1} less each)
    @Test
    public void test_UndauntedOnBoth() {
        addCard(Zone.BATTLEFIELD, playerA, vox);
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 4);
        addCard(Zone.HAND, playerA, "Alpha Myr"); // {2}
        addCard(Zone.HAND, playerA, "Wedgelight Rammer"); // {3}{W} Spacecraft

        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN); // the first-main trigger
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Alpha Myr");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Wedgelight Rammer");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Alpha Myr", 1);
        assertPermanentCount(playerA, "Wedgelight Rammer", 1);
        assertPermanentCount(playerA, "Robot Token", 1);
    }

    // only the next one: a second artifact creature spell pays in full
    @Test
    public void test_OnlyTheNext() {
        addCard(Zone.BATTLEFIELD, playerA, vox);
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 2);
        addCard(Zone.HAND, playerA, "Alpha Myr", 2);

        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN); // the first-main trigger
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Alpha Myr");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPlayableAbility("second costs {2}", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Alpha Myr", false);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Alpha Myr", 1);
    }

    // a list spelled as printed, « Võx », finds the card
    @Test
    public void test_NameWithTilde() {
        Assert.assertEquals(vox, CardNameUtil.normalizeCardName("Võx, Child of the Collective"));
    }
}

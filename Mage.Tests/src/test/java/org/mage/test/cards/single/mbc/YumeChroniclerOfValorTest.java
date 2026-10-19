package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class YumeChroniclerOfValorTest extends CardTestPlayerBase {

    private static final String yume = "Yume, Chronicler of Valor";

    // the first spell of the turn has assist; the helper paid, so both draw
    @Test
    public void test_AssistedFirstSpellDraws() {
        addCard(Zone.BATTLEFIELD, playerA, yume);
        addCard(Zone.HAND, playerA, "Craw Wurm"); // {4}{G}{G}
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 6 - 2);
        addCard(Zone.BATTLEFIELD, playerB, "Forest", 2); // assist pay

        disableManaAutoPayment(playerA);
        activateManaAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: Add {G}", 4);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Craw Wurm");
        setChoice(playerA, "Green", 4); // normal pay x4
        setChoice(playerA, "Assist"); // activate assist
        addTarget(playerA, playerB); // player to assist
        setChoice(playerB, "X=2");
        setChoice(playerA, "Colorless", 2 - 1);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Craw Wurm", 1);
        assertHandCount(playerA, 1);
        assertHandCount(playerB, 1);
    }

    // the second spell of the turn has no assist
    @Test
    public void test_SecondSpellNoAssist() {
        addCard(Zone.BATTLEFIELD, playerA, yume);
        addCard(Zone.HAND, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, "Craw Wurm"); // {4}{G}{G}
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 6);
        addCard(Zone.BATTLEFIELD, playerB, "Forest", 2);

        checkPlayableAbility("first spell with assist", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Craw Wurm", true);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPlayableAbility("second spell, four lands left", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Craw Wurm", false);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerA, "Craw Wurm", 1);
        assertHandCount(playerB, 0);
    }
}

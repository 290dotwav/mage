package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class LokiLordOfMisruleTest extends CardTestPlayerBase {

    private static final String loki = "Loki, Lord of Misrule";

    @Test
    public void test_OthersBecomeNonlegendaryCopies_UntilEndOfTurn() {
        addCard(Zone.BATTLEFIELD, playerA, loki);
        addCard(Zone.BATTLEFIELD, playerA, "Island");
        addCard(Zone.BATTLEFIELD, playerA, "Isamaru, Hound of Konda"); // legendary 2/2
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Memnite");
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{U}, {T}: Choose");
        addTarget(playerA, "Isamaru, Hound of Konda");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        // Loki, Grizzly Bears and Memnite are nonlegendary copies of Isamaru: all four stay (no legend rule)
        checkPermanentCount("copies", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Isamaru, Hound of Konda", 4);
        checkPermanentCount("opponent unaffected", 1, PhaseStep.PRECOMBAT_MAIN, playerB, "Hill Giant", 1);

        setStopAt(2, PhaseStep.UPKEEP);
        setStrictChooseMode(true);
        execute();

        assertPermanentCount(playerA, "Isamaru, Hound of Konda", 1);
        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertPermanentCount(playerA, loki, 1);
    }
}

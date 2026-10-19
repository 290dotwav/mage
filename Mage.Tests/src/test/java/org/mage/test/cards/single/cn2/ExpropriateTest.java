package org.mage.test.cards.single.cn2;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * {@link mage.cards.e.Expropriate Expropriate} {7}{U}{U}: "Council's dilemma -- Starting with you, each player votes
 * for time or money. For each time vote, take an extra turn after this one. For each money vote, choose a permanent
 * owned by the voter and gain control of it." A money vote reached the permanents the voter controlled, not the ones
 * it owns (found on the ClaudeMTG table's thirteenth mechanics sweep).
 */
public class ExpropriateTest extends CardTestPlayerBase {

    private static final String expropriate = "Expropriate";

    @Test
    public void test_MoneyTakesAPermanentTheVoterOwns() {
        addCard(Zone.HAND, playerA, expropriate, 1);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 9);
        addCard(Zone.BATTLEFIELD, playerB, "Memnite", 1);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, expropriate);
        setChoice(playerA, true); // time
        setChoice(playerB, false); // money
        setChoice(playerA, "Memnite");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Memnite", 1);
        assertPermanentCount(playerB, "Memnite", 0);
    }

    @Test
    public void test_MoneyDoesNotReachWhatTheVoterControlsButDoesNotOwn() {
        // Donate: "Target player gains control of target permanent you control."
        addCard(Zone.HAND, playerA, "Donate", 1);
        addCard(Zone.HAND, playerA, expropriate, 1);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 12);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears", 1);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Donate");
        addTarget(playerA, playerB);
        addTarget(playerA, "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPermanentCount("the bear is playerB's now", 1, PhaseStep.PRECOMBAT_MAIN, playerB, "Grizzly Bears", 1);

        // playerB owns no permanent: its money vote takes nothing (playerA owns the bear it controls)
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, expropriate);
        setChoice(playerA, true); // time
        setChoice(playerB, false); // money

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerB, "Grizzly Bears", 1);
        assertPermanentCount(playerA, "Grizzly Bears", 0);
    }
}

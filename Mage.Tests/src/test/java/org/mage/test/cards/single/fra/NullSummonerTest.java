package org.mage.test.cards.single.fra;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class NullSummonerTest extends CardTestPlayerBase {

    private static final String summoner = "Null Summoner";

    // exile a nonland card; with threshold it can be cast, with mana of any type
    @Test
    public void test_ExileAndCastWithThreshold() {
        addCard(Zone.BATTLEFIELD, playerA, "Underground River", 5);
        addCard(Zone.HAND, playerA, summoner);
        addCard(Zone.GRAVEYARD, playerA, "Swamp", 7);
        addCard(Zone.HAND, playerB, "Lightning Bolt");
        addCard(Zone.HAND, playerB, "Mountain");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, summoner);
        addTarget(playerA, playerB);
        setChoice(playerA, "Lightning Bolt");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkExileCount("exiled", 1, PhaseStep.PRECOMBAT_MAIN, playerB, "Lightning Bolt", 1);

        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Lightning Bolt", playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 17);
        assertGraveyardCount(playerB, "Lightning Bolt", 1);
        assertHandCount(playerB, "Mountain", 1);
    }

    // without threshold the exiled card can't be cast
    @Test
    public void test_NoThresholdNoCast() {
        addCard(Zone.BATTLEFIELD, playerA, "Underground River", 5);
        addCard(Zone.HAND, playerA, summoner);
        addCard(Zone.GRAVEYARD, playerA, "Swamp", 6);
        addCard(Zone.HAND, playerB, "Lightning Bolt");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, summoner);
        addTarget(playerA, playerB);
        setChoice(playerA, "Lightning Bolt");
        checkPlayableAbility("no threshold", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Cast Lightning Bolt", false);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertExileCount(playerB, "Lightning Bolt", 1);
    }

    // put onto the battlefield without casting: no trigger
    @Test
    public void test_NotCastNoTrigger() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 3);
        addCard(Zone.HAND, playerA, "Reanimate");
        addCard(Zone.GRAVEYARD, playerA, summoner);
        addCard(Zone.HAND, playerB, "Lightning Bolt");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Reanimate", summoner);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, summoner, 1);
        assertHandCount(playerB, "Lightning Bolt", 1);
    }
}

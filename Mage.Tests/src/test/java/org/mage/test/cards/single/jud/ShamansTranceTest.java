package org.mage.test.cards.single.jud;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class ShamansTranceTest extends CardTestPlayerBase {

    private static final String trance = "Shaman's Trance";

    @Test
    public void test_CastAndPlayFromOpponentsGraveyard() {
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 4);
        addCard(Zone.HAND, playerA, trance);
        addCard(Zone.GRAVEYARD, playerB, "Lightning Bolt");
        addCard(Zone.GRAVEYARD, playerB, "Forest");

        checkPlayableAbility("before", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Lightning Bolt", false);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, trance);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        playLand(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Forest");
        // only this turn
        checkPlayableAbility("next turn", 3, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast Lightning Bolt", false);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 17);
        assertGraveyardCount(playerB, "Lightning Bolt", 1); // back to its owner's graveyard
        assertPermanentCount(playerA, "Forest", 1);
    }

    @Test
    public void test_FlashbackFromOpponentsGraveyard_AndOpponentCant() {
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.HAND, playerA, trance);
        addCard(Zone.GRAVEYARD, playerB, "Think Twice"); // flashback {2}{U}
        addCard(Zone.GRAVEYARD, playerB, "Deep Analysis"); // flashback {1}{U}, pay 3 life
        addCard(Zone.BATTLEFIELD, playerB, "Island", 3);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, trance);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        // the opponent can't cast from their own graveyard this turn
        checkPlayableAbility("B can't", 1, PhaseStep.PRECOMBAT_MAIN, playerB, "Flashback", false);
        activateAbility(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Flashback {2}{U}");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerA, 1);
        assertExileCount(playerB, "Think Twice", 1);
    }
}

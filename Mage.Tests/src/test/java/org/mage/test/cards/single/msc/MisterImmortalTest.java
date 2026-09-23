package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class MisterImmortalTest extends CardTestPlayerBase {

    // {2}{G}: Return this card from your graveyard or from exile to the battlefield tapped.
    private static final String IMMORTAL = "Mister Immortal";

    @Test
    public void test_FromGraveyard() {
        addCard(Zone.GRAVEYARD, playerA, IMMORTAL);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 3);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}{G}: Return");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, IMMORTAL, 1);
        assertTapped(IMMORTAL, true);
    }

    @Test
    public void test_FromExile() {
        addCard(Zone.EXILED, playerA, IMMORTAL);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 3);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}{G}: Return");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, IMMORTAL, 1);
        assertTapped(IMMORTAL, true);
    }

    @Test
    public void test_NotFromBattlefieldOrHand() {
        addCard(Zone.BATTLEFIELD, playerA, IMMORTAL);
        addCard(Zone.HAND, playerA, IMMORTAL);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 3);

        checkPlayableAbility("no activation", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}{G}: Return", false);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
    }

    @Test
    public void test_KilledAndBack() {
        addCard(Zone.BATTLEFIELD, playerA, IMMORTAL);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 3);
        addCard(Zone.HAND, playerB, "Swords to Plowshares");
        addCard(Zone.BATTLEFIELD, playerB, "Plains");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerB, "Swords to Plowshares", IMMORTAL);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        activateAbility(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "{2}{G}: Return");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, IMMORTAL, 1);
        assertLife(playerA, 22);
    }
}

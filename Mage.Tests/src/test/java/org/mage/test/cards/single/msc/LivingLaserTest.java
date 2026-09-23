package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class LivingLaserTest extends CardTestPlayerBase {

    // Haste
    // Whenever Living Laser attacks, for each card you've discarded this turn, create a token that's a copy of
    // Living Laser, except the token isn't legendary. The tokens enter tapped and attacking. Exile the tokens at
    // the beginning of the next end step.
    private static final String LASER = "Living Laser";

    @Test
    public void test_TwoDiscardsTwoTokens() {
        addCard(Zone.BATTLEFIELD, playerA, LASER);
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Island", 2);
        addCard(Zone.HAND, playerA, "Faithless Looting");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Faithless Looting");
        attack(1, playerA, LASER, playerB);

        checkPermanentCount("three lasers", 1, PhaseStep.END_COMBAT, playerA, LASER, 3);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.UPKEEP);
        execute();

        assertLife(playerB, 20 - 4 * 3);
        assertPermanentCount(playerA, LASER, 1);
    }

    @Test
    public void test_NoDiscardNoToken() {
        addCard(Zone.BATTLEFIELD, playerA, LASER);

        attack(1, playerA, LASER, playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertLife(playerB, 20 - 4);
        assertPermanentCount(playerA, LASER, 1);
    }
}

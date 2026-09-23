package org.mage.test.cards.single.msc;

import mage.abilities.keyword.IndestructibleAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class NickFurySpymasterTest extends CardTestPlayerBase {

    // First strike
    // Whenever a creature you control attacks alone, draw a card. Then you may put a creature card with mana value 3
    // or less from your hand onto the battlefield. It enters tapped and attacking and gains indestructible until end of turn.
    private static final String FURY = "Nick Fury, Spymaster";

    @Test
    public void test_AttackAlonePutsAttacker() {
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Island");
        addCard(Zone.BATTLEFIELD, playerA, FURY);
        addCard(Zone.HAND, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, "Hill Giant"); // mana value 4: not allowed

        attack(1, playerA, FURY, playerB);
        setChoice(playerA, true);
        setChoice(playerA, "Grizzly Bears");

        checkAbility("indestructible", 1, PhaseStep.DECLARE_BLOCKERS, playerA, "Grizzly Bears", IndestructibleAbility.class, true);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertTapped("Grizzly Bears", true);
        assertLife(playerB, 20 - 4 - 2);
        assertHandCount(playerA, "Island", 1);
        assertHandCount(playerA, "Hill Giant", 1);
    }
}

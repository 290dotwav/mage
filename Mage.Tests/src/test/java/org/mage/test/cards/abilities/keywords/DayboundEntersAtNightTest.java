package org.mage.test.cards.abilities.keywords;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * 702.145b: daybound means "If it is night and this permanent is represented by a double-faced card,
 * it enters transformed". A Brutal Cathar cast at night is a Moonrage Brute as it enters: its front
 * face's "when this creature enters" never triggers.
 */
public class DayboundEntersAtNightTest extends CardTestPlayerBase {

    private static final String cathar = "Brutal Cathar";
    private static final String brute = "Moonrage Brute";
    private static final String bears = "Grizzly Bears";

    @Test
    public void testCastAtNightEntersTransformed() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 3);
        addCard(Zone.HAND, playerA, cathar);
        addCard(Zone.BATTLEFIELD, playerB, bears);

        runCode("set game to night", 1, PhaseStep.UPKEEP, playerA, (i, p, game) -> game.setDaytime(false));
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, cathar);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertPermanentCount(playerA, brute, 1);
        assertPermanentCount(playerA, cathar, 0);
        assertPermanentCount(playerB, bears, 1);
    }

    @Test
    public void testCastByDayEntersFrontFaceUp() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 3);
        addCard(Zone.HAND, playerA, cathar);
        addCard(Zone.BATTLEFIELD, playerB, bears);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, cathar);
        addTarget(playerA, bears);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertPermanentCount(playerA, cathar, 1);
        assertPermanentCount(playerB, bears, 0);
        assertExileCount(playerB, bears, 1);
    }
}

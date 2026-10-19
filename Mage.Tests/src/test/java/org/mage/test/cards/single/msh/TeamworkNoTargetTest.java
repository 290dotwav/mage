package org.mage.test.cards.single.msh;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * Go Nuts! ("Choose one. If this spell was cast using teamwork, choose both instead."): with no creature an opponent
 * controls, the fight mode has no target, so paying teamwork could only make the cast fail. It was offered, and a
 * "Yes" undid the whole cast without a word (seen on a door).
 */
public class TeamworkNoTargetTest extends CardTestPlayerBase {

    private static final String goNuts = "Go Nuts!";

    @Test
    public void testNoOpposingCreatureTeamworkNotOffered() {
        addCard(Zone.HAND, playerA, goNuts);
        addCard(Zone.BATTLEFIELD, playerA, "Forest");
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant");
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, goNuts);
        // strict mode: a "Teamwork 3?" asked here would fail for want of an answer
        setModeChoice(playerA, "1"); // +1/+1 counter on target creature
        addTarget(playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.BEGIN_COMBAT);
        execute();

        assertCounterCount(playerA, "Grizzly Bears", CounterType.P1P1, 1);
        assertTapped("Hill Giant", false);
        assertGraveyardCount(playerA, goNuts, 1);
    }
}

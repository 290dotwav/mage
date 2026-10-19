package org.mage.test.cards.abilities.keywords;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * 702.192a Paradigm represents two spell abilities, one of which creates a delayed triggered ability. Paradigm
 * means "If this is the first time a spell you control with this spell's name has resolved this game, at the
 * beginning of each of your precombat main phases for the rest of the game, create a copy of this object in
 * exile. You may cast the copy without paying its mana cost" and "Exile this spell."
 */
public class ParadigmTest extends CardTestPlayerBase {

    private static final String practicum = "Germination Practicum"; // {3}{G}{G} sorcery: two +1/+1 counters on each creature you control
    private static final String bears = "Grizzly Bears";

    @Test
    public void testExiledThenCopyCastAtEachPrecombatMain() {
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 5);
        addCard(Zone.BATTLEFIELD, playerA, bears);
        addCard(Zone.HAND, playerA, practicum);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, practicum);
        // turn 3, then turn 5: the delayed trigger's copy, cast free
        setChoice(playerA, true);
        setChoice(playerA, true);

        setStrictChooseMode(true);
        setStopAt(5, PhaseStep.BEGIN_COMBAT);
        execute();

        assertExileCount(playerA, practicum, 1);
        assertGraveyardCount(playerA, practicum, 0);
        assertCounterCount(playerA, bears, CounterType.P1P1, 2 + 2 + 2);
    }

    @Test
    public void testTheCopyMayBeDeclined() {
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 5);
        addCard(Zone.BATTLEFIELD, playerA, bears);
        addCard(Zone.HAND, playerA, practicum);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, practicum);
        setChoice(playerA, false); // turn 3: not cast
        setChoice(playerA, true); // turn 5: cast

        setStrictChooseMode(true);
        setStopAt(5, PhaseStep.BEGIN_COMBAT);
        execute();

        assertCounterCount(playerA, bears, CounterType.P1P1, 2 + 2);
    }

    @Test
    public void testOnlyTheFirstResolutionCreatesTheTrigger() {
        // A second Practicum resolving later is exiled too, and makes no second copy each turn.
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 10);
        addCard(Zone.BATTLEFIELD, playerA, bears);
        addCard(Zone.HAND, playerA, practicum, 2);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, practicum);
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, practicum);
        setChoice(playerA, true); // turn 3: one copy, one question

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.BEGIN_COMBAT);
        execute();

        assertExileCount(playerA, practicum, 2);
        assertCounterCount(playerA, bears, CounterType.P1P1, 2 + 2 + 2);
    }

    @Test
    public void testACounteredSpellDoesNothing() {
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 5);
        addCard(Zone.BATTLEFIELD, playerA, bears);
        addCard(Zone.HAND, playerA, practicum);
        addCard(Zone.BATTLEFIELD, playerB, "Island", 2);
        addCard(Zone.HAND, playerB, "Counterspell");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, practicum);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerB, "Counterspell", practicum, practicum);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.BEGIN_COMBAT);
        execute();

        assertGraveyardCount(playerA, practicum, 1);
        assertExileCount(playerA, practicum, 0);
        assertCounterCount(playerA, bears, CounterType.P1P1, 0);
    }
}

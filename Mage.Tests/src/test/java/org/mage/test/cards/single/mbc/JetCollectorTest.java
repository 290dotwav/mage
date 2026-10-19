package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class JetCollectorTest extends CardTestPlayerBase {

    private static final String collector = "Jet Collector";

    // second main phase with four cards in the graveyard: Mox Jet, once
    @Test
    public void test_ConjureOnce() {
        addCard(Zone.BATTLEFIELD, playerA, collector);
        addCard(Zone.GRAVEYARD, playerA, "Swamp", 4);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerA, "Mox Jet", 1);
    }

    // three cards: no Mox
    @Test
    public void test_NotEnoughCards() {
        addCard(Zone.BATTLEFIELD, playerA, collector);
        addCard(Zone.GRAVEYARD, playerA, "Swamp", 3);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertHandCount(playerA, "Mox Jet", 0);
    }

    // {X}{B}: a creature card with mana value X back with a finality counter
    @Test
    public void test_Reanimate() {
        addCard(Zone.BATTLEFIELD, playerA, collector);
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 3);
        addCard(Zone.GRAVEYARD, playerA, "Grizzly Bears");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{X}{B}: Return");
        setChoice(playerA, "X=2");
        addTarget(playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertCounterCount(playerA, "Grizzly Bears", CounterType.FINALITY, 1);
    }
}

package org.mage.test.cards.single.ice;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class SpoilsOfWarTest extends CardTestPlayerBase {

    private static final String spoils = "Spoils of War";

    @Test
    public void test_XIsOpponentsArtifactAndCreatureCards() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 4);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant");
        addCard(Zone.HAND, playerA, spoils);
        addCard(Zone.GRAVEYARD, playerB, "Ornithopter"); // artifact creature: counts once
        addCard(Zone.GRAVEYARD, playerB, "Grizzly Bears", 2);
        addCard(Zone.GRAVEYARD, playerB, "Island"); // doesn't count
        addCard(Zone.GRAVEYARD, playerA, "Hill Giant", 3); // your own graveyard doesn't count

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, spoils);
        setChoice(playerA, "PlayerB"); // the opponent whose graveyard sets X
        addTargetAmount(playerA, "Grizzly Bears", 2);
        addTargetAmount(playerA, "Hill Giant", 1);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertCounterCount(playerA, "Grizzly Bears", CounterType.P1P1, 2);
        assertCounterCount(playerA, "Hill Giant", CounterType.P1P1, 1);
        assertTappedCount("Swamp", true, 4);
    }
}

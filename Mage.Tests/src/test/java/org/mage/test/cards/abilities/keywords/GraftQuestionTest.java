package org.mage.test.cards.abilities.keywords;

import mage.abilities.Ability;
import mage.abilities.keyword.GraftAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.game.permanent.Permanent;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * Graft (702.58a): "Whenever another creature enters, if this permanent has a +1/+1 counter on it, you may move a
 * +1/+1 counter from this permanent onto that creature." Its "may" was asked with the keyword's reminder text, which
 * names neither the creature nor whose it is, and graft sees every creature that enters, an opponent's too.
 */
public class GraftQuestionTest extends CardTestPlayerBase {

    // Simic Initiate {G} 0/0, graft 1
    private static final String initiate = "Simic Initiate";

    @Test
    public void testOwnCreatureMoved() {
        addCard(Zone.BATTLEFIELD, playerA, initiate);
        addCard(Zone.HAND, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");
        setChoice(playerA, true); // Move a +1/+1 counter from Simic Initiate onto Grizzly Bears?

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.BEGIN_COMBAT);
        execute();

        assertCounterCount(playerA, "Grizzly Bears", CounterType.P1P1, 1);
        assertGraveyardCount(playerA, initiate, 1);
    }

    @Test
    public void testOpponentsCreatureDeclined() {
        addCard(Zone.BATTLEFIELD, playerA, initiate);
        addCard(Zone.HAND, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Forest", 2);

        castSpell(2, PhaseStep.PRECOMBAT_MAIN, playerB, "Grizzly Bears");
        setChoice(playerA, false);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.BEGIN_COMBAT);
        execute();

        assertCounterCount(playerB, "Grizzly Bears", CounterType.P1P1, 0);
        assertCounterCount(playerA, initiate, CounterType.P1P1, 1);
    }

    @Test
    public void testQuestionNamesTheCreatureAndWhose() {
        addCard(Zone.BATTLEFIELD, playerA, initiate);
        addCard(Zone.BATTLEFIELD, playerA, "Llanowar Elves");
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        Permanent from = getPermanent(initiate, playerA);
        Permanent elves = getPermanent("Llanowar Elves", playerA);
        Permanent bears = getPermanent("Grizzly Bears", playerB);
        Ability graft = from.getAbilities().stream().filter(GraftAbility.class::isInstance).findFirst().get();
        Assert.assertEquals("Move a +1/+1 counter from " + from.getIdName() + " onto " + elves.getIdName() + "?", GraftAbility.moveQuestion(from, elves, graft, currentGame));
        Assert.assertEquals("Move a +1/+1 counter from " + from.getIdName() + " onto " + bears.getIdName() + " (controlled by PlayerB)?", GraftAbility.moveQuestion(from, bears, graft, currentGame));
    }
}

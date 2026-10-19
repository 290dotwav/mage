package org.mage.test.cards.single.mbc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import mage.game.permanent.Permanent;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class SifaGrentTest extends CardTestPlayerBase {

    private static final String sifa = "Sifa Grent";

    // a creature dies: drain 1; +1 goads
    @Test
    public void test_DrainAndGoad() {
        addCard(Zone.BATTLEFIELD, playerA, sifa);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");
        addCard(Zone.HAND, playerA, "Lightning Bolt");
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Hill Giant");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", "Grizzly Bears");
        addTarget(playerA, playerB); // drain
        activateAbility(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "+1: Goad");
        addTarget(playerA, "Hill Giant");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 19);
        assertLife(playerA, 21);
        Permanent giant = getPermanent("Hill Giant");
        Assert.assertTrue(giant.getGoadingPlayers().contains(playerA.getId()));
        assertCounterCount(playerA, sifa, CounterType.LOYALTY, 5);
    }

    // -6: the opponent's top two cards, playable for free this turn
    @Test
    public void test_MinusSix() {
        skipInitShuffling();
        addCard(Zone.BATTLEFIELD, playerA, sifa);
        removeAllCardsFromLibrary(playerB);
        addCard(Zone.LIBRARY, playerB, "Mountain");
        addCard(Zone.LIBRARY, playerB, "Lava Axe"); // {4}{R}

        runCode("loyalty 6", 1, PhaseStep.UPKEEP, playerA, (info, player, game) -> {
            Permanent permanent = game.getBattlefield().getAllActivePermanents().stream()
                    .filter(p -> p.getName().equals(sifa)).findFirst().orElse(null);
            permanent.addCounters(CounterType.LOYALTY.createInstance(2), playerA.getId(), null, game);
        });
        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "-6: Each opponent");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lava Axe", playerB);
        playLand(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Mountain");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 15);
        assertPermanentCount(playerA, "Mountain", 1);
        assertGraveyardCount(playerA, sifa, 1);
    }
}

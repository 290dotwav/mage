package org.mage.test.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mage.collectors.services.GameStatsDataCollector;
import mage.collectors.services.GameStatsWatcher;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

import java.util.Collections;

/**
 * The leaderboard's numbers (GameStatsWatcher, GameStatsDataCollector): who dealt the damage,
 * who killed what, and nothing counted twice after a rollback.
 */
public class GameStatsWatcherTest extends CardTestPlayerBase {

    private GameStatsWatcher stats() {
        GameStatsWatcher watcher = currentGame.getState().getWatcher(GameStatsWatcher.class);
        Assert.assertNotNull("the stats watcher is in the game", watcher);
        return watcher;
    }

    /** The collector adds it at the game's start; the test server does not enable the collector. */
    private void watch() {
        currentGame.getState().addWatcher(new GameStatsWatcher());
    }

    @Test
    public void boltOnACreatureAndOnAPlayer() {
        watch();
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);
        addCard(Zone.HAND, playerA, "Lightning Bolt", 2);
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertLife(playerB, 17);
        assertGraveyardCount(playerB, "Grizzly Bears", 1);
        GameStatsWatcher s = stats();
        Assert.assertEquals(3, s.getDamageToPlayers(playerA.getId()));
        Assert.assertEquals(3, s.getDamageToCreatures(playerA.getId()));
        Assert.assertEquals(1, s.getCreaturesKilled(playerA.getId()));
        Assert.assertEquals(0, s.getCreaturesLost(playerA.getId()));
        Assert.assertEquals(1, s.getCreaturesLost(playerB.getId()));
        Assert.assertEquals(2, s.getSpellsCast(playerA.getId()));
        Assert.assertEquals(0, s.getSpellsCast(playerB.getId()));
        Assert.assertEquals(0, s.getUnknownKills());
    }

    @Test
    public void combatTradeIsOneKillEach() {
        watch();
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Runeclaw Bear");

        attack(1, playerA, "Grizzly Bears");
        block(1, playerB, "Runeclaw Bear", "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertGraveyardCount(playerA, "Grizzly Bears", 1);
        assertGraveyardCount(playerB, "Runeclaw Bear", 1);
        GameStatsWatcher s = stats();
        Assert.assertEquals(1, s.getCreaturesKilled(playerA.getId()));
        Assert.assertEquals(1, s.getCreaturesKilled(playerB.getId()));
        Assert.assertEquals(1, s.getCreaturesLost(playerA.getId()));
        Assert.assertEquals(1, s.getCreaturesLost(playerB.getId()));
        Assert.assertEquals(2, s.getDamageToCreatures(playerA.getId()));
        Assert.assertEquals(2, s.getDamageToCreatures(playerB.getId()));
        Assert.assertEquals(0, s.getDamageToPlayers(playerA.getId()));
    }

    @Test
    public void destroyIsTheCastersAndZeroToughnessIsNobodys() {
        watch();
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 4);
        addCard(Zone.HAND, playerA, "Murder");
        addCard(Zone.HAND, playerA, "Disfigure");
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Runeclaw Bear");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Murder", "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Disfigure", "Runeclaw Bear");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertGraveyardCount(playerB, 2);
        GameStatsWatcher s = stats();
        Assert.assertEquals(1, s.getCreaturesKilled(playerA.getId()));
        Assert.assertEquals(2, s.getCreaturesLost(playerB.getId()));
        Assert.assertEquals("0 toughness (704.5f) is credited to nobody", 1, s.getUnknownKills());
    }

    @Test
    public void lifeGainedIsCounted() {
        watch();
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 2);
        addCard(Zone.HAND, playerA, "Revitalize");
        addCard(Zone.LIBRARY, playerA, "Plains");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Revitalize");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertLife(playerA, 23);
        Assert.assertEquals(3, stats().getLifeGained(playerA.getId()));
    }

    @Test
    public void rollbackCountsNothingTwice() {
        watch();
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);
        addCard(Zone.HAND, playerA, "Lightning Bolt");

        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);
        rollbackTurns(3, PhaseStep.POSTCOMBAT_MAIN, playerA, 0);
        rollbackAfterActionsStart();
        castSpell(3, PhaseStep.POSTCOMBAT_MAIN, playerA, "Lightning Bolt", playerB);
        rollbackAfterActionsEnd();

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 17);
        GameStatsWatcher s = stats();
        Assert.assertEquals(3, s.getDamageToPlayers(playerA.getId()));
        Assert.assertEquals(1, s.getSpellsCast(playerA.getId()));
    }

    @Test
    public void theLastBoltEliminatesAndTheLineSaysSo() {
        watch();
        setLife(playerB, 3);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);
        addCard(Zone.HAND, playerA, "Lightning Bolt");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        Assert.assertTrue(currentGame.hasEnded());
        GameStatsWatcher s = stats();
        Assert.assertEquals(playerA.getId(), s.getEliminatedBy(playerB.getId()));
        Assert.assertEquals(Integer.valueOf(1), s.getEliminatedTurn(playerB.getId()));

        JsonObject line = GameStatsDataCollector.record(currentGame, Collections.emptySet());
        Assert.assertEquals(1, line.get("v").getAsInt());
        Assert.assertEquals(playerA.getName(), line.get("winner").getAsString());
        Assert.assertFalse(line.get("draw").getAsBoolean());
        Assert.assertEquals(1, line.get("turns").getAsInt());
        JsonArray players = line.getAsJsonArray("players");
        Assert.assertEquals(2, players.size());
        JsonObject a = null;
        JsonObject b = null;
        for (int i = 0; i < players.size(); i++) {
            JsonObject p = players.get(i).getAsJsonObject();
            if (p.get("name").getAsString().equals(playerA.getName())) {
                a = p;
            } else {
                b = p;
            }
        }
        Assert.assertNotNull(a);
        Assert.assertNotNull(b);
        Assert.assertTrue(a.get("winner").getAsBoolean());
        Assert.assertFalse(b.get("winner").getAsBoolean());
        Assert.assertFalse(b.get("quit").getAsBoolean());
        Assert.assertEquals(1, b.get("eliminatedTurn").getAsInt());
        Assert.assertEquals(playerA.getName(), b.get("eliminatedBy").getAsString());
        Assert.assertEquals(1, a.getAsJsonObject("stats").get("playersEliminated").getAsInt());
        Assert.assertEquals(3, a.getAsJsonObject("stats").get("damageToPlayers").getAsInt());
        Assert.assertEquals(16, a.get("deckHash").getAsString().length());

        // a concession is nobody's elimination
        JsonObject conceded = GameStatsDataCollector.record(currentGame, Collections.singleton(playerB.getId()));
        for (int i = 0; i < 2; i++) {
            JsonObject p = conceded.getAsJsonArray("players").get(i).getAsJsonObject();
            if (p.get("name").getAsString().equals(playerB.getName())) {
                Assert.assertTrue(p.get("quit").getAsBoolean());
                Assert.assertTrue(p.get("eliminatedBy").isJsonNull());
            } else {
                Assert.assertEquals(0, p.getAsJsonObject("stats").get("playersEliminated").getAsInt());
            }
        }
    }
}

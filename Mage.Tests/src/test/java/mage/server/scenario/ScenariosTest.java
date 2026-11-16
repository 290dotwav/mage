package mage.server.scenario;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.permanent.Permanent;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * The site's lessons (Scenarios): each position is what the list says once the game has
 * started (no hand drawn, no mulligan, life as written, who starts), and each one's answer
 * wins while its likely wrong tries do not. playerA is the student, playerB Claude.
 */
public class ScenariosTest extends CardTestPlayerBase {

    private void lesson(String id) {
        Scenario scenario = Scenarios.get(id);
        Assert.assertNotNull(id, scenario);
        Scenarios.apply(scenario, currentGame, gameOptions, playerA.getId());
    }

    @Test
    public void unknownIdIsNoScenario() {
        Assert.assertNull(Scenarios.get("../init.txt"));
        Assert.assertNull(Scenarios.get(null));
    }

    @Test
    public void bienvenueTwoAttacksOnTwoTurns() {
        lesson("tuto1");
        attack(1, playerA, "Grizzly Bears");
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 2);
        assertPermanentCount(playerA, "Grizzly Bears", 1);
        assertPermanentCount(playerB, 0);
        assertHandCount(playerA, 0);
        assertLibraryCount(playerA, 5);
        assertLife(playerA, 20);
    }

    @Test
    public void bienvenueSecondAttackWins() {
        lesson("tuto1");
        attack(1, playerA, "Grizzly Bears");
        attack(3, playerA, "Grizzly Bears");
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 0);
        assertHandCount(playerA, "Grizzly Bears", 1);
        Assert.assertTrue("the Rival has lost", currentGame.hasEnded() || !playerB.isInGame());
    }

    @Test
    public void coupDeGracePositionAsWritten() {
        lesson("m1");
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        Assert.assertEquals("the student starts", playerA.getId(), currentGame.getActivePlayerId());
        assertHandCount(playerA, 3);
        assertHandCount(playerA, "Shock", 1);
        assertHandCount(playerA, "Raging Goblin", 1);
        assertHandCount(playerA, "Giant Growth", 1);
        assertHandCount(playerB, 0);
        assertLibraryCount(playerA, 5);
        assertPermanentCount(playerA, "Mountain", 1);
        assertPermanentCount(playerA, "Forest", 1);
        assertPermanentCount(playerA, 2);
        assertPermanentCount(playerB, 0);
        assertLife(playerA, 20);
        assertLife(playerB, 3);
        // nothing of the decks they joined with is left in a zone
        Assert.assertEquals(0, playerA.getSideboard().size());
        Assert.assertEquals("library from the top down", "Mountain", playerA.getLibrary().getFromTop(currentGame).getName());
    }

    @Test
    public void coupDeGraceGoblinAndGrowthWin() {
        lesson("m1");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Raging Goblin");
        attack(1, playerA, "Raging Goblin");
        castSpell(1, PhaseStep.DECLARE_BLOCKERS, playerA, "Giant Growth", "Raging Goblin");
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, -1);
        assertLostTheGame(playerB);
    }

    @Test
    public void coupDeGraceShockFirstLeavesNoRedForTheGoblin() {
        lesson("m1");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerB);
        checkPlayableAbility("no red left", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Cast Raging Goblin", false);
        checkPlayableAbility("growth has no creature", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Cast Giant Growth", false);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 1);
    }

    @Test
    public void alAssautTheWurmJustArrived() {
        lesson("m2");
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        Permanent wurm = getPermanent("Craw Wurm", playerA);
        Assert.assertTrue("the Wurm came this turn", wurm.hasSummoningSickness());
        Assert.assertFalse(getPermanent("Wind Drake", playerA).hasSummoningSickness());
        Assert.assertFalse(getPermanent("Hill Giant", playerA).hasSummoningSickness());
        assertTappedCount("Forest", true, 5);
        assertTappedCount("Island", true, 1);
        assertHandCount(playerA, "Giant Growth", 1);
        assertLife(playerB, 7);
    }

    @Test
    public void alAssautElvesHomeForTheGrowth() {
        lesson("m2");
        attack(1, playerA, "Wind Drake");
        attack(1, playerA, "Hill Giant");
        attack(1, playerA, "Savannah Lions");
        block(1, playerB, "Grizzly Bears", "Hill Giant");
        castSpell(1, PhaseStep.DECLARE_BLOCKERS, playerA, "Giant Growth", "Wind Drake"); // the Elves' green
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 0);
        assertLostTheGame(playerB);
    }

    @Test
    public void alAssautEverythingThatCanAttackIsShort() {
        lesson("m2");
        attack(1, playerA, "Wind Drake");
        attack(1, playerA, "Hill Giant");
        attack(1, playerA, "Savannah Lions");
        attack(1, playerA, "Llanowar Elves");
        block(1, playerB, "Grizzly Bears", "Hill Giant");
        checkPlayableAbility("no green left", 1, PhaseStep.DECLARE_BLOCKERS, playerA, "Cast Giant Growth", false);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 2);
    }

    @Test
    public void tiensBonWallOnCyclopsGiantOnBrigandThenTheCounter() {
        lesson("m3");
        // nobody tells Claude to attack: all three must, by their own text
        block(1, playerA, "Wall of Wood", "Bloodrock Cyclops");
        block(1, playerA, "Hill Giant", "Valley Dasher");
        // a player who has lost takes his cards with him (800.4a): looked at before
        checkGraveyardCount("dasher died", 2, PhaseStep.PRECOMBAT_MAIN, playerB, "Valley Dasher", 1);
        checkLife("at 1", 2, PhaseStep.PRECOMBAT_MAIN, playerA, 1);
        attack(2, playerA, "Hill Giant");
        attack(2, playerA, "Grizzly Bears");
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 1);
        assertGraveyardCount(playerA, "Wall of Wood", 1);
        assertLife(playerB, 0);
        assertLostTheGame(playerB);
    }

    @Test
    public void tiensBonGiantOnCyclopsLeavesTooLittle() {
        lesson("m3");
        block(1, playerA, "Wall of Wood", "Valley Dasher");
        block(1, playerA, "Hill Giant", "Bloodrock Cyclops");
        attack(2, playerA, "Grizzly Bears");
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 1);
        assertLife(playerB, 3);
    }

    @Test
    public void coupFourreMastodonOnCyclopsReproachOnJuggernaut() {
        lesson("m4");
        block(1, playerA, "Siege Mastodon", "Bloodrock Cyclops");
        castSpell(1, PhaseStep.DECLARE_BLOCKERS, playerA, "Gideon's Reproach", "Juggernaut");
        checkGraveyardCount("juggernaut died", 2, PhaseStep.PRECOMBAT_MAIN, playerB, "Juggernaut", 1);
        checkGraveyardCount("cyclops died", 2, PhaseStep.PRECOMBAT_MAIN, playerB, "Bloodrock Cyclops", 1);
        attack(2, playerA, "Siege Mastodon");
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 3);
        assertLife(playerB, 0);
        assertLostTheGame(playerB);
    }

    @Test
    public void coupFourreTheReproachWaitsForAnAttacker() {
        lesson("m4");
        checkPlayableAbility("nothing attacks yet", 1, PhaseStep.UPKEEP, playerA, "Cast Gideon's Reproach", false);
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();
    }

    @Test
    public void laMachineArtistLastIsTen() {
        lesson("m5");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Siege-Gang Commander");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN, playerA);
        String[] order = {"Siege-Gang Commander", "Goblin Token", "Goblin Token", "Goblin Token", "Blood Artist"};
        sacrificeInOrder(order);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 0);
        assertLostTheGame(playerB);
    }

    @Test
    public void laMachineArtistFirstIsSix() {
        lesson("m5");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Siege-Gang Commander");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN, playerA);
        sacrificeInOrder(new String[]{"Blood Artist", "Siege-Gang Commander", "Goblin Token", "Goblin Token", "Goblin Token"});
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 4);
    }

    private void sacrificeInOrder(String[] order) {
        for (int i = 0; i < order.length; i++) {
            activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Sacrifice a creature", playerB);
            setChoice(playerA, order[i]);
            // the last one ends the game: nothing is left to wait for
            if (i < order.length - 1) {
                waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN, playerA);
            }
        }
    }

}

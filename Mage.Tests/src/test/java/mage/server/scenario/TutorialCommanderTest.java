package mage.server.scenario;

import mage.constants.PhaseStep;
import mage.constants.RangeOfInfluence;
import mage.game.lesson.RivalScript;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.player.TestComputerPlayerSimple;
import org.mage.test.player.TestPlayer;
import org.mage.test.serverside.base.CardTestCommanderDuelBase;

/**
 * The tutorial's sixth lesson, « Le commandant » (Scenarios tuto6, TUTO.md mission 6): a Commander
 * duel, the student at 40 with Isamaru in the command zone, the Rival at 6 who does nothing. Isamaru
 * (turn 1), the Plains and his attack (6 to 4, 2 of commander damage), Fling sacrificing him back to
 * the command zone (4 to 2), Isamaru again with the tax of 2 and Shock (2 to 0). The Rival is the
 * real "Computer - simple", as in TutorialTestBase.
 */
public class TutorialCommanderTest extends CardTestCommanderDuelBase {

    @Override
    protected TestPlayer createPlayer(String name, RangeOfInfluence rangeOfInfluence) {
        if ("PlayerB".equals(name)) {
            TestPlayer rival = new TestPlayer(new TestComputerPlayerSimple(name, rangeOfInfluence, 1));
            rival.setAIPlayer(true);
            return rival;
        }
        return super.createPlayer(name, rangeOfInfluence);
    }

    private RivalScript lesson() {
        Scenarios.apply(Scenarios.get("tuto6"), currentGame, gameOptions, playerA.getId());
        return gameOptions.rivalScript;
    }

    @Test
    public void positionAsWritten() {
        RivalScript script = lesson();
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        Assert.assertNotNull(script);
        Assert.assertTrue(script.getLines().isEmpty());
        assertLife(playerA, 40);
        assertLife(playerB, 6);
        assertCommandZoneCount(playerA, "Isamaru, Hound of Konda", 1);
        assertCommandZoneCount(playerA, "Ob Nixilis of the Black Oath", 0);
        assertCommandZoneCount(playerB, "Ob Nixilis of the Black Oath", 0);
        assertPermanentCount(playerA, "Plains", 1);
        assertPermanentCount(playerA, "Mountain", 2);
        assertHandCount(playerA, "Fling", 1);
        assertHandCount(playerA, 1);
        Assert.assertEquals("Plains", playerA.getLibrary().getFromTop(currentGame).getName());
        assertPermanentCount(playerB, 0);
    }

    @Test
    public void theLessonAsWritten() {
        lesson();
        // 6.6 Isamaru, a white mana
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Isamaru, Hound of Konda");
        checkPermanentCount("6.6", 1, PhaseStep.END_TURN, playerA, "Isamaru, Hound of Konda", 1);
        // 6.11 the Plains drawn and put down, Isamaru attacks: 6 to 4
        checkHandCardCount("6.11 a Plains drawn", 3, PhaseStep.PRECOMBAT_MAIN, playerA, "Plains", 1);
        playLand(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Plains");
        attack(3, playerA, "Isamaru, Hound of Konda");
        checkLife("6.11", 3, PhaseStep.POSTCOMBAT_MAIN, playerB, 4);
        // 6.16 b Fling on the Rival, Isamaru sacrificed; 6.17 to the command zone; 4 to 2
        castSpell(3, PhaseStep.POSTCOMBAT_MAIN, playerA, "Fling", playerB);
        setChoice(playerA, "Isamaru, Hound of Konda"); // the sacrifice
        setChoice(playerA, true); // to the command zone
        waitStackResolved(3, PhaseStep.POSTCOMBAT_MAIN, playerA);
        checkLife("6.17", 3, PhaseStep.END_TURN, playerB, 2);
        checkCommandCardCount("6.19 back in his zone", 3, PhaseStep.END_TURN, playerA, "Isamaru, Hound of Konda", 1);
        // 6.21 Isamaru with the tax ({2}{W}: the two Plains and a Mountain), then Shock: 2 to 0
        checkHandCardCount("6.19 Shock drawn", 5, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", 1);
        // the site's payment, imposed: the two Plains and a Mountain, a Mountain kept for the Shock
        activateManaAbility(5, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: Add {W}");
        activateManaAbility(5, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: Add {W}");
        activateManaAbility(5, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: Add {R}");
        castSpell(5, PhaseStep.PRECOMBAT_MAIN, playerA, "Isamaru, Hound of Konda");
        waitStackResolved(5, PhaseStep.PRECOMBAT_MAIN, playerA);
        checkPermanentCount("6.21 Isamaru again", 5, PhaseStep.PRECOMBAT_MAIN, playerA, "Isamaru, Hound of Konda", 1);
        castSpell(5, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", playerB);
        setStopAt(5, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 0);
        assertLostTheGame(playerB);
        assertLife(playerA, 40);
    }

    @Test
    public void theTaxIsThreeMana() {
        lesson();
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Isamaru, Hound of Konda");
        playLand(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Plains");
        attack(3, playerA, "Isamaru, Hound of Konda");
        castSpell(3, PhaseStep.POSTCOMBAT_MAIN, playerA, "Fling", playerB);
        setChoice(playerA, "Isamaru, Hound of Konda");
        setChoice(playerA, true);
        // 6.20 « Cette fois : 3 mana, dont un blanc »
        castSpell(5, PhaseStep.PRECOMBAT_MAIN, playerA, "Isamaru, Hound of Konda");
        setStopAt(5, PhaseStep.BEGIN_COMBAT);
        execute();

        assertPermanentCount(playerA, "Isamaru, Hound of Konda", 1);
        long tapped = currentGame.getBattlefield().getAllActivePermanents(playerA.getId()).stream()
                .filter(p -> p.isLand(currentGame) && p.isTapped()).count();
        Assert.assertEquals("the tax of 2 and the white", 3, tapped);
        assertLife(playerB, 2);
    }
}

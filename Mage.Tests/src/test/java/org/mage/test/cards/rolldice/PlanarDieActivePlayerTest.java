package org.mage.test.cards.rolldice;

import mage.abilities.Ability;
import mage.abilities.effects.common.PlaneswalkEffect;
import mage.constants.PhaseStep;
import mage.constants.Planes;
import mage.constants.Zone;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * 901.9: "Any time the active player has priority and the stack is empty, but only during a main
 * phase of their turn, that player may roll the planar die." Only the active player, and only in
 * a main phase of that player's own turn: the die of another player's turn is not his.
 * <p>
 * Every plane wrote its die as an ability anybody may activate (TargetController.ANY) whenever a
 * main phase's stack is empty, so the player who was not playing could roll in someone else's main
 * phase, and pay the active player's increasing cost for it.
 */
public class PlanarDieActivePlayerTest extends CardTestPlayerBase {

    private static final String DIE = "{0}: Roll the planar";

    @Test
    public void test_OnlyTheActivePlayerRollsInHisMainPhase() {
        addPlane(playerA, Planes.PLANE_AKOUM);

        // turn 1 is A's: A may roll, B may not
        checkPlayableAbility("A rolls in his main", 1, PhaseStep.PRECOMBAT_MAIN, playerA, DIE, true);
        checkPlayableAbility("B not in A's main", 1, PhaseStep.PRECOMBAT_MAIN, playerB, DIE, false);
        checkPlayableAbility("B not in A's second main", 1, PhaseStep.POSTCOMBAT_MAIN, playerB, DIE, false);
        // turn 2 is B's: the other way round, though the plane is still A's
        checkPlayableAbility("B rolls in his main", 2, PhaseStep.PRECOMBAT_MAIN, playerB, DIE, true);
        checkPlayableAbility("A not in B's main", 2, PhaseStep.PRECOMBAT_MAIN, playerA, DIE, false);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();
    }

    @Test
    public void test_NotOutsideAMainPhaseNorOverAStack() {
        addPlane(playerA, Planes.PLANE_AKOUM);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);
        addCard(Zone.HAND, playerA, "Lightning Bolt", 1);

        checkPlayableAbility("not in upkeep", 1, PhaseStep.UPKEEP, playerA, DIE, false);
        checkPlayableAbility("not in combat", 1, PhaseStep.DECLARE_ATTACKERS, playerA, DIE, false);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);
        checkPlayableAbility("not over a stack", 1, PhaseStep.PRECOMBAT_MAIN, playerA, DIE, false);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPlayableAbility("again once it resolved", 1, PhaseStep.PRECOMBAT_MAIN, playerA, DIE, true);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 3);
    }

    @Test
    public void test_TheRollerPaysHisOwnIncreasingCost() {
        addPlane(playerA, Planes.PLANE_AKOUM);
        addCard(Zone.BATTLEFIELD, playerB, "Mountain", 1);

        // B's turn: his first roll is free, the second costs {1}
        activateAbility(2, PhaseStep.PRECOMBAT_MAIN, playerB, DIE);
        setDieRollResult(playerB, 5); // blank
        waitStackResolved(2, PhaseStep.PRECOMBAT_MAIN);
        activateAbility(2, PhaseStep.PRECOMBAT_MAIN, playerB, DIE);
        setDieRollResult(playerB, 5); // blank
        waitStackResolved(2, PhaseStep.PRECOMBAT_MAIN);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertTappedCount("Mountain", true, 1);
    }

    /**
     * 701.31b: planeswalking puts the plane on the bottom of the planar deck and turns the top card
     * face up, so the table never lands on the plane it just left, and (their communal deck, 901.15)
     * goes through every plane before seeing one again. Their engine kept a list of the planes seen
     * that nothing ever wrote: a planeswalk could deal the very plane it left (seen on a door,
     * Tazeem to Tazeem).
     */
    @Test
    public void test_PlaneswalkNeverDealsThePlaneJustLeftAndGoesRoundAllOfThem() {
        addPlane(playerA, Planes.PLANE_AKOUM);
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        int all = Planes.values().length;
        List<String> dealt = new ArrayList<>();
        dealt.add(currentGame.getState().getCurrentPlane().getName());
        for (int i = 0; i < 3 * all; i++) {
            Ability source = currentGame.getState().getCurrentPlane().getAbilities().get(0);
            new PlaneswalkEffect(false).apply(currentGame, source);
            String now = currentGame.getState().getCurrentPlane().getName();
            Assert.assertNotEquals("planeswalk " + (i + 1) + " dealt the plane it left", dealt.get(dealt.size() - 1), now);
            dealt.add(now);
        }
        // the first round: every plane once before any comes back
        Assert.assertEquals("the first " + all + " planes are all different: " + dealt.subList(0, all), all, new HashSet<>(dealt.subList(0, all)).size());
    }
}

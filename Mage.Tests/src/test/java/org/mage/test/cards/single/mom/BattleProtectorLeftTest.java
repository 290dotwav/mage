package org.mage.test.cards.single.mom;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.permanent.Permanent;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestMultiPlayerBaseWithRangeAll;

/**
 * 310.11. If a battle that isn't being attacked has no player designated as its protector, or its protector is a
 * player who can't be its protector, its controller chooses an appropriate player to be its protector. If no player
 * can be chosen this way, the battle is put into its owner's graveyard. This is a state-based action.
 * <p>
 * A protector who left the game (conceded, 800.4a) still stood as one: the battle could never be attacked by anyone
 * but him.
 */
public class BattleProtectorLeftTest extends CardTestMultiPlayerBaseWithRangeAll {

    private static final String invasion = "Invasion of Tarkir"; // {1}{R}, Battle - Siege, defense 5

    @Test
    public void testNewProtectorWhenTheProtectorConcedes() {
        // Player order: A -> D -> C -> B
        addCard(Zone.HAND, playerA, invasion);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, invasion);
        setChoice(playerA, "PlayerB"); // protector
        concede(1, PhaseStep.POSTCOMBAT_MAIN, playerB);
        setChoice(playerA, "PlayerC"); // the new protector, among those still in the game

        setStopAt(1, PhaseStep.END_TURN);
        execute();

        Permanent battle = getPermanent(invasion, playerA);
        Assert.assertEquals("a protector still in the game", playerC.getId(), battle.getProtectorId());
    }
}

package org.mage.test.player.human;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.filter.common.FilterCreatureForCombatBlock;
import mage.filter.predicate.permanent.ControllerIdPredicate;
import mage.game.permanent.Permanent;
import mage.player.human.HumanPlayer;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

import java.util.List;
import java.util.UUID;

/**
 * {@link HumanPlayer#possibleBlockers}: the "Select blockers" question's list of creatures that may
 * block. It named a suspected Frantic Scapegoat (701.60c: it can't block), the client offered it,
 * and the block was refused and asked again.
 */
public class PossibleBlockersTest extends CardTestPlayerBase {

    @Test
    public void aCreatureThatCantBlock_isNotOffered() {
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Bloodghast"); // "can't block"
        addCard(Zone.BATTLEFIELD, playerA, "Mountain");
        addCard(Zone.HAND, playerA, "Frantic Scapegoat");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Frantic Scapegoat");
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        Permanent goat = getPermanent("Frantic Scapegoat", playerA);
        Permanent bears = getPermanent("Grizzly Bears", playerA);
        Assert.assertTrue("entered suspected", goat.isSuspected());
        FilterCreatureForCombatBlock filter = new FilterCreatureForCombatBlock();
        filter.add(new ControllerIdPredicate(playerA.getId()));
        List<UUID> possible = HumanPlayer.possibleBlockers(currentGame, filter, playerA.getId());
        Assert.assertTrue(possible.toString(), possible.contains(bears.getId()));
        Assert.assertFalse("a suspected creature can't block", possible.contains(goat.getId()));
        Assert.assertFalse("nor one that says so", possible.contains(getPermanent("Bloodghast", playerA).getId()));
    }
}

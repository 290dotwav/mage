package org.mage.test.player.human;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.permanent.Permanent;
import mage.player.human.HumanPlayer;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

import java.util.UUID;

/**
 * {@link HumanPlayer#declareAttackPlan}: the site's whole attack declaration in one answer to
 * "Select attackers", each creature at its own defender, instead of one answer (and one game view
 * each way) per creature. The owner: "Quand je declare des attaquants faut qu'ils se declarent tous
 * en meme temps pas un par un. Imagine j'en ai 10000".
 */
public class AttackPlanTest extends CardTestPlayerBase {

    private void atDeclareAttackers() {
        setStopAt(1, PhaseStep.DECLARE_ATTACKERS);
        execute();
        Assert.assertTrue("the combat knows its defenders", currentGame.getCombat().getDefenders().contains(playerB.getId()));
    }

    private UUID id(String name) {
        return getPermanent(name, playerA).getId();
    }

    @Test
    public void everyPairIsDeclaredAtItsDefender_inOneAnswer() {
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant");
        addCard(Zone.BATTLEFIELD, playerA, "Savannah Lions");
        addCard(Zone.BATTLEFIELD, playerB, "Jace Beleren");
        atDeclareAttackers();

        UUID jace = getPermanent("Jace Beleren", playerB).getId();
        HumanPlayer.declareAttackPlan(playerA, ""
                + id("Grizzly Bears") + "=" + playerB.getId() + ";"
                + id("Hill Giant") + "=" + jace, currentGame);

        Assert.assertEquals(2, currentGame.getCombat().getAttackers().size());
        Assert.assertEquals(playerB.getId(), currentGame.getCombat().getDefenderId(id("Grizzly Bears")));
        Assert.assertEquals(jace, currentGame.getCombat().getDefenderId(id("Hill Giant")));
        Assert.assertTrue("declared attackers are tapped", getPermanent("Grizzly Bears", playerA).isTapped());
        Assert.assertFalse("one left out stays home", getPermanent("Savannah Lions", playerA).isAttacking());
    }

    @Test
    public void aCreatureTakenBack_thenSentElsewhere() {
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Jace Beleren");
        atDeclareAttackers();

        UUID bears = id("Grizzly Bears");
        UUID jace = getPermanent("Jace Beleren", playerB).getId();
        HumanPlayer.declareAttackPlan(playerA, bears + "=" + playerB.getId(), currentGame);
        Assert.assertEquals(playerB.getId(), currentGame.getCombat().getDefenderId(bears));
        HumanPlayer.declareAttackPlan(playerA, bears + "=;" + bears + "=" + jace, currentGame);
        Assert.assertEquals("moved to the planeswalker", jace, currentGame.getCombat().getDefenderId(bears));
        HumanPlayer.declareAttackPlan(playerA, bears + "=", currentGame);
        Assert.assertTrue("taken back", currentGame.getCombat().getAttackers().isEmpty());
    }

    @Test
    public void whatTheRulesRefuseIsLeftOut() {
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Wall of Wood"); // defender
        addCard(Zone.BATTLEFIELD, playerB, "Savannah Lions");
        atDeclareAttackers();

        Permanent lions = getPermanent("Savannah Lions", playerB);
        HumanPlayer.declareAttackPlan(playerA, "nonsense;"
                + id("Wall of Wood") + "=" + playerB.getId() + ";"
                + lions.getId() + "=" + playerB.getId() + ";"
                + id("Grizzly Bears") + "=" + playerA.getId() + ";"
                + id("Grizzly Bears") + "=" + playerB.getId(), currentGame);

        Assert.assertEquals("only the Bears, at the opponent", 1, currentGame.getCombat().getAttackers().size());
        Assert.assertEquals(playerB.getId(), currentGame.getCombat().getDefenderId(id("Grizzly Bears")));
    }
}

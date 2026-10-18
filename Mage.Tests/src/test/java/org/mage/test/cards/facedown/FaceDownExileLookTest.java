package org.mage.test.cards.facedown;

import mage.cards.Card;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.util.CardUtil;
import mage.view.CardView;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * 406.3: a card exiled face down can't be examined by any player except when instructions allow
 * it. Gonti, Lord of Luxury exiles a card of an opponent's library face down, and its controller
 * "may look at it". Every view showed that card to its owner (the controller or owner of a card
 * in exile, `canShowAsControlled`) and blanked it for Gonti's controller, who may look at it and
 * cast it: the other way round. Seen on the web door, 18 October: the host's screen held the
 * face of the card the chair's Gonti took, and the chair's held a back.
 */
public class FaceDownExileLookTest extends CardTestPlayerBase {

    @Test
    public void test_GontiCardIsShownToGontisControllerOnly() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 4);
        addCard(Zone.HAND, playerA, "Gonti, Lord of Luxury", 1);
        addCard(Zone.LIBRARY, playerB, "Lightning Bolt", 1);
        skipInitShuffling();

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Gonti, Lord of Luxury");
        addTarget(playerA, playerB);
        setChoice(playerA, "Lightning Bolt");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        Card bolt = currentGame.getExile().getAllCards(currentGame).stream()
                .filter(c -> c.getName().equals("Lightning Bolt"))
                .findFirst().orElse(null);
        Assert.assertNotNull("the Bolt is in exile", bolt);
        Assert.assertTrue("face down", bolt.isFaceDown(currentGame));
        Assert.assertTrue("Gonti's controller may look at it", CardUtil.canShowAsControlled(bolt, currentGame, playerA.getId()));
        Assert.assertFalse("its owner may not", CardUtil.canShowAsControlled(bolt, currentGame, playerB.getId()));
        // and the views say so: the card for A ("Face Down: Lightning Bolt", the real one beside it), nothing of it for B
        Assert.assertTrue(new CardView(bolt, currentGame, CardUtil.canShowAsControlled(bolt, currentGame, playerA.getId())).getName().contains("Lightning Bolt"));
        Assert.assertFalse(new CardView(bolt, currentGame, CardUtil.canShowAsControlled(bolt, currentGame, playerB.getId())).getName().contains("Lightning Bolt"));
    }

    @Test
    public void test_ForetoldCardIsStillShownToItsOwner() {
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);
        addCard(Zone.HAND, playerA, "Demon Bolt", 1);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Fore");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        Card bolt = currentGame.getExile().getAllCards(currentGame).stream()
                .filter(c -> c.getName().equals("Demon Bolt"))
                .findFirst().orElse(null);
        Assert.assertNotNull(bolt);
        Assert.assertTrue(CardUtil.canShowAsControlled(bolt, currentGame, playerA.getId()));
        Assert.assertFalse(CardUtil.canShowAsControlled(bolt, currentGame, playerB.getId()));
    }
}

package org.mage.test.cards.abilities.keywords;

import mage.abilities.costs.common.DiscardTargetCost;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.filter.common.FilterLandCard;
import mage.target.common.TargetCardInHand;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * A discard paid as a cost says so: jump-start (702.133a), retrace (702.81a) and every "discard a card:" cost built on
 * DiscardTargetCost asked a bare "Select a card" / "Select a land card", a pick that reads as any choice to the person
 * and to a seat that answers for him. DiscardCardCost already said "(discard cost)".
 */
public class DiscardCostHintTest extends CardTestPlayerBase {

    @Test
    public void testBareDiscardCostSaysItIsADiscard() {
        Assert.assertEquals("Select a card (discard cost)", new DiscardTargetCost(new TargetCardInHand()).getTargets().get(0).getMessage(null));
        Assert.assertEquals("Select a land card (discard cost)", new DiscardTargetCost(new TargetCardInHand(new FilterLandCard())).getTargets().get(0).getMessage(null));
    }

    @Test
    public void testOwnHintKept() {
        TargetCardInHand target = new TargetCardInHand();
        target.withChooseHint("to fuel it");
        Assert.assertEquals("to fuel it", new DiscardTargetCost(target).getTargets().get(0).getChooseHint());
    }

    @Test
    public void testCopyKeepsTheHint() {
        DiscardTargetCost cost = new DiscardTargetCost(new TargetCardInHand());
        Assert.assertEquals("discard cost", cost.copy().getTargets().get(0).getChooseHint());
    }

    @Test
    public void testJumpStartStillCastsAndDiscards() {
        // Radical Idea {1}{U} instant: Draw a card. Jump-start
        addCard(Zone.GRAVEYARD, playerA, "Radical Idea", 1);
        addCard(Zone.HAND, playerA, "Grizzly Bears", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.LIBRARY, playerA, "Island", 3);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Radical Idea with jump-start");
        setChoice(playerA, "Grizzly Bears"); // the card discarded

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.BEGIN_COMBAT);
        execute();

        assertGraveyardCount(playerA, "Grizzly Bears", 1);
        assertExileCount(playerA, "Radical Idea", 1);
        assertHandCount(playerA, 1);
    }

    @Test
    public void testRetraceStillCastsAndDiscardsALand() {
        // Flame Jab {R} sorcery: 1 damage to any target. Retrace
        addCard(Zone.GRAVEYARD, playerA, "Flame Jab", 1);
        addCard(Zone.HAND, playerA, "Forest", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Flame Jab with retrace", playerB);
        setChoice(playerA, "Forest");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.BEGIN_COMBAT);
        execute();

        assertLife(playerB, 19);
        assertGraveyardCount(playerA, "Forest", 1);
        assertGraveyardCount(playerA, "Flame Jab", 1);
    }
}

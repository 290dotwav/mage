package org.mage.test.cards.single.hob;

import mage.constants.CardType;
import mage.constants.PhaseStep;
import mage.constants.SubType;
import mage.constants.Zone;
import mage.game.permanent.Permanent;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class SupperForSpidersTest extends CardTestPlayerBase {

    private static final String supper = "Supper for Spiders";

    @Test
    public void test_StealsDeadCreaturesAsFood() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Soul Warden"); // Whenever another creature enters, you gain 1 life.
        addCard(Zone.HAND, playerA, supper);
        addCard(Zone.HAND, playerA, "Lightning Bolt");
        addCard(Zone.BATTLEFIELD, playerB, "Llanowar Elves");
        addCard(Zone.GRAVEYARD, playerB, "Grizzly Bears"); // not from the battlefield this turn

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", "Llanowar Elves");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, supper);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.BEGIN_COMBAT);
        execute();

        assertPermanentCount(playerA, "Llanowar Elves", 1);
        assertGraveyardCount(playerB, "Grizzly Bears", 1);
        Permanent elves = getPermanent("Llanowar Elves", playerA);
        Assert.assertTrue(elves.isArtifact(currentGame));
        Assert.assertFalse(elves.isCreature(currentGame));
        Assert.assertTrue(elves.hasSubtype(SubType.FOOD, currentGame));
        Assert.assertFalse(elves.hasSubtype(SubType.ELF, currentGame));
        Assert.assertEquals(1, elves.getCardType(currentGame).size());
        Assert.assertTrue(elves.getCardType(currentGame).contains(CardType.ARTIFACT));
        // it never entered as a creature
        assertLife(playerA, 20);
    }

    @Test
    public void test_FoodAbility() {
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 4);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);
        addCard(Zone.HAND, playerA, supper);
        addCard(Zone.HAND, playerA, "Lightning Bolt");
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, supper);
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        activateAbility(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "{2}, {T}, Sacrifice");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 23);
        assertGraveyardCount(playerB, "Grizzly Bears", 1);
    }
}

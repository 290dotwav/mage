package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class DragonManReformedRobotTest extends CardTestPlayerBase {

    // Flying
    // Dragon Man's power is equal to the greatest mana value among noncreature permanents you control and noncreature cards in your graveyard.
    // You may cast this card from your graveyard by discarding a card in addition to paying its other costs.
    private static final String DRAGON_MAN = "Dragon Man, Reformed Robot";

    @Test
    public void test_Power() {
        addCard(Zone.BATTLEFIELD, playerA, DRAGON_MAN);
        addCard(Zone.BATTLEFIELD, playerA, "Mind Stone"); // 2
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant"); // creature: ignored
        addCard(Zone.GRAVEYARD, playerA, "Divination"); // 3
        addCard(Zone.GRAVEYARD, playerA, "Craw Wurm"); // creature: ignored

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertPowerToughness(playerA, DRAGON_MAN, 3, 5);
    }

    @Test
    public void test_CastFromGraveyard() {
        addCard(Zone.GRAVEYARD, playerA, DRAGON_MAN);
        addCard(Zone.HAND, playerA, "Swamp");
        addCard(Zone.BATTLEFIELD, playerA, "Tundra", 4);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, DRAGON_MAN);
        setChoice(playerA, "Swamp"); // discard

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, DRAGON_MAN, 1);
        assertGraveyardCount(playerA, "Swamp", 1);
        assertTappedCount("Tundra", true, 4);
    }

    @Test
    public void test_NoCardToDiscard() {
        addCard(Zone.GRAVEYARD, playerA, DRAGON_MAN);
        addCard(Zone.BATTLEFIELD, playerA, "Tundra", 4);

        checkPlayableAbility("can't cast", 1, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast " + DRAGON_MAN, false);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
    }
}

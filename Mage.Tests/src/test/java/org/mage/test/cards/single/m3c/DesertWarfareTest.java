package org.mage.test.cards.single.m3c;

import mage.abilities.keyword.HasteAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author ClaudeMTG
 */
public class DesertWarfareTest extends CardTestPlayerBase {

    private static final String warfare = "Desert Warfare";
    // {3}{G} Enchantment
    // Whenever you sacrifice a Desert and whenever a Desert card is put into your graveyard from your
    //   hand or library, put that card onto the battlefield under your control at the beginning of your
    //   next end step.
    // At the beginning of combat on your turn, if you control five or more Deserts, create that many
    //   1/1 red, green, and white Sand Warrior creature tokens. They gain haste.
    private static final String desert = "Desert of the Indomitable";

    @Test
    public void testSacrificedDesertComesBackAtEndStep() {
        addCard(Zone.BATTLEFIELD, playerA, warfare);
        addCard(Zone.BATTLEFIELD, playerA, desert);
        // {T}, Sacrifice a land: You gain 2 life.
        addCard(Zone.BATTLEFIELD, playerA, "Zuran Orb");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Sacrifice a land: You gain 2 life");
        setChoice(playerA, desert);

        checkPermanentCount("gone after sacrifice", 1, PhaseStep.BEGIN_COMBAT, playerA, desert, 0);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.UPKEEP);
        execute();

        assertLife(playerA, 20 + 2);
        assertPermanentCount(playerA, desert, 1);
        assertGraveyardCount(playerA, 0);
    }

    @Test
    public void testDiscardedDesertComesBackAtEndStep() {
        addCard(Zone.BATTLEFIELD, playerA, warfare);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);
        addCard(Zone.HAND, playerA, desert);
        // As an additional cost to cast this spell, discard a card. Draw two cards.
        addCard(Zone.HAND, playerA, "Tormenting Voice");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Tormenting Voice");
        setChoice(playerA, desert); // discard it

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.UPKEEP);
        execute();

        assertPermanentCount(playerA, desert, 1);
        assertGraveyardCount(playerA, desert, 0);
    }

    @Test
    public void testFiveDesertsMakeHastySandWarriors() {
        addCard(Zone.BATTLEFIELD, playerA, warfare);
        addCard(Zone.BATTLEFIELD, playerA, desert, 5);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Sand Warrior Token", 5);
        assertAbility(playerA, "Sand Warrior Token", HasteAbility.getInstance(), true, 5);
    }

    @Test
    public void testFourDesertsMakeNothing() {
        addCard(Zone.BATTLEFIELD, playerA, warfare);
        addCard(Zone.BATTLEFIELD, playerA, desert, 4);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Sand Warrior Token", 0);
    }
}

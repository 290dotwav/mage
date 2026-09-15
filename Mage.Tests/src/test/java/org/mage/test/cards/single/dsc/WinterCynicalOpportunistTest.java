package org.mage.test.cards.single.dsc;

import mage.abilities.keyword.DeathtouchAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.counters.CounterType;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author ClaudeMTG
 */
public class WinterCynicalOpportunistTest extends CardTestPlayerBase {

    private static final String winter = "Winter, Cynical Opportunist";
    // {2}{B}{G} Legendary Creature - Human Warlock 2/5
    // Deathtouch
    // Whenever Winter attacks, mill three cards.
    // Delirium - At the beginning of your end step, you may exile any number of cards from your
    //   graveyard with four or more card types among them. If you do, put a permanent card from
    //   among them onto the battlefield with a finality counter on it.

    @Test
    public void testDeathtouchAndAttackMill() {
        addCard(Zone.BATTLEFIELD, playerA, winter);

        attack(1, playerA, winter);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertAbility(playerA, winter, DeathtouchAbility.getInstance(), true);
        assertLife(playerB, 20 - 2);
        assertGraveyardCount(playerA, 3);
    }

    @Test
    public void testDeliriumReanimatesWithFinalityCounter() {
        addCard(Zone.BATTLEFIELD, playerA, winter);
        // four card types in the graveyard
        addCard(Zone.GRAVEYARD, playerA, "Sol Ring"); // artifact
        addCard(Zone.GRAVEYARD, playerA, "Grizzly Bears"); // creature
        addCard(Zone.GRAVEYARD, playerA, "Rancor"); // enchantment
        addCard(Zone.GRAVEYARD, playerA, "Lightning Bolt"); // instant

        setChoice(playerA, true); // use the delirium ability
        setChoice(playerA, "Sol Ring^Grizzly Bears^Rancor^Lightning Bolt"); // exile them
        setChoice(playerA, "Sol Ring"); // permanent card to put onto the battlefield

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.UPKEEP);
        execute();

        assertPermanentCount(playerA, "Sol Ring", 1);
        assertCounterCount(playerA, "Sol Ring", CounterType.FINALITY, 1);
        assertGraveyardCount(playerA, 0);
        assertExileCount(playerA, 3);
    }

    @Test
    public void testDeliriumDoesNothingWithoutFourTypes() {
        addCard(Zone.BATTLEFIELD, playerA, winter);
        addCard(Zone.GRAVEYARD, playerA, "Grizzly Bears"); // creature
        addCard(Zone.GRAVEYARD, playerA, "Rancor"); // enchantment

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.UPKEEP);
        execute();

        assertGraveyardCount(playerA, 2);
        assertExileCount(playerA, 0);
    }
}

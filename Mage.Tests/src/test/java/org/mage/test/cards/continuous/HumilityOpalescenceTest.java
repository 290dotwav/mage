package org.mage.test.cards.continuous;

import mage.abilities.keyword.FlyingAbility;
import mage.constants.CardType;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * Humility and Opalescence, the classic of the layers (613.6, 613.7). Opalescence makes Humility a
 * creature in layer 4; Humility then takes every creature's abilities away in layer 6, its own
 * included, and its effect, having started, still sets every creature to 1/1 in layer 7b. Which
 * of the two sets Humility's own size in 7b is decided by their timestamps.
 * <p>
 * The engine dropped Humility's effect from layer 7 once Humility had lost its ability: every
 * creature kept its size (a Serra Angel 4/4 with no abilities), Humility 4/4 whatever the order.
 */
public class HumilityOpalescenceTest extends CardTestPlayerBase {

    @Test
    public void opalescenceAfterHumility() {
        addCard(Zone.BATTLEFIELD, playerA, "Humility");
        addCard(Zone.HAND, playerA, "Opalescence"); // {2}{W}{W}
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 4);
        addCard(Zone.BATTLEFIELD, playerB, "Serra Angel");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Opalescence");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        // Opalescence's 7b is the later: Humility a 4/4 creature with no abilities, every other creature 1/1.
        assertType("Humility", CardType.CREATURE, true);
        assertPowerToughness(playerA, "Humility", 4, 4);
        assertPowerToughness(playerB, "Serra Angel", 1, 1);
        assertAbility(playerB, "Serra Angel", FlyingAbility.getInstance(), false);
        assertType("Opalescence", CardType.CREATURE, false);
    }

    @Test
    public void humilityAfterOpalescence() {
        addCard(Zone.BATTLEFIELD, playerA, "Opalescence");
        addCard(Zone.HAND, playerA, "Humility"); // {2}{W}{W}
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 4);
        addCard(Zone.BATTLEFIELD, playerB, "Serra Angel");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Humility");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        // Humility's 7b is the later: every creature 1/1 with no abilities, Humility too.
        assertType("Humility", CardType.CREATURE, true);
        assertPowerToughness(playerA, "Humility", 1, 1);
        assertPowerToughness(playerB, "Serra Angel", 1, 1);
        assertAbility(playerB, "Serra Angel", FlyingAbility.getInstance(), false);
    }
}

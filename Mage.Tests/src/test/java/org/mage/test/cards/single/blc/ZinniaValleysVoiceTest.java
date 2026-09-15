package org.mage.test.cards.single.blc;

import mage.abilities.keyword.FlyingAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author ClaudeMTG
 */
public class ZinniaValleysVoiceTest extends CardTestPlayerBase {

    private static final String zinnia = "Zinnia, Valley's Voice";
    // {U}{R}{W} Legendary Creature - Bird Bard 1/3
    // Flying
    // Zinnia gets +X/+0, where X is the number of other creatures you control with base power 1.
    // Creature spells you cast gain offspring {2} as you cast them.

    @Test
    public void testNoBoostWithoutBasePowerOneCreatures() {
        addCard(Zone.BATTLEFIELD, playerA, zinnia);
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant"); // 3/3, base power 3, doesn't count

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPowerToughness(playerA, zinnia, 1, 3);
    }

    @Test
    public void testBoostFromBasePowerOneCreatures() {
        addCard(Zone.BATTLEFIELD, playerA, zinnia);
        addCard(Zone.BATTLEFIELD, playerA, "Memnite", 2); // 1/1
        addCard(Zone.BATTLEFIELD, playerA, "Hill Giant"); // 3/3, base power 3, doesn't count

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertAbility(playerA, zinnia, FlyingAbility.getInstance(), true);
        assertPowerToughness(playerA, zinnia, 1 + 2, 3);
    }

    @Test
    public void testCreatureSpellsGainOffspring() {
        addCard(Zone.BATTLEFIELD, playerA, zinnia);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 4);
        addCard(Zone.HAND, playerA, "Grizzly Bears"); // {1}{G} 2/2

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");
        setChoice(playerA, true); // pay the offspring cost

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertTappedCount("Forest", true, 4);
        // the real bear plus its 1/1 token copy
        assertPermanentCount(playerA, "Grizzly Bears", 2);
        assertPowerToughness(playerA, "Grizzly Bears", 1, 1, mage.filter.Filter.ComparisonScope.Any);
    }

    @Test
    public void testOffspringIsOptional() {
        addCard(Zone.BATTLEFIELD, playerA, zinnia);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 4);
        addCard(Zone.HAND, playerA, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");
        setChoice(playerA, false); // do not pay the offspring cost

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertTappedCount("Forest", true, 2);
        assertPermanentCount(playerA, "Grizzly Bears", 1);
    }
}

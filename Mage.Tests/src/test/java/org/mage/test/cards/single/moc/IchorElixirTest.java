package org.mage.test.cards.single.moc;

import mage.constants.PhaseStep;
import mage.constants.Planes;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author ClaudeMTG
 */
public class IchorElixirTest extends CardTestPlayerBase {

    private static final String elixir = "Ichor Elixir";
    // If you would roll one or more planar dice, instead roll that many planar dice plus one and ignore one.
    // {T}: Add {C}{C}.

    /**
     * Without the Elixir only a single planar die is rolled, so a blank roll stays a blank roll.
     */
    @Test
    public void testNoExtraRollWithoutElixir() {
        // Whenever you roll {CHAOS}, create a 7/7 colorless Eldrazi creature token with annihilator 1
        addPlane(playerA, Planes.PLANE_HEDRON_FIELDS_OF_AGADEEM);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{0}: Roll the planar");
        setDieRollResult(playerA, 5); // blank

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Eldrazi Token", 0);
    }

    /**
     * With the Elixir two planar dice are rolled and one of them is ignored, so the blank roll
     * can be thrown away and the chaos roll kept.
     */
    @Test
    public void testExtraRollKeepChaos() {
        addPlane(playerA, Planes.PLANE_HEDRON_FIELDS_OF_AGADEEM);
        addCard(Zone.BATTLEFIELD, playerA, elixir);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{0}: Roll the planar");
        setDieRollResult(playerA, 5); // blank
        setDieRollResult(playerA, 1); // chaos
        setChoice(playerA, "Chaos Roll"); // ignore the blank one

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, elixir, 1);
        assertPermanentCount(playerA, "Eldrazi Token", 1);
    }

    /**
     * The extra roll is a real choice, the ignored roll never happened.
     */
    @Test
    public void testExtraRollKeepBlank() {
        addPlane(playerA, Planes.PLANE_HEDRON_FIELDS_OF_AGADEEM);
        addCard(Zone.BATTLEFIELD, playerA, elixir);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{0}: Roll the planar");
        setDieRollResult(playerA, 5); // blank
        setDieRollResult(playerA, 1); // chaos
        setChoice(playerA, "Blank Roll"); // ignore the chaos one

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Eldrazi Token", 0);
    }

    /**
     * {T}: Add {C}{C}.
     */
    @Test
    public void testManaAbility() {
        addCard(Zone.BATTLEFIELD, playerA, elixir);
        addCard(Zone.HAND, playerA, "Ornithopter"); // {0}
        addCard(Zone.HAND, playerA, "Alpha Myr"); // {2}

        activateManaAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: Add {C}{C}");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Alpha Myr");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Alpha Myr", 1);
        assertTapped(elixir, true);
    }
}

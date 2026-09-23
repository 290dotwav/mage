package org.mage.test.cards.single.msc;

import mage.ObjectColor;
import mage.constants.PhaseStep;
import mage.constants.SubType;
import mage.constants.Zone;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class SauronDinoDevoteeTest extends CardTestPlayerBase {

    // Flying
    // Whenever Sauron enters or attacks, choose one --
    // * Cure Cancer -- You gain 3 life.
    // * Turn People into Dinosaurs -- Put a saurian counter on another target creature. It's a green Dinosaur with
    //   base power and toughness 5/5 for as long as it has a saurian counter on it.
    private static final String SAURON = "Sauron, Dino Devotee";

    @Test
    public void test_DinosaurUntilCounterRemoved() {
        addCard(Zone.HAND, playerA, SAURON);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 5);
        addCard(Zone.BATTLEFIELD, playerB, "Suntail Hawk"); // 1/1 white Bird, flying
        // Sacrifice Vampire Hexmage: Remove all counters from target permanent.
        addCard(Zone.BATTLEFIELD, playerB, "Vampire Hexmage");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, SAURON);
        setModeChoice(playerA, "2");
        addTarget(playerA, "Suntail Hawk");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);

        checkPT("5/5", 1, PhaseStep.PRECOMBAT_MAIN, playerB, "Suntail Hawk", 5, 5);
        checkSubType("dinosaur", 1, PhaseStep.PRECOMBAT_MAIN, playerB, "Suntail Hawk", SubType.DINOSAUR, true);
        checkSubType("no longer a bird", 1, PhaseStep.PRECOMBAT_MAIN, playerB, "Suntail Hawk", SubType.BIRD, false);
        checkColor("green", 1, PhaseStep.PRECOMBAT_MAIN, playerB, "Suntail Hawk", "G", true);
        checkColor("not white", 1, PhaseStep.PRECOMBAT_MAIN, playerB, "Suntail Hawk", "W", false);

        activateAbility(1, PhaseStep.POSTCOMBAT_MAIN, playerB, "Sacrifice", "Suntail Hawk");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPowerToughness(playerB, "Suntail Hawk", 1, 1);
        assertSubtype("Suntail Hawk", SubType.BIRD);
    }

    @Test
    public void test_GainLife() {
        addCard(Zone.HAND, playerA, SAURON);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 5);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, SAURON);
        setModeChoice(playerA, "1");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 23);
    }
}

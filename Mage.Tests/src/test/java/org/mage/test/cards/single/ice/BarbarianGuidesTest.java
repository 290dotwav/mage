package org.mage.test.cards.single.ice;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class BarbarianGuidesTest extends CardTestPlayerBase {

    private static final String guides = "Barbarian Guides";

    @Test
    public void test_SnowForestwalk_ThenReturn() {
        addCard(Zone.BATTLEFIELD, playerA, guides);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Snow-Covered Forest");
        addCard(Zone.BATTLEFIELD, playerB, "Wall of Wood");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}{R}, {T}: Choose", "Grizzly Bears");
        setChoice(playerA, "Forest");
        attack(1, playerA, "Grizzly Bears");
        block(1, playerB, "Wall of Wood", "Grizzly Bears"); // not allowed, ignored

        setStopAt(2, PhaseStep.UPKEEP);
        execute();

        assertLife(playerB, 18);
        assertHandCount(playerA, "Grizzly Bears", 1);
    }

    @Test
    public void test_NonSnowForestDoesNotCount() {
        addCard(Zone.BATTLEFIELD, playerA, guides);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerB, "Forest");
        addCard(Zone.BATTLEFIELD, playerB, "Wall of Wood");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}{R}, {T}: Choose", "Grizzly Bears");
        setChoice(playerA, "Forest");
        attack(1, playerA, "Grizzly Bears");
        block(1, playerB, "Wall of Wood", "Grizzly Bears");

        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertLife(playerB, 20);
    }
}

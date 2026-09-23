package org.mage.test.cards.single.msc;

import mage.abilities.keyword.FlyingAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class StormQueenOfWakandaTest extends CardTestPlayerBase {

    // Flying
    // Whenever Storm attacks, until end of turn, another target attacking creature gains flying and gets +X/+0, where X is Storm's power.
    // Whenever a creature with flying attacks you, Storm deals damage equal to her power to that creature.
    private static final String STORM = "Storm, Queen of Wakanda";

    @Test
    public void test_AttackBoost() {
        addCard(Zone.BATTLEFIELD, playerA, STORM);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");

        attack(1, playerA, STORM, playerB);
        attack(1, playerA, "Grizzly Bears", playerB);
        addTarget(playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertPowerToughness(playerA, "Grizzly Bears", 2 + 4, 2);
        assertAbility(playerA, "Grizzly Bears", FlyingAbility.getInstance(), true);
        assertLife(playerB, 20 - 4 - 6);
    }

    @Test
    public void test_FlyerAttacksYou() {
        addCard(Zone.BATTLEFIELD, playerA, STORM);
        addCard(Zone.BATTLEFIELD, playerB, "Serra Angel"); // 4/4 flying
        addCard(Zone.BATTLEFIELD, playerB, "Grizzly Bears");

        attack(2, playerB, "Serra Angel", playerA);
        attack(2, playerB, "Grizzly Bears", playerA);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerB, "Serra Angel", 1);
        assertPermanentCount(playerB, "Grizzly Bears", 1);
        assertLife(playerA, 20 - 2);
    }
}

package org.mage.test.cards.single.msc;

import mage.abilities.keyword.FlyingAbility;
import mage.abilities.keyword.VigilanceAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class HulklingYoungAvengerTest extends CardTestPlayerBase {

    // Flying
    // Whenever you cast a noncreature spell, Hulkling becomes a copy of up to one other target creature until end of
    // turn, except his name is Hulkling, Young Avenger, he's 4/4, and he has flying and this ability.
    private static final String HULKLING = "Hulkling, Young Avenger";

    @Test
    public void test_CopyKeeps44FlyingAndAbility() {
        addCard(Zone.BATTLEFIELD, playerA, HULKLING);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);
        addCard(Zone.HAND, playerA, "Lightning Bolt", 2);
        addCard(Zone.BATTLEFIELD, playerB, "Wall of Wood"); // 0/3 defender
        addCard(Zone.BATTLEFIELD, playerB, "Serra Angel"); // 4/4 flying vigilance

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", playerB);
        addTarget(playerA, "Wall of Wood");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPT("4/4 wall", 1, PhaseStep.PRECOMBAT_MAIN, playerA, HULKLING, 4, 4);
        checkAbility("flying", 1, PhaseStep.PRECOMBAT_MAIN, playerA, HULKLING, FlyingAbility.class, true);

        // still has the ability: copies again
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Lightning Bolt", playerB);
        addTarget(playerA, "Serra Angel");
        waitStackResolved(1, PhaseStep.POSTCOMBAT_MAIN);
        checkAbility("vigilance", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, HULKLING, VigilanceAbility.class, true);

        setStrictChooseMode(true);
        setStopAt(2, PhaseStep.UPKEEP);
        execute();

        assertPowerToughness(playerA, HULKLING, 4, 4);
        assertAbility(playerA, HULKLING, VigilanceAbility.getInstance(), false);
    }
}

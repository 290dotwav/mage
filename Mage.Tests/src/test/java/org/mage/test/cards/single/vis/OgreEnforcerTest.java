package org.mage.test.cards.single.vis;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class OgreEnforcerTest extends CardTestPlayerBase {

    private static final String ogre = "Ogre Enforcer"; // 4/4

    @Test
    public void test_SeveralSourcesDontKill() {
        addCard(Zone.BATTLEFIELD, playerB, ogre);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 4);
        addCard(Zone.HAND, playerA, "Shock", 2);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", ogre);
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", ogre);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertPermanentCount(playerB, ogre, 1);
        assertDamageReceived(playerB, ogre, 4);
    }

    @Test
    public void test_SingleSourceKills() {
        addCard(Zone.BATTLEFIELD, playerB, ogre);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 5);
        addCard(Zone.HAND, playerA, "Fireball");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Fireball", ogre);
        setChoice(playerA, "X=4");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertGraveyardCount(playerB, ogre, 1);
    }

    @Test
    public void test_ShrunkBelowOneSourcesDamage() {
        addCard(Zone.BATTLEFIELD, playerB, ogre);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Swamp", 1);
        addCard(Zone.HAND, playerA, "Lightning Bolt");
        addCard(Zone.HAND, playerA, "Shock");
        addCard(Zone.HAND, playerA, "Disfigure"); // -2/-2

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", ogre); // 3
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", ogre); // 2: 5 in total, no single source 4
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);
        checkPermanentCount("alive", 1, PhaseStep.PRECOMBAT_MAIN, playerB, ogre, 1);
        // 2/2 now: the Bolt's 3 is lethal from a single source
        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Disfigure", ogre);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerB, ogre, 1);
    }

    @Test
    public void test_DamageWearsOff() {
        addCard(Zone.BATTLEFIELD, playerB, ogre);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 5);
        addCard(Zone.HAND, playerA, "Lightning Bolt");
        addCard(Zone.HAND, playerA, "Shock");

        // turn 1: 3 damage from the Bolt, wears off; turn 3: Shock 2 + ... still fine
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Lightning Bolt", ogre);
        castSpell(3, PhaseStep.PRECOMBAT_MAIN, playerA, "Shock", ogre);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerB, ogre, 1);
    }
}

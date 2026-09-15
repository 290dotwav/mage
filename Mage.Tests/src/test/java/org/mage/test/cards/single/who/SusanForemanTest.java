package org.mage.test.cards.single.who;

import mage.abilities.keyword.DoctorsCompanionAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author ClaudeMTG
 */
public class SusanForemanTest extends CardTestPlayerBase {

    private static final String susan = "Susan Foreman";
    // {1}{G} Legendary Creature - Time Lord 1/1
    // If you would planeswalk, instead look at the top two cards of your planar deck, put one on the
    //   bottom of your planar deck and the other on top, then planeswalk. (not implemented, no planar deck)
    // {T}: Add {G}.
    // Doctor's companion

    @Test
    public void testCastAndManaAbility() {
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);
        addCard(Zone.HAND, playerA, susan);
        // {G} creature, so it can only be cast off Susan's mana
        addCard(Zone.HAND, playerA, "Llanowar Elves");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, susan);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, susan, 1);
        assertPowerToughness(playerA, susan, 1, 1);
        assertAbility(playerA, susan, DoctorsCompanionAbility.getInstance(), true);
    }

    @Test
    public void testTapsForGreen() {
        addCard(Zone.BATTLEFIELD, playerA, susan);
        addCard(Zone.HAND, playerA, "Llanowar Elves"); // {G}

        activateManaAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: Add {G}");
        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Llanowar Elves");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Llanowar Elves", 1);
        assertTapped(susan, true);
    }
}

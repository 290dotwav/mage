package org.mage.test.cards.single.trc;

import mage.abilities.keyword.ProtectionAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class BorgIncursionTest extends CardTestPlayerBase {

    private static final String incursion = "Borg Incursion";

    // two Borgs, then every artifact creature he controls (the new Borgs too) has protection from the chosen colour; a nonartifact one does not
    @Test
    public void test_TokensThenProtection() {
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Alpha Myr");
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, incursion);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, incursion);
        setChoice(playerA, "Red");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        assertPermanentCount(playerA, "Borg Token", 2);
        assertAbilityCount(playerA, "Borg Token", ProtectionAbility.class, 1);
        assertAbilityCount(playerA, "Alpha Myr", ProtectionAbility.class, 1);
        assertAbilityCount(playerA, "Grizzly Bears", ProtectionAbility.class, 0);
    }
}

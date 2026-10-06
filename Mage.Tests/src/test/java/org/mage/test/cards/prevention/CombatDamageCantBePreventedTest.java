package org.mage.test.cards.prevention;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * Frenzied Baloth: "Combat damage can't be prevented." It is a rule-modifying
 * effect on the PREVENT_DAMAGE event, so a prevention effect still applies to
 * the damage but prevents none of it (615.12). A prevention effect that
 * answered such damage by replacing the whole damage event (returning true
 * from replaceEvent) dealt nothing at all: Comeuppance stopped a lethal
 * attack of Plants made unpreventable by a Frenzied Baloth.
 */
public class CombatDamageCantBePreventedTest extends CardTestPlayerBase {

    private static final String BALOTH = "Frenzied Baloth"; // 3/1 trample, haste

    @Test
    public void comeuppanceCantStopUnpreventableCombatDamage() {
        addCard(Zone.BATTLEFIELD, playerA, BALOTH);
        // Prevent all damage that would be dealt to you and planeswalkers you control this turn by sources you
        // don't control. If damage from a creature source is prevented this way, Comeuppance deals that much
        // damage to that creature. ...
        addCard(Zone.HAND, playerB, "Comeuppance");
        addCard(Zone.BATTLEFIELD, playerB, "Plains", 4);

        attack(1, playerA, BALOTH);
        castSpell(1, PhaseStep.DECLARE_ATTACKERS, playerB, "Comeuppance");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertGraveyardCount(playerB, "Comeuppance", 1);
        assertLife(playerB, 20 - 3);
        // nothing was prevented, so Comeuppance deals nothing back
        assertPermanentCount(playerA, BALOTH, 1);
        assertDamageReceived(playerA, BALOTH, 0);
    }

    @Test
    public void comeuppanceStillPreventsAndDealsBackWithoutBaloth() {
        addCard(Zone.BATTLEFIELD, playerA, "Raging Goblin"); // 1/1 haste
        addCard(Zone.HAND, playerB, "Comeuppance");
        addCard(Zone.BATTLEFIELD, playerB, "Plains", 4);

        attack(1, playerA, "Raging Goblin");
        castSpell(1, PhaseStep.DECLARE_ATTACKERS, playerB, "Comeuppance");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertLife(playerB, 20);
        assertGraveyardCount(playerA, "Raging Goblin", 1);
    }

    @Test
    public void obscuringHazeCantStopUnpreventableCombatDamage() {
        addCard(Zone.BATTLEFIELD, playerA, BALOTH);
        // Prevent all damage that would be dealt this turn by creatures your opponents control.
        addCard(Zone.HAND, playerB, "Obscuring Haze");
        addCard(Zone.BATTLEFIELD, playerB, "Forest", 3);

        attack(1, playerA, BALOTH);
        castSpell(1, PhaseStep.DECLARE_ATTACKERS, playerB, "Obscuring Haze");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertGraveyardCount(playerB, "Obscuring Haze", 1);
        assertLife(playerB, 20 - 3);
    }

    @Test
    public void fogCantStopUnpreventableCombatDamage() {
        addCard(Zone.BATTLEFIELD, playerA, BALOTH);
        addCard(Zone.HAND, playerB, "Fog");
        addCard(Zone.BATTLEFIELD, playerB, "Forest", 1);

        attack(1, playerA, BALOTH);
        castSpell(1, PhaseStep.DECLARE_ATTACKERS, playerB, "Fog");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertGraveyardCount(playerB, "Fog", 1);
        assertLife(playerB, 20 - 3);
    }

    @Test
    public void inkshieldMakesNoTokensAndTakesTheDamage() {
        addCard(Zone.BATTLEFIELD, playerA, BALOTH);
        // Prevent all combat damage that would be dealt to you this turn. For each 1 damage prevented this way,
        // create a 2/1 white and black Inkling creature token with flying.
        addCard(Zone.HAND, playerB, "Inkshield");
        addCard(Zone.BATTLEFIELD, playerB, "Plains", 3);
        addCard(Zone.BATTLEFIELD, playerB, "Swamp", 2);

        attack(1, playerA, BALOTH);
        castSpell(1, PhaseStep.DECLARE_ATTACKERS, playerB, "Inkshield");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertLife(playerB, 20 - 3);
        assertPermanentCount(playerB, "Inkling Token", 0);
    }

    @Test
    public void swansDieToUnpreventableCombatDamage() {
        addCard(Zone.BATTLEFIELD, playerA, BALOTH);
        // 4/3 Flying. If a source would deal damage to this creature, prevent that damage. The source's
        // controller draws cards equal to the damage prevented this way.
        addCard(Zone.BATTLEFIELD, playerB, "Swans of Bryn Argoll");

        attack(1, playerA, BALOTH);
        block(1, playerB, "Swans of Bryn Argoll", BALOTH);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertGraveyardCount(playerB, "Swans of Bryn Argoll", 1);
        assertGraveyardCount(playerA, BALOTH, 1);
        assertHandCount(playerA, 0);
    }

    @Test
    public void mindskinnerStillMillsWhenTheDamageIsDealt() {
        addCard(Zone.BATTLEFIELD, playerA, BALOTH);
        // If a source you control would deal damage to an opponent, prevent that damage and each opponent mills
        // that many cards. The milling is an additional effect: it still happens (615.12).
        addCard(Zone.BATTLEFIELD, playerA, "The Mindskinner");

        attack(1, playerA, BALOTH);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_COMBAT);
        execute();

        assertLife(playerB, 20 - 3);
        assertGraveyardCount(playerB, 3);
    }
}

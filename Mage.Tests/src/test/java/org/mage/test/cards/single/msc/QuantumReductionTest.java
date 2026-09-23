package org.mage.test.cards.single.msc;

import mage.abilities.keyword.FlyingAbility;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

public class QuantumReductionTest extends CardTestPlayerBase {

    private static final String reduction = "Quantum Reduction";

    @Test
    public void test_FlashWithTeamwork_OnOpponentsTurn() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears"); // power 2
        addCard(Zone.HAND, playerA, reduction);
        addCard(Zone.BATTLEFIELD, playerB, "Serra Angel"); // 4/4 flying, vigilance

        attack(2, playerB, "Serra Angel", playerA);
        castSpell(2, PhaseStep.DECLARE_ATTACKERS, playerA, reduction, "Serra Angel");
        setChoice(playerA, "Grizzly Bears"); // teamwork is mandatory: tap creatures with total power 2

        setStopAt(2, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertPermanentCount(playerA, reduction, 1);
        assertTapped("Grizzly Bears", true);
        assertPowerToughness(playerB, "Serra Angel", -1, 4);
        assertAbility(playerB, "Serra Angel", FlyingAbility.getInstance(), false);
        assertLife(playerA, 20);
    }

    @Test
    public void test_NoTeamwork_NoFlash() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Memnite"); // power 1: can't pay teamwork 2
        addCard(Zone.HAND, playerA, reduction);
        addCard(Zone.BATTLEFIELD, playerB, "Serra Angel");

        checkPlayableAbility("no flash without teamwork", 2, PhaseStep.PRECOMBAT_MAIN, playerA, "Cast " + reduction, false);

        setStopAt(2, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();
    }

    @Test
    public void test_SorcerySpeed_TeamworkOptional() {
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears");
        addCard(Zone.HAND, playerA, reduction);
        addCard(Zone.BATTLEFIELD, playerB, "Serra Angel");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, reduction, "Serra Angel");
        setChoice(playerA, false); // don't pay teamwork

        setStopAt(1, PhaseStep.END_TURN);
        setStrictChooseMode(true);
        execute();

        assertTapped("Grizzly Bears", false);
        assertPowerToughness(playerB, "Serra Angel", -1, 4);
    }
}

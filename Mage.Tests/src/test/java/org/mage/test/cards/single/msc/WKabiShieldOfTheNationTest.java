package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestCommander4Players;

public class WKabiShieldOfTheNationTest extends CardTestCommander4Players {

    private static final String wkabi = "W'Kabi, Shield of the Nation";

    @Test
    public void test_AttackWithCommander_WithBigArtifact() {
        addCard(Zone.COMMAND, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);
        addCard(Zone.BATTLEFIELD, playerA, wkabi);
        addCard(Zone.BATTLEFIELD, playerA, "Thran Dynamo"); // artifact, mana value 4
        addCard(Zone.BATTLEFIELD, playerA, "Bronze Sable"); // artifact, mana value 2

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");
        attack(5, playerA, "Grizzly Bears", playerB);

        setStopAt(5, PhaseStep.END_COMBAT);
        setStrictChooseMode(true);
        execute();

        assertPermanentCount(playerA, "Rhino Token", 1);
    }

    @Test
    public void test_AttackWithCommander_WithoutBigArtifact_NoToken() {
        addCard(Zone.COMMAND, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);
        addCard(Zone.BATTLEFIELD, playerA, wkabi);
        addCard(Zone.BATTLEFIELD, playerA, "Bronze Sable"); // artifact, mana value 2

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");
        attack(5, playerA, "Grizzly Bears", playerB);

        setStopAt(5, PhaseStep.END_COMBAT);
        setStrictChooseMode(true);
        execute();

        assertPermanentCount(playerA, "Rhino Token", 0);
    }

    @Test
    public void test_AttackWithoutCommander_NoToken() {
        addCard(Zone.BATTLEFIELD, playerA, wkabi);
        addCard(Zone.BATTLEFIELD, playerA, "Thran Dynamo");

        attack(5, playerA, wkabi, playerB);

        setStopAt(5, PhaseStep.END_COMBAT);
        setStrictChooseMode(true);
        execute();

        assertPermanentCount(playerA, "Rhino Token", 0);
    }
}

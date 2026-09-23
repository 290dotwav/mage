package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class VirtualAssistantTest extends CardTestPlayerBase {

    // Defender
    // Whenever you cast a spell using teamwork, create a 1/1 colorless Robot Hero artifact creature token with flying.
    private static final String ASSISTANT = "Virtual Assistant";
    // Teamwork 3; Choose one. If this spell was cast using teamwork, choose both instead.
    // * Put a +1/+1 counter on target creature. * Target creature you control fights target creature an opponent controls.
    private static final String GO_NUTS = "Go Nuts!";

    @Test
    public void test_WithTeamwork() {
        addCard(Zone.BATTLEFIELD, playerA, ASSISTANT);
        addCard(Zone.BATTLEFIELD, playerB, "Memnite");
        addCard(Zone.BATTLEFIELD, playerA, "Forest");
        addCard(Zone.HAND, playerA, GO_NUTS);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, GO_NUTS);
        setChoice(playerA, true); // pay teamwork 3
        setModeChoice(playerA, "1");
        setModeChoice(playerA, "2");
        addTarget(playerA, ASSISTANT); // +1/+1 counter
        addTarget(playerA, ASSISTANT); // fight: yours
        addTarget(playerA, "Memnite"); // fight: opponent's
        setChoice(playerA, ASSISTANT); // teamwork creature, power 3

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.BEGIN_COMBAT);
        execute();

        assertPermanentCount(playerA, "Robot Hero Token", 1);
        assertGraveyardCount(playerB, "Memnite", 1);
    }

    @Test
    public void test_WithoutTeamwork() {
        addCard(Zone.BATTLEFIELD, playerA, ASSISTANT);
        addCard(Zone.BATTLEFIELD, playerA, "Forest");
        addCard(Zone.HAND, playerA, GO_NUTS);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, GO_NUTS);
        setChoice(playerA, false); // don't pay teamwork
        setModeChoice(playerA, "1");
        addTarget(playerA, ASSISTANT);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.BEGIN_COMBAT);
        execute();

        assertPermanentCount(playerA, "Robot Hero Token", 0);
        assertPowerToughness(playerA, ASSISTANT, 4, 4);
    }
}

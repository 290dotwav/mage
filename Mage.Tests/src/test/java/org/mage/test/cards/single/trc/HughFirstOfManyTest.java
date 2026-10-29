package org.mage.test.cards.single.trc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class HughFirstOfManyTest extends CardTestPlayerBase {

    private static final String hugh = "Hugh, First of Many";

    // an artifact spell with replicate {2}{U} paid once: two of it, the copy a token
    @Test
    public void test_ArtifactReplicate() {
        addCard(Zone.BATTLEFIELD, playerA, hugh);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 3);
        addCard(Zone.HAND, playerA, "Ornithopter");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Ornithopter");
        setChoice(playerA, "Yes"); // replicate
        setChoice(playerA, "No"); // only once

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Ornithopter", 2);
        assertTappedCount("Island", true, 3);
    }

    // not on a nonartifact spell
    @Test
    public void test_NotOnNonArtifact() {
        addCard(Zone.BATTLEFIELD, playerA, hugh);
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 5);
        addCard(Zone.HAND, playerA, "Grizzly Bears");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Grizzly Bears", 1);
    }


    // replicate paid twice: three of them, the two copies tokens
    @Test
    public void test_ArtifactReplicateTwice() {
        addCard(Zone.BATTLEFIELD, playerA, hugh);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 6);
        addCard(Zone.HAND, playerA, "Ornithopter");

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Ornithopter");
        setChoice(playerA, "Yes"); // replicate
        setChoice(playerA, "Yes"); // twice
        setChoice(playerA, "No");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertPermanentCount(playerA, "Ornithopter", 3);
        assertTokenCount(playerA, "Ornithopter", 2);
        assertTappedCount("Island", true, 6);
    }
}

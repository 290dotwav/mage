package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class ScarletWitchChaoticAvengerTest extends CardTestPlayerBase {

    // Flying
    // Whenever Scarlet Witch deals combat damage to a player, look at the top two cards of your library, then exile
    // them face down. Then you may cast a Hero or noncreature spell from among cards exiled with Scarlet Witch without paying its mana cost.
    private static final String WITCH = "Scarlet Witch, Chaotic Avenger";

    @Test
    public void test_CastNoncreatureFree() {
        skipInitShuffling();
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Grizzly Bears"); // creature, not a Hero: not castable
        addCard(Zone.LIBRARY, playerA, "Lightning Bolt");
        addCard(Zone.BATTLEFIELD, playerA, WITCH);

        attack(1, playerA, WITCH, playerB);
        setChoice(playerA, true); // cast Lightning Bolt for free
        addTarget(playerA, playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 3 - 3);
        assertGraveyardCount(playerA, "Lightning Bolt", 1);
        assertExileCount(playerA, "Grizzly Bears", 1);
    }

    @Test
    public void test_EarlierExiledCardStillAvailable() {
        skipInitShuffling();
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Island");
        addCard(Zone.LIBRARY, playerA, "Island");
        addCard(Zone.LIBRARY, playerA, "Grizzly Bears");
        addCard(Zone.LIBRARY, playerA, "Lightning Bolt");
        addCard(Zone.BATTLEFIELD, playerA, WITCH);

        attack(1, playerA, WITCH, playerB);
        setChoice(playerA, false); // keep the Bolt in exile
        // turn 3: the draw takes an Island and the last Island is exiled; the Bolt exiled on turn 1 is still
        // among the cards exiled with her
        attack(3, playerA, WITCH, playerB);
        setChoice(playerA, true);
        addTarget(playerA, playerB);

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 3 - 3 - 3);
    }
}

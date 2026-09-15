package org.mage.test.cards.single.mkc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author ClaudeMTG
 */
public class EyeOfDuskmantleTest extends CardTestPlayerBase {

    private static final String eye = "Eye of Duskmantle";
    // {5}{B}{B} Creature - Eye 3/8
    // Flying, lifelink
    // You may play lands and cast spells from among cards in your graveyard you've surveilled this turn.
    // If you cast a spell this way, you pay life equal to its mana value rather than paying its mana cost.
    private static final String curate = "Curate"; // {U} instant, surveil 2, draw a card

    private void initLibrary() {
        skipInitShuffling();
        removeAllCardsFromLibrary(playerA);
        // bottom : Forest, Lightning Bolt, Grizzly Bears : top
        addCard(Zone.LIBRARY, playerA, "Forest");
        addCard(Zone.LIBRARY, playerA, "Lightning Bolt"); // {R}, mana value 1
        addCard(Zone.LIBRARY, playerA, "Grizzly Bears");
    }

    @Test
    public void testCastSurveilledCardFromGraveyardForLife() {
        initLibrary();
        addCard(Zone.BATTLEFIELD, playerA, eye);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.HAND, playerA, curate);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, curate);
        addTarget(playerA, "Grizzly Bears^Lightning Bolt"); // surveil both into the graveyard

        castSpell(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Lightning Bolt", playerB);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 3);
        assertLife(playerA, 20 - 1); // 1 life instead of {R}
        assertTappedCount("Island", true, 2); // only Curate was paid with mana
        assertGraveyardCount(playerA, "Lightning Bolt", 1);
    }

    @Test
    public void testPlayLandSurveilledIntoTheGraveyard() {
        skipInitShuffling();
        removeAllCardsFromLibrary(playerA);
        addCard(Zone.LIBRARY, playerA, "Grizzly Bears"); // bottom
        addCard(Zone.LIBRARY, playerA, "Forest"); // top
        addCard(Zone.BATTLEFIELD, playerA, eye);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.HAND, playerA, curate);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, curate);
        addTarget(playerA, "Forest"); // surveil the Forest into the graveyard

        playLand(1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Forest");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20); // no life paid for a land
        assertPermanentCount(playerA, "Forest", 1);
    }

    @Test
    public void testCardMilledInsteadOfSurveilledIsNotCastable() {
        initLibrary();
        addCard(Zone.BATTLEFIELD, playerA, eye);
        addCard(Zone.BATTLEFIELD, playerA, "Island");
        // Mill three cards.
        addCard(Zone.HAND, playerA, "Tome Scour"); // {U}, mill five cards

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Tome Scour", playerA);

        checkPlayableAbility("milled, not surveilled", 1, PhaseStep.POSTCOMBAT_MAIN,
                playerA, "Cast Lightning Bolt", false);

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertGraveyardCount(playerA, "Lightning Bolt", 1);
    }
}

package org.mage.test.cards.single.spm;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 *
 * @author Jmlundeen
 */
public class OscorpIndustriesTest extends CardTestPlayerBase {

    /*
    Oscorp Industries
    
    Land
    This land enters tapped.
    When this land enters from a graveyard, you lose 2 life.
    {T}: Add {U}, {B}, or {R}.
    Mayhem
    */
    private static final String oscorpIndustries = "Oscorp Industries";

    /*
    Thought Courier
    {1}{U}
    Creature - Human Wizard
    {tap}: Draw a card, then discard a card.
    1/1
    */
    private static final String thoughtCourier = "Thought Courier";

    @Test
    public void testOscorpIndustries() {
        setStrictChooseMode(true);

        addCard(Zone.HAND, playerA, oscorpIndustries);
        addCard(Zone.BATTLEFIELD, playerA, thoughtCourier);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: Draw");
        setChoice(playerA, oscorpIndustries);

        playLand(1, PhaseStep.POSTCOMBAT_MAIN, playerA, oscorpIndustries);

        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20 - 2);
        assertPermanentCount(playerA, oscorpIndustries, 1);
    }

    @Test
    public void testOscorpIndustriesNoMayhem() {
        setStrictChooseMode(true);

        addCard(Zone.HAND, playerA, oscorpIndustries);
        addCard(Zone.BATTLEFIELD, playerA, thoughtCourier);

        playLand(1, PhaseStep.POSTCOMBAT_MAIN, playerA, oscorpIndustries);

        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20);
        assertPermanentCount(playerA, oscorpIndustries, 1);
    }

    @Test
    public void testCantPlayWithoutDiscard() {
        setStrictChooseMode(true);

        addCard(Zone.GRAVEYARD, playerA, oscorpIndustries);

        checkPlayableAbility("Can't play without discard", 1, PhaseStep.PRECOMBAT_MAIN, playerA,
                "Play " + oscorpIndustries, false);

        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20);
        assertPermanentCount(playerA, oscorpIndustries, 0);
    }

    @Test
    public void testOscorpIndustriesNextTurn() {
        setStrictChooseMode(true);

        addCard(Zone.HAND, playerA, oscorpIndustries);
        addCard(Zone.BATTLEFIELD, playerA, thoughtCourier);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{T}: Draw");
        setChoice(playerA, oscorpIndustries);

        checkPlayableAbility("Can't play without discard", 3, PhaseStep.PRECOMBAT_MAIN, playerA,
                "Play " + oscorpIndustries, false);

        setStopAt(3, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertLife(playerA, 20);
        assertPermanentCount(playerA, oscorpIndustries, 0);
    }

    /**
     * Mayhem lets this land be played, by its owner: not a Grizzly Bears discarded beside it, and not another
     * player's discarded card. (Before, any Oscorp Industries in any graveyard let anybody play anything discarded
     * that turn.)
     */
    @Test
    public void testOnlyTheLandItselfByItsOwner() {
        setStrictChooseMode(true);

        addCard(Zone.HAND, playerA, oscorpIndustries);
        addCard(Zone.HAND, playerA, "Grizzly Bears");
        addCard(Zone.BATTLEFIELD, playerA, "Forest", 2);
        addCard(Zone.HAND, playerA, "Faithless Looting");
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 1);
        addCard(Zone.LIBRARY, playerA, "Island", 2);
        // the opponent's Oscorp, discarded on his own turn before: in his graveyard all along
        addCard(Zone.GRAVEYARD, playerB, oscorpIndustries);

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Faithless Looting");
        setChoice(playerA, oscorpIndustries + "^Grizzly Bears");

        checkPlayableAbility("the bear discarded beside it", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Cast Grizzly Bears", false);
        checkPlayableAbility("the land itself", 1, PhaseStep.POSTCOMBAT_MAIN, playerA, "Play " + oscorpIndustries, true);
        playLand(1, PhaseStep.POSTCOMBAT_MAIN, playerA, oscorpIndustries);

        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerA, 20 - 2);
        assertPermanentCount(playerA, oscorpIndustries, 1);
        assertGraveyardCount(playerA, "Grizzly Bears", 1);
        assertGraveyardCount(playerB, oscorpIndustries, 1);
    }
}

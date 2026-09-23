package org.mage.test.cards.single.msc;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

/**
 * @author Claude
 */
public class NegativeZonePortalTest extends CardTestPlayerBase {

    // {2}, {T}: Exile target card from an opponent's graveyard. If it's a creature card, draw a card.
    // At the beginning of your upkeep, if there are four or more creature cards exiled with this artifact, flip a coin.
    // If you lose the flip, sacrifice this artifact and return a card exiled with it at random to its owner's hand.
    private static final String PORTAL = "Negative Zone Portal";

    @Test
    public void test_ExileCreatureDraws() {
        addCard(Zone.BATTLEFIELD, playerA, PORTAL);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 4);
        addCard(Zone.GRAVEYARD, playerB, "Grizzly Bears");
        addCard(Zone.GRAVEYARD, playerB, "Divination");

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}, {T}: Exile", "Grizzly Bears");
        activateAbility(3, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}, {T}: Exile", "Divination");

        setStrictChooseMode(true);
        setStopAt(3, PhaseStep.END_TURN);
        execute();

        assertExileCount(playerB, 2);
        // one draw from the creature, one from turn 3's draw step
        assertHandCount(playerA, 2);
    }

    @Test
    public void test_LoseFlip() {
        addCard(Zone.BATTLEFIELD, playerA, PORTAL);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 8);
        addCard(Zone.GRAVEYARD, playerB, "Grizzly Bears", 4);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}, {T}: Exile", "Grizzly Bears");
        activateAbility(3, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}, {T}: Exile", "Grizzly Bears");
        activateAbility(5, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}, {T}: Exile", "Grizzly Bears");
        activateAbility(7, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}, {T}: Exile", "Grizzly Bears");
        // turn 9 upkeep: four creature cards exiled -> flip
        setFlipCoinResult(playerA, false);

        setStrictChooseMode(true);
        setStopAt(9, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertGraveyardCount(playerA, PORTAL, 1);
        assertHandCount(playerB, "Grizzly Bears", 1);
        assertExileCount(playerB, "Grizzly Bears", 3);
    }

    @Test
    public void test_WinFlip() {
        addCard(Zone.BATTLEFIELD, playerA, PORTAL);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 8);
        addCard(Zone.GRAVEYARD, playerB, "Grizzly Bears", 4);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}, {T}: Exile", "Grizzly Bears");
        activateAbility(3, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}, {T}: Exile", "Grizzly Bears");
        activateAbility(5, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}, {T}: Exile", "Grizzly Bears");
        activateAbility(7, PhaseStep.PRECOMBAT_MAIN, playerA, "{2}, {T}: Exile", "Grizzly Bears");
        setFlipCoinResult(playerA, true);

        setStrictChooseMode(true);
        setStopAt(9, PhaseStep.PRECOMBAT_MAIN);
        execute();

        assertPermanentCount(playerA, PORTAL, 1);
        assertExileCount(playerB, "Grizzly Bears", 4);
    }
}

package org.mage.test.cards.asthough;

import mage.abilities.Ability;
import mage.cards.Card;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.util.ManaUtil;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

import java.util.Map;

/**
 * {@link ManaUtil#asThoughPayers}: what else may pay each mana an ability owes, as the payment's
 * GAME_PLAY_MANA tells a client that pays by itself (HumanPlayer's "asThoughMana" option).
 */
public class AsThoughPayersTest extends CardTestPlayerBase {

    private Map<String, String> payersOf(String cardName) {
        Card card = currentGame.getCards().stream()
                .filter(c -> c.getName().equals(cardName) && c.getOwnerId().equals(playerA.getId()))
                .findFirst().orElse(null);
        Assert.assertNotNull(cardName, card);
        Ability spell = card.getSpellAbility();
        return ManaUtil.asThoughPayers(spell, currentGame);
    }

    @Test
    public void nothingInPlay_nothingElsePays() {
        addCard(Zone.HAND, playerA, "Grizzly Bears");
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        Assert.assertTrue(payersOf("Grizzly Bears").isEmpty());
    }

    @Test
    public void chromaticOrrery_anyManaPaysEveryColor() {
        // You may spend mana as though it were mana of any color.
        addCard(Zone.BATTLEFIELD, playerA, "Chromatic Orrery");
        addCard(Zone.HAND, playerA, "Grizzly Bears");
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        Map<String, String> payers = payersOf("Grizzly Bears");
        Assert.assertEquals("WUBRC", payers.get("G"));
        Assert.assertEquals("UBRGC", payers.get("W"));
    }

    @Test
    public void chromaticOrrery_notForTheOpponent() {
        addCard(Zone.BATTLEFIELD, playerB, "Chromatic Orrery");
        addCard(Zone.HAND, playerA, "Grizzly Bears");
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        Assert.assertTrue(payersOf("Grizzly Bears").isEmpty());
    }

    @Test
    public void sunglassesOfUrza_whitePaysRedOnly() {
        // You may spend white mana as though it were red mana.
        addCard(Zone.BATTLEFIELD, playerA, "Sunglasses of Urza");
        addCard(Zone.HAND, playerA, "Lightning Bolt");
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        Map<String, String> payers = payersOf("Lightning Bolt");
        Assert.assertEquals("W", payers.get("R"));
        Assert.assertEquals(1, payers.size());
    }
}

package org.mage.test.serverside;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.view.CardView;
import mage.view.GameView;
import mage.view.PlayerView;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

import java.util.List;

/**
 * GUI tests: the count of card types in graveyards (Tarmogoyf, Emrakul, the
 * Promised End, delirium cards) says which graveyards it counts. It read
 * "Card types in the number of card types among cards in your graveyard: 2".
 *
 * @author ClaudeMTG
 */
public class CardTypesInGraveyardHintTest extends CardTestPlayerBase {

    private List<String> rulesOf(GameView gameView, String name) {
        PlayerView playerView = gameView.getPlayers().get(0);
        CardView cardView = playerView.getBattlefield().values().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElse(null);
        Assert.assertNotNull("must have " + name + " in battlefield", cardView);
        return cardView.getRules();
    }

    @Test
    public void test_Tarmogoyf_AllGraveyards() {
        // Tarmogoyf's power is equal to the number of card types among cards in all graveyards
        addCard(Zone.BATTLEFIELD, playerA, "Tarmogoyf", 1);
        addCard(Zone.GRAVEYARD, playerA, "Grizzly Bears", 1);
        addCard(Zone.GRAVEYARD, playerB, "Forest", 1);

        runCode("hint", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            List<String> rules = rulesOf(getGameView(player), "Tarmogoyf");
            Assert.assertTrue("the hint names the graveyards: " + rules,
                    rules.stream().anyMatch(r -> r.contains("Card types in all graveyards: 2 (Creature, Land)")));
            Assert.assertFalse("not the count's own words: " + rules,
                    rules.stream().anyMatch(r -> r.contains("Card types in the number of")));
        });

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
    }

    @Test
    public void test_EmrakulThePromisedEnd_YourGraveyard() {
        // Emrakul, the Promised End costs {1} less to cast for each card type among cards in your graveyard
        addCard(Zone.HAND, playerA, "Emrakul, the Promised End", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Wastes", 1);
        addCard(Zone.GRAVEYARD, playerA, "Grizzly Bears", 1);

        runCode("hint", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            CardView emrakul = getGameView(player).getMyHand().values().stream()
                    .filter(c -> c.getName().equals("Emrakul, the Promised End"))
                    .findFirst()
                    .orElse(null);
            Assert.assertNotNull(emrakul);
            Assert.assertTrue("the hint names your graveyard: " + emrakul.getRules(),
                    emrakul.getRules().stream().anyMatch(r -> r.contains("Card types in your graveyard: 1 (Creature)")));
        });

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
    }
}

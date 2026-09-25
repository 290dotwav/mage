package org.mage.test.serverside;

import mage.abilities.hint.HintUtils;
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
 * GUI tests: "Can't be blocked" restriction hint in the permanent's rules, for
 * effects held by the engine rather than printed on the creature
 *
 * @author ClaudeMTG
 */
public class CantBeBlockedHintTest extends CardTestPlayerBase {

    private static final String HINT = HintUtils.HINT_ICON_RESTRICT + "Can't be blocked";

    private List<String> rulesOf(GameView gameView, String name) {
        PlayerView playerView = gameView.getPlayers().get(0);
        CardView cardView = playerView.getBattlefield().values().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElse(null);
        Assert.assertNotNull("must have " + name + " in battlefield", cardView);
        return cardView.getRules();
    }

    private boolean hasHint(List<String> rules, String sourceName) {
        return rules.stream().anyMatch(s -> s.startsWith(HINT + " (" + sourceName + " ["));
    }

    private boolean hasAnyHint(List<String> rules) {
        return rules.stream().anyMatch(s -> s.startsWith(HINT));
    }

    @Test
    public void test_RoguesPassage() {
        // {4}, {T}: Target creature can't be blocked this turn.
        addCard(Zone.BATTLEFIELD, playerA, "Rogue's Passage", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Wastes", 4);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Runeclaw Bear", 1);

        runCode("no hint before", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            GameView gameView = getGameView(player);
            Assert.assertFalse("no hint before activation", hasAnyHint(rulesOf(gameView, "Grizzly Bears")));
        });

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "{4}, {T}: Target creature", "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);

        runCode("hint after", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            GameView gameView = getGameView(player);
            List<String> rules = rulesOf(gameView, "Grizzly Bears");
            Assert.assertTrue("targeted creature must have the hint: " + rules, hasHint(rules, "Rogue's Passage"));
            Assert.assertTrue("hint follows the hint start mark", rules.contains(HintUtils.HINT_START_MARK));
            Assert.assertFalse("plain creature must not have the hint", hasAnyHint(rulesOf(gameView, "Runeclaw Bear")));
        });

        attack(1, playerA, "Grizzly Bears");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        assertLife(playerB, 20 - 2);
    }

    @Test
    public void test_WhispersilkCloak() {
        // Equipped creature can't be blocked and has shroud.
        addCard(Zone.BATTLEFIELD, playerA, "Whispersilk Cloak", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Wastes", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Runeclaw Bear", 1);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Equip {2}", "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);

        runCode("hint", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            GameView gameView = getGameView(player);
            List<String> rules = rulesOf(gameView, "Grizzly Bears");
            Assert.assertTrue("equipped creature must have the hint: " + rules, hasHint(rules, "Whispersilk Cloak"));
            Assert.assertFalse("plain creature must not have the hint", hasAnyHint(rulesOf(gameView, "Runeclaw Bear")));
        });

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
    }

    @Test
    public void test_ConditionalEvasionHasNoHint() {
        // Equipped creature gets +1/+0 and can't be blocked by creatures with flying.
        addCard(Zone.BATTLEFIELD, playerA, "Skyblinder Staff", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Wastes", 3);
        addCard(Zone.BATTLEFIELD, playerA, "Grizzly Bears", 1);

        activateAbility(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Equip {3}", "Grizzly Bears");
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN);

        runCode("no hint", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            GameView gameView = getGameView(player);
            List<String> rules = rulesOf(gameView, "Grizzly Bears");
            Assert.assertTrue("restriction applies", game.getContinuousEffects()
                    .getApplicableRestrictionEffects(game.getBattlefield().getAllPermanents().stream()
                            .filter(p -> p.getName().equals("Grizzly Bears")).findFirst().get(), game).size() > 0);
            Assert.assertFalse("blocked-by-some creature must not have the plain hint: " + rules, hasAnyHint(rules));
        });

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();
    }
}

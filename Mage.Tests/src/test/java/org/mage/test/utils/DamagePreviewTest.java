package org.mage.test.utils;

import mage.abilities.Ability;
import mage.cards.Card;
import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.stack.StackAbility;
import mage.game.stack.StackObject;
import mage.util.DamagePreview;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The damage a target question announces (ClaudeMTG fork, {@link DamagePreview}): FG, on
 * Warstorm Surge, « a aucun moment je vois combien de dm je vais infliger ». Only what is
 * known for sure while the target is chosen; anything else sends nothing.
 */
public class DamagePreviewTest extends CardTestPlayerBase {

    @Test
    public void warstormSurgeTellsThePowerOfTheCreatureThatEntered() {
        addCard(Zone.BATTLEFIELD, playerA, "Warstorm Surge");
        addCard(Zone.BATTLEFIELD, playerA, "Plains", 5);
        addCard(Zone.HAND, playerA, "Serra Angel"); // 4/4

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Serra Angel");
        addTarget(playerA, playerB); // the trigger's target
        waitStackResolved(1, PhaseStep.PRECOMBAT_MAIN, playerA, true); // the Angel only: its trigger stays on the stack

        final Map<String, Serializable> seen = new HashMap<>();
        runCode("read the preview of the trigger", 1, PhaseStep.PRECOMBAT_MAIN, playerA, (info, player, game) -> {
            StackObject top = game.getStack().getFirstOrNull();
            Assert.assertTrue("the trigger is on the stack", top instanceof StackAbility);
            Ability trigger = ((StackAbility) top).getStackAbility();
            DamagePreview.putInto(seen, trigger.getTargets().get(0), trigger, game);
        });

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.POSTCOMBAT_MAIN);
        execute();

        Assert.assertEquals(seen.toString(), 4, seen.get(DamagePreview.KEY_AMOUNT));
        Assert.assertEquals("Serra Angel", seen.get(DamagePreview.KEY_BY));
        Assert.assertEquals(Boolean.TRUE, seen.get(DamagePreview.KEY_MAY_CHANGE));
        assertLife(playerB, 20 - 4);
    }

    @Test
    public void lightningBoltTellsThree() {
        DamagePreview preview = previewOfSpellInHand("Lightning Bolt", null);
        Assert.assertNotNull(preview);
        Assert.assertEquals(3, preview.getAmount());
        Assert.assertNull(preview.getBy());
        Assert.assertFalse(preview.isMayChange());
    }

    @Test
    public void anXSpellTellsX() {
        // Blaze deals X damage to any target
        DamagePreview blaze = previewOfSpellInHand("Blaze", 5);
        Assert.assertNotNull(blaze);
        Assert.assertEquals(5, blaze.getAmount());
        Assert.assertFalse(blaze.isMayChange());
    }

    @Test
    public void fireballTellsXForItsFirstTarget() {
        // Fireball: X divided evenly among its targets, all of it for the first one
        DamagePreview fireball = previewOfSpellInHand("Fireball", 5);
        Assert.assertNotNull(fireball);
        Assert.assertEquals(5, fireball.getAmount());
        Assert.assertTrue("a further target divides it", fireball.isMayChange());
    }

    @Test
    public void aCostPaidAfterTheTargetsTellsNothing() {
        // Fling: damage equal to the power of the creature sacrificed as a cost, paid after targets
        Assert.assertNull(previewOfSpellInHand("Fling", null));

        Map<String, Serializable> options = new HashMap<>();
        options.put(DamagePreview.KEY_AMOUNT, 7); // a stale one from an earlier question
        Card fling = cardInHand("Fling");
        Ability ability = fling.getSpellAbility().copy();
        DamagePreview.putInto(options, ability.getTargets().get(0), ability, currentGame);
        Assert.assertFalse(options.toString(), options.containsKey(DamagePreview.KEY_AMOUNT));
    }

    private Card cardInHand(String name) {
        return currentGame.getPlayer(playerA.getId()).getHand().getCards(currentGame).stream()
                .filter(card -> card.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError(name + " is not in hand"));
    }

    /**
     * The preview of the first target of a spell in hand, as it is asked while the spell is
     * cast (X already announced, 601.2b).
     */
    private DamagePreview previewOfSpellInHand(String name, Integer x) {
        addCard(Zone.HAND, playerA, name);
        setStopAt(1, PhaseStep.PRECOMBAT_MAIN);
        execute();

        Ability ability = cardInHand(name).getSpellAbility().copy();
        ability.setControllerId(playerA.getId());
        if (x != null) {
            ability.setCostsTag("X", x);
        }
        return DamagePreview.compute(ability.getTargets().get(0), ability, currentGame);
    }
}

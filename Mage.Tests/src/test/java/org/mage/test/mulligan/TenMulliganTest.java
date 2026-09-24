package org.mage.test.mulligan;

import mage.game.mulligan.MulliganType;
import mage.game.mulligan.TenMulligan;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * "Mulligan à 10": the opening hand is 10 cards, and the n-th mulligan draws 10, 10, 9, 9, 8, 8,
 * then 7 cards; the surplus over 7 goes on the bottom, chosen in ONE pick when the hand is kept.
 * The test base's stub player does not shuffle and puts a returned hand on the bottom of the
 * library, so positions are predictable: deck of 40, the opening hand is the top 10 (library 30).
 * <p>
 * Each player runs their own mulligan on a thread of their own (nobody waits for anybody), so a
 * step's assertion fails on that thread; the test base carries it back to the test.
 */
public class TenMulliganTest extends MulliganTestBase {

    @Test
    public void testDrawSizes() {
        int[] expected = {10, 10, 9, 9, 8, 8, 7, 7, 7};
        for (int n = 1; n <= expected.length; n++) {
            assertEquals("mulligan " + n, expected[n - 1], TenMulligan.cardsDrawn(7, n));
        }
        assertEquals(8, TenMulligan.cardsDrawn(5, 1)); // custom starting hand size: keep + 3
    }

    /** The surplus of a kept hand: one pick of {@code owed} cards, taken from the front of the hand. */
    private static void keepAndBottom(MulliganScenarioTest scenario, int handSize, int owed, List<UUID> discarded) {
        scenario.discardBottom(count -> {
            scenario.assertSizes(handSize, 40 - handSize);
            assertEquals(owed, count);
            List<UUID> picked = new ArrayList<>();
            scenario.getHand().stream().limit(count).forEach(picked::add);
            discarded.addAll(picked);
            return picked;
        });
    }

    @Test
    public void testTenMulligan_NoMulligan_OpeningTenKeepsSeven() {
        MulliganScenarioTest scenario = new MulliganScenarioTest(MulliganType.TEN, 0);
        Set<UUID> hand1 = new HashSet<>();
        List<UUID> discarded = new ArrayList<>();
        scenario.mulligan(() -> {
            // the opening hand is the big one: ten cards, not seven
            scenario.assertSizes(10, 30);
            hand1.addAll(scenario.getHand());
            return false;
        });
        keepAndBottom(scenario, 10, 3, discarded);
        scenario.run(() -> {
            scenario.assertSizes(7, 33);
            Set<UUID> kept = new HashSet<>(hand1);
            kept.removeAll(discarded);
            assertEquals(kept, scenario.getHand());
            assertEquals(new HashSet<>(discarded), new HashSet<>(scenario.getNBottomOfLibrary(3)));
        });
    }

    @Test
    public void testTenMulligan_OneMulligan_SeesTenKeepsSeven() {
        MulliganScenarioTest scenario = new MulliganScenarioTest(MulliganType.TEN, 0);
        Set<UUID> hand1 = new HashSet<>();
        Set<UUID> hand2 = new HashSet<>();
        List<UUID> discarded = new ArrayList<>();
        scenario.mulligan(() -> {
            scenario.assertSizes(10, 30);
            hand1.addAll(scenario.getHand());
            return true;
        });
        // the whole 10-card hand is seen before deciding: nothing goes to the bottom yet
        scenario.mulligan(() -> {
            scenario.assertSizes(10, 30);
            hand2.addAll(scenario.getHand());
            // the first hand went back to the bottom (10 of the 30)
            assertEquals(hand1, new HashSet<>(scenario.getLibraryRangeSize(20, 10)));
            return false;
        });
        keepAndBottom(scenario, 10, 3, discarded);
        scenario.run(() -> {
            scenario.assertSizes(7, 33);
            Set<UUID> kept = new HashSet<>(hand2);
            kept.removeAll(discarded);
            assertEquals(kept, scenario.getHand());
            assertEquals(hand1, new HashSet<>(scenario.getLibraryRangeSize(20, 10)));
            assertEquals(new HashSet<>(discarded), new HashSet<>(scenario.getNBottomOfLibrary(3)));
        });
    }

    @Test
    public void testTenMulligan_TwoMulligans_BothFree() {
        MulliganScenarioTest scenario = new MulliganScenarioTest(MulliganType.TEN, 0);
        Set<UUID> hand1 = new HashSet<>();
        Set<UUID> hand2 = new HashSet<>();
        Set<UUID> hand3 = new HashSet<>();
        List<UUID> discarded = new ArrayList<>();
        scenario.mulligan(() -> {
            scenario.assertSizes(10, 30);
            hand1.addAll(scenario.getHand());
            return true;
        });
        scenario.mulligan(() -> {
            scenario.assertSizes(10, 30);
            hand2.addAll(scenario.getHand());
            return true;
        });
        scenario.mulligan(() -> {
            scenario.assertSizes(10, 30); // second mulligan draws 10 again
            hand3.addAll(scenario.getHand());
            // library, top to bottom: 10 untouched cards, hand1 (10), hand2 (10)
            assertEquals(hand1, new HashSet<>(scenario.getLibraryRangeSize(10, 10)));
            assertEquals(hand2, new HashSet<>(scenario.getLibraryRangeSize(20, 10)));
            return false;
        });
        keepAndBottom(scenario, 10, 3, discarded);
        scenario.run(() -> {
            scenario.assertSizes(7, 33);
            Set<UUID> kept = new HashSet<>(hand3);
            kept.removeAll(discarded);
            assertEquals(kept, scenario.getHand());
            assertEquals(new HashSet<>(discarded), new HashSet<>(scenario.getNBottomOfLibrary(3)));
        });
    }

    @Test
    public void testTenMulligan_AlwaysMulligan_DrawsShrinkThenStops() {
        MulliganScenarioTest scenario = new MulliganScenarioTest(MulliganType.TEN, 0);
        // hand size seen before each mulligan: the opening hand, then what mulligans 1..6 drew
        int[] seen = {10, 10, 10, 9, 9, 8, 8};
        for (int size : seen) {
            final int handSize = size;
            scenario.mulligan(() -> {
                scenario.assertSizes(handSize, 40 - handSize);
                return true;
            });
        }
        // the 7th mulligan draws 7: nothing to bottom, and no 8th mulligan is offered (cap)
        scenario.run(() -> scenario.assertSizes(7, 33));
    }

    @Test
    public void testTenMulligan_FreeMulliganOptionIgnored() {
        MulliganScenarioTest scenario = new MulliganScenarioTest(MulliganType.TEN, 1);
        List<UUID> discarded = new ArrayList<>();
        scenario.mulligan(() -> {
            scenario.assertSizes(10, 30);
            return true;
        });
        scenario.mulligan(() -> {
            scenario.assertSizes(10, 30);
            return true;
        });
        scenario.mulligan(() -> {
            scenario.assertSizes(10, 30);
            return true;
        });
        // the third mulligan draws 9, free mulligan option or not
        scenario.mulligan(() -> {
            scenario.assertSizes(9, 31);
            return false;
        });
        keepAndBottom(scenario, 9, 2, discarded);
        scenario.run(() -> scenario.assertSizes(7, 33));
    }

    /**
     * A question that breaks is a hand kept, and kept like any other: the surplus still goes
     * under. It used to end the mulligan with the ten cards still in hand.
     */
    @Test
    public void testTenMulligan_BrokenQuestion_StillKeepsSeven() {
        MulliganScenarioTest scenario = new MulliganScenarioTest(MulliganType.TEN, 0);
        List<UUID> discarded = new ArrayList<>();
        scenario.mulligan(() -> {
            scenario.assertSizes(10, 30);
            return true;
        });
        scenario.mulligan(() -> {
            scenario.assertSizes(10, 30);
            throw new IllegalStateException("the question broke");
        });
        keepAndBottom(scenario, 10, 3, discarded);
        scenario.run(() -> {
            scenario.assertSizes(7, 33);
            assertEquals(new HashSet<>(discarded), new HashSet<>(scenario.getNBottomOfLibrary(3)));
        });
    }
}

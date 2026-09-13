package org.mage.test.mulligan;

import mage.game.mulligan.MulliganType;
import mage.game.mulligan.TenMulligan;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * "Mulligan à 10": the n-th mulligan draws 10, 10, 9, 9, 8, 8, then 7 cards; the surplus over 7
 * goes on the bottom, chosen, when the hand is kept. The test base's stub player does not
 * shuffle and puts a returned hand on the bottom of the library, so positions are predictable:
 * deck of 40, the first hand is the top 7 (library 33).
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

    @Test
    public void testTenMulligan_NoMulligan() {
        MulliganScenarioTest scenario = new MulliganScenarioTest(MulliganType.TEN, 0);
        Set<UUID> hand1 = new HashSet<>();
        scenario.mulligan(() -> {
            scenario.assertSizes(7, 33);
            hand1.addAll(scenario.getHand());
            return false;
        });
        scenario.run(() -> {
            scenario.assertSizes(7, 33);
            assertEquals(hand1, scenario.getHand());
        });
    }

    @Test
    public void testTenMulligan_OneMulligan_SeesTenKeepsSeven() {
        MulliganScenarioTest scenario = new MulliganScenarioTest(MulliganType.TEN, 0);
        Set<UUID> hand1 = new HashSet<>();
        Set<UUID> hand2 = new HashSet<>();
        List<UUID> discarded = new ArrayList<>();
        scenario.mulligan(() -> {
            scenario.assertSizes(7, 33);
            hand1.addAll(scenario.getHand());
            return true;
        });
        // the whole 10-card hand is seen before deciding: nothing goes to the bottom yet
        scenario.mulligan(() -> {
            scenario.assertSizes(10, 30);
            hand2.addAll(scenario.getHand());
            // the first hand went back to the bottom (7 of the 30)
            assertEquals(hand1, new HashSet<>(scenario.getLibraryRangeSize(30 - 7, 7)));
            return false;
        });
        // keeping: 3 cards to the bottom, one at a time
        for (int i = 0; i < 3; i++) {
            final int before = 10 - i;
            scenario.discardBottom(count -> {
                scenario.assertSizes(before, 40 - before);
                assertEquals(1, count);
                UUID card = scenario.getHand().iterator().next();
                discarded.add(card);
                return Collections.singletonList(card);
            });
        }
        scenario.run(() -> {
            scenario.assertSizes(7, 33);
            Set<UUID> kept = new HashSet<>(hand2);
            kept.removeAll(discarded);
            assertEquals(kept, scenario.getHand());
            assertEquals(hand1, new HashSet<>(scenario.getLibraryRangeSize(30 - 7, 7)));
            assertEquals(discarded, scenario.getNBottomOfLibrary(3));
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
            scenario.assertSizes(7, 33);
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
            // library, top to bottom: 13 untouched cards, hand1 (7), hand2 (10)
            assertEquals(hand1, new HashSet<>(scenario.getLibraryRangeSize(13, 7)));
            assertEquals(hand2, new HashSet<>(scenario.getLibraryRangeSize(20, 10)));
            return false;
        });
        for (int i = 0; i < 3; i++) {
            final int before = 10 - i;
            scenario.discardBottom(count -> {
                scenario.assertSizes(before, 40 - before);
                assertEquals(1, count);
                UUID card = scenario.getHand().iterator().next();
                discarded.add(card);
                return Collections.singletonList(card);
            });
        }
        scenario.run(() -> {
            scenario.assertSizes(7, 33);
            Set<UUID> kept = new HashSet<>(hand3);
            kept.removeAll(discarded);
            assertEquals(kept, scenario.getHand());
            assertEquals(discarded, scenario.getNBottomOfLibrary(3));
        });
    }

    @Test
    public void testTenMulligan_AlwaysMulligan_DrawsShrinkThenStops() {
        MulliganScenarioTest scenario = new MulliganScenarioTest(MulliganType.TEN, 0);
        // hand size seen before each mulligan: the first hand, then what mulligans 1..6 drew
        int[] seen = {7, 10, 10, 9, 9, 8, 8};
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
        scenario.mulligan(() -> {
            scenario.assertSizes(7, 33);
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
        scenario.mulligan(() -> {
            scenario.assertSizes(9, 31);
            return false;
        });
        scenario.discardBottom(count -> {
            scenario.assertSizes(9, 31);
            assertEquals(1, count);
            return Collections.singletonList(scenario.getHand().iterator().next());
        });
        scenario.discardBottom(count -> {
            scenario.assertSizes(8, 32);
            assertEquals(1, count);
            return Collections.singletonList(scenario.getHand().iterator().next());
        });
        scenario.run(() -> scenario.assertSizes(7, 33));
    }
}

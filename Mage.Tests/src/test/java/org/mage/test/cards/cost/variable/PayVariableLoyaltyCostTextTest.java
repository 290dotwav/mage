package org.mage.test.cards.cost.variable;

import mage.abilities.costs.common.PayVariableLoyaltyCost;
import org.junit.Assert;
import org.junit.Test;

/**
 * A planeswalker's -X is announced as "Announce the value for {X} (loyalty counters to remove)":
 * the words were "loyality counters to remove", shown on every English screen (Ugin, Tamiyo,
 * Chandra's -X) and read by the Claude chairs.
 */
public class PayVariableLoyaltyCostTextTest {

    @Test
    public void test_AnnouncementSpellsLoyalty() {
        Assert.assertEquals("loyalty counters to remove", new PayVariableLoyaltyCost().getActionText());
    }
}

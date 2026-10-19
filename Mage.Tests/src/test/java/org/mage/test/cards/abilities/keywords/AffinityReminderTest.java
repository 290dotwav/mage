package org.mage.test.cards.abilities.keywords;

import mage.abilities.keyword.AffinityAbility;
import mage.constants.AffinityType;
import org.junit.Assert;
import org.junit.Test;

/**
 * 702.41a's reminder, "This spell costs {1} less to cast for each [one of them] you control.": the thing counted in
 * the singular. Every type made from a filter alone said its plural there ("for each snow lands", "for each
 * Islands", "for each outlaws"), so no French printing paired with the line on the table's zoom.
 */
public class AffinityReminderTest {

    private static String each(AffinityType type) {
        String rule = new AffinityAbility(type).getRule();
        int at = rule.indexOf("for each ");
        return rule.substring(at + "for each ".length(), rule.indexOf(" you control"));
    }

    @Test
    public void testTheCountedThingIsSingular() {
        Assert.assertEquals("artifact", each(AffinityType.ARTIFACTS));
        Assert.assertEquals("artifact creature", each(AffinityType.ARTIFACT_CREATURES));
        Assert.assertEquals("enchantment", each(AffinityType.ENCHANTMENTS));
        Assert.assertEquals("planeswalker", each(AffinityType.PLANESWALKERS));
        Assert.assertEquals("Plains", each(AffinityType.PLAINS));
        Assert.assertEquals("Island", each(AffinityType.ISLANDS));
        Assert.assertEquals("Forest", each(AffinityType.FORESTS));
        Assert.assertEquals("Ally", each(AffinityType.ALLIES));
        Assert.assertEquals("Elf", each(AffinityType.ELVES));
        Assert.assertEquals("Equipment", each(AffinityType.EQUIPMENT));
        Assert.assertEquals("Food", each(AffinityType.FOOD));
        Assert.assertEquals("Gate", each(AffinityType.GATES));
        Assert.assertEquals("Sliver", each(AffinityType.SLIVERS));
        Assert.assertEquals("outlaw", each(AffinityType.OUTLAWS));
        Assert.assertEquals("historic permanent", each(AffinityType.HISTORIC));
        Assert.assertEquals("snow land", each(AffinityType.SNOW_LANDS));
        Assert.assertEquals("token", each(AffinityType.TOKENS));
        for (AffinityType type : AffinityType.values()) {
            String one = each(type);
            Assert.assertFalse(type + ": " + one, one.endsWith("s") && !one.equals("Plains"));
        }
    }
}

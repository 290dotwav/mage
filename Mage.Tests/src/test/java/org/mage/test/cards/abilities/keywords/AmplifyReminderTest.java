package org.mage.test.cards.abilities.keywords;

import mage.abilities.effects.common.AmplifyEffect.AmplifyFactor;
import mage.abilities.keyword.AmplifyAbility;
import org.junit.Assert;
import org.junit.Test;

/**
 * 702.38a's reminder as the table's zoom shows it. It read "As this enter the battlefield, put one +1/+1 counters on it
 * for each card you reveal that shares a type with it in your hand." (Embalmed Brawler on a local door, thirteenth
 * sweep): the verb, the count and the noun disagreed, and "a type" is a creature type.
 */
public class AmplifyReminderTest {

    @Test
    public void testReminderReadsAsPrinted() {
        Assert.assertEquals("Amplify 1 <i>(As this creature enters, put a +1/+1 counter on it for each card you reveal in your hand that shares a creature type with it.)</i>",
                new AmplifyAbility(AmplifyFactor.Amplify1).getRule());
        Assert.assertEquals("Amplify 2 <i>(As this creature enters, put two +1/+1 counters on it for each card you reveal in your hand that shares a creature type with it.)</i>",
                new AmplifyAbility(AmplifyFactor.Amplify2).getRule());
        Assert.assertEquals("Amplify 3 <i>(As this creature enters, put three +1/+1 counters on it for each card you reveal in your hand that shares a creature type with it.)</i>",
                new AmplifyAbility(AmplifyFactor.Amplify3).getRule());
    }
}

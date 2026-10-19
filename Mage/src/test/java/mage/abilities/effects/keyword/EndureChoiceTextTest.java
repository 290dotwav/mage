package mage.abilities.effects.keyword;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * {@link EndureSourceEffect#choiceText}: endure's question (701.63a) read "or create a 2 2/2
 * Spirit token?", the article put before the number and the size after it.
 */
public class EndureChoiceTextTest {

    @Test
    public void theTokensSizeOnce_withItsArticle() {
        assertEquals("Put two +1/+1 counters on Krumar Initiate or create a 2/2 Spirit token?",
                EndureSourceEffect.choiceText("Krumar Initiate", 2));
        assertEquals("Put a +1/+1 counter on Krumar Initiate or create a 1/1 Spirit token?",
                EndureSourceEffect.choiceText("Krumar Initiate", 1));
        assertEquals("Put eight +1/+1 counters on Krumar Initiate or create an 8/8 Spirit token?",
                EndureSourceEffect.choiceText("Krumar Initiate", 8));
    }
}

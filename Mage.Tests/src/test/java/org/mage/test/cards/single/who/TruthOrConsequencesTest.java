package org.mage.test.cards.single.who;

import mage.constants.PhaseStep;
import mage.constants.Zone;
import mage.game.events.TableEvent;
import org.junit.Assert;
import org.junit.Test;
import org.mage.test.serverside.base.CardTestPlayerBase;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link mage.cards.t.TruthOrConsequences Truth or Consequences} {2}{U}{R}: "Secret council -- Each player secretly
 * votes for truth or consequences, then those votes are revealed. ..." Each vote was announced to the table as soon
 * as it was cast, so every later voter saw the earlier ones (found on the ClaudeMTG table's fourteenth mechanics
 * sweep): TwoChoiceVote kept its own "secret" field, hiding VoteHandler's, which doVotes reads.
 */
public class TruthOrConsequencesTest extends CardTestPlayerBase {

    private static final String truth = "Truth or Consequences";

    @Test
    public void test_NoVoteIsShownBeforeEveryPlayerHasVoted() {
        addCard(Zone.HAND, playerA, truth, 1);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 2);
        addCard(Zone.BATTLEFIELD, playerA, "Mountain", 2);

        // What playerB still had to answer when each vote line reached the table.
        List<String> shown = new ArrayList<>();
        currentGame.addTableEventListener(event -> {
            if (event.getEventType() == TableEvent.EventType.INFO
                    && event.getMessage() != null && event.getMessage().contains("voted for")) {
                shown.add(event.getMessage() + " / playerB's choices left: " + playerB.getChoices().size());
            }
        });

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, truth);
        setChoice(playerA, true); // truth
        setChoice(playerB, false); // consequences

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        Assert.assertEquals("both votes revealed: " + shown, 2, shown.size());
        for (String line : shown) {
            Assert.assertTrue("a vote was shown before playerB voted: " + shown, line.endsWith("choices left: 0"));
        }
        assertHandCount(playerA, 1); // one truth vote
        assertLife(playerB, 20 - 3); // one consequences vote, the only opponent
    }

    @Test
    public void test_OpenVoteIsStillShownAtOnce() {
        // Expropriate's council's dilemma is no secret: each vote is announced as it is cast.
        addCard(Zone.HAND, playerA, "Expropriate", 1);
        addCard(Zone.BATTLEFIELD, playerA, "Island", 9);
        addCard(Zone.BATTLEFIELD, playerB, "Memnite", 1);

        List<String> shown = new ArrayList<>();
        currentGame.addTableEventListener(event -> {
            if (event.getEventType() == TableEvent.EventType.INFO
                    && event.getMessage() != null && event.getMessage().contains("voted for")) {
                shown.add(event.getMessage() + " / playerB's choices left: " + playerB.getChoices().size());
            }
        });

        castSpell(1, PhaseStep.PRECOMBAT_MAIN, playerA, "Expropriate");
        setChoice(playerA, true); // time
        setChoice(playerB, false); // money
        setChoice(playerA, "Memnite");

        setStrictChooseMode(true);
        setStopAt(1, PhaseStep.END_TURN);
        execute();

        Assert.assertEquals("both votes shown: " + shown, 2, shown.size());
        Assert.assertTrue("playerA's vote was shown before playerB voted: " + shown, shown.get(0).endsWith("choices left: 1"));
    }
}

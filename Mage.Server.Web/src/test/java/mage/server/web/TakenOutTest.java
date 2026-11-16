package mage.server.web;

import mage.cards.decks.Deck;
import mage.constants.RangeOfInfluence;
import mage.game.match.MatchImpl;
import mage.game.match.MatchOptions;
import mage.players.StubPlayer;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A name still on a chair is not always a player still in the game (TableOps.leftTheMatch): on 15 November the
 * owner came back an hour after his phone put the page away, his session had expired at the half hour and taken
 * him out of the match, and the door answered « re-seated (reconnection) » with nothing to replay.
 */
class TakenOutTest {

    private static MatchImpl match() {
        return new MatchImpl(new MatchOptions("t", "Commander Free For All", false)) {
            @Override
            public void startGame() {
            }
        };
    }

    @Test
    void aPlayerStillInTheMatchTakesHisChairBack() {
        MatchImpl match = match();
        StubPlayer fg = new StubPlayer("fgmuller", RangeOfInfluence.ALL);
        match.addPlayer(fg, new Deck());
        assertFalse(TableOps.leftTheMatch(match, fg.getId()));
    }

    @Test
    void aPlayerTheMatchLetGoHasNoChairToTakeBack() {
        MatchImpl match = match();
        StubPlayer fg = new StubPlayer("fgmuller", RangeOfInfluence.ALL);
        match.addPlayer(fg, new Deck());
        match.getPlayer(fg.getId()).setQuit(true);
        assertTrue(TableOps.leftTheMatch(match, fg.getId()));
    }

    @Test
    void noMatchYetOrNoSuchPlayerIsNotTakenOut() {
        assertFalse(TableOps.leftTheMatch(null, UUID.randomUUID()));
        assertFalse(TableOps.leftTheMatch(match(), UUID.randomUUID()));
        assertFalse(TableOps.leftTheMatch(match(), null));
    }
}

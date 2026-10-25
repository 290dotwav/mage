package mage.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A person of the site's tables whose connection drops in a running game is not put out of it after
 * three minutes (« j'essaye de jouer un land mais ça marche pas »: his iPhone had the page in the
 * background from 14:42:44 to 14:45:53, their server expired him at 14:45:52 and he was out of the game).
 */
class UserGraceTest {

    @Test
    void aPersonInARunningGameHasHalfAnHour() {
        assertEquals(30 * 60, UserManagerImpl.sessionExpireAfterSecs(true));
        // what he was given that afternoon: 3 min 9 s away is now well inside the grace
        assertTrue(3 * 60 + 9 < UserManagerImpl.sessionExpireAfterSecs(true));
    }

    @Test
    void everybodyElseKeepsThreeMinutes() {
        // a chair (no site header), a user at no game: as before
        assertEquals(3 * 60, UserManagerImpl.sessionExpireAfterSecs(false));
    }

    @Test
    void aFreshUserIsNeitherKeptNorPlaying() {
        User user = new User(null, "fgmuller", "127.0.0.1", null);
        assertFalse(user.isKeptWhileAway());
        // no game session: the controller list is never asked
        assertFalse(user.isPlayingAGame());
        user.setKeptWhileAway(true);
        assertTrue(user.isKeptWhileAway());
    }

    @Test
    void theTableIsToldAtFirstThenEveryFiveMinutes() {
        int threeMinutes = UserManagerImpl.sessionExpireAfterSecs(false);
        int halfHour = UserManagerImpl.sessionExpireAfterSecs(true);
        // three minutes: every check, as before
        assertTrue(UserManagerImpl.informNow(30, threeMinutes));
        assertTrue(UserManagerImpl.informNow(170, threeMinutes));
        // the half hour: the first three minutes, then one check in ten
        assertTrue(UserManagerImpl.informNow(60, halfHour));
        assertFalse(UserManagerImpl.informNow(200, halfHour));
        assertTrue(UserManagerImpl.informNow(305, halfHour));
        assertFalse(UserManagerImpl.informNow(400, halfHour));
        assertTrue(UserManagerImpl.informNow(610, halfHour));
    }
}

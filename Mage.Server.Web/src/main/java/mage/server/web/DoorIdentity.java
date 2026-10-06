package mage.server.web;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Who a socket may play as, when the site says so.
 * <p>
 * The site on blacklotusmtg.com checks its player's account, then carries his socket to the
 * door through Caddy's locked address with the header {@value #HEADER}: the account's name,
 * URI-encoded (ClaudeMTG apps/server/src/gate.ts). Caddy lets nothing onto that address
 * without the site's key, so the header there is the site's word. A socket that carries one
 * plays under that name and no other: its table create, human join, watch and close, its raw
 * connectUser, a human roomJoinTable and its chat lines. A watcher's name, the account's with
 * the site's " 👀" mark (apps/web/src/xmage/tables.ts spectatorName), is his too.
 * The bots he seats keep their own names: an AI join is not a person.
 * <p>
 * A socket without the header is not restricted: the machine's own Claude chairs, which come
 * from the machine itself, and the open address while it lasts. The header can only narrow
 * what a socket may do, never widen it, so a forged one on the open address gains nothing.
 */
final class DoorIdentity {

    static final String HEADER = "X-Door-Player";

    /** The most characters their server takes in a user name (the site's MAX_NAME). */
    static final int MAX_NAME = 14;

    /** The site's watcher mark: a space and the eyes emoji, three UTF-16 units. */
    static final String WATCHER_MARK = " 👀";

    /** Raw-call parameters that name a person (mage-server-signatures.txt). */
    private static final Set<String> NAME_PARAMS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("userName", "username", "name")));

    /** Raw calls whose "name" is a seat's, which a bot's seat may take under its own name. */
    private static final Set<String> SEATING_CALLS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("roomJoinTable", "roomJoinTournament")));

    static final DoorIdentity ANYONE = new DoorIdentity(null);

    /** The account's name, or null for a socket that may use any. */
    final String player;

    private DoorIdentity(String player) {
        this.player = player;
    }

    /** From the handshake's header value: absent or blank is {@link #ANYONE}. */
    static DoorIdentity fromHeader(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return ANYONE;
        }
        String name;
        try {
            name = URLDecoder.decode(raw.trim(), "UTF-8").trim();
        } catch (UnsupportedEncodingException | IllegalArgumentException ex) {
            // A header the site did not write: a name nobody has, so this socket can sit nowhere.
            name = raw.trim();
        }
        return name.isEmpty() ? ANYONE : new DoorIdentity(name);
    }

    boolean restricted() {
        return player != null;
    }

    /** The name the site gives a watcher (spectatorName): the account's, cut to fit, and the mark. */
    static String watcherName(String player) {
        String name = player.trim().isEmpty() ? "Player" : player.trim();
        int room = MAX_NAME - WATCHER_MARK.length();
        return (name.length() > room ? name.substring(0, room) : name) + WATCHER_MARK;
    }

    /** May this socket act under {@code name}? */
    boolean allows(String name) {
        if (player == null) {
            return true;
        }
        return name != null && (player.equals(name) || watcherName(player).equals(name));
    }

    /** Refuses a name that is not this socket's, saying whose it is. */
    void check(String name, String what) {
        if (!allows(name)) {
            throw new IllegalArgumentException(what + ": this connection plays as '" + player + "', not '" + name + "'");
        }
    }

    /**
     * The raw call's arguments, by the signature's parameter names: every one that names a
     * person must be this socket's, except a seat taken by a bot (playerType not Human).
     */
    void checkCall(String method, String[] paramNames, Object[] values) {
        if (player == null) {
            return;
        }
        boolean botSeat = false;
        if (SEATING_CALLS.contains(method)) {
            for (int i = 0; i < paramNames.length; i++) {
                if ("playerType".equals(paramNames[i]) && values[i] instanceof Enum && !"HUMAN".equals(((Enum<?>) values[i]).name())) {
                    botSeat = true;
                }
            }
        }
        for (int i = 0; i < paramNames.length; i++) {
            if (NAME_PARAMS.contains(paramNames[i]) && !(botSeat && "name".equals(paramNames[i]))) {
                check(values[i] == null ? null : String.valueOf(values[i]), method);
            }
        }
    }
}

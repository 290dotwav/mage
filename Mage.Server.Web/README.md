# Mage.Server.Web — the web door

A WebSocket server (JSON both ways) living in the same JVM as `Mage.Server`, so a
browser can play through XMage without JBoss Remoting. It re-implements no rule:
every inbound `call` frame becomes the `MageServer` method of the same name, every
`ClientCallback` fired at the session becomes a `callback` frame serialized by Gson.
Contract: `docs/XMAGE-WIRE.md` in the ClaudeMTG repository, piece A.

## Build and run

```sh
mvn -pl Mage.Server.Web -am install          # builds Mage, Mage.Common, Mage.Sets, Mage.Server, this
```

The door is found by `Mage.Server` through `java.util.ServiceLoader`
(`META-INF/services/mage.server.ServerExtension`): put this module's jar and its two
dependencies on the server classpath and start the server as usual — nothing else.

```sh
java -cp "Mage.Server/target/mage-server-1.4.61.jar:Mage.Server.Web/target/mage-server-web-1.4.61.jar:\
$HOME/.m2/repository/org/java-websocket/Java-WebSocket/1.5.7/Java-WebSocket-1.5.7.jar:\
$HOME/.m2/repository/org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar:<the server's usual classpath>" \
     -Dxmage.web.port=17172 mage.server.Main -testMode=true
```

Settings: `-Dxmage.web.port` (default 17172), `-Dxmage.web.host` (default 0.0.0.0),
`-Dxmage.web.enabled=false` to keep the jar without listening,
`-Dxmage.web.pingSeconds` (default 15, see Keepalive), `-Dxmage.web.aiThinkSeconds`
(default 0 = the AI's own timing; the `thinkSeconds` of a `table join` when the frame has
none). The log says `Web door: 70 MageServer methods wired` then
`Web door listening on ws://0.0.0.0:17172 (ping every 15 s)`.

## Keepalive

The door pings every socket every 15 s (WebSocket ping frames; a browser answers with a pong
by itself, nothing to do in the page) and each pong refreshes the user's last activity on the
server, exactly as their own client's `MageServer.ping` does. Without it the server's
`UserManagerImpl` (a check every 30 s) tells the opponents
`<name> catch connection problems for N secs (left before expire: M secs)` about any user
idle for more than 30 s — a human waiting for the bots to think, since only `sendPlayer*`
answers and `ping` refresh the activity. A socket that stops answering pongs is closed after
1.5 x 15 s, the usual lost-connection path (the seat is kept 3 minutes, reconnect by name).
A `call ping` frame (`args: ["<info>"]`) still works and also sets the ping info shown in the
users list.

## Frames

Server → browser:

| kind | fields | when |
| --- | --- | --- |
| `hello` | `sessionId`, `version`, `testMode`, `methods[]` | first frame on a new socket |
| `callback` | `method` (`ClientCallbackMethod` name), `gameId` (the callback's objectId: game id for `GAME_*`, chat/table id otherwise), `messageId`, `data` | every `ClientCallback` fired at this session |
| `result` | `method`, `data`, `id?` | the return value of a `call` (null for void) or of `table start` (`table.start`) |
| `joined` | `tableId`, `playerId`, `name`, `reconnected`, `deckWarnings[]`, `deckReplaced`, `id?` | after `table create` / `table join` |
| `error` | `message`, `id?` | a refused frame, or a callback the door could not serialize (named in the message) |

`id`: any inbound frame may carry an `id`; the reply echoes it.

Browser → server:

```json
{ "kind": "call", "method": "sendPlayerBoolean", "gameId": "<uuid>", "args": [false] }
{ "kind": "call", "method": "sendPlayerAction", "gameId": "<uuid>", "args": ["PASS_PRIORITY_UNTIL_NEXT_TURN"] }
{ "kind": "call", "method": "gameJoin", "gameId": "<uuid>" }
{ "kind": "table", "op": "create", "gameType": "Commander Free For All", "seats": 4, "deck": "<.dck text>", "name": "Alice",
  "seatTypes": ["Human", "Computer - mad", "Computer - mad", "Computer - mad"], "tableName": "FG", "deckType": "Variant Magic - Commander",
  "mulliganType": "TEN" }
{ "kind": "table", "op": "join", "tableId": "<uuid>", "deck": "<.dck text>", "name": "Bot1", "playerType": "Computer - mad", "skill": 2, "thinkSeconds": 3 }
{ "kind": "table", "op": "join", "tableId": "<uuid>", "name": "Alice" }
{ "kind": "table", "op": "start", "tableId": "<uuid>" }
{ "kind": "game", "op": "pause", "gameId": "<uuid>", "seconds": 120 }
{ "kind": "game", "op": "resume", "gameId": "<uuid>" }
{ "kind": "game", "op": "status", "gameId": "<uuid>" }
```

`call`: `method` is the exact `MageServer` name (see `hello.methods`, or
`src/main/resources/mage/server/web/mage-server-signatures.txt`). The `sessionId`
parameter is always the socket's own, `gameId` fills the `gameId` parameter, `args`
fill the remaining parameters in order: UUIDs as strings, enums by name (or their
`toString`, e.g. `"Computer - mad"`), `MageVersion` ignored (the server's is used),
`Object` (`sendPlayerAction` data) as UUID string / string / integer / boolean,
anything else (`MatchOptions`, `DeckCardLists`, `UserData`…) as Gson JSON. A missing
trailing arg is `""` for a String, zero/false for primitives, null otherwise.

`table`: `name` is the seat's player name; on a socket with no user yet, `create` and
a human `join` connect one under that name (anonymous mode, `connectUser` +
`connectSetUserData`). `create` seats its creator with `deck`. Seats are typed at
creation (`seatTypes`, default all `Human`) because `Table.getNextAvailableSeat`
matches the `PlayerType`. A `join` by a name already seated at that table is a
**reconnection**: no new seat, `deck` not needed, `joined.reconnected` is true and their
`User.onReconnect` replays `JOINED_TABLE`, `START_GAME`, `GAME_INIT` and the open
question (works within their 180 s user-expiry window, by user name).

Player types (`seatTypes` at `create`, `playerType` at `join`): the `PlayerType` enum by name
or by its description, matched against what the server registered from `config.xml` — a
`join` with an unknown one is refused with the list. `Human`; `Computer - mad`
(`ComputerPlayer7`: game-tree simulations on both main phases and the attackers/blockers
steps of *every* player's turn, `thinkSeconds` caps each one, three of them saturate a
2-vCPU machine); `Computer - simple` (`ComputerPlayerSimple`: no simulation at all, so it
answers at once — on its own main phases it plays a land and casts the most expensive
spell it can pay for, attacks and blocks with the mad bot's static heuristics, and takes
every other decision with the base AI's heuristics; `skill` and `thinkSeconds` are
accepted and ignored). `Computer - draftbot` is registered but does nothing in a game.

A game needs, in this order: `table create` → `table join` × seats−1 → `table start` →
callback `START_GAME` (`data.gameId`, `data.playerId`) → `call gameJoin` with that
`gameId` → `GAME_INIT`, then `GAME_UPDATE*` and the questions (`GAME_ASK`, `GAME_SELECT`,
`GAME_TARGET`…), answered with `sendPlayerBoolean` / `sendPlayerUUID` /
`sendPlayerString` / `sendPlayerInteger` / `sendPlayerManaType` / `sendPlayerAction`.

Decks: `.dck` text; the `SB:` block is the command zone. `[???:k]` set markers are
accepted (the importer picks a printing by name; `deckReplaced` counts them). A deck is
refused only on an importer `ERROR` line (unknown card name).

`GameView.stack{}` entries get an extra `controllerId` (the controlling player's UUID,
read from the live game), added to the JSON only — their view classes are untouched.
It is left out when the object is already gone from the stack.

`GameView.players[]` entries get two more, the same way (`Commanders.java`), in a commander
game only: `commanderDamage` — `{ "<commander card id>": 7, … }`, the combat damage that
player has TAKEN, by the card id of the commander that dealt it, zero left out — and
`commanderIds`, that player's own commander card ids in every zone, sorted. Both are read
off the live game (`Game.getCommandersIds`, then the `CommanderInfoWatcher` of each
commander card, which is where their own 903.10a check reads it); nothing in a `PlayerView`
carries the count otherwise, only an English line the watcher pins on the commander's card.
`commanderIds` is what names the damage keys: `commandList` holds a commander only while it
waits in the command zone, and a commander deals its damage from the battlefield.

## Holding the game (`game` frames)

`pause` holds the running game for every seat but the one asking, `resume`
gives it back, `status` says how it stands; each answers
`{ kind: "result", method: "game.pause", data: { paused, by, playerId, millisLeft } }`
and the first two say so to every seat in the game's chat
(`⏸ <name> paused the game` / `⏸ <name> resumed the game`).

Guards: the game must be running, the asker must hold a seat at it (a watcher
cannot stop a table), a hold lapses by itself after `GameHold.MAX_MILLIS`
(two minutes) so a lost browser cannot freeze a table, and anybody at the
table may lift it.

`Game.pause()` is **not** used: every loop of `GameImpl` *returns* on it, so
`Game.start()` returns to `GameWorker`, which ends the game there and then
(`endGameWithResult`), and `resume()` runs the rest of the match on the thread
that calls it. `mage.game.GameHold` instead is read by the game thread itself
just before `player.priority(game)` in `GameImpl.playPriority`, and waits
there; simulated games are never held.

## Proof

```sh
node Mage.Server.Web/proof/client.mjs ws://localhost:17172 3        # Node >= 22, no dependency
```

Plays a four-seat Commander Free For All (1 human + 3 `Computer - mad`, sample decks
from `Mage.Client/release/sample-decks`), checks that every `GameView` has the
contract's top-level keys, answers the questions, passes priority with
`sendPlayerAction`, drops and reopens the socket once to prove reconnection, and
prints every frame. Ends with `PROOF OK`.

## What was changed in their files

- `Mage.Server/src/main/java/mage/server/Main.java`: the `MageServerImpl` goes into a
  local (1 line) and, after `server.start()`, `ServiceLoader.load(ServerExtension.class)`
  is iterated (4 lines).
- `Mage.Server/src/main/java/mage/server/ServerExtension.java`: new, the one-method hook.
- `pom.xml`: the `<module>Mage.Server.Web</module>` line.

Nothing in `Session`, `SessionManagerImpl`, `MageServerImpl` or the view classes.

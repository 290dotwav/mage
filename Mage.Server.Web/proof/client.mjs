// Proof client for the web door (docs/XMAGE-WIRE.md, piece A).
// Drives a whole four-seat Commander Free For All through ws://localhost:17172 with
// nothing but the wire's JSON frames, and prints every frame it sends and receives:
//   table create (1 Human + 3 "Computer - mad") -> 3 x table join -> table start
//   -> START_GAME -> call gameJoin -> GAME_INIT/GAME_UPDATE with a complete GameView
//   -> answers GAME_ASK / GAME_SELECT / GAME_TARGET / ... -> sendPlayerAction PASS_PRIORITY_UNTIL_NEXT_TURN
//   -> after N turns: sendPlayerAction CONCEDE, matchQuit, close.
// Reconnection (what piece D relies on): after a few GAME_UPDATEs the socket is dropped on
// purpose, a new one is opened and "table join { tableId, name }" (no deck) is sent: the door
// must answer joined { reconnected: true } and their User.onReconnect must replay
// JOINED_TABLE, START_GAME, GAME_INIT and the open question. Disable with a third arg "noreconnect".
// Needs Node >= 22 (built-in WebSocket), no dependency.
//   node Mage.Server.Web/proof/client.mjs [ws://localhost:17172] [turnsToPlay=3] [noreconnect]
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const url = process.argv[2] ?? 'ws://localhost:17172';
const turnsToPlay = Number(process.argv[3] ?? 3);
const testReconnect = process.argv[4] !== 'noreconnect';
const forkRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const deckDir = path.join(forkRoot, 'Mage.Client/release/sample-decks/Commander/Commander 2013');
const deckFiles = ['Power Hungry.dck', 'Eternal Bargain.dck', 'Evasive Maneuvers.dck', 'Mind Seize.dck'];
const decks = deckFiles.map((f) => readFileSync(path.join(deckDir, f), 'utf8'));
// a fresh user name per run: a name the server still knows (180 s after a drop) is a reconnection, not a new player
const myName = `WebProof${Date.now() % 100000}`;

const VIEW_KEYS = ['players', 'myHand', 'stack', 'exiles', 'revealed', 'lookedAt', 'combat', 'phase', 'step', 'turn',
  'activePlayerId', 'activePlayerName', 'priorityPlayerName', 'canPlayObjects', 'opponentHands', 'watchedHands',
  'myPlayerId', 'priorityTime', 'bufferTime'];

const t0 = Date.now();
const stamp = () => `[${((Date.now() - t0) / 1000).toFixed(1).padStart(6)}s]`;
const log = (...a) => console.log(stamp(), ...a);
const clip = (s, n = 700) => (s.length > n ? s.slice(0, n) + ` …(${s.length} chars)` : s);

const stats = { received: {}, sent: {}, answered: 0, passActions: 0, fullViews: 0, missingKeys: null, errors: 0, reconnected: null, updatesAfterReconnect: 0, stackShown: 0 };
let tableId, gameId, myPlayerId, firstViewShown = false, passedThisTurn = -1, finishing = false, done = false;
let seq = 0, updates = 0, reconnectPhase = testReconnect ? 'pending' : 'off'; // pending -> dropping -> rejoining -> done
const pending = new Map();

let ws;

function send(frame) {
  frame.id = ++seq;
  const text = JSON.stringify(frame);
  stats.sent[frame.kind === 'call' ? `call ${frame.method}` : `table ${frame.op}`] ??= 0;
  stats.sent[frame.kind === 'call' ? `call ${frame.method}` : `table ${frame.op}`]++;
  log('->', clip(JSON.stringify({ ...frame, deck: frame.deck ? `<${frame.deck.length} chars of .dck>` : undefined }), 300));
  if (ws.readyState !== WebSocket.OPEN) return Promise.reject(new Error('socket not open'));
  ws.send(text);
  return new Promise((resolve, reject) => pending.set(frame.id, { resolve, reject }));
}
const call = (method, args = []) => send({ kind: 'call', method, gameId, args });

function answer(method, args) {
  stats.answered++;
  return call(method, args);
}

async function run() {
  const me = await send({
    kind: 'table', op: 'create', gameType: 'Commander Free For All', seats: 4,
    seatTypes: ['Human', 'Computer - mad', 'Computer - mad', 'Computer - mad'],
    deck: decks[0], name: myName, tableName: 'web door proof',
  });
  tableId = me.tableId;
  myPlayerId = me.playerId;
  log(`   table ${tableId}, my seat playerId ${myPlayerId}`);
  for (let i = 1; i <= 3; i++) {
    await send({ kind: 'table', op: 'join', tableId, deck: decks[i], name: `Bot${i}`, playerType: 'Computer - mad', skill: 2 });
  }
  await send({ kind: 'table', op: 'start', tableId });
}

function viewOf(cb) {
  const d = cb.data;
  if (!d || typeof d !== 'object') return null;
  if (Array.isArray(d.players)) return d;            // GameView
  if (d.gameView && Array.isArray(d.gameView.players)) return d.gameView; // GameClientMessage
  return null;
}

function describe(view) {
  const players = view.players.map((p) => `${p.name}${p.hasPriority ? '*' : ''} life=${p.life} hand=${p.handCount} bf=${Object.keys(p.battlefield ?? {}).length} lib=${p.libraryCount}`);
  return `turn ${view.turn} ${view.phase}/${view.step} active=${view.activePlayerName} priority=${view.priorityPlayerName} | ${players.join(' | ')} | myHand=${Object.keys(view.myHand ?? {}).length} stack=${Object.keys(view.stack ?? {}).length}`;
}

async function onCallback(cb, raw) {
  stats.received[cb.method] = (stats.received[cb.method] ?? 0) + 1;
  const view = viewOf(cb);
  const msg = cb.data && typeof cb.data === 'object' && 'message' in cb.data ? cb.data.message : undefined;
  log(`<- callback ${cb.method} msg#${cb.messageId} gameId=${cb.gameId} (${raw.length} bytes)` + (view ? `\n            ${describe(view)}` : '') + (msg !== undefined && cb.method !== 'GAME_UPDATE' ? `\n            message: ${clip(String(msg), 200)}` : ''));

  if (view) {
    for (const [sid, so] of Object.entries(view.stack ?? {})) {
      if (stats.stackShown < 4) {
        stats.stackShown++;
        const owner = view.players.find((p) => p.playerId === so.controllerId)?.name;
        log(`   stack entry ${sid.slice(0, 8)} "${so.name}" controllerId=${so.controllerId} (${owner ?? 'unknown'})`);
      }
    }
    const missing = VIEW_KEYS.filter((k) => !(k in view));
    if (missing.length === 0) stats.fullViews++;
    if (!firstViewShown) {
      firstViewShown = true;
      stats.missingKeys = missing;
      log(`   first GameView keys: ${Object.keys(view).join(', ')}`);
      log(`   contract keys missing: ${missing.length ? missing.join(', ') : 'none'}; myPlayerId=${view.myPlayerId} (seat playerId from joined: ${myPlayerId})`);
      const p = view.players.find((x) => x.playerId === view.myPlayerId) ?? view.players[0];
      log(`   PlayerView keys (${p.name}): ${Object.keys(p).join(', ')}`);
      const card = Object.values(view.myHand ?? {})[0] ?? Object.values(p.battlefield ?? {})[0];
      if (card) log(`   a CardView: ${clip(JSON.stringify(card), 500)}`);
    }
    updates++;
    if (reconnectPhase === 'done') stats.updatesAfterReconnect++;
    if (reconnectPhase === 'pending' && gameId && updates >= 3) {
      reconnectPhase = 'dropping';
      log('   === dropping the socket on purpose (reconnection test) ===');
      ws.close();
      return;
    }
    if (!finishing && view.turn >= turnsToPlay + 1) {
      finishing = true;
      log(`   reached turn ${view.turn}: conceding and quitting`);
      await call('sendPlayerAction', ['CONCEDE']);
      await call('matchQuit');
      setTimeout(() => finish(0), 1500);
      return;
    }
  }
  if (finishing) return;

  switch (cb.method) {
    case 'START_GAME':
      gameId = cb.data.gameId;
      log(`   game ${gameId} started for playerId ${cb.data.playerId}; joining the game`);
      await call('gameJoin');
      break;
    case 'GAME_ASK':
      log(`   raw frame: ${clip(raw, 600)}`);
      await answer('sendPlayerBoolean', [false]); // "Mulligan?" -> keep; any other yes/no -> no
      break;
    case 'GAME_SELECT': {
      if (view && passedThisTurn !== view.turn) {
        passedThisTurn = view.turn;
        stats.passActions++;
        log(`   raw frame: ${clip(raw, 600)}`);
        await answer('sendPlayerAction', ['PASS_PRIORITY_UNTIL_NEXT_TURN']);
      } else {
        await answer('sendPlayerBoolean', [false]); // pass / done
      }
      break;
    }
    case 'GAME_TARGET': {
      const targets = cb.data.targets ?? [];
      const choosable = Object.keys(cb.data.cardsView1 ?? {});
      const pick = targets[0] ?? choosable[0];
      if (cb.data.flag && pick) await answer('sendPlayerUUID', [pick]);
      else await answer('sendPlayerBoolean', [false]);
      break;
    }
    case 'GAME_CHOOSE_ABILITY': {
      const first = Object.keys(cb.data.choices ?? {})[0];
      if (first) await answer('sendPlayerUUID', [first]); else await answer('sendPlayerBoolean', [false]);
      break;
    }
    case 'GAME_CHOOSE_CHOICE': {
      const c = cb.data.choice ?? {};
      const key = Object.keys(c.keyChoices ?? {})[0] ?? (Array.isArray(c.choices) ? c.choices[0] : Object.keys(c.choices ?? {})[0]);
      if (key) await answer('sendPlayerString', [key]); else await answer('sendPlayerBoolean', [false]);
      break;
    }
    case 'GAME_CHOOSE_PILE':
      await answer('sendPlayerBoolean', [true]);
      break;
    case 'GAME_GET_AMOUNT':
      await answer('sendPlayerInteger', [cb.data.min ?? 0]);
      break;
    case 'GAME_PLAY_MANA':
    case 'GAME_PLAY_XMANA':
    case 'GAME_GET_MULTI_AMOUNT':
      await answer('sendPlayerBoolean', [false]);
      break;
    case 'GAME_OVER':
    case 'END_GAME_INFO':
      finishing = true;
      setTimeout(() => finish(0), 1000);
      break;
    default:
      break;
  }
}

function connect() {
  ws = new WebSocket(url);
  ws.onopen = () => log(`connected to ${url}`);
  ws.onerror = (e) => { log('socket error', e.message ?? e); finish(2); };
  ws.onclose = (e) => {
    log(`socket closed (${e.code})`);
    if (reconnectPhase === 'dropping') {
      reconnectPhase = 'rejoining';
      for (const p of pending.values()) p.reject(new Error('socket dropped'));
      pending.clear();
      log('   === reopening and re-seating with table join { tableId, name } (no deck) ===');
      setTimeout(connect, 500);
      return;
    }
    finish(done ? 0 : 3);
  };
  ws.onmessage = (e) => {
    const raw = String(e.data);
    let f;
    try { f = JSON.parse(raw); } catch { log('<- not JSON:', clip(raw, 200)); return; }
    switch (f.kind) {
      case 'hello':
        log(`<- hello session=${f.sessionId} version=${f.version} testMode=${f.testMode} methods=${f.methods.length}`);
        if (reconnectPhase === 'rejoining') {
          send({ kind: 'table', op: 'join', tableId, name: myName })
            .then((j) => { stats.reconnected = j.reconnected === true; reconnectPhase = 'done'; log(`   re-seated: reconnected=${j.reconnected} playerId=${j.playerId} (was ${myPlayerId})`); })
            .catch((err) => { log('re-seat failed:', err.message ?? err); finish(1); });
        } else {
          run().catch((err) => { log('setup failed:', err.message ?? err); finish(1); });
        }
        break;
      case 'callback':
        onCallback(f, raw).catch((err) => log('handler failed:', err.message ?? err));
        break;
      case 'joined':
        log(`<- ${clip(raw, 300)}`);
        pending.get(f.id)?.resolve(f); pending.delete(f.id);
        break;
      case 'result':
        if (f.method !== 'sendPlayerBoolean' && f.method !== 'sendPlayerUUID') log(`<- ${clip(raw, 300)}`);
        pending.get(f.id)?.resolve(f.data); pending.delete(f.id);
        break;
      case 'error':
        stats.errors++;
        log(`<- ${clip(raw, 400)}`);
        if (f.id !== undefined) { pending.get(f.id)?.reject(new Error(f.message)); pending.delete(f.id); }
        break;
      default:
        log('<- unknown frame', clip(raw, 200));
    }
  };
}
connect();

function finish(code) {
  if (done) return;
  done = true;
  const reconnectOk = !testReconnect || (stats.reconnected === true && stats.updatesAfterReconnect > 0);
  const ok = stats.fullViews > 0 && (stats.missingKeys?.length ?? 1) === 0 && stats.answered > 0 && stats.passActions > 0 && reconnectOk;
  console.log('\n===== proof summary =====');
  console.log('received callbacks:', JSON.stringify(stats.received));
  console.log('sent frames       :', JSON.stringify(stats.sent));
  console.log(`complete GameViews received: ${stats.fullViews}; questions answered: ${stats.answered}; PASS_PRIORITY_UNTIL_NEXT_TURN sent: ${stats.passActions}; error frames: ${stats.errors}`);
  if (testReconnect) console.log(`reconnection: joined.reconnected=${stats.reconnected}, game frames after re-seat: ${stats.updatesAfterReconnect}`);
  console.log(ok ? 'PROOF OK' : 'PROOF INCOMPLETE');
  try { ws.close(); } catch {}
  setTimeout(() => process.exit(ok ? code : Math.max(code, 1)), 200);
}

setTimeout(() => { log('timeout'); finish(2); }, 300_000);

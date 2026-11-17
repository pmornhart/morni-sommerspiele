/**
 * Echter Test von Morni Sommerspiele mit Test-Spielern (mineflayer) auf Paper 26.1.2 – gestartet von test/run.sh.
 *   node sommerspiele-test.cjs <host> <port> <rcon-host:port> <rcon-passwort-datei> <server-ordner>
 *
 * Spieler: Anna (deutsch, Party-Leiterin mit Carl), Bert (englisch), Carl, Dora. Sprache und Party kommen vom
 * Test-Netzwerk (test/fake-network), das auch Statistik, Zustand, „spielt“, „Platz frei“ und „zur Lobby“ in calls.log
 * mitschreibt.
 *
 * Boote, Loren und Läufer fahren in mineflayer nicht von selbst – dafür gibt es /sommeradmin fahren|laufen (das Plugin
 * schiebt sie die Strecke entlang, die Zeitnahme sieht es wie echt) und /sommeradmin schiessen (genau gezielter Pfeil
 * mit Schwerkraft und Wind). Bert schießt einmal wirklich mit dem Bogen.
 *
 * Verfasst mit Claude 🤖
 */
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const { Vec3 } = require('vec3');
const { rcon } = require('./rcon.cjs');

const [HOST = '127.0.0.1', PORT = '29300', RCON = '127.0.0.1:29310', PASS_FILE, SERVER] = process.argv.slice(2);
const PASS = fs.readFileSync(PASS_FILE, 'utf8').trim();
const VERSION = process.env.MC_VERSION ?? '26.1';
const FLOOR = 100;
const SPAWN = { x: 0.5, y: 101, z: 30.5 };
/** rueckkehr-sekunden im Test (test/run.sh). */
const COMEBACK = 20;

const results = [];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
function check(name, ok, detail = '') {
  results.push({ name, ok: Boolean(ok) });
  console.log(`${ok ? '✓' : '✗'} ${name}${detail ? ` – ${detail}` : ''}`);
}

/** Text aus Chat-Komponenten (String, JSON oder NBT) zusammensetzen. */
function plain(v) {
  if (v == null) return '';
  if (typeof v === 'string') {
    if (v.startsWith('{') || v.startsWith('[')) {
      try {
        return plain(JSON.parse(v));
      } catch {
        return v;
      }
    }
    return v;
  }
  if (Array.isArray(v)) return v.map(plain).join('');
  if (typeof v !== 'object') return String(v);
  if (v.type === 'compound') return plain(v.value);
  if (v.type === 'list') return plain(v.value?.value ?? []);
  if (v.type === 'string') return v.value;
  let s = '';
  if ('text' in v) s += plain(v.text);
  if ('' in v) s += plain(v['']);
  if ('translate' in v) s += plain(v.translate);
  if ('extra' in v) s += plain(v.extra);
  return s;
}

function offlineUuid(name) {
  const h = crypto.createHash('md5').update(`OfflinePlayer:${name}`, 'utf8').digest();
  h[6] = (h[6] & 0x0f) | 0x30;
  h[8] = (h[8] & 0x3f) | 0x80;
  const x = h.toString('hex');
  return `${x.slice(0, 8)}-${x.slice(8, 12)}-${x.slice(12, 16)}-${x.slice(16, 20)}-${x.slice(20)}`;
}

const cmd = (...commands) => rcon(RCON, PASS, commands);
const dataDir = path.join(SERVER, 'plugins/MorniSommerspiele');
const playerFile = (name) => path.join(dataDir, 'spieler', `${offlineUuid(name)}.yml`);
const readFile = (file) => (fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '');
const calls = () =>
  readFile(path.join(SERVER, 'plugins/MoinMorni/calls.log'))
    .split('\n')
    .filter(Boolean)
    .map((l) => JSON.parse(l));
const inWorld = (c) => `execute in minecraft:sommerspiele run ${c}`;
/** Netzwerk-Aufrufe einer Art für einen Namen (ab Stelle {from} in calls.log). */
const callsOf = (t, name, from = 0) => calls().slice(from).filter((c) => c.t === t && c.name === name);

async function waitFor(cond, ms = 5000, step = 150) {
  const start = Date.now();
  while (Date.now() - start < ms) {
    if (await cond()) return true;
    await sleep(step);
  }
  return Boolean(await cond());
}

let lastJoin = 0;
const bots = {};
async function join(name) {
  const wait = lastJoin + 3500 - Date.now();
  if (wait > 0) await sleep(wait);
  lastJoin = Date.now();
  const bot = mineflayer.createBot({ host: HOST, port: Number(PORT), username: name, version: VERSION, auth: 'offline' });
  bot.chats = [];
  bot.bars = [];
  bot.titles = [];
  bot.kickReason = null;
  bot.on('messagestr', (m, position) => (position === 'game_info' ? bot.bars : bot.chats).push(m));
  bot._client.on('action_bar', (p) => bot.bars.push(plain(p.text)));
  bot._client.on('set_title_text', (p) => bot.titles.push(plain(p.text)));
  bot.on('kicked', (r) => {
    bot.kickReason = plain(r);
  });
  bot.on('error', (e) => console.log(`  (${name}: ${e.message})`));
  await new Promise((resolve, reject) => {
    bot.once('spawn', resolve);
    bot.once('kicked', (r) => reject(new Error(`${name} gekickt: ${plain(r)}`)));
    setTimeout(() => reject(new Error(`${name}: kein Spawn`)), 30000);
  });
  await sleep(1500);
  bots[name] = bot;
  return bot;
}

const slot = (bot, i) => bot.inventory.slots[36 + i];
const near = (p, q, d) => Math.abs(p.x - q.x) < d && Math.abs(p.y - q.y) < d && Math.abs(p.z - q.z) < d;
const sees = (bot, re) => bot.chats.some((c) => re.test(c));
const sawBar = (bot, re) => bot.bars.some((c) => re.test(c));
const sawTitle = (bot, re) => bot.titles.some((t) => re.test(t));
const clear = (...list) =>
  list.forEach((b) => {
    b.chats.length = 0;
    b.bars.length = 0;
    b.titles.length = 0;
  });

/** /sommeradmin status als Werte. */
async function status() {
  const lines = (await cmd('sommeradmin status'))[0].split('\n');
  const s = { top: {}, comp: {}, racers: {}, stage: {}, teams: {}, karts: {}, archers: {}, runners: {} };
  const kv = (line) => {
    const o = {};
    for (const part of line.trim().split(' ')) {
      const i = part.indexOf('=');
      if (i > 0) {
        const v = part.slice(i + 1);
        o[part.slice(0, i)] = /^-?\d+(\.\d+)?$/.test(v) ? Number(v) : v === 'true' ? true : v === 'false' ? false : v;
      }
    }
    return o;
  };
  for (const line of lines) {
    const o = kv(line);
    if ('bereit' in o) s.top = o;
    else if ('wettkampf' in o) s.comp = o;
    else if ('r' in o) s.racers[o.r] = o;
    else if ('stage' in o) s.stage = o;
    else if ('t' in o) s.teams[o.t] = o;
    else if ('k' in o) s.karts[o.k] = o;
    else if ('a' in o) s.archers[o.a] = o;
    else if ('l' in o) s.runners[o.l] = o;
  }
  return s;
}

async function openWithItem(bot, hotbar) {
  bot.setQuickBarSlot(hotbar);
  await sleep(250);
  const opened = new Promise((resolve) => {
    bot.once('windowOpen', resolve);
    setTimeout(() => resolve(null), 4000);
  });
  bot.activateItem();
  return opened;
}

async function closeWindow(bot) {
  if (bot.currentWindow) bot.closeWindow(bot.currentWindow);
  await sleep(200);
}

async function useItem(bot, hotbar) {
  bot.setQuickBarSlot(hotbar);
  await sleep(250);
  bot.activateItem();
  await sleep(400);
}

/** Bis der Lauf/die Phase erreicht ist. */
async function waitStage(pred, ms = 30000) {
  let s = null;
  const ok = await waitFor(async () => {
    s = await status();
    return pred(s);
  }, ms, 400);
  return ok ? s : null;
}


const P = ['Anna', 'Bert', 'Carl', 'Dora'];
const posY = async (name) => Number(String(await cmd(`data get entity ${name} Pos[1]`)).match(/(-?\d+\.\d+)d/)?.[1]);

async function main() {
  // ------------------------------------------------------------------ Dorf
  await sleep(4000);
  const holos = String(await cmd(inWorld('execute positioned 0 101 0 if entity @e[type=text_display,distance=..60]')));
  check('Schrift im Dorf steht (Tafeln, Schilder)', /: (\d+)/.test(holos) && Number(holos.match(/: (\d+)/)[1]) >= 8, holos.trim());
  const rail = String(await cmd(inWorld('execute if block -170 125 -183 minecraft:rail')));
  check('Schienen der Seifenkiste oben am Start', /passed|Test passed/i.test(rail), rail.trim());
  const water = String(await cmd(inWorld('execute if block 84 100 50 minecraft:water')));
  check('Bergsee mit Wasser in Bahn 1', /passed/i.test(water), water.trim());
  const hay = String(await cmd(inWorld('execute if block -10 101 82 minecraft:hay_block')));
  check('Heuballen-Hürde auf dem Almlauf', /passed/i.test(hay), hay.trim());

  const anna = await join('Anna');
  const bert = await join('Bert');
  check('Ankunft im Sommerdorf', near(anna.entity.position, SPAWN, 3) && anna.game.gameMode === 'adventure',
    JSON.stringify(anna.entity.position));
  const below = anna.blockAt(anna.entity.position.offset(0, -1, 0));
  check('Fester Boden unter den Füßen', below && below.name !== 'air', below?.name);
  check('Schnellleiste: Kompass, Farbstoff, Gold, Bett',
    slot(anna, 0)?.name === 'compass' && slot(anna, 1)?.name === 'gray_dye' && slot(anna, 4)?.name === 'gold_nugget' &&
      slot(anna, 8)?.name === 'red_bed', [0, 1, 4, 8].map((i) => slot(anna, i)?.name).join(','));
  check('Englisch (Sprache vom Netzwerk): „Vote & watch“', plain(slot(bert, 0)?.customName).includes('Vote & watch'));
  check('Begrüßung + Erklärung im Chat', sees(anna, /Willkommen im Sommerdorf am Bergsee/) && sees(anna, /Seifenkiste/));
  check('Englische Begrüßung', sees(bert, /Welcome to the summer village/));
  check('Leiste rechts (Scoreboard) vorhanden', await waitFor(() => Object.keys(anna.scoreboards ?? {}).length > 0, 4000));
  check('Netzwerk: Zustand „waiting“', calls().some((c) => c.t === 'state' && c.state === 'waiting' && c.mode === 'sommerspiele'));

  let win = await openWithItem(anna, 0);
  check('Kompass öffnet „Was kommt als Nächstes?“ mit Wettkampf und vier Disziplinen',
    win && plain(win.slots[9]?.customName).includes('Wettkampf') && plain(win.slots[11]?.customName).includes('Almlauf') &&
      plain(win.slots[13]?.customName).includes('Bogenschießen') && /regatta/i.test(plain(win.slots[15]?.customName)) &&
      plain(win.slots[17]?.customName).includes('Seifenkisten'), [9, 11, 13, 15, 17].map((i) => plain(win?.slots[i]?.customName)).join(' / '));
  await anna.clickWindow(9, 0, 0);
  await sleep(600);
  check('Stimme für den Wettkampf', sees(anna, /Deine Stimme: Wettkampf/));
  bert.chat('/abstimmen wettkampf');
  await sleep(600);
  check('Bert stimmt per Befehl (englisch)', sees(bert, /Your vote: Competition/));
  win = await openWithItem(anna, 4);
  check('Gold-Nugget öffnet „Medaillen & Bestwerte“', win && plain(win.title).includes('Medaillen'), win && plain(win.title));
  await closeWindow(anna);

  // ------------------------------------------------------------------ Saison: Gegenstück zu Wintersport
  const cfgFile = path.join(dataDir, 'config.yml');
  const cfg = readFile(cfgFile);
  fs.writeFileSync(cfgFile, cfg.replace('saison: immer', 'saison: sommer'));
  await cmd('sommeradmin testdatum 2027-01-15', 'sommeradmin neuladen');
  check('Januar + Saison „sommer“: Netzwerk-Zustand „closed“',
    await waitFor(() => { const st = calls().filter((c) => c.t === 'state'); return st[st.length - 1]?.state === 'closed'; }, 4000));
  clear(anna, bert);
  await useItem(anna, 1);
  check('Bereit geht nicht: Winterpause mit Datum', sees(anna, /Winterpause.*1\. April/), anna.chats.slice(-1)[0]);
  check('Aktionsleiste nennt die Pause (englisch)', await waitFor(() => sawBar(bert, /Winter break.*April 1/), 3000), bert.bars.slice(-1)[0]);
  await cmd('sommeradmin testdatum 2027-07-01');
  check('Im Juli ist offen (Zustand wieder „waiting“)', await waitFor(() => {
    const st = calls().filter((c) => c.t === 'state');
    return st[st.length - 1]?.state === 'waiting';
  }, 4000));
  fs.writeFileSync(cfgFile, cfg);
  await cmd('sommeradmin testdatum aus', 'sommeradmin neuladen');

  // ------------------------------------------------------------------ Wettkampf: Start
  const carl = await join('Carl');
  let dora = await join('Dora');
  clear(anna, bert, carl, dora);
  for (const b of [anna, bert, carl, dora]) {
    b.setQuickBarSlot(1);
    await sleep(150);
    b.activateItem();
    await sleep(250);
  }
  check('Alle bereit → Countdown', await waitFor(() => sawBar(anna, /Start in \d+ s · Wettkampf/), 4000), anna.bars.slice(-1)[0]);
  let s = await waitStage((x) => x.comp.disziplin === 'almlauf' && x.stage.phase === 'COUNT', 30000);
  check('Wettkampf startet mit dem Almlauf', Boolean(s), JSON.stringify(s?.comp));
  check('„Auf geht\'s!“ mit 4 Leuten', sees(anna, /Auf geht's! Wettkampf \(alle vier\) mit 4 Leuten/));
  check('Netzwerk: Zustand „running“ und alle vier „spielen“', await waitFor(() => calls().some((c) => c.t === 'state' && c.state === 'running')
    && P.every((n) => callsOf('playing', n).some((c) => c.playing === true)), 3000));
  check('Erklärung zum Almlauf (englisch)', sees(bert, /Alpine run: 1 lap/));
  const before = anna.entity.position.clone();
  anna.setControlState('forward', true);
  await sleep(900);
  anna.clearControlStates();
  await sleep(500);
  check('Vor dem Start: alle stehen fest', Math.abs(anna.entity.position.z - before.z) < 0.9, `${before} → ${anna.entity.position}`);

  // ------------------------------------------------------------------ Almlauf
  s = await waitStage((x) => x.stage.phase === 'RUN', 10000);
  check('Massenstart: LOS', Boolean(s) && sawTitle(anna, /LOS/) && sawTitle(bert, /GO/));
  // Anna läuft wirklich ein paar Blöcke nach Norden, dann übernimmt der Selbstläufer.
  anna.setControlState('forward', true);
  anna.setControlState('sprint', true);
  await sleep(1500);
  anna.clearControlStates();
  check('Anna läuft wirklich los (nach Norden)', anna.entity.position.z < before.z - 2, `${before.z} → ${anna.entity.position.z}`);
  await cmd('sommeradmin laufen Anna');
  await sleep(1500);
  await cmd('sommeradmin laufen Carl');
  await sleep(1500);
  await cmd('sommeradmin laufen Bert', 'sommeradmin laufen Dora');
  check('Kontrollpunkte zählen', await waitStage((x) => x.runners.Anna?.cp >= 2, 20000) != null, JSON.stringify((await status()).runners.Anna));
  // Zurück ins Spiel: Dora verliert die Verbindung mitten im Lauf und kommt gleich wieder.
  let mark = calls().length;
  const doraCp = (await status()).runners.Dora?.cp ?? 0;
  dora.quit();
  check('Almlauf: Dora weg, Platz bleibt frei (holdSlot 20)', await waitFor(() =>
    callsOf('hold-slot', 'Dora', mark).some((c) => c.seconds === COMEBACK), 5000), JSON.stringify(calls().slice(mark)));
  check('Die anderen erfahren es', await waitFor(() => sees(anna, /Dora hat die Verbindung verloren/), 3000));
  await sleep(2500);
  dora = await join('Dora');
  check('Almlauf: Dora zurück, gleicher Stand', await waitFor(async () => {
    const x = (await status()).runners.Dora;
    return x?.state === 'run' && x.cp >= doraCp;
  }, 6000) && sees(dora, /Willkommen zurück/), JSON.stringify((await status()).runners.Dora));
  check('Platz wieder frei (holdSlot 0)', callsOf('hold-slot', 'Dora', mark).some((c) => c.seconds === 0));
  await cmd('sommeradmin laufen Dora');
  // Carl benutzt die Feder (zurück zum letzten Kontrollpunkt) und läuft dann weiter.
  await sleep(1500);
  await useItem(carl, 0);
  check('Feder: zurück zum letzten Kontrollpunkt', await waitFor(() => sees(carl, /Zurück auf die Strecke/), 3000));
  await cmd('sommeradmin laufen Carl');
  s = await waitStage((x) => x.comp.disziplin !== 'almlauf' || x.comp.phase === 'BETWEEN', 120000);
  check('Alle im Ziel, Almlauf gewertet', Boolean(s), JSON.stringify(s?.comp));
  await waitFor(() => sees(anna, /Medaillenspiegel/), 8000);
  check('„Im Ziel!“ mit Zeit (de/en)', sees(anna, /Im Ziel! Zeit: \d:\d\d,\d\d/) && sees(bert, /Finished! Time: \d:\d\d\.\d\d/));
  check('Ergebnisliste und Gold für Anna (zuerst los)', sees(anna, /Ergebnis Almlauf/) && sees(anna, /GOLD! Du gewinnst Almlauf/),
    anna.chats.filter((c) => /Gold|GOLD|Silber|Bronze/.test(c)).join(' | '));
  const st1 = calls().filter((c) => c.t === 'stats' && c.name === 'Anna');
  check('Netzwerk: Gold + Zeit für Anna', st1.some((c) => c.add.gold === 1 && c.add.races === 1) &&
    st1.some((c) => c.best.time_almlauf_ms > 0 && c.lower === true), JSON.stringify(st1));
  check('„Gleich geht es weiter mit Bogenschießen“', await waitFor(() => sees(anna, /weiter mit Bogenschießen/), 5000));

  // ------------------------------------------------------------------ Bogenschießen
  s = await waitStage((x) => x.comp.disziplin === 'bogen' && x.stage.phase === 'shoot', 30000);
  check('Bogenschießen: alle an ihrer Bahn, Durchgang 1', Boolean(s) && Object.keys(s.archers).length === 4 && s.stage.round === 1,
    JSON.stringify(s?.stage));
  check('Erklärung und Titel „Durchgang 1/3 · 20 Blöcke“', sees(anna, /Bogenschießen: drei Durchgänge/) && sawTitle(anna, /Durchgang 1\/3/));
  await waitFor(() => slot(anna, 0)?.name === 'bow', 4000);
  check('Bogen und drei Pfeile', slot(anna, 0)?.name === 'bow' && slot(anna, 1)?.name === 'arrow' && slot(anna, 1)?.count === 3,
    `${slot(anna, 0)?.name} ${slot(anna, 1)?.name}×${slot(anna, 1)?.count}`);
  await cmd('sommeradmin wind 0');
  // Bert schießt einmal wirklich (mineflayer): auf die Scheibe in 20 Blöcken, etwas höher wegen der Schwerkraft.
  const bertLane = s.archers.Bert.lane - 1;
  const bx = -129 + 6 * bertLane + 0.5;
  bert.setQuickBarSlot(0);
  await sleep(300);
  await bert.lookAt(new Vec3(bx, FLOOR + 4.3, 60), true);
  bert.activateItem();
  await sleep(1300);
  await bert.lookAt(new Vec3(bx, FLOOR + 4.3, 60), true);
  bert.deactivateItem();
  check('Echter Bogenschuss zählt (Pfeil 1 von 3)', await waitFor(async () => (await status()).archers.Bert?.fired === 1, 4000),
    JSON.stringify((await status()).archers.Bert));
  check('Treffer/Daneben wird angezeigt', await waitFor(() => sawBar(bert, /Hit|Miss|Bullseye/), 4000), bert.bars.slice(-2).join(' | '));
  const shootRound = async (offs) => {
    for (let i = 0; i < 3; i++) {
      for (const [n, o] of Object.entries(offs)) {
        if (o[i] !== undefined) await cmd(`sommeradmin schiessen ${n} ${o[i]}`);
      }
      await sleep(400);
    }
  };
  await shootRound({ Anna: [0, 0, 0], Bert: [undefined, 0.6, 0.6], Carl: [1.0, 1.0, 1.0], Dora: [3.2, 3.2, 3.2] });
  check('Anna: Mitte! (10)', await waitFor(() => sawBar(anna, /Mitte! 10/), 4000), anna.bars.slice(-2).join(' | '));
  check('Dora daneben (0)', await waitFor(() => sawBar(dora, /Daneben/), 4000));
  s = await waitStage((x) => x.stage.round === 2 && x.stage.phase === 'shoot', 30000);
  check('Durchgang 1 gewertet, Durchgang 2 (35 Blöcke)', Boolean(s) && sees(anna, /Durchgang 1: 30 Ringe/), anna.chats.filter((c) => /Durchgang/.test(c)).join(' | '));
  // Wind: stark von Westen – der gezielte Pfeil gleicht ihn aus und trifft trotzdem (der Wind wirkt also wirklich).
  await cmd('sommeradmin wind 3');
  await shootRound({ Anna: [0, 0, 0], Bert: [0.6, 0.6, 0.6], Carl: [1.0, 1.0, 1.0], Dora: [3.2, 3.2, 3.2] });
  s = await waitStage((x) => x.stage.round === 3 && x.stage.phase === 'shoot', 30000);
  check('Wind 3 ausgeglichen: wieder 30 Ringe', Boolean(s) && sees(anna, /Durchgang 2: 30 Ringe/), anna.chats.filter((c) => /Durchgang 2/.test(c)).join(' | '));
  await cmd('sommeradmin wind 0');
  await shootRound({ Anna: [0, 0, 0], Bert: [0.6, 0.6, 0.6], Carl: [1.0, 1.0, 1.0], Dora: [3.2, 3.2, 3.2] });
  s = await waitStage((x) => x.comp.disziplin !== 'bogen' || x.comp.phase === 'BETWEEN', 40000);
  check('Bogenschießen gewertet', Boolean(s));
  await waitFor(() => sees(anna, /Ergebnis Bogenschießen/), 6000);
  check('Ergebnisliste in Ringen: Anna 90 Ringe mit Mitten, Gold', sees(anna, /1\. Anna – 90 Ringe \(\d× Mitte\)/) && sees(anna, /GOLD! Du gewinnst Bogenschießen/),
    anna.chats.filter((c) => /Ringe/.test(c)).join(' | '));
  check('Netzwerk: Ringe für Anna', calls().some((c) => c.t === 'stats' && c.name === 'Anna' && c.add.bogen_rings === 90 && c.best.bogen_best === 90),
    JSON.stringify(calls().filter((c) => c.t === 'stats' && c.name === 'Anna').slice(-2)));

  // ------------------------------------------------------------------ Regatta
  s = await waitStage((x) => x.comp.disziplin === 'regatta' && x.stage.phase === 'COUNT', 30000);
  check('Regatta: drei Boote (Anna & Carl im Zweier, Bert, Dora)', Boolean(s) && Object.keys(s.teams).length === 3 && s.teams['Anna+Carl'],
    JSON.stringify(Object.keys(s?.teams ?? {})));
  check('Zweier-Hinweis', sees(carl, /im Zweier: Anna lenkt/));
  s = await status();
  check('Alle sitzen im Boot am Startsteg', Object.values(s.teams).every((t) => t.boat) && [anna, bert, carl, dora].every((b) => b.vehicle != null),
    Object.values(s.teams).map((t) => `${t.t}:${t.passengers}`).join(' '));
  const zStart = s.teams.Bert?.z;
  s = await waitStage((x) => x.stage.phase === 'RUN', 12000);
  check('Countdown, LOS – vorher hält der Startrichter die Boote', Boolean(s) && Math.abs((s?.teams.Bert?.z ?? 0) - zStart) < 0.8);
  await cmd('sommeradmin fahren Anna ziel', 'sommeradmin fahren Bert tor 0 daneben', 'sommeradmin fahren Dora ziel');
  check('Tor verpasst: Strafe (englisch „Gate missed! +4 s“)', await waitFor(() => sawBar(bert, /Gate missed! \+4 s/), 6000), bert.bars.slice(-3).join(' | '));
  check('Strömung wird in der Aktionsleiste angezeigt', await waitFor(() => sawBar(anna, /Strömung/), 15000), anna.bars.slice(-3).join(' | '));
  await cmd('sommeradmin fahren Bert ziel');
  s = await waitStage((x) => x.comp.disziplin !== 'regatta' || x.comp.phase === 'BETWEEN', 90000);
  check('Regatta gewertet', Boolean(s));
  await waitFor(() => sees(anna, /Ergebnis Ruderregatta/), 6000);
  check('Ergebnis: Anna & Carl gemeinsam, Bert mit verpasstem Tor', sees(anna, /Anna & Carl/) && sees(anna, /1 Tor\(e\) verpasst/),
    anna.chats.filter((c) => /Anna & Carl|verpasst/.test(c)).join(' | '));
  check('Medaille für beide im Zweier', sees(carl, /Ruderregatta/) && calls().some((c) => c.t === 'stats' && c.name === 'Carl' && c.best.time_regatta_ms > 0));

  // ------------------------------------------------------------------ Seifenkiste
  s = await waitStage((x) => x.comp.disziplin === 'seifenkiste' && x.stage.phase === 'COUNT', 30000);
  check('Seifenkiste: vier Loren in einem Lauf', Boolean(s) && Object.keys(s.karts).length === 4, JSON.stringify(s?.stage));
  check('Erklärung Bremse und Weiche', sees(anna, /S \(rückwärts\) = bremsen/) && sees(anna, /Weiche/) && sees(bert, /derail/));
  check('Alle sitzen in der Lore', Object.values(s.karts).every((k) => k.seated) && [anna, bert, carl, dora].every((b) => b.vehicle != null),
    Object.values(s.karts).map((k) => `${k.k}:${k.seated}`).join(' '));
  const s0 = s.karts.Anna.s;
  // Sneak darf nicht aus der Lore werfen.
  anna.setControlState('sneak', true);
  await sleep(600);
  anna.setControlState('sneak', false);
  await sleep(400);
  check('Aussteigen geht im Lauf nicht (Schleichen)', anna.vehicle != null);
  s = await waitStage((x) => x.stage.phase === 'RUN', 12000);
  check('Vor dem LOS steht die Lore', Boolean(s) && sawTitle(anna, /LOS/) && Math.abs(s.karts.Anna.s - s0) < 1.5, `${s0} → ${s?.karts.Anna.s}`);
  await cmd('sommeradmin fahren Anna auto steil', 'sommeradmin fahren Carl auto kehre', 'sommeradmin fahren Bert vollgas kehre',
    'sommeradmin fahren Dora auto kehre');
  check('Lore fährt bergab (Tempo steigt)', await waitStage((x) => x.karts.Anna?.v > 0.5, 15000) != null, JSON.stringify((await status()).karts.Anna));
  const yA = await posY('Anna');
  check('Lore mit Fahrer fährt die Schienen hinab', await waitFor(async () => (await posY('Anna')) < yA - 3, 10000), `${yA}`);
  check('Vollgas ohne Bremse: Bert entgleist („Derailed!“)', await waitFor(() => sawTitle(bert, /Derailed/), 20000), bert.titles.slice(-3).join(','));
  check('Weiche: Anna nimmt das Steilstück', await waitStage((x) => x.karts.Anna?.switched === true && x.karts.Anna.steep === true, 25000) != null);
  // Zurück ins Spiel in der Lore: Dora geht und kommt wieder.
  mark = calls().length;
  dora.quit();
  check('Seifenkiste: Dora weg, die Lore wartet (holdSlot)', await waitFor(async () =>
    callsOf('hold-slot', 'Dora', mark).some((c) => c.seconds === COMEBACK) && (await status()).karts.Dora?.state === 'run', 5000));
  await sleep(2000);
  dora = await join('Dora');
  check('Seifenkiste: Dora sitzt wieder in ihrer Lore', await waitFor(async () => (await status()).karts.Dora?.seated === true && dora.vehicle != null, 8000),
    JSON.stringify((await status()).karts.Dora));
  await cmd('sommeradmin fahren Dora auto kehre');
  // Zu spät: Bert geht und kommt nicht rechtzeitig wieder.
  mark = calls().length;
  bert.quit();
  check('Bert weg (holdSlot)', await waitFor(() => callsOf('hold-slot', 'Bert', mark).some((c) => c.seconds === COMEBACK), 5000));
  s = await waitStage((x) => x.comp.phase === 'CEREMONY' || !x.comp.wettkampf, 150000);
  check('Alle Loren durch – Siegerehrung', Boolean(s), JSON.stringify(s?.comp));
  check('Bert zu spät: hat den Wettkampf verlassen (holdSlot 0)', sees(anna, /Bert ist nicht rechtzeitig zurückgekommen/) &&
    callsOf('hold-slot', 'Bert', mark).some((c) => c.seconds === 0), anna.chats.filter((c) => /Bert/.test(c)).slice(-2).join(' | '));
  check('Ergebnis Seifenkiste mit Entgleisungen', sees(anna, /Ergebnis Seifenkistenrennen/));
  check('Siegerehrung: Gesamtsieg', await waitFor(() => sees(anna, /Gesamtsieg für/), 5000), anna.chats.filter((c) => /Gesamtsieg/.test(c)).join(''));
  check('Bei der Siegerehrung „spielt“ niemand mehr', ['Anna', 'Carl', 'Dora'].every((n) => {
    const l = callsOf('playing', n);
    return l.length > 0 && l[l.length - 1].playing === false;
  }));
  check('Zurück im Dorf', await waitFor(() => near(anna.entity.position, SPAWN, 4), 20000), JSON.stringify(anna.entity.position));
  check('Netzwerk: Wettkampf gezählt', calls().some((c) => c.t === 'stats' && c.name === 'Anna' && c.add.events === 1));
  check('Spielerdatei mit Bestwerten', /bestwert:/.test(readFile(playerFile('Anna'))) && /bogen:/.test(readFile(playerFile('Anna'))),
    readFile(playerFile('Anna')).slice(0, 300));
  check('Bestenliste gespeichert', /seifenkiste/.test(readFile(path.join(dataDir, 'bestenliste.yml'))));
  clear(anna);
  anna.chat('/top bogen');
  check('/top bogen zeigt Ringe', await waitFor(() => sees(anna, /90 Ringe/), 3000));
  anna.setQuickBarSlot(8);
  await sleep(250);
  anna.activateItem();
  check('Bett: zur Lobby – fragt das Netzwerk', await waitFor(() => calls().some((c) => c.t === 'ask-lobby' && c.name === 'Anna'), 3000));

  for (const b of Object.values(bots)) b.quit();
  await sleep(500);
  const failed = results.filter((r) => !r.ok);
  console.log(`\n${results.length - failed.length}/${results.length} grün`);
  if (failed.length) {
    console.log('Fehlgeschlagen:');
    failed.forEach((f) => console.log(`  ✗ ${f.name}`));
  }
  process.exit(failed.length ? 1 : 0);
}

main().catch((e) => {
  console.error(e);
  for (const b of Object.values(bots)) b.quit();
  const failed = results.filter((r) => !r.ok);
  console.log(`\n${results.length - failed.length}/${results.length} grün (abgebrochen)`);
  process.exit(1);
});

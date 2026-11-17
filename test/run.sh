#!/usr/bin/env bash
# Echter Test von Morni Sommerspiele: baut das Plugin und das Test-Netzwerk, richtet einen Paper-Server ein (offline,
# eigener Port, RCON an), startet ihn, schickt Test-Spieler (mineflayer) durch alle Abläufe und hält ihn wieder an.
#
#   test/run.sh                       # alles
#   KEEP=1 test/run.sh                # Server danach laufen lassen (zum Nachschauen)
#   KEEP=1 SKIP_TEST=1 test/run.sh    # nur Server starten
#   FRESH=1 test/run.sh               # Welt und Daten vorher löschen (baut alles neu)
#
# Erwartet (Vorgaben für die MoinMorni-Sandbox, alles per Umgebungsvariable änderbar):
#   SOMMER_JAVA      Java 25 (Ordner; sonst die JDK 25 aus dem Cache, sonst JAVA_HOME)
#   PAPER_JAR        Paper 26.1.2 (mineflayer kann bis 26.1)
#   TEMPLATE         ein schon einmal gestarteter Paper-26.1.2-Ordner (libraries/, cache/, versions/ werden kopiert,
#                    damit nichts heruntergeladen werden muss) – optional
#   SOMMER_NODE_MODULES  node_modules mit mineflayer
#   EXTRA_PLUGINS    weitere Jars für den Server (z. B. ViaVersion + ViaBackwards für einen echten 26.3-Client)
#
# Verfasst mit Claude 🤖
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(dirname "$HERE")"
CACHE="${MM_CACHE:-/home/user/mm-cache}"
CACHED_JDK="$(ls -d "$CACHE"/jdk/jdk-25* 2>/dev/null | head -1 || true)"
JAVA_HOME="${SOMMER_JAVA:-${CACHED_JDK:-${JAVA_HOME:-}}}"
export JAVA_HOME
JAVA="$JAVA_HOME/bin/java"
PAPER_JAR="${PAPER_JAR:-$CACHE/jars/paper-26.1.2.jar}"
TEMPLATE="${TEMPLATE:-$CACHE/servers/t1}"
[ -d "$TEMPLATE" ] || TEMPLATE="$CACHE/servers/duels-test"
SERVER="${SERVER_DIR:-$CACHE/servers/sommerspiele-test}"
PORT="${PORT:-29300}"
RCON_PORT="${RCON_PORT:-29310}"
MEMORY="${MEMORY:-2G}"
export NODE_PATH="${SOMMER_NODE_MODULES:-$CACHE/node/node_modules}${NODE_PATH:+:$NODE_PATH}"

echo "== Bauen"
(cd "$REPO" && nice -n 15 mvn -q -B -o -DskipTests package)
(cd "$REPO" && nice -n 15 mvn -q -B -o -f test/fake-network/pom.xml package)

echo "== Server einrichten: $SERVER"
if [ "${FRESH:-0}" = "1" ]; then
  rm -rf "$SERVER/sommerspiele" "$SERVER/world/dimensions/minecraft/sommerspiele" "$SERVER/plugins/MorniSommerspiele"
fi
mkdir -p "$SERVER/plugins/MorniSommerspiele" "$SERVER/plugins/MoinMorni"
if [ -d "$TEMPLATE" ] && [ ! -d "$SERVER/libraries" ]; then
  cp -r "$TEMPLATE/libraries" "$TEMPLATE/cache" "$TEMPLATE/versions" "$SERVER/" 2>/dev/null || true
fi
cp "$PAPER_JAR" "$SERVER/paper.jar"
echo "eula=true" > "$SERVER/eula.txt"
RCON_PASS="$(head -c 18 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 20)"
printf '%s' "$RCON_PASS" > "$SERVER/.rcon-pass"
chmod 600 "$SERVER/.rcon-pass"
cat > "$SERVER/server.properties" <<PROPS
online-mode=false
enforce-secure-profile=false
server-port=$PORT
enable-rcon=true
rcon.port=$RCON_PORT
rcon.password=$RCON_PASS
level-name=world
level-type=minecraft\:flat
generator-settings={"layers"\:[{"block"\:"minecraft\:bedrock","height"\:1}],"biome"\:"minecraft\:plains"}
generate-structures=false
spawn-protection=0
view-distance=6
simulation-distance=4
max-players=20
white-list=false
enforce-whitelist=false
motd=Morni Sommerspiele Test
allow-flight=true
PROPS
# Testwerte: kurze Countdowns und Pausen.
# Zurück ins Spiel nach 20 Sekunden (der Test kommt einmal rechtzeitig und einmal zu spät zurück).
cat > "$SERVER/plugins/MorniSommerspiele/config.yml" <<'CONF'
welt: sommerspiele
rueckkehr-sekunden: 20
saison: immer
saison-von: 4
saison-bis: 11
tageszeit: tag
bluetenstaub: true
dorf:
  countdown-sekunden: 8
  mindestens-spieler: 3
wettkampf:
  hoechstens-spieler: 12
  pause-sekunden: 3
  siegerehrung-sekunden: 5
almlauf:
  runden: 1
  zeitlimit-sekunden: 180
  tempo: 1
bogen:
  pfeile: 3
  zeit-sekunden: 40
  wind: 2
regatta:
  zweier: true
  strafe-sekunden: 4
  zeitlimit-sekunden: 120
  stroemung: 2
seifenkiste:
  zeitlimit-sekunden: 120
  weiche: true
CONF
# Test-Netzwerk: Bert spricht Englisch, Anna und Carl sind eine Party (Anna führt).
cat > "$SERVER/plugins/MoinMorni/fake.properties" <<'FAKE'
lang.Bert=en
party.Anna=Anna,Carl
FAKE
rm -f "$SERVER/plugins/MoinMorni/calls.log"
rm -rf "$SERVER/plugins/MorniSommerspiele/spieler" "$SERVER/plugins/MorniSommerspiele/bestenliste.yml"
rm -f "$SERVER"/plugins/Morni-Sommerspiele.jar
cp "$REPO/target/Morni-Sommerspiele.jar" "$SERVER/plugins/"
cp "$REPO/test/fake-network/target/MoinMorni-FakeNetwork.jar" "$SERVER/plugins/"
for jar in ${EXTRA_PLUGINS:-}; do
  cp "$jar" "$SERVER/plugins/"
done

echo "== Server starten (Port $PORT, RCON $RCON_PORT)"
cd "$SERVER"
: > console.log
nice -n 15 "$JAVA" -Xms256M -Xmx"$MEMORY" -jar paper.jar --nogui < /dev/null > console.log 2>&1 &
PID=$!
echo "$PID" > "$SERVER/.pid"
stop_server() {
  if kill -0 "$PID" 2>/dev/null; then
    node "$HERE/rcon.cjs" "127.0.0.1:$RCON_PORT" "$SERVER/.rcon-pass" stop >/dev/null 2>&1 || kill "$PID" || true
    for _ in $(seq 1 60); do kill -0 "$PID" 2>/dev/null || break; sleep 1; done
    kill -9 "$PID" 2>/dev/null || true
  fi
}
if [ "${KEEP:-0}" != "1" ]; then
  trap stop_server EXIT
fi

for _ in $(seq 1 600); do
  if grep -q "Sommerspiele sind bereit" console.log; then break; fi
  if ! kill -0 "$PID" 2>/dev/null; then echo "Server ist abgestürzt:"; tail -40 console.log; exit 1; fi
  sleep 1
done
grep -q "Sommerspiele sind bereit" console.log || { echo "Sommerspiele wurden nicht bereit:"; tail -40 console.log; exit 1; }
grep -E "Fertig gebaut|Sommerspiele sind bereit|Netzwerk" console.log || true

if [ "${SKIP_TEST:-0}" = "1" ]; then
  echo "Server läuft (PID $PID) – anhalten mit: node $HERE/rcon.cjs 127.0.0.1:$RCON_PORT $SERVER/.rcon-pass stop"
  exit 0
fi

echo "== Test-Spieler"
STATUS=0
node "$HERE/sommerspiele-test.cjs" 127.0.0.1 "$PORT" "127.0.0.1:$RCON_PORT" "$SERVER/.rcon-pass" "$SERVER" || STATUS=$?

echo "== Fehler im Server-Log:"
if grep -qE "ERROR|Exception" console.log; then
  grep -E "ERROR|Exception" console.log | head -20
else
  echo "keine"
fi
exit $STATUS

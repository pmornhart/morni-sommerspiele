# Stand – Morni Sommerspiele

**Was es ist:** Der Sommer-Modus des MoinMorni-Netzwerks (Paper-Plugin `MorniSommerspiele`, Java 25, Paper 26.x):
2–12 Leute treten in Almlauf, Bogenschießen, Ruderregatta und Seifenkistenrennen an – um Medaillen und den Gesamtsieg.
Jede Disziplin geht auch einzeln. Netzwerk-Kennung `sommerspiele`, englisch „Summer Games“. Wunsch von Philip
(17.11.2026): Gegenstück zu Wintersport – offen, wenn Wintersport Sommerpause hat, und umgekehrt.

## Entscheidungen (17.11., erster Stand)

- **Saison:** `saison: sommer` mit `saison-von: 4`, `saison-bis: 11` (April bis November) als Vorgabe – genau das
  Gegenstück zu Wintersport (1. Dezember bis 31. März), so ist immer einer der beiden Modi offen (Test
  `summerIsTheExactCounterpartOfWinter`). Mai bis September geht über die zwei Monatswerte; `immer` = ganzjährig.
  Außerhalb: Zustand `closed`, „Winterpause – wieder ab 1. April“ und ein Hinweis auf Wintersport.
- **Vier Disziplinen** (Wettkampf in dieser Reihenfolge, Finale Seifenkiste): Almlauf, Bogenschießen, Ruderregatta,
  Seifenkistenrennen. Steinstoßen/Sackhüpfen weggelassen (vier Disziplinen reichen für einen Wettkampf von ~6 Minuten).
- **Seifenkiste ohne Vanilla-Lorenphysik:** Die Lore fährt auf echten Schienen, bewegt wird sie aber von einer eigenen,
  getesteten Fahrphysik (`logic/Kart`: Lageenergie → Tempo, Rollwiderstand, Fahrtwind; vorwärts schubst an, rückwärts
  bremst; zu schnell in die Kurve = entgleist, 2 s Stillstand, aus dem Stand weiter). Grund: Vanilla-Loren sind langsam,
  springen bei Tempo aus Kurven und lassen sich nicht bremsen. Eingaben über `Player#getCurrentInput` – gleich für Java
  und Bedrock. **Weiche** auf Terrasse 4: vorwärts = Steilstück (spart vier Kurven, gut gebremst ~2 s schneller),
  rechts = Kehren (Vorgabe). Aussteigen geht im Lauf nicht (Schleichen bleibt drin).
- **Regatta:** Bojen-Tore wie die Slalom-Tore von Wintersport, Boote vor dem Start vom „Startrichter“ festgehalten (kein
  Starttor im Wasser), Strömungen als seitlicher Schub (Plugin) plus sichtbare Gischt und blaue Fliesen am Steg,
  Strudel = echte Vanilla-Blasensäulen (Magma), Partys rudern zu zweit (`regatta.zweier`).
- **Bogenschießen:** echte Pfeile mit Schwerkraft, Wind als Schub je Tick (`bogen.wind` 0–3, je Durchgang zufällig),
  Wertung aus dem Treffpunkt auf der 5×5-Scheibe in zehn Ringen, Gleichstand: Mitten. Gewertet wird nach Ringen (größer
  ist besser) – `Ranking` kann jetzt beides.
- **Almlauf:** Massenstart, Hindernisse aus Blöcken (Bedrock-tauglich), echte Kühe auf der Weide (bleiben drin),
  Feder = zurück zum letzten Kontrollpunkt.
- Übernommen von Wintersport: Dorf/Abstimmung/Countdown, Wettkampf-Ablauf, Medaillen, Medaillenspiegel, Punkte,
  Siegerehrung, Zuschauen, Partys, Statistik, Netzwerk-Schnittstelle 2 (byte-gleich), Zurück ins Spiel, Läufe zu vier
  Bahnen, Linien- und Tor-Erkennung, Bau aus Bauplänen.

## Aufbau

- `src/main/java/de/moinmorni/sommerspiele/`: `SommerspielePlugin` (Start, Welt, Takt, Saison, Zustand), `Settings`,
  `Texts` (de/en), `Ui`, `Items`, `Holo`, `Sidebar`, `Boards`, `Pollen`, `Commands` (`/abstimmen`, `/bereit`,
  `/zuschauen`, `/verlassen`, `/stats`, `/top`, `/sommeradmin`).
- `logic/` (ohne Server, mit Tests): `Discipline`, `Program`, `Season`, `Ranking`, `Medal`, `MedalTable`, `Heats`,
  `Teams`, `Groups`, `Votes`, `Comeback`, `Line`/`Gate`, `RaceClock`, `TimeFormat`, `Kart` (Fahrphysik), `Archery`
  (Ringe, Pfeilflug, Zielen, Wind), `RunProgress` (Almlauf).
- `course/` (ohne Server, mit Tests): `Terrain` (Kistenberg in Terrassen), `Path`, `Kiste` (Schienenwege, Weiche,
  Kurven), `Lake` (Bahnen, Tore, Strömungen, Strudel), `Range` (Schießplatz), `Alm` (Rundkurs, Hindernisse), `Places`.
- `world/`: `Plan`, `Decor`, `Ground`, `Forest`, `Zones`, `VillagePlan`, `KistePlan`, `LakePlan`, `RangePlan`,
  `AlmPlan`, `Stands`, `Construction` (`VERSION` 1), `VoidGenerator`.
- `game/`: `Games`, `Competition`, `Stage`, `Racer`, `Result`, `AlmRace`, `ArcheryStage`, `RegattaRace`, `KartRace`.
- `village/`: `Village`, `VillageMenus`. `listen/`, `net/`, `menu/` wie bei Wintersport.
- `test/`: `run.sh` (Ports 29300/29310), `sommerspiele-test.cjs`, `rcon.cjs`, `fake-network/`.

## Stand (17.11.2026)

- Version 0.1.0 (Branch `claude/erster-stand`, noch nicht veröffentlicht – wartet auf Philips Go).
- Unit-Tests: **42** grün (Wertung nach Zeit und Ringen, Medaillenspiegel, Saison als genaues Gegenstück zu
  Wintersport, Almlauf-Fortschritt, Daten/Bestenliste mit „größer ist besser“, Texte; Strecken: Kurven nur auf ebenem
  Boden, Weiche teilt den Weg bis zur Weiche, Bahnen berühren sich nicht, Seifenkiste fahrbar – klug gebremst ohne
  Entgleisen 15–40 s, spät bremsen schneller als früh, Vollgas entgleist und verliert Zeit, Steilstück gut gefahren
  schneller –, Regatta-Tore/Bojen/Strudel, Ringe, Zielen mit Schwerkraft und Wind trifft die Mitte auf allen
  Entfernungen, Almlauf geschlossen, Hindernisse springbar, Holzstöße lassen drei Spalten frei, Kontrollpunkte der
  Reihe nach). Übersetzt gegen Paper-API 26.3 und 26.1.
- Echter Test (`FRESH=1 test/run.sh`, Paper 26.1.2 + Test-Netzwerk, vier Test-Spieler, Bau 1 042 137 Blöcke in 40 s):
  **83/83** (erster Lauf 80/83: zwei Test-Prüfungen zu streng, und Läufer konnten vor dem Start losgehen – mineflayer
  achtet nicht aufs Tempo 0; jetzt hält der Almlauf alle bis zum Startschuss auf ihrem Platz), Server-Log ohne Fehler –
  Dorf (Schrift, Schienen, See, Heuballen), Ankunft, Schnellleiste, de/en, Abstimmungsmenü mit fünf Programmen,
  Medaillen-Menü, Saison (Januar → `closed` mit „Winterpause … 1. April“, Juli offen), Wettkampf mit vier Leuten;
  Almlauf (Start gehalten, echter Lauf los, Kontrollpunkte, Dora weg und zurück mit Stand, Feder, Gold, Netzwerk-Zeit);
  Bogenschießen (Bogen und Pfeile, ein echter Bogenschuss zählt, Mitte! 10, daneben, Durchgänge 20/35/50, Wind 3
  ausgeglichen – der Wind wirkt –, 90 Ringe Gold, `bogen_rings`/`bogen_best`); Regatta (Zweier für die Party, Boote am
  Steg gehalten, Tor verpasst +4 s, Strömung in der Aktionsleiste, gemeinsames Ergebnis); Seifenkiste (vier Loren,
  Schleichen wirft nicht heraus, Lore steht vor dem LOS, fährt bergab, Vollgas entgleist, Steilstück an der Weiche,
  Dora weg und wieder in ihrer Lore, Bert zu spät → verlassen), Siegerehrung mit Gesamtsieg, niemand „spielt“ mehr,
  zurück im Dorf, Spielerdatei, Bestenliste, `/top bogen`, Bett → `askToLobby`.

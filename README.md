# Morni Sommerspiele

Der Sommer-Modus des Minecraft-Netzwerks **MoinMorni** (moinmorni.net): **Sommerspiele im Dorf am Bergsee – Almlauf,
Bogenschießen, Ruderregatta und Seifenkistenrennen, um Gold, Silber und Bronze.** 2 bis 12 Leute treten im
**Wettkampf** aus allen vier Disziplinen an; jede Disziplin geht auch einzeln. Ein Paper-Plugin für Paper 26.x
(Java 25). Es läuft im Netzwerk und auch ganz allein.

Englischer Name: **Summer Games**. Netzwerk-Kennung: `sommerspiele`. Das Gegenstück zu
[Wintersport](https://github.com/pmornhart/morni-wintersport): offen, wenn Wintersport Pause hat.

## So wird gespielt

1. Im **Sommerdorf** (Dorfplatz mit weiß-blauem Maibaum, Almhütten mit Geranien, Biergarten, Blumenbeete,
   Siegertreppchen) stimmt man mit dem **Kompass** ab – *Wettkampf (alle vier)* oder eine Disziplin allein – und sagt mit
   dem **grünen Farbstoff** „bereit“. Ab zwei Leuten (oder sobald jemand bereit ist) läuft ein Countdown; sind alle
   bereit, geht es nach 5 Sekunden los. Höchstens 12 Leute, Partys bleiben zusammen.
2. Jede Disziplin wird gewertet: die ersten drei bekommen **Gold, Silber, Bronze** (gleicher Wert = gleicher Platz).
   Punkte für den Gesamtsieg: 10, 8, 6, 5, 4, 3, 2, danach je 1, ohne Wertung 0. Nach jeder Disziplin steht der
   **Medaillenspiegel** im Chat und in der Leiste rechts.
3. Zum Schluss die **Siegerehrung** am Treppchen (mit Feuerwerk). Allein ist es ein Training: keine Medaille, aber der
   Wert zählt für die Bestenliste.

## Die Disziplinen (eigene Strecken, aus dem Code gebaut)

| Disziplin | So geht's |
|---|---|
| **Almlauf** | Massenstart auf einem Rundkurs über die Alm, links und rechts ein Zaun: drei **Heuballen-Hürden** und ein Baumstamm (springen), der **Bach** (wer die Trittsteine trifft, bleibt trocken und schnell), das **Moor** (Seelensand bremst – auf den Bohlen bleiben), zwei **Gatter** an der **Kuhweide** mit echten Kühen, **Holzstöße im Slalom** und die **Heutreppe**. Acht Kontrollpunkte der Reihe nach, wer zuerst über die Ziellinie kommt, gewinnt. |
| **Bogenschießen** | Alle gleichzeitig, jede Person an ihrer Bahn. Drei **Durchgänge** auf 20, 35 und 50 Blöcke, je 5 Pfeile. Die Pfeile **fallen** wie in Minecraft, und je Durchgang weht ein **Wind** (Pfeile in der Aktionsleiste, Wolken über dem Platz), der sie abtreibt. Ringe: Mitte 10, außen 1; bei Gleichstand zählen die Treffer genau in die Mitte. |
| **Ruderregatta** | Im Boot über den Bergsee: vier Bahnen nebeneinander, zwischen den Bahnen Holzstege. Acht **Bojen-Tore** (zwischen den beiden Bojen durch, rot = Tor links, gelb = rechts), verpasst = **4 s Strafe**. In den **Strömungen** (blaue Fliesen am Steg) treibt das Boot seitlich, **Strudel** (Seerosen warnen) ziehen das Boot hinab – zurück zum letzten Tor. **Partys rudern zu zweit.** |
| **Seifenkistenrennen** | In der Lore die Schienen den Kistenberg hinab – vier Bahnen nebeneinander, Kehren auf den Terrassen, schräge Schienen an den Absätzen. Bergab wird man von selbst schneller, **S (rückwärts) bremst**, **W schubst an** (nur langsam). Wer zu schnell in eine Kurve fährt, **entgleist** und verliert Zeit. In der Mitte die **Weiche**: **W** = geradeaus das **Steilstück** (kürzer, aber man kommt schnell an die nächste Kurve), **D** = rechts in die **Kehren** (sicher). |

Steuerung im Boot: `W` = rudern, `A`/`D` = lenken. In der Lore: `S` = bremsen, `W` = anschubsen bzw. Steilstück, `D` =
Kehren. Alles über Minecrafts eigene Eingaben – geht auch mit Bedrock. Die **Feder** bringt zum letzten Tor bzw.
Kontrollpunkt zurück (die Zeit läuft weiter), der **rote Farbstoff** gibt die Disziplin auf (im Wettkampf bleibt man
dabei). Menüs sind Truhen.

## Die Welt

Eine eigene leere Welt (`sommerspiele`, Wiesen-Biom) mit allem aus dem Code: das Tal mit Blumenwiesen, Eichen, Birken
und Fichten, im Norden der Kistenberg in acht Terrassen mit Starthäusern und Zielbögen, im Osten der Bergsee mit Stegen,
Bootshaus und Tribüne, im Westen der überdachte Bogenschießplatz mit Strohwand, im Süden der Almlauf mit Kuhweide und
Heustadel, ringsum Berge mit Schneeresten. Immer Tag (oder per Einstellung Abend), leise schwebender Blütenstaub.
Tafeln im Dorf: ewiger Medaillenspiegel und Bestwerte; an jeder Strecke eine Anzeigetafel mit dem laufenden Lauf.

## Saison

`saison: sommer` (Vorgabe) mit `saison-von: 4` und `saison-bis: 11` – offen vom **1. April bis 30. November**, genau
dann, wenn Wintersport (1. Dezember bis 31. März) Pause hat: So ist immer einer der beiden Modi offen. Mai bis September
geht mit `saison-von: 5`, `saison-bis: 9`. `saison: immer` = das ganze Jahr. Außerhalb meldet der Modus dem Netzwerk den
Zustand `closed` (Winterpause), das Dorf bleibt zum Anschauen offen und erklärt freundlich, wann es wieder losgeht.

## Befehle

| Befehl | Was er tut |
|---|---|
| `/abstimmen [wettkampf\|almlauf\|bogen\|regatta\|seifenkiste]` | abstimmen (ohne Angabe: Menü) – auch `/vote` |
| `/bereit` | im Dorf bereit melden – auch `/ready` |
| `/zuschauen` | dem laufenden Wettkampf zuschauen – auch `/spectate` |
| `/verlassen` | Wettkampf oder Zuschauen verlassen – auch `/leave` |
| `/stats [spieler]` · `/top [disziplin]` | Medaillen und Bestwerte |
| `/sommeradmin info\|status\|start [programm]\|stopp\|neubauen\|neuladen\|testdatum <datum\|aus>` | Verwaltung (`moinmorni.sommerspiele.admin`); `fahren`, `laufen`, `schiessen`, `wind` nur für Tests |

Einstellungen stehen nur in der `config.yml` (Saison, Tageszeit, Countdown, Zeitlimits, Runden, Pfeile, Wind,
Zweier, Strafe, Strömung, Weiche) – im Netzwerk im Panel.

**Zurück ins Spiel:** Verliert jemand mitten im Wettkampf die Verbindung, bleibt sein Platz `rueckkehr-sekunden` lang
frei (Vorgabe 90, 0 = aus). Kommt er rechtzeitig wieder, geht es dort weiter: im Boot bzw. in der Lore sitzt er gleich
wieder, im Almlauf läuft er mit seinen Kontrollpunkten weiter, beim Bogenschießen steht er an seiner Matte mit den
übrigen Pfeilen. Zu spät: Er hat den Wettkampf verlassen.

## Im Netzwerk

Das Netzwerk-Plugin („MoinMorni“) stellt die Schnittstelle `de.moinmorni.api.MoinMorni` bereit; die Sommerspiele nutzen
sie, wenn sie da ist (`softdepend`): Sprache, Party (zusammen in den Wettkampf, im Zweier-Ruderboot), Statistik,
Zustand (`waiting`, `running`, `closed`), zurück zur Lobby, „spielt gerade“ (dann fragt „/lobby“ erst nach) und
„Platz frei halten“ (Zurück ins Spiel).

Statistik: `events`, `wins`, `races`, `points`, `gold`, `silver`, `bronze`, `bogen_rings`, `bogen_centres`,
`kiste_crashes`; Bestwerte `best_points`, `bogen_best` (größer ist besser) und `time_almlauf_ms`, `time_regatta_ms`,
`time_seifenkiste_ms` (kleiner ist besser).

## Bauen und testen

```
JAVA_HOME=<jdk-25> mvn -q -DskipTests package   # Jar: target/Morni-Sommerspiele.jar
JAVA_HOME=<jdk-25> mvn test                     # Unit-Tests (Wertung, Saison, Strecken fahrbar, Physik, Ringe)
JAVA_HOME=<jdk-25> mvn -q -Ppaper-26.1 compile  # übersetzt auch gegen Paper 26.1
FRESH=1 test/run.sh                             # echter Server (Paper 26.1.2) + vier Test-Spieler (mineflayer)
```

`test/run.sh` richtet einen Paper-Server auf Port 29300 (RCON 29310) mit dem Test-Netzwerk ein und schickt Test-Spieler
durch Dorf, Saison, alle vier Disziplinen, Zurück ins Spiel und Siegerehrung.

---

Verfasst mit Claude 🤖

# Morni Sommerspiele

@STAND.md

## Regeln für dieses Projekt

- Repo ist **öffentlich**: keine Geheimnisse, Tokens oder privaten Daten committen (Geheimes nur in `.env`, nie im Repo).
  Auch keine Test-Server-Ordner, Welten, Spielerdaten oder RCON-Passwörter.
- Der Modus heißt **Sommerspiele** (englisch „Summer Games“), das Repo `pmornhart/morni-sommerspiele`, die Jar
  `Morni-Sommerspiele.jar`, das Plugin `MorniSommerspiele`, die Netzwerk-Kennung `sommerspiele`. Er ist das Gegenstück zu
  Wintersport (`pmornhart/morni-wintersport`): offen, wenn dort Pause ist (Vorgabe April bis November).
- Texte im Spiel auf Deutsch (Standard) und Englisch, freundlich, keine Fachbegriffe ohne Erklärung. Alle Texte stehen
  in `Texts.java` (MiniMessage, beide Sprachen); `TextsTest` prüft Platzhalter, Zeichen, dass jeder Schlüssel aus dem
  Code existiert und dass kein Platzhalter wie ein Standard-Tag heißt (`<gold>` wäre eine Farbe – darum `<ng>`).
  Eigene Farb-Tags: `<em>` Smaragd, `<au>` Gold, `<bl>` Himmelblau, `<si>` Silber, `<bz>` Bronze (nicht `<br>`).
- **„Verfasst mit Claude 🤖“** als letzte Zeile in Commit-Messages (durch eine Leerzeile getrennt), in PRs, Issues und
  Release-Notes; einmal als Fußzeile im README.
- Commits mit Stichpunkten `- Neu:` / `- Behoben:` / `- Verbessert:` / `- Entfernt:`.
- Keine fremden Marken, Figuren, Namen, Texte oder Grafiken (keine Mojang-, Nintendo-, Hypixel- oder Inhalte anderer
  Server und Spiele): nur die Spielmechanik von Sommer-Wettkämpfen als Vorbild, eigene Namen, eigene Bauten.
- Die Netzwerk-Schnittstelle `src/api/java/de/moinmorni/api/MoinMorni.java` ist eine Kopie aus `pmornhart/moinmorni.net`
  (`plugins/moinmorni-network/src/main/java/de/moinmorni/api/MoinMorni.java`) und muss **genau gleich** bleiben. Sie wird
  nur mitübersetzt und kommt **nicht** in die Jar. Nur `net/Connected` darf sie benutzen.
- Sommerspiele müssen auch ohne Netzwerk laufen (`softdepend: [MoinMorni]`).
- Paper 26.x, Java 25. Der Code muss auch gegen Paper 26.1 übersetzen (`mvn -Ppaper-26.1 compile`, läuft in der CI).
- Regeln ohne Server (Wertung, Medaillen, Punkte, Läufe, Saison, Fahrphysik der Seifenkiste, Ringe und Pfeilflug,
  Fortschritt im Almlauf, Abstimmung, Zeiten) gehören in `logic/`, die Lage aller Strecken (Berg, Schienen, See, Tore,
  Schießplatz, Rundkurs, Hindernisse) in `course/` – beides ohne Bukkit, mit Tests. Strecken nur so ändern, dass
  `CourseTest` grün bleibt (Kurven nur auf ebenem Boden, beide Wege an der Weiche fahrbar und klug gebremst ohne
  Entgleisen, Hindernisse springbar, Tore im Wasser).
- Ändern sich Dorf oder Strecken: `Construction.VERSION` erhöhen – dann baut der Server beim nächsten Start neu.
  Blocknamen von 26.x benutzen (z. B. `iron_chain`, nicht `chain`).
- Neue Version: `<version>` in `pom.xml` erhöhen und `docs/releases/v<version>.md` anlegen; ein Push auf `main` baut das
  Release mit `Morni-Sommerspiele.jar` – das Netzwerk holt es sich von dort.
- Vor jeder Veröffentlichung (Merge nach `main`) Philips Go abwarten.
- Nach jeder sichtbaren Änderung Bilder wie im echten Minecraft (echte Client-Texturen nur lokal, nie ins Repo).
- „Alles im Panel“: jede Einstellung, die per Befehl geht, muss auch im Panel gehen. Darum haben die Sommerspiele nur die
  `config.yml` und Verwaltungs-Befehle zum Testen (`/sommeradmin`); die Anbindung an den Einstellungs-Katalog gehört
  ins Netzwerk-Repo.
- Bedrock (Geyser) muss mitspielen können: Menüs sind Truhen, Steuerung nur über Minecrafts eigene Eingaben
  (vorwärts/rückwärts/links/rechts kommen über `Player#getCurrentInput` auch von Bedrock).
- Echter Test nach jeder Änderung am Spielablauf: `test/run.sh` (siehe README), Ports 29300/29310.
- `STAND.md` nach jedem größeren Schritt aktualisieren.

package de.moinmorni.sommerspiele;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Alle Texte für Spieler, auf Deutsch und Englisch, als MiniMessage. Eigene Farben: {@code <em>} Smaragd,
 * {@code <au>} Gold, {@code <bl>} Himmelblau, {@code <si>} Silber, {@code <bz>} Bronze. Namen von Spielern kommen immer
 * als {@code Placeholder.unparsed} hinein.
 */
public final class Texts {

    public static final int EMERALD = 0x2BB673;
    public static final int GOLD = 0xF2B632;
    public static final int BLUE = 0x8FD3FF;
    public static final int SILVER = 0xC8D0D8;
    public static final int BRONZE = 0xD08A4A;

    public static final MiniMessage MM = MiniMessage.builder()
            .editTags(t -> t.resolvers(
                    TagResolver.resolver("em", Tag.styling(TextColor.color(EMERALD))),
                    TagResolver.resolver("au", Tag.styling(TextColor.color(GOLD))),
                    TagResolver.resolver("bl", Tag.styling(TextColor.color(BLUE))),
                    TagResolver.resolver("si", Tag.styling(TextColor.color(SILVER))),
                    TagResolver.resolver("bz", Tag.styling(TextColor.color(BRONZE)))))
            .build();

    private static final Map<String, String[]> TEXTS = new LinkedHashMap<>();

    private Texts() {
    }

    private static void t(String key, String de, String en) {
        if (TEXTS.put(key, new String[] {de, en}) != null) {
            throw new IllegalStateException("Text doppelt: " + key);
        }
    }

    static {
        t("prefix", "<au>☀</au> <em>Sommerspiele</em> <dark_gray>»</dark_gray> ", "<au>☀</au> <em>Summer Games</em> <dark_gray>»</dark_gray> ");
        t("mode", "Sommerspiele", "Summer Games");

        // ---------- Allgemein ----------
        t("only-players", "Das geht nur im Spiel.", "Only possible in-game.");
        t("not-ready", "<gray>Das Sommerdorf wird gerade gebaut – gleich geht's los!</gray>",
                "<gray>The summer village is being built – just a moment!</gray>");
        t("unknown-player", "<gray>Niemanden namens <white><name></white> gefunden.</gray>",
                "<gray>Nobody called <white><name></white> found.</gray>");
        t("lobby.kick", "<au>MoinMorni</au>\n<gray>Zurück in die Lobby: Bitte verbinde dich neu mit dem Netzwerk.</gray>",
                "<au>MoinMorni</au>\n<gray>Back to the lobby: please reconnect to the network.</gray>");
        t("lobby.going", "<gray>Ab in die Lobby – bis bald!</gray>", "<gray>Off to the lobby – see you soon!</gray>");
        t("unit.rings", "<n> Ringe", "<n> points");

        // ---------- Dorf ----------
        t("village.title", "<au>☀ Sommerspiele</au>", "<au>☀ Summer Games</au>");
        t("village.subtitle", "<gray>Almlauf · Bogen · Regatta · Seifenkiste – um Gold, Silber und Bronze!</gray>",
                "<gray>Alpine run · archery · regatta · soapbox – go for gold!</gray>");
        t("village.welcome", "<gray>Willkommen im <em>Sommerdorf am Bergsee</em>! Mit dem <white>Kompass</white> stimmst du ab, was als Nächstes kommt, mit dem <white>grünen Farbstoff</white> sagst du „bereit“.</gray>",
                "<gray>Welcome to the <em>summer village by the mountain lake</em>! Use the <white>compass</white> to vote for what comes next and the <white>green dye</white> to say you're ready.</gray>");
        t("village.how", "<gray>So geht's: Im <em>Wettkampf</em> lauft ihr den <white>Almlauf</white> über Heuballen und Bach, schießt mit dem <white>Bogen</white>, rudert die <white>Regatta</white> und rast in der <white>Seifenkiste</white> den Berg hinab. Die Besten jeder Disziplin bekommen <au>Gold</au>, <si>Silber</si> und <bz>Bronze</bz>, Punkte entscheiden den Gesamtsieg. Bis es losgeht: Biergarten und Blumenwiesen!</gray>",
                "<gray>How it works: in the <em>competition</em> you run the <white>alpine run</white> over hay bales and the stream, shoot with the <white>bow</white>, row the <white>regatta</white> and race the <white>soapbox</white> down the mountain. The best of each event win <au>gold</au>, <si>silver</si> and <bz>bronze</bz>, points decide the overall winner. Until then: beer garden and flower meadows!</gray>");
        t("village.waiting", "<gray>Warte auf Mitspieler … (<white><n></white> da)</gray>", "<gray>Waiting for players … (<white><n></white> here)</gray>");
        t("village.countdown", "<gray>Start in <au><secs> s</au> · <em><program></em></gray>",
                "<gray>Starting in <au><secs> s</au> · <em><program></em></gray>");
        t("village.busy", "<gray>Gerade läuft ein Wettkampf – mit dem Kompass kannst du zuschauen.</gray>",
                "<gray>A competition is running – use the compass to watch.</gray>");
        t("village.ready", "<em>Du bist bereit.</em> <gray>Sind alle bereit, geht's gleich los.</gray>",
                "<em>You're ready.</em> <gray>Once everyone is ready, we start.</gray>");
        t("village.not-ready", "<gray>Du bist nicht mehr bereit.</gray>", "<gray>You're no longer ready.</gray>");
        t("village.voted", "<gray>Deine Stimme: <em><program></em>.</gray>", "<gray>Your vote: <em><program></em>.</gray>");
        t("village.full", "<gray>Der Wettkampf ist voll (höchstens 12) – du bist beim nächsten dabei.</gray>",
                "<gray>The competition is full (12 at most) – you're in the next one.</gray>");
        t("village.starting", "<em>Auf geht's!</em> <gray><au><program></au> mit <white><n></white> Leuten.</gray>",
                "<em>Here we go!</em> <gray><au><program></au> with <white><n></white> people.</gray>");
        t("village.closed", "<gray>Die Sommerspiele machen gerade <bl>Winterpause</bl> – ab dem <white><date></white> geht es wieder los. Das Dorf und der Biergarten sind trotzdem offen – und drüben bei <white>Wintersport</white> ist jetzt Saison!</gray>",
                "<gray>The Summer Games are on their <bl>winter break</bl> – they're back on <white><date></white>. The village and beer garden are still open – and over at the <white>Winter Games</white> it's season now!</gray>");
        t("village.closed.title", "<bl>❄ Winterpause</bl>", "<bl>❄ Winter break</bl>");
        t("village.closed.sub", "<gray>Wieder ab <white><date></white></gray>", "<gray>Back on <white><date></white></gray>");
        t("village.closed.bar", "<gray>Winterpause – Wettkämpfe wieder ab <white><date></white></gray>",
                "<gray>Winter break – competitions again from <white><date></white></gray>");

        t("item.vote", "<em>✎ Abstimmen & Zuschauen</em>", "<em>✎ Vote & watch</em>");
        t("item.vote.lore", "<gray>Rechtsklick: Wettkampf oder eine Disziplin wählen</gray>\n<gray>oder einem laufenden Wettkampf zuschauen.</gray>",
                "<gray>Right-click: choose the competition or one event</gray>\n<gray>or watch a running competition.</gray>");
        t("item.ready", "<em>✔ Bereit</em>", "<em>✔ Ready</em>");
        t("item.ready.lore", "<gray>Rechtsklick: bereit / nicht bereit.</gray>", "<gray>Right-click: ready / not ready.</gray>");
        t("item.stats", "<au>★ Medaillen & Bestwerte</au>", "<au>★ Medals & records</au>");
        t("item.stats.lore", "<gray>Rechtsklick: deine Medaillen und die Besten.</gray>",
                "<gray>Right-click: your medals and the best.</gray>");
        t("item.lobby", "<bl>⌂ Zurück zur Lobby</bl>", "<bl>⌂ Back to the lobby</bl>");
        t("item.lobby.lore", "<gray>Rechtsklick: zurück ins Netzwerk-Dorf.</gray>", "<gray>Right-click: back to the network village.</gray>");
        t("item.reset", "<bl>⟲ Zurück auf die Strecke</bl>", "<bl>⟲ Back on track</bl>");
        t("item.reset.lore", "<gray>Rechtsklick: zurück zum letzten Tor bzw. Kontrollpunkt,</gray>\n<gray>falls du feststeckst. Die Zeit läuft weiter.</gray>",
                "<gray>Right-click: back to the last gate or checkpoint</gray>\n<gray>if you're stuck. The clock keeps running.</gray>");
        t("item.giveup", "<red>✖ Aufgeben</red>", "<red>✖ Give up</red>");
        t("item.giveup.lore", "<gray>Rechtsklick: diese Disziplin aufgeben</gray>\n<gray>(im Wettkampf bleibst du dabei).</gray>",
                "<gray>Right-click: give up this event</gray>\n<gray>(you stay in the competition).</gray>");
        t("item.leave", "<red>✖ Zurück ins Dorf</red>", "<red>✖ Back to the village</red>");
        t("item.leave.lore", "<gray>Rechtsklick: Zuschauen beenden.</gray>", "<gray>Right-click: stop watching.</gray>");
        t("item.bow", "<au>➹ Wettkampf-Bogen</au>", "<au>➹ Competition bow</au>");
        t("item.bow.lore", "<gray>Ganz spannen und auf die Mitte zielen –</gray>\n<gray>der Pfeil fällt, und der Wind treibt ihn ab!</gray>",
                "<gray>Draw fully and aim for the centre –</gray>\n<gray>the arrow drops, and the wind pushes it aside!</gray>");
        t("item.bib", "<white>Startnummer <au><n></au></white>", "<white>Bib number <au><n></au></white>");

        // ---------- Programme und Disziplinen ----------
        t("program.wettkampf", "Wettkampf (alle vier)", "Competition (all four)");
        t("program.wettkampf.desc", "<gray>Almlauf, Bogenschießen, Regatta und Seifenkiste –</gray>\n<gray>Medaillen in jeder Disziplin, Punkte für den Gesamtsieg.</gray>",
                "<gray>Alpine run, archery, regatta and soapbox race –</gray>\n<gray>medals in every event, points for the overall win.</gray>");
        t("discipline.almlauf", "Almlauf", "Alpine Run");
        t("discipline.almlauf.desc", "<gray>Hindernislauf über die Alm: Heuballen, Bach mit</gray>\n<gray>Trittsteinen, Moor, Kuhweide und Holzstöße.</gray>",
                "<gray>Obstacle run across the pasture: hay bales, a stream</gray>\n<gray>with stepping stones, the bog, cows and log piles.</gray>");
        t("discipline.bogen", "Bogenschießen", "Archery");
        t("discipline.bogen.desc", "<gray>Drei Durchgänge auf 20, 35 und 50 Blöcke –</gray>\n<gray>der Pfeil fällt und der Wind weht. Mitte = 10 Ringe.</gray>",
                "<gray>Three rounds at 20, 35 and 50 blocks –</gray>\n<gray>arrows drop and the wind blows. Centre = 10 points.</gray>");
        t("discipline.regatta", "Ruderregatta", "Rowing Regatta");
        t("discipline.regatta.desc", "<gray>Im Boot über den Bergsee durch acht Bojen-Tore –</gray>\n<gray>Strömung und Strudel! Verpasst = <white><secs> s</white> Strafe.</gray>",
                "<gray>Row across the lake through eight buoy gates –</gray>\n<gray>currents and whirlpools! Missed = <white><secs> s</white> penalty.</gray>");
        t("discipline.seifenkiste", "Seifenkistenrennen", "Soapbox Race");
        t("discipline.seifenkiste.desc", "<gray>In der Lore die Schienen den Berg hinab –</gray>\n<gray>bremsen vor den Kurven, an der Weiche wählen!</gray>",
                "<gray>Down the mountain on rails in a minecart –</gray>\n<gray>brake before the bends, pick your way at the switch!</gray>");

        // ---------- Menüs ----------
        t("menu.vote.title", "Was kommt als Nächstes?", "What's next?");
        t("menu.vote.votes", "<gray>Stimmen: <white><n></white></gray>", "<gray>Votes: <white><n></white></gray>");
        t("menu.vote.click", "<em>▶ Klicken zum Abstimmen</em>", "<em>▶ Click to vote</em>");
        t("menu.vote.mine", "<au>✔ Deine Stimme</au>", "<au>✔ Your vote</au>");
        t("menu.vote.best", "<gray>Bestwert: <au><time></au> <white><name></white></gray>",
                "<gray>Record: <au><time></au> <white><name></white></gray>");
        t("menu.vote.watch", "<em>◉ Zuschauen</em>", "<em>◉ Watch</em>");
        t("menu.vote.watch.lore", "<gray>Gerade läuft: <white><discipline></white></gray>\n<em>▶ Klicken zum Zuschauen</em>",
                "<gray>Now running: <white><discipline></white></gray>\n<em>▶ Click to watch</em>");
        t("menu.vote.closed", "<gray>Winterpause – wieder ab <white><date></white>.</gray>",
                "<gray>Winter break – back on <white><date></white>.</gray>");
        t("menu.close", "<red>Schließen</red>", "<red>Close</red>");
        t("menu.stats.title", "Medaillen & Bestwerte", "Medals & records");
        t("menu.stats.me", "<au>Deine Medaillen</au>", "<au>Your medals</au>");
        t("menu.stats.me.lore", "<au>● <ng></au> Gold · <si>● <ns></si> Silber · <bz>● <nb></bz> Bronze\n<gray>Wettkämpfe: <white><events></white> · Gesamtsiege: <white><wins></white></gray>\n<gray>Punkte: <white><points></white></gray>",
                "<au>● <ng></au> gold · <si>● <ns></si> silver · <bz>● <nb></bz> bronze\n<gray>Competitions: <white><events></white> · overall wins: <white><wins></white></gray>\n<gray>Points: <white><points></white></gray>");
        t("menu.stats.best", "<gray>Dein Bestwert: <au><time></au></gray>", "<gray>Your record: <au><time></au></gray>");
        t("menu.stats.none", "<gray>Noch kein Wert.</gray>", "<gray>No record yet.</gray>");
        t("menu.stats.top", "<white><place>.</white> <gray><name></gray> <au><time></au>", "<white><place>.</white> <gray><name></gray> <au><time></au>");
        t("menu.stats.rings", "<gray>Ringe insgesamt: <white><rings></white> · Mitten: <white><centres></white></gray>",
                "<gray>Total points: <white><rings></white> · bullseyes: <white><centres></white></gray>");
        t("menu.stats.table", "<au>Ewiger Medaillenspiegel</au>", "<au>All-time medal table</au>");

        // ---------- Wettkampf ----------
        t("comp.bib", "<gray>Deine Startnummer: <au><n></au>. Viel Glück!</gray>", "<gray>Your bib number: <au><n></au>. Good luck!</gray>");
        t("comp.next.title", "<au><discipline></au>", "<au><discipline></au>");
        t("comp.next.sub", "<gray>Disziplin <white><n></white> von <white><total></white></gray>", "<gray>Event <white><n></white> of <white><total></white></gray>");
        t("comp.result.header", "<au>━━ Ergebnis <discipline> ━━</au>", "<au>━━ Result <discipline> ━━</au>");
        t("comp.result.row", "<medal> <white><place>.</white> <name> <gray>–</gray> <au><time></au><extra>",
                "<medal> <white><place>.</white> <name> <gray>–</gray> <au><time></au><extra>");
        t("comp.result.dnf", "<gray>✖ <name> – nicht gewertet</gray>", "<gray>✖ <name> – not ranked</gray>");
        t("comp.result.penalty", " <gray>(<white><n></white> Tor(e) verpasst)</gray>", " <gray>(<white><n></white> gate(s) missed)</gray>");
        t("comp.result.crashes", " <gray>(<white><n></white>× entgleist)</gray>", " <gray>(derailed <white><n></white>×)</gray>");
        t("comp.result.centres", " <gray>(<white><n></white>× Mitte)</gray>", " <gray>(<white><n></white> bullseye(s))</gray>");
        t("comp.medal.gold", "<au>● GOLD!</au> <gray>Du gewinnst <white><discipline></white>!</gray>", "<au>● GOLD!</au> <gray>You win the <white><discipline></white>!</gray>");
        t("comp.medal.silver", "<si>● Silber!</si> <gray>Platz 2 – <white><discipline></white>.</gray>", "<si>● Silver!</si> <gray>2nd place in the <white><discipline></white>.</gray>");
        t("comp.medal.bronze", "<bz>● Bronze!</bz> <gray>Platz 3 – <white><discipline></white>.</gray>", "<bz>● Bronze!</bz> <gray>3rd place in the <white><discipline></white>.</gray>");
        t("comp.medal.title.gold", "<au>● Gold</au>", "<au>● Gold</au>");
        t("comp.medal.title.silver", "<si>● Silber</si>", "<si>● Silver</si>");
        t("comp.medal.title.bronze", "<bz>● Bronze</bz>", "<bz>● Bronze</bz>");
        t("comp.training", "<gray>Allein ist es ein <white>Training</white>: keine Medaille, aber dein Wert zählt für die Bestenliste.</gray>",
                "<gray>Alone it's <white>training</white>: no medal, but your result counts for the leaderboard.</gray>");
        t("comp.standings.header", "<em>━━ Medaillenspiegel ━━</em>", "<em>━━ Medal table ━━</em>");
        t("comp.standings.row", "<white><place>.</white> <name> <au>●<ng></au> <si>●<ns></si> <bz>●<nb></bz> <gray>· <points> Pkt.</gray>",
                "<white><place>.</white> <name> <au>●<ng></au> <si>●<ns></si> <bz>●<nb></bz> <gray>· <points> pts</gray>");
        t("comp.between", "<gray>Gleich geht es weiter mit <em><discipline></em> …</gray>", "<gray>Coming up next: <em><discipline></em> …</gray>");
        t("comp.winner", "<au>★ Gesamtsieg für <white><name></white> mit <points> Punkten!</au>", "<au>★ Overall winner: <white><name></white> with <points> points!</au>");
        t("comp.left", "<gray><white><name></white> hat den Wettkampf verlassen.</gray>", "<gray><white><name></white> left the competition.</gray>");
        t("comp.away", "<gray><white><name></white> hat die Verbindung verloren – der Platz bleibt <white><secs> Sekunden</white> frei.</gray>",
                "<gray><white><name></white> lost the connection – the spot stays free for <white><secs> seconds</white>.</gray>");
        t("comp.away.late", "<gray><white><name></white> ist nicht rechtzeitig zurückgekommen und hat den Wettkampf verlassen.</gray>",
                "<gray><white><name></white> didn't make it back in time and left the competition.</gray>");
        t("comp.welcome-back", "<gray>Willkommen zurück! Du bist wieder im Wettkampf – gleiche Disziplin, gleicher Lauf, deine Werte zählen weiter.</gray>",
                "<gray>Welcome back! You're back in the competition – same event, same run, your results still count.</gray>");
        t("comp.welcome-back.title", "<em>Willkommen zurück!</em>", "<em>Welcome back!</em>");
        t("comp.welcome-back.sub", "<gray>Weiter geht's – genau da, wo du warst.</gray>", "<gray>Carry on – right where you left off.</gray>");
        t("comp.back.all", "<gray><white><name></white> ist zurück im Wettkampf.</gray>", "<gray><white><name></white> is back in the competition.</gray>");
        t("comp.over.title", "<au>Siegerehrung</au>", "<au>Award ceremony</au>");
        t("comp.over.sub", "<gray>Glückwunsch an alle!</gray>", "<gray>Congratulations to everyone!</gray>");
        t("comp.podium.1", "<au>1</au>", "<au>1</au>");
        t("comp.podium.2", "<si>2</si>", "<si>2</si>");
        t("comp.podium.3", "<bz>3</bz>", "<bz>3</bz>");
        t("comp.back", "<gray>Zurück ins Dorf – bis zum nächsten Mal!</gray>", "<gray>Back to the village – see you next time!</gray>");
        t("comp.giveup", "<gray>Du hast aufgegeben – schau den anderen zu.</gray>", "<gray>You gave up – watch the others.</gray>");

        // ---------- Läufe, Countdown, Ziel ----------
        t("heat.title", "<au>Lauf <n> von <total></au>", "<au>Heat <n> of <total></au>");
        t("heat.your-turn", "<em>Du bist dran!</em> <gray>Bahn <white><lane></white> – gleich geht's los.</gray>",
                "<em>Your turn!</em> <gray>Lane <white><lane></white> – we start in a moment.</gray>");
        t("heat.wait", "<gray>Du startest im <white>Lauf <n></white> – schau bis dahin von der Tribüne zu.</gray>",
                "<gray>You race in <white>heat <n></white> – watch from the stand until then.</gray>");
        t("count.number", "<au><n></au>", "<au><n></au>");
        t("count.go", "<em>LOS!</em>", "<em>GO!</em>");
        t("count.sub", "<gray>Fertig machen …</gray>", "<gray>Get ready …</gray>");
        t("race.gate.missed", "<red>Tor verpasst!</red> <gray>+<secs> s</gray>", "<red>Gate missed!</red> <gray>+<secs> s</gray>");
        t("race.finish.title", "<em>Im Ziel!</em>", "<em>Finished!</em>");
        t("race.finish.sub", "<au><time></au>", "<au><time></au>");
        t("race.finish.msg", "<em>Im Ziel!</em> <gray>Zeit: <au><time></au><extra></gray>", "<em>Finished!</em> <gray>Time: <au><time></au><extra></gray>");
        t("race.finish.penalty", " <gray>(davon <white><secs> s</white> Strafe)</gray>", " <gray>(incl. <white><secs> s</white> penalty)</gray>");
        t("race.dnf", "<gray>Die Zeit ist um – leider <white>nicht im Ziel</white>.</gray>", "<gray>Time's up – <white>did not finish</white>.</gray>");
        t("race.reset", "<gray>Zurück auf die Strecke – die Zeit läuft weiter!</gray>", "<gray>Back on track – the clock keeps running!</gray>");
        t("race.bar.penalty", " <dark_gray>·</dark_gray> <red>+<secs> s</red>", " <dark_gray>·</dark_gray> <red>+<secs> s</red>");
        t("race.bar.watch", "<gray>Lauf <white><n></white>/<white><total></white> läuft · <white><left></white> noch auf der Strecke</gray>",
                "<gray>Heat <white><n></white>/<white><total></white> running · <white><left></white> still on course</gray>");

        // ---------- Regatta ----------
        t("regatta.intro", "<gray>Regatta: <white>W</white> = rudern, <white>A</white>/<white>D</white> = lenken. Durch jedes Bojen-Tor – zwischen den beiden Bojen (<red>rot</red> = Tor links, <yellow>gelb</yellow> = Tor rechts). Verpasst = <white><secs> s</white> Strafe. Blaue Fliesen am Steg: <bl>Strömung</bl> treibt dich ab. Seerosen warnen vor <red>Strudeln</red>!</gray>",
                "<gray>Regatta: <white>W</white> = row, <white>A</white>/<white>D</white> = steer. Pass every buoy gate between its two buoys (<red>red</red> = gate on the left, <yellow>yellow</yellow> = on the right). Missed = <white><secs> s</white> penalty. Blue tiles on the jetty: a <bl>current</bl> pushes you aside. Lily pads warn of <red>whirlpools</red>!</gray>");
        t("regatta.pair", "<gray>Ihr rudert im <em>Zweier</em>: <white><name></white> lenkt.</gray>", "<gray>You row as a <em>pair</em>: <white><name></white> steers.</gray>");
        t("regatta.whirlpool", "<red>Strudel!</red> <gray>Das Boot wurde hinabgezogen – zurück zum letzten Tor.</gray>",
                "<red>Whirlpool!</red> <gray>The boat got pulled under – back to the last gate.</gray>");
        t("regatta.bar", "<au><time></au> <dark_gray>·</dark_gray> <gray>Tor <white><n></white>/<white><total></white></gray><extra>",
                "<au><time></au> <dark_gray>·</dark_gray> <gray>Gate <white><n></white>/<white><total></white></gray><extra>");
        t("regatta.bar.east", " <dark_gray>·</dark_gray> <bl>Strömung →</bl>", " <dark_gray>·</dark_gray> <bl>current →</bl>");
        t("regatta.bar.west", " <dark_gray>·</dark_gray> <bl>← Strömung</bl>", " <dark_gray>·</dark_gray> <bl>← current</bl>");

        // ---------- Seifenkiste ----------
        t("kiste.intro", "<gray>Seifenkiste: Bergab wirst du von selbst schneller. <white>S</white> (rückwärts) = bremsen, <white>W</white> = anschubsen, wenn du langsam bist. Vor jeder Kurve bremsen – wer zu schnell ist, <red>entgleist</red> und verliert Zeit!</gray>",
                "<gray>Soapbox: downhill you speed up by yourself. <white>S</white> (back) = brake, <white>W</white> = push off when you're slow. Brake before every bend – too fast and you <red>derail</red> and lose time!</gray>");
        t("kiste.fork.intro", "<gray>In der Mitte liegt eine <au>Weiche</au>: <white>W</white> = geradeaus das <red>Steilstück</red> (kürzer, aber schnell!), <white>D</white> = rechts in die <em>Kehren</em> (sicher). Ohne Wahl: Kehren.</gray>",
                "<gray>Halfway down there's a <au>switch</au>: <white>W</white> = straight down the <red>steep part</red> (shorter, but fast!), <white>D</white> = right into the <em>hairpins</em> (safe). No choice: hairpins.</gray>");
        t("kiste.count.sub", "<gray>S = bremsen · W = anschubsen</gray>", "<gray>S = brake · W = push</gray>");
        t("kiste.crash.title", "<red>Entgleist!</red>", "<red>Derailed!</red>");
        t("kiste.crash.sub", "<gray>Zu schnell in der Kurve – gleich geht's weiter</gray>", "<gray>Too fast in the bend – back on in a moment</gray>");
        t("kiste.finish.crashes", " <gray>(<white><n></white>× entgleist)</gray>", " <gray>(derailed <white><n></white>×)</gray>");
        t("kiste.fork.steep", "<red>⇩ Steilstück!</red> <gray>Gut bremsen vor der nächsten Kurve.</gray>",
                "<red>⇩ Steep part!</red> <gray>Brake hard before the next bend.</gray>");
        t("kiste.fork.bends", "<em>↪ Kehren</em> <gray>– sicher hinab.</gray>", "<em>↪ Hairpins</em> <gray>– safely down.</gray>");
        t("kiste.bar", "<au><time></au> <dark_gray>·</dark_gray> <gray><white><speed></white> km/h</gray>",
                "<au><time></au> <dark_gray>·</dark_gray> <gray><white><speed></white> km/h</gray>");
        t("kiste.bar.crash", "<au><time></au> <dark_gray>·</dark_gray> <red>entgleist …</red>", "<au><time></au> <dark_gray>·</dark_gray> <red>derailed …</red>");
        t("kiste.bar.fork.steep", "<au><time></au> <dark_gray>·</dark_gray> <au>Weiche:</au> <red>Steilstück</red> <gray>(D = Kehren)</gray>",
                "<au><time></au> <dark_gray>·</dark_gray> <au>Switch:</au> <red>steep part</red> <gray>(D = hairpins)</gray>");
        t("kiste.bar.fork.bends", "<au><time></au> <dark_gray>·</dark_gray> <au>Weiche:</au> <em>Kehren</em> <gray>(W = Steilstück)</gray>",
                "<au><time></au> <dark_gray>·</dark_gray> <au>Switch:</au> <em>hairpins</em> <gray>(W = steep part)</gray>");

        // ---------- Bogenschießen ----------
        t("bogen.intro", "<gray>Bogenschießen: drei Durchgänge mit je <white><n></white> Pfeilen auf 20, 35 und 50 Blöcke. Die Mitte zählt <au>10</au>, der äußere Ring <white>1</white>. Der Pfeil <white>fällt</white> – weiter weg höher zielen! Achte auf den <bl>Wind</bl>.</gray>",
                "<gray>Archery: three rounds of <white><n></white> arrows at 20, 35 and 50 blocks. The centre scores <au>10</au>, the outer ring <white>1</white>. Arrows <white>drop</white> – aim higher further out! Mind the <bl>wind</bl>.</gray>");
        t("bogen.round.title", "<au>Durchgang <n>/<total></au>", "<au>Round <n>/<total></au>");
        t("bogen.round.sub", "<gray><white><d></white> Blöcke · <wind></gray>", "<gray><white><d></white> blocks · <wind></gray>");
        t("bogen.go", "<em>Schießen!</em>", "<em>Shoot!</em>");
        t("bogen.wind.none", "windstill", "no wind");
        t("bogen.wind.east", "Wind <bl><arrows></bl> (<n>)", "wind <bl><arrows></bl> (<n>)");
        t("bogen.wind.west", "Wind <bl><arrows></bl> (<n>)", "wind <bl><arrows></bl> (<n>)");
        t("bogen.hit", "<em>Treffer: <n></em>", "<em>Hit: <n></em>");
        t("bogen.hit.centre", "<au>★ Mitte! 10</au>", "<au>★ Bullseye! 10</au>");
        t("bogen.miss", "<red>Daneben!</red>", "<red>Miss!</red>");
        t("bogen.round.result", "<gray>Durchgang <white><n></white>: <au><rings></au> Ringe · insgesamt <au><total></au></gray>",
                "<gray>Round <white><n></white>: <au><rings></au> points · total <au><total></au></gray>");
        t("bogen.bar", "<gray>Durchgang <white><n></white>/<white><total></white> · Pfeil <white><arrow></white>/<white><arrows></white> · <au><rings></au> Ringe · <wind> · <white><secs> s</white></gray>",
                "<gray>Round <white><n></white>/<white><total></white> · arrow <white><arrow></white>/<white><arrows></white> · <au><rings></au> pts · <wind> · <white><secs> s</white></gray>");

        // ---------- Almlauf ----------
        t("alm.intro", "<gray>Almlauf: <white><laps></white> Runde(n) im Uhrzeigersinn über die Alm – über die <white>Heuballen</white> springen, im <white>Bach</white> die Trittsteine finden, im <white>Moor</white> auf den Bohlen bleiben, über die <white>Gatter</white> der Kuhweide und im Slalom um die Holzstöße. Mit der Feder geht's zum letzten Kontrollpunkt zurück.</gray>",
                "<gray>Alpine run: <white><laps></white> lap(s) clockwise across the pasture – jump the <white>hay bales</white>, find the stepping stones in the <white>stream</white>, stay on the planks in the <white>bog</white>, over the <white>gates</white> of the cow pasture and slalom round the log piles. The feather takes you back to the last checkpoint.</gray>");
        t("alm.lap", "<gray>Runde <white><n></white> von <white><total></white> geschafft!</gray>", "<gray>Lap <white><n></white> of <white><total></white> done!</gray>");
        t("alm.bar", "<au><time></au> <dark_gray>·</dark_gray> <gray>Runde <white><n></white>/<white><total></white> · Posten <white><cp></white>/<white><cps></white></gray>",
                "<au><time></au> <dark_gray>·</dark_gray> <gray>Lap <white><n></white>/<white><total></white> · checkpoint <white><cp></white>/<white><cps></white></gray>");

        // ---------- Leiste rechts ----------
        t("sidebar.title", "<au><b>☀ Sommerspiele</b></au>", "<au><b>☀ Summer Games</b></au>");
        t("sidebar.village.players", "<gray>Im Dorf: <white><n></white></gray>", "<gray>In the village: <white><n></white></gray>");
        t("sidebar.village.next", "<gray>Nächstes: <em><program></em></gray>", "<gray>Next: <em><program></em></gray>");
        t("sidebar.village.count", "<gray>Start in <au><secs> s</au></gray>", "<gray>Starts in <au><secs> s</au></gray>");
        t("sidebar.village.running", "<gray>Läuft: <em><discipline></em></gray>", "<gray>Running: <em><discipline></em></gray>");
        t("sidebar.village.closed", "<gray>Winterpause bis <white><date></white></gray>", "<gray>Winter break until <white><date></white></gray>");
        t("sidebar.medals", "<au>●<ng></au> <si>●<ns></si> <bz>●<nb></bz> <gray>deine Medaillen</gray>",
                "<au>●<ng></au> <si>●<ns></si> <bz>●<nb></bz> <gray>your medals</gray>");
        t("sidebar.address", "<au>moinmorni.net</au>", "<au>moinmorni.net</au>");
        t("sidebar.discipline", "<em><discipline></em> <gray>(<n>/<total>)</gray>", "<em><discipline></em> <gray>(<n>/<total>)</gray>");
        t("sidebar.heat", "<gray>Lauf <white><n></white> von <white><total></white></gray>", "<gray>Heat <white><n></white> of <white><total></white></gray>");
        t("sidebar.round", "<gray>Durchgang <white><n></white>/<white><total></white></gray>", "<gray>Round <white><n></white>/<white><total></white></gray>");
        t("sidebar.rings", "<au><n></au> <gray>Ringe</gray>", "<au><n></au> <gray>pts</gray>");
        t("sidebar.time", "<gray>Zeit: <au><time></au></gray>", "<gray>Time: <au><time></au></gray>");
        t("sidebar.table", "<white>Medaillenspiegel</white>", "<white>Medal table</white>");
        t("sidebar.row", "<white><place>.</white> <gray><name></gray> <au><ng></au><gray>/</gray><si><ns></si><gray>/</gray><bz><nb></bz>",
                "<white><place>.</white> <gray><name></gray> <au><ng></au><gray>/</gray><si><ns></si><gray>/</gray><bz><nb></bz>");
        t("sidebar.number", "<gray>Startnummer <au><n></au></gray>", "<gray>Bib <au><n></au></gray>");

        // ---------- Tafeln und Schilder ----------
        t("board.medals", "<au><b>Ewiger Medaillenspiegel</b></au>", "<au><b>All-time medal table</b></au>");
        t("board.medals.row", "<white><place>.</white> <name> <au>●<ng></au> <si>●<ns></si> <bz>●<nb></bz>",
                "<white><place>.</white> <name> <au>●<ng></au> <si>●<ns></si> <bz>●<nb></bz>");
        t("board.empty", "<gray>Noch niemand – sei die oder der Erste!</gray>", "<gray>Nobody yet – be the first!</gray>");
        t("board.times", "<au><b>Bestwerte</b></au>", "<au><b>Records</b></au>");
        t("board.times.head", "<em><discipline></em>", "<em><discipline></em>");
        t("board.times.row", "<white><place>.</white> <name> <au><time></au>", "<white><place>.</white> <name> <au><time></au>");
        t("board.live", "<au><b><discipline></b></au>\n<gray><status></gray>", "<au><b><discipline></b></au>\n<gray><status></gray>");
        t("board.live.idle", "Gerade kein Wettkampf", "Nothing running right now");
        t("board.live.heat", "Lauf <n> von <total>", "Heat <n> of <total>");
        t("board.live.row", "<white><place>.</white> <name> <au><time></au>", "<white><place>.</white> <name> <au><time></au>");
        t("label.village", "<au><b>☀ Sommerdorf am Bergsee</b></au>\n<gray>Sommerspiele auf MoinMorni</gray>", "<au><b>☀ Summer Village by the Lake</b></au>\n<gray>Summer Games on MoinMorni</gray>");
        t("label.podium", "<au><b>Siegertreppchen</b></au>", "<au><b>Winners' podium</b></au>");
        t("label.beergarden", "<au>Biergarten</au>\n<gray>Radler, Limo und Brezn</gray>", "<au>Beer garden</au>\n<gray>Shandy, lemonade and pretzels</gray>");
        t("label.sign", "<em>⇦ Seifenkiste · Bogenschießen</em>\n<em>Regatta am Bergsee ⇨</em>\n<em>⇩ Almlauf</em>",
                "<em>⇦ Soapbox · archery</em>\n<em>Regatta on the lake ⇨</em>\n<em>⇩ Alpine run</em>");
        t("label.start", "<au>Start</au> <gray>· Bahn <white><n></white></gray>", "<au>Start</au> <gray>· lane <white><n></white></gray>");
        t("label.finish", "<au><b>Ziel</b></au>", "<au><b>Finish</b></au>");
        t("label.range", "<white>Bahn <n></white>", "<white>Lane <n></white>");
        t("label.kiste", "<au><b>Seifenkisten-Start</b></au>\n<gray>Rot = Steilstück · Grün = Kehren</gray>",
                "<au><b>Soapbox start</b></au>\n<gray>Red = steep part · green = hairpins</gray>");
        t("label.alm", "<au><b>Almlauf</b></au>\n<gray>Start und Ziel</gray>", "<au><b>Alpine Run</b></au>\n<gray>Start and finish</gray>");
        t("label.pasture", "<em>Kuhweide</em>\n<gray>Bitte nicht die Kühe schubsen!</gray>", "<em>Cow pasture</em>\n<gray>Please don't push the cows!</gray>");

        // ---------- Statistik, Zuschauen ----------
        t("stats.header", "<au>━━ <name> ━━</au>", "<au>━━ <name> ━━</au>");
        t("stats.medals", "<au>●<ng></au> Gold · <si>●<ns></si> Silber · <bz>●<nb></bz> Bronze <gray>· <points> Punkte</gray>",
                "<au>●<ng></au> gold · <si>●<ns></si> silver · <bz>●<nb></bz> bronze <gray>· <points> points</gray>");
        t("stats.events", "<gray>Wettkämpfe: <white><events></white> · Starts: <white><races></white> · Gesamtsiege: <white><wins></white></gray>",
                "<gray>Competitions: <white><events></white> · starts: <white><races></white> · overall wins: <white><wins></white></gray>");
        t("stats.best", "<gray><discipline>: <au><time></au></gray>", "<gray><discipline>: <au><time></au></gray>");
        t("stats.rings", "<gray>Ringe insgesamt: <white><rings></white> · Mitten: <white><centres></white></gray>",
                "<gray>Total points: <white><rings></white> · bullseyes: <white><centres></white></gray>");
        t("stats.none", "<gray>Noch keine Werte.</gray>", "<gray>No stats yet.</gray>");
        t("top.header", "<au>━━ Bestwerte <discipline> ━━</au>", "<au>━━ Records <discipline> ━━</au>");
        t("spectate.start", "<gray>Du schaust zu: <em><discipline></em>. Mit dem roten Farbstoff geht's zurück ins Dorf.</gray>",
                "<gray>You're watching: <em><discipline></em>. Use the red dye to go back to the village.</gray>");
        t("spectate.none", "<gray>Gerade läuft kein Wettkampf.</gray>", "<gray>No competition is running right now.</gray>");
        t("leave.done", "<gray>Zurück im Dorf.</gray>", "<gray>Back in the village.</gray>");
        t("leave.nothing", "<gray>Du bist schon im Dorf.</gray>", "<gray>You're already in the village.</gray>");
    }

    /** Zeit oder (beim Bogenschießen) Ringe als Text. */
    public static String value(de.moinmorni.sommerspiele.logic.Discipline d, long v, String lang) {
        return d.scored() ? raw(lang, "unit.rings").replace("<n>", String.valueOf(v))
                : de.moinmorni.sommerspiele.logic.TimeFormat.format(v, lang);
    }

    /** Alle Schlüssel (für Tests). */
    public static Set<String> keys() {
        return Collections.unmodifiableSet(TEXTS.keySet());
    }

    /** Roher MiniMessage-Text. Unbekannte Sprache → Deutsch. */
    public static String raw(String lang, String key) {
        String[] t = TEXTS.get(key);
        if (t == null) {
            return key;
        }
        return "en".equals(lang) ? t[1] : t[0];
    }

    /** Text ohne Vorsilbe; für Gegenstände ohne kursiv. */
    public static Component text(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, key), resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** Nachricht für den Chat, mit „☀ Sommerspiele »“ davor. */
    public static Component msg(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, "prefix")).append(MM.deserialize(raw(lang, key), resolvers));
    }

    /** Mehrzeiliger Text als einzelne Zeilen (für Beschreibungen von Gegenständen). */
    public static List<Component> lines(String lang, String key, TagResolver... resolvers) {
        List<Component> out = new ArrayList<>();
        for (String line : raw(lang, key).split("\n")) {
            out.add(MM.deserialize(line, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        return out;
    }

    /** Reiner Text (ohne Formatierung). */
    public static String plain(String lang, String key) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(MM.deserialize(raw(lang, key)));
    }

    /** Text als Komponente aus rohem MiniMessage mit Platzhaltern (für zusammengesetzte Zeilen). */
    public static Component parse(String raw, TagResolver... resolvers) {
        return MM.deserialize(raw, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}

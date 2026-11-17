package de.moinmorni.sommerspiele;

import de.moinmorni.sommerspiele.course.Alm;
import de.moinmorni.sommerspiele.course.Kiste;
import de.moinmorni.sommerspiele.course.Lake;
import de.moinmorni.sommerspiele.course.Places;
import de.moinmorni.sommerspiele.course.Range;
import de.moinmorni.sommerspiele.course.Terrain;
import de.moinmorni.sommerspiele.data.Leaderboard;
import de.moinmorni.sommerspiele.data.PlayerData;
import de.moinmorni.sommerspiele.game.Competition;
import de.moinmorni.sommerspiele.game.Result;
import de.moinmorni.sommerspiele.logic.Discipline;
import de.moinmorni.sommerspiele.logic.MedalTable;
import de.moinmorni.sommerspiele.logic.Ranking;
import de.moinmorni.sommerspiele.village.Village;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Schrift im Dorf und an den Strecken (je Sprache): Schilder, der ewige Medaillenspiegel, die Bestzeiten, die
 * Anzeigetafeln an den Zielen (laufender Lauf) – und die Leiste rechts für alle im Dorf.
 */
public final class Boards {

    private static final int BOARD_ROWS = 6;
    private static final int TIME_ROWS = 3;
    private static final int LIVE_ROWS = 6;
    private static final float BIG = 1.6f;
    private static final float NORMAL = 1.0f;

    private final SommerspielePlugin plugin;
    private final Sidebar sidebar = new Sidebar();
    private final List<Holo> labels = new ArrayList<>();
    private final Map<Discipline, Holo> live = new EnumMap<>(Discipline.class);
    private Holo medals;
    private Holo times;

    public Boards(SommerspielePlugin plugin) {
        this.plugin = plugin;
    }

    public Sidebar sidebar() {
        return sidebar;
    }

    private Location at(double x, double y, double z) {
        World w = plugin.world();
        return new Location(w, x, Terrain.FLOOR_Y + y, z);
    }

    private void label(double x, double y, double z, String key, float scale) {
        labels.add(Holo.spawn(plugin, at(x, y, z), lang -> Texts.text(lang, key), scale));
    }

    public void spawnAll() {
        removeAll();
        label(0.5, 11.5, Places.PODIUM[1] - 2.4, "label.village", BIG);
        label(0.5, 7.4, Places.PODIUM[1] - 2.2, "label.podium", NORMAL);
        label(0.5, 2.4, Places.PODIUM[1] + 1.7, "comp.podium.1", BIG);
        label(-3.5, 1.4, Places.PODIUM[1] + 1.7, "comp.podium.2", BIG);
        label(4.5, 0.4, Places.PODIUM[1] + 1.7, "comp.podium.3", BIG);
        label(Places.BEERGARDEN[0] + 0.5, 6.2, Places.BEERGARDEN[1] + 0.5, "label.beergarden", NORMAL);
        label(-7.5, 3.0, 21.5, "label.sign", NORMAL);
        for (int lane = 0; lane < Kiste.lanes(); lane++) {
            int n = lane + 1;
            labels.add(Holo.spawn(plugin, at(Kiste.x(lane, 0), Terrain.TOP + 5.2, Kiste.START_Z - 2.5),
                    lang -> Texts.text(lang, "label.start", Placeholder.unparsed("n", String.valueOf(n))), NORMAL));
            label(Kiste.finishX(lane), 7.6, Math.floor(Kiste.FINISH_Z) + 0.5, "label.finish", NORMAL);
        }
        label(Kiste.x(0, 0) - 4, Terrain.TOP + 8.5, Kiste.START_Z - 6, "label.kiste", BIG);
        for (int lane = 0; lane < Lake.lanes(); lane++) {
            int n = lane + 1;
            labels.add(Holo.spawn(plugin, at(Lake.centerX(lane), 3.6, Lake.START_DOCK_Z - 1.5),
                    lang -> Texts.text(lang, "label.start", Placeholder.unparsed("n", String.valueOf(n))), NORMAL));
            label(Lake.centerX(lane), 7.6, Math.floor(Lake.FINISH_Z) + 0.5, "label.finish", NORMAL);
        }
        for (int k = 0; k < Range.LANES; k++) {
            int n = k + 1;
            labels.add(Holo.spawn(plugin, at(Range.laneX(k) + 0.5, 3.3, Range.MAT_Z - 1.2),
                    lang -> Texts.text(lang, "label.range", Placeholder.unparsed("n", String.valueOf(n))), NORMAL));
        }
        label(Alm.WEST_X + 0.5, 7.4, Alm.LAP_Z + 0.5, "label.alm", BIG);
        label((Alm.PASTURE[0] + Alm.PASTURE[2]) / 2.0, 4.6, Alm.PASTURE[1] - 5.5, "label.pasture", NORMAL);
        double mz = Math.floor(Places.BOARD_MEDALS[2]) - 2 + 0.8;
        medals = Holo.spawn(plugin, at(Places.BOARD_MEDALS[0], Places.BOARD_MEDALS[1], mz), this::medalText, NORMAL);
        times = Holo.spawn(plugin, at(Places.BOARD_TIMES[0], Places.BOARD_TIMES[1], mz), this::timesText, NORMAL);
        live.put(Discipline.SEIFENKISTE, Holo.spawn(plugin, at(Places.STAND_KISTE[0] + 0.5, 8.5, -21.5),
                lang -> liveText(lang, Discipline.SEIFENKISTE, List.of(), 0, 0), NORMAL));
        live.put(Discipline.REGATTA, Holo.spawn(plugin, at(Places.STAND_REGATTA[0] + 0.5, 9.5, Places.STAND_REGATTA[1] - 4.5),
                lang -> liveText(lang, Discipline.REGATTA, List.of(), 0, 0), NORMAL));
        live.put(Discipline.BOGEN, Holo.spawn(plugin, at(Places.STAND_BOGEN[0] + 0.5, 7.5, Range.MAT_Z - 4.5),
                lang -> liveText(lang, Discipline.BOGEN, List.of(), 0, 0), NORMAL));
        live.put(Discipline.ALMLAUF, Holo.spawn(plugin, at(Places.STAND_ALM[0] + 4.5, 7.5, Places.STAND_ALM[1] + 0.5),
                lang -> liveText(lang, Discipline.ALMLAUF, List.of(), 0, 0), NORMAL));
    }

    public void removeAll() {
        labels.forEach(Holo::remove);
        labels.clear();
        live.values().forEach(Holo::remove);
        live.clear();
        if (medals != null) {
            medals.remove();
            medals = null;
        }
        if (times != null) {
            times.remove();
            times = null;
        }
    }

    private Component medalText(String lang) {
        Component c = Texts.text(lang, "board.medals");
        List<MedalTable.Row> rows = plugin.board().medalTable();
        if (rows.isEmpty()) {
            return c.append(Component.newline()).append(Texts.text(lang, "board.empty"));
        }
        for (int i = 0; i < Math.min(BOARD_ROWS, rows.size()); i++) {
            MedalTable.Row r = rows.get(i);
            c = c.append(Component.newline()).append(Texts.text(lang, "board.medals.row",
                    Placeholder.unparsed("place", String.valueOf(r.place())), Placeholder.unparsed("name", plugin.board().nameOf(r.id())),
                    Placeholder.unparsed("ng", String.valueOf(r.gold())), Placeholder.unparsed("ns", String.valueOf(r.silver())),
                    Placeholder.unparsed("nb", String.valueOf(r.bronze()))));
        }
        return c;
    }

    private Component timesText(String lang) {
        Component c = Texts.text(lang, "board.times");
        for (Discipline d : Discipline.values()) {
            c = c.append(Component.newline()).append(Texts.text(lang, "board.times.head",
                    Placeholder.unparsed("discipline", Texts.plain(lang, "discipline." + d.id()))));
            List<Leaderboard.Time> top = plugin.board().top(d);
            if (top.isEmpty()) {
                c = c.append(Component.newline()).append(Texts.text(lang, "board.empty"));
            }
            for (int i = 0; i < Math.min(TIME_ROWS, top.size()); i++) {
                c = c.append(Component.newline()).append(Texts.text(lang, "board.times.row",
                        Placeholder.unparsed("place", String.valueOf(i + 1)), Placeholder.unparsed("name", top.get(i).name()),
                        Placeholder.unparsed("time", Texts.value(d, top.get(i).ms(), lang))));
            }
        }
        return c;
    }

    private Component liveText(String lang, Discipline d, List<Result> results, int heat, int heats) {
        String status = heat <= 0 ? Texts.raw(lang, "board.live.idle")
                : Texts.raw(lang, "board.live.heat").replace("<n>", String.valueOf(heat)).replace("<total>", String.valueOf(heats));
        Component c = Texts.text(lang, "board.live", Placeholder.unparsed("discipline", Texts.plain(lang, "discipline." + d.id())),
                Placeholder.unparsed("status", status));
        List<Ranking.Entry> entries = new ArrayList<>();
        Map<String, Result> byId = new java.util.HashMap<>();
        for (Result r : results) {
            if (r.finished() && (!d.scored() || r.rings() > 0)) {
                entries.add(new Ranking.Entry(r.id(), r.value(d), true));
                byId.put(r.id(), r);
            }
        }
        List<Ranking.Placed> ranked = Ranking.rank(entries, d.scored());
        for (int i = 0; i < Math.min(LIVE_ROWS, ranked.size()); i++) {
            Ranking.Placed p = ranked.get(i);
            c = c.append(Component.newline()).append(Texts.text(lang, "board.live.row",
                    Placeholder.unparsed("place", String.valueOf(p.place())), Placeholder.unparsed("name", byId.get(p.entry().id()).names()),
                    Placeholder.unparsed("time", Competition.show(d, byId.get(p.entry().id()), lang))));
        }
        return c;
    }

    /** Anzeigetafel am Ziel einer Disziplin auf den neuesten Stand. */
    public void live(Discipline d, List<Result> results, int heat, int heats) {
        Holo h = live.get(d);
        if (h != null) {
            h.update(lang -> liveText(lang, d, results, heat, heats));
        }
    }

    public void refreshVillage() {
        if (medals == null || !medals.valid() || times == null || !times.valid()
                || labels.stream().anyMatch(h -> !h.valid())) {
            if (plugin.ready()) {
                spawnAll();
                for (Player p : plugin.world().getPlayers()) {
                    Holo.refresh(plugin, p, plugin.lang(p));
                }
            }
            return;
        }
        medals.update(this::medalText);
        times.update(this::timesText);
    }

    /** Leiste rechts für alle im Dorf. */
    public void villageSidebars() {
        Village v = plugin.village();
        for (UUID u : v.waiting()) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) {
                continue;
            }
            String lang = plugin.lang(p);
            List<Component> lines = new ArrayList<>();
            lines.add(Texts.text(lang, "sidebar.village.players", Placeholder.unparsed("n", String.valueOf(v.waiting().size()))));
            if (!plugin.open()) {
                lines.add(Texts.text(lang, "sidebar.village.closed", Placeholder.unparsed("date", plugin.reopening(lang))));
            } else if (plugin.games().running()) {
                lines.add(Texts.text(lang, "sidebar.village.running", Placeholder.unparsed("discipline",
                        Texts.plain(lang, "discipline." + plugin.games().discipline().id()))));
            } else {
                lines.add(Texts.text(lang, "sidebar.village.next", Placeholder.unparsed("program", Village.name(lang, v.next()))));
                if (v.countdown() >= 0) {
                    lines.add(Texts.text(lang, "sidebar.village.count", Placeholder.unparsed("secs", String.valueOf(v.countdown()))));
                }
            }
            lines.add(Component.empty());
            PlayerData d = plugin.data(p);
            lines.add(Texts.text(lang, "sidebar.medals", Placeholder.unparsed("ng", String.valueOf(d.gold())),
                    Placeholder.unparsed("ns", String.valueOf(d.silver())), Placeholder.unparsed("nb", String.valueOf(d.bronze()))));
            lines.add(Component.text(" "));
            lines.add(Texts.text(lang, "sidebar.address"));
            sidebar.show(p, Texts.text(lang, "sidebar.title"), lines);
        }
    }
}

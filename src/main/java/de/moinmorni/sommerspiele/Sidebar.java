package de.moinmorni.sommerspiele;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Leiste rechts: eigene Anzeige je Spieler, Zeilen als eigene Namen (ohne Zahlen). Die Teams des Haupt-Scoreboards
 * (Ränge des Netzwerks über dem Kopf und in der Tab-Liste) werden mitkopiert, damit sie nicht verschwinden.
 */
public final class Sidebar {

    private static final String OBJECTIVE = "mm_winter";
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public void show(Player p, Component title, List<Component> lines) {
        Scoreboard sb = boards.computeIfAbsent(p.getUniqueId(), u -> Bukkit.getScoreboardManager().getNewScoreboard());
        if (p.getScoreboard() != sb) {
            p.setScoreboard(sb);
        }
        Objective o = sb.getObjective(OBJECTIVE);
        if (o == null) {
            o = sb.registerNewObjective(OBJECTIVE, Criteria.DUMMY, title);
            o.setDisplaySlot(DisplaySlot.SIDEBAR);
            o.numberFormat(NumberFormat.blank());
        } else {
            o.displayName(title);
        }
        copyTeams(sb);
        int n = lines.size();
        for (String entry : sb.getEntries()) {
            int idx = index(entry);
            if (idx < 0 || idx >= n) {
                sb.resetScores(entry);
            }
        }
        for (int i = 0; i < n; i++) {
            Score s = o.getScore(entry(i));
            s.customName(lines.get(i));
            s.setScore(n - i);
        }
    }

    public void hide(Player p) {
        Scoreboard sb = boards.remove(p.getUniqueId());
        if (sb != null && p.getScoreboard() == sb) {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public void forget(UUID uuid) {
        boards.remove(uuid);
    }

    private static String entry(int i) {
        return "line" + i;
    }

    private static int index(String entry) {
        if (!entry.startsWith("line")) {
            return -1;
        }
        try {
            return Integer.parseInt(entry.substring(4));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Teams (Ränge) aus dem Haupt-Scoreboard übernehmen. */
    private static void copyTeams(Scoreboard sb) {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team t : main.getTeams()) {
            Team c = sb.getTeam(t.getName());
            if (c == null) {
                c = sb.registerNewTeam(t.getName());
            }
            if (!c.prefix().equals(t.prefix())) {
                c.prefix(t.prefix());
            }
            if (!c.suffix().equals(t.suffix())) {
                c.suffix(t.suffix());
            }
            if (t.hasColor() && (!c.hasColor() || !c.color().equals(t.color()))) {
                c.color(net.kyori.adventure.text.format.NamedTextColor.nearestTo(t.color()));
            }
            for (Team.Option opt : Team.Option.values()) {
                if (c.getOption(opt) != t.getOption(opt)) {
                    c.setOption(opt, t.getOption(opt));
                }
            }
            for (String entry : t.getEntries()) {
                if (!c.hasEntry(entry)) {
                    c.addEntry(entry);
                }
            }
            for (String entry : c.getEntries()) {
                if (!t.hasEntry(entry)) {
                    c.removeEntry(entry);
                }
            }
        }
        for (Team c : sb.getTeams()) {
            if (main.getTeam(c.getName()) == null) {
                c.unregister();
            }
        }
    }
}

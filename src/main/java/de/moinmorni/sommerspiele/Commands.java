package de.moinmorni.sommerspiele;

import de.moinmorni.sommerspiele.data.Leaderboard;
import de.moinmorni.sommerspiele.data.PlayerData;
import de.moinmorni.sommerspiele.game.AlmRace;
import de.moinmorni.sommerspiele.game.ArcheryStage;
import de.moinmorni.sommerspiele.game.Competition;
import de.moinmorni.sommerspiele.game.KartRace;
import de.moinmorni.sommerspiele.game.RegattaRace;
import de.moinmorni.sommerspiele.logic.Discipline;
import de.moinmorni.sommerspiele.logic.Program;
import de.moinmorni.sommerspiele.village.VillageMenus;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** Alle Befehle. Einstellungen gibt es hier keine (die stehen in der config.yml, später im Panel). */
final class Commands implements TabExecutor {

    private static final List<String> ADMIN_SUBS = List.of("info", "status", "start", "stopp", "neubauen", "neuladen",
            "testdatum", "fahren", "laufen", "schiessen", "wind");

    private final SommerspielePlugin plugin;

    Commands(SommerspielePlugin plugin) {
        this.plugin = plugin;
    }

    private String lang(CommandSender s) {
        return s instanceof Player p ? plugin.lang(p) : "de";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("sommeradmin")) {
            return admin(sender, args);
        }
        if (name.equals("stats")) {
            stats(sender, args);
            return true;
        }
        if (name.equals("top")) {
            top(sender, args);
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Texts.msg("de", "only-players"));
            return true;
        }
        switch (name) {
            case "bereit" -> plugin.village().toggleReady(p);
            case "abstimmen" -> {
                Program prog = args.length > 0 ? Program.byId(args[0]) : null;
                if (prog == null || !plugin.village().inVillage(p.getUniqueId()) || !plugin.open()) {
                    VillageMenus.vote(plugin, p);
                } else {
                    plugin.village().vote(p, prog);
                }
            }
            case "zuschauen" -> {
                if (plugin.games().of(p.getUniqueId()) == null) {
                    plugin.games().spectate(p);
                }
            }
            case "verlassen" -> {
                Competition c = plugin.games().of(p.getUniqueId());
                if (c != null) {
                    c.leave(p);
                    p.sendMessage(Texts.msg(plugin.lang(p), "leave.done"));
                } else {
                    plugin.village().enter(p);
                    p.sendMessage(Texts.msg(plugin.lang(p), "leave.nothing"));
                }
            }
            default -> {
            }
        }
        return true;
    }

    private void stats(CommandSender sender, String[] args) {
        String lang = lang(sender);
        Optional<PlayerData> data;
        if (args.length > 0) {
            Player online = Bukkit.getPlayerExact(args[0]);
            UUID uuid = online != null ? online.getUniqueId() : offlineUuid(args[0]);
            data = uuid == null ? Optional.empty() : plugin.store().read(uuid);
            if (data.isEmpty()) {
                sender.sendMessage(Texts.msg(lang, "unknown-player", Placeholder.unparsed("name", args[0])));
                return;
            }
        } else if (sender instanceof Player p) {
            data = Optional.of(plugin.data(p));
        } else {
            sender.sendMessage(Texts.msg(lang, "only-players"));
            return;
        }
        PlayerData d = data.get();
        sender.sendMessage(Texts.text(lang, "stats.header", Placeholder.unparsed("name", d.name())));
        if (d.races() == 0) {
            sender.sendMessage(Texts.text(lang, "stats.none"));
            return;
        }
        sender.sendMessage(Texts.text(lang, "stats.medals", Placeholder.unparsed("ng", String.valueOf(d.gold())),
                Placeholder.unparsed("ns", String.valueOf(d.silver())), Placeholder.unparsed("nb", String.valueOf(d.bronze())),
                Placeholder.unparsed("points", String.valueOf(d.points()))));
        sender.sendMessage(Texts.text(lang, "stats.events", Placeholder.unparsed("events", String.valueOf(d.events())),
                Placeholder.unparsed("races", String.valueOf(d.races())), Placeholder.unparsed("wins", String.valueOf(d.wins()))));
        for (Discipline disc : Discipline.values()) {
            if (d.best(disc) > 0) {
                sender.sendMessage(Texts.text(lang, "stats.best", Placeholder.unparsed("discipline",
                        Texts.plain(lang, "discipline." + disc.id())), Placeholder.unparsed("time", Texts.value(disc, d.best(disc), lang))));
            }
        }
        if (d.rings() > 0) {
            sender.sendMessage(Texts.text(lang, "stats.rings", Placeholder.unparsed("rings", String.valueOf(d.rings())),
                    Placeholder.unparsed("centres", String.valueOf(d.centres()))));
        }
    }

    private static UUID offlineUuid(String name) {
        @SuppressWarnings("deprecation")
        OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(name);
        return op == null ? null : op.getUniqueId();
    }

    private void top(CommandSender sender, String[] args) {
        String lang = lang(sender);
        List<Discipline> which = args.length > 0 && Discipline.byId(args[0]) != null ? List.of(Discipline.byId(args[0]))
                : Arrays.asList(Discipline.values());
        for (Discipline d : which) {
            sender.sendMessage(Texts.text(lang, "top.header", Placeholder.unparsed("discipline", Texts.plain(lang, "discipline." + d.id()))));
            List<Leaderboard.Time> top = plugin.board().top(d);
            if (top.isEmpty()) {
                sender.sendMessage(Texts.text(lang, "board.empty"));
            }
            for (int i = 0; i < top.size(); i++) {
                sender.sendMessage(Texts.text(lang, "board.times.row", Placeholder.unparsed("place", String.valueOf(i + 1)),
                        Placeholder.unparsed("name", top.get(i).name()), Placeholder.unparsed("time", Texts.value(d, top.get(i).ms(), lang))));
            }
        }
    }

    // ---------- Verwaltung (zum Testen) ----------

    private boolean admin(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("/sommeradmin " + String.join("|", ADMIN_SUBS)));
            return true;
        }
        Competition c = plugin.games().current();
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "info" -> sender.sendMessage(Component.text("Sommerspiele " + plugin.getPluginMeta().getVersion()
                    + " · bereit=" + plugin.ready() + " · Saison=" + plugin.settings().season().id() + " · offen=" + plugin.open()
                    + " · Netzwerk=" + plugin.net().connected()));
            case "status" -> {
                List<String> lines = new ArrayList<>();
                lines.add("bereit=" + plugin.ready() + " offen=" + plugin.open() + " dorf=" + plugin.village().waiting().size()
                        + " countdown=" + plugin.village().countdown() + " naechstes=" + plugin.village().next().id()
                        + " laeuft=" + (c != null) + " zeit=" + plugin.world().getTime());
                if (c != null) {
                    lines.addAll(c.debug());
                }
                sender.sendMessage(Component.text(String.join("\n", lines)));
            }
            case "start" -> {
                Program prog = args.length > 1 ? Program.byId(args[1]) : plugin.village().next();
                if (prog == null || !plugin.ready() || !plugin.open() || !plugin.village().start(prog)) {
                    sender.sendMessage(Component.text("Kein Start (läuft schon, nicht bereit, geschlossen oder niemand da)."));
                } else {
                    sender.sendMessage(Component.text("Gestartet: " + prog.id()));
                }
            }
            case "stopp" -> {
                plugin.games().abort();
                sender.sendMessage(Component.text("Wettkampf abgebrochen."));
            }
            case "neubauen" -> {
                sender.sendMessage(Component.text("Baue neu …"));
                plugin.rebuild(() -> sender.sendMessage(Component.text("Neu gebaut.")));
            }
            case "neuladen" -> {
                plugin.reloadSettings();
                sender.sendMessage(Component.text("config.yml neu geladen · Saison=" + plugin.settings().season().id()
                        + " · offen=" + plugin.open()));
            }
            case "testdatum" -> {
                if (args.length < 2 || args[1].equalsIgnoreCase("aus")) {
                    plugin.testDate(null);
                } else {
                    try {
                        plugin.testDate(LocalDate.parse(args[1]));
                    } catch (DateTimeParseException e) {
                        sender.sendMessage(Component.text("Datum wie 2027-01-15"));
                        return true;
                    }
                }
                plugin.stateChanged();
                sender.sendMessage(Component.text("Heute=" + plugin.today() + " · offen=" + plugin.open()));
            }
            case "fahren" -> drive(sender, c, args);
            case "laufen" -> walk(sender, c, args);
            case "schiessen" -> shoot(sender, c, args);
            case "wind" -> {
                if (c != null && c.stage() instanceof ArcheryStage a && args.length > 1) {
                    a.wind(Integer.parseInt(args[1]));
                    sender.sendMessage(Component.text("wind=" + a.wind()));
                } else {
                    sender.sendMessage(Component.text("Kein Bogenschießen."));
                }
            }
            default -> sender.sendMessage(Component.text("/sommeradmin " + String.join("|", ADMIN_SUBS)));
        }
        return true;
    }

    /**
     * /sommeradmin fahren &lt;spieler&gt; … – zum Testen: Regatta {@code tor <n> [daneben] | ziel}, Seifenkiste
     * {@code auto|vollgas|aus [steil|kehre]}.
     */
    private void drive(CommandSender sender, Competition c, String[] args) {
        Player p = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : null;
        if (args.length < 3 || c == null || p == null) {
            sender.sendMessage(Component.text("Kein Rennen oder unbekannt."));
            return;
        }
        String how = args[2].toLowerCase(Locale.ROOT);
        if (c.stage() instanceof RegattaRace race) {
            List<double[]> points = switch (how) {
                case "tor" -> args.length > 3 ? race.gatePoints(p, Integer.parseInt(args[3]), args.length > 4) : List.of();
                case "ziel" -> race.finishPoints(p);
                default -> List.of();
            };
            boolean ok = !points.isEmpty() && race.drive(p, points);
            sender.sendMessage(Component.text(ok ? "fährt " + points.size() + " Punkte" : "geht nicht"));
        } else if (c.stage() instanceof KartRace race) {
            KartRace.Pilot mode = switch (how) {
                case "auto" -> KartRace.Pilot.AUTO;
                case "vollgas" -> KartRace.Pilot.FULL;
                default -> KartRace.Pilot.NONE;
            };
            Boolean steep = args.length > 3 ? Boolean.valueOf(args[3].equalsIgnoreCase("steil")) : null;
            sender.sendMessage(Component.text(race.pilot(p, mode, steep) ? "fährt " + how : "geht nicht"));
        } else {
            sender.sendMessage(Component.text("Kein Rennen mit Boot oder Lore."));
        }
    }

    /** /sommeradmin laufen &lt;spieler&gt; – Läufer eine Runde selbst laufen lassen (Tests). */
    private void walk(CommandSender sender, Competition c, String[] args) {
        if (args.length < 2 || c == null || !(c.stage() instanceof AlmRace race)) {
            sender.sendMessage(Component.text("Kein Almlauf."));
            return;
        }
        Player p = Bukkit.getPlayerExact(args[1]);
        if (p == null) {
            sender.sendMessage(Component.text("Unbekannt: " + args[1]));
            return;
        }
        List<double[]> points = race.lapPoints(p);
        boolean ok = !points.isEmpty() && race.walk(p, points);
        sender.sendMessage(Component.text(ok ? "läuft " + points.size() + " Punkte" : "geht nicht"));
    }

    /** /sommeradmin schiessen &lt;spieler&gt; [daneben] – genau gezielter Pfeil (Tests), daneben in Blöcken. */
    private void shoot(CommandSender sender, Competition c, String[] args) {
        if (args.length < 2 || c == null || !(c.stage() instanceof ArcheryStage stage)) {
            sender.sendMessage(Component.text("Kein Bogenschießen."));
            return;
        }
        Player p = Bukkit.getPlayerExact(args[1]);
        if (p == null) {
            sender.sendMessage(Component.text("Unbekannt: " + args[1]));
            return;
        }
        double side = args.length > 2 ? Double.parseDouble(args[2]) : 0;
        sender.sendMessage(Component.text(stage.autoShot(p, side) ? "geschossen" : "geht nicht"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("sommeradmin") && args.length == 1) {
            return filter(ADMIN_SUBS, args[0]);
        }
        if (name.equals("sommeradmin") && args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return filter(Arrays.stream(Program.values()).map(Program::id).toList(), args[1]);
        }
        if (name.equals("abstimmen") && args.length == 1) {
            return filter(Arrays.stream(Program.values()).map(Program::id).toList(), args[0]);
        }
        if (name.equals("top") && args.length == 1) {
            return filter(Arrays.stream(Discipline.values()).map(Discipline::id).toList(), args[0]);
        }
        if (name.equals("stats") && args.length == 1) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[0]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(p)).toList();
    }
}

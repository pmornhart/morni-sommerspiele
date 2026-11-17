package de.moinmorni.sommerspiele.game;

import de.moinmorni.sommerspiele.Items;
import de.moinmorni.sommerspiele.Texts;
import de.moinmorni.sommerspiele.Ui;
import de.moinmorni.sommerspiele.SommerspielePlugin;
import de.moinmorni.sommerspiele.course.Places;
import de.moinmorni.sommerspiele.course.Terrain;
import de.moinmorni.sommerspiele.data.PlayerData;
import de.moinmorni.sommerspiele.logic.Comeback;
import de.moinmorni.sommerspiele.logic.Discipline;
import de.moinmorni.sommerspiele.logic.Medal;
import de.moinmorni.sommerspiele.logic.MedalTable;
import de.moinmorni.sommerspiele.logic.Program;
import de.moinmorni.sommerspiele.logic.Ranking;
import de.moinmorni.sommerspiele.logic.TimeFormat;
import de.moinmorni.sommerspiele.village.Village;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

/**
 * Ein Wettkampf: eine oder vier Disziplinen nacheinander, nach jeder die Wertung mit Medaillen und Punkten, am Ende
 * die Siegerehrung am Treppchen im Dorf. Die Statistik geht nach jeder Disziplin ans Netzwerk.
 */
public final class Competition {

    public enum Phase { STAGE, BETWEEN, CEREMONY }

    public static final int SLOT_GIVE_UP = 8;
    public static final int SLOT_RESET = 0;
    private static final int TABLE_LINES = 5;
    private static final int FIREWORKS_EVERY = 2;

    private final SommerspielePlugin plugin;
    private final Program program;
    private final List<Racer> racers = new ArrayList<>();
    private final List<List<Racer>> parties = new ArrayList<>();
    private final MedalTable table = new MedalTable();
    private final Set<UUID> spectators = new HashSet<>();
    private final Map<Discipline, List<Ranking.Placed>> placed = new LinkedHashMap<>();
    private final Map<String, Result> lastResults = new HashMap<>();
    /** Wer gerade die Verbindung verloren hat und zurückkommen darf (Frist in Spielticks). */
    private final Comeback comeback = new Comeback();
    /** Wem das Netzwerk gerade „spielt“ glaubt (nur Änderungen werden gemeldet). */
    private final Set<UUID> told = new HashSet<>();
    private long nowTick;
    private int index = -1;
    private Stage stage;
    private Phase phase = Phase.STAGE;
    private int timer;
    private boolean over;

    Competition(SommerspielePlugin plugin, Program program, List<List<Player>> groups) {
        this.plugin = plugin;
        this.program = program;
        int n = 1;
        for (List<Player> g : groups) {
            List<Racer> party = new ArrayList<>();
            for (Player p : g) {
                Racer r = new Racer(p.getUniqueId(), p.getName(), n++);
                racers.add(r);
                party.add(r);
                table.join(r.id());
            }
            parties.add(party);
        }
    }

    SommerspielePlugin plugin() {
        return plugin;
    }

    public Program program() {
        return program;
    }

    public Phase phase() {
        return phase;
    }

    public Stage stage() {
        return stage;
    }

    public List<Racer> racers() {
        return racers;
    }

    List<List<Racer>> parties() {
        return parties;
    }

    public MedalTable table() {
        return table;
    }

    /** Laufende Disziplin (zwischen zwei Disziplinen die nächste). */
    public Discipline discipline() {
        List<Discipline> ds = program.disciplines();
        return ds.get(Math.max(0, Math.min(ds.size() - 1, phase == Phase.BETWEEN ? index + 1 : index)));
    }

    public int disciplineNumber() {
        return Math.min(program.disciplines().size(), (phase == Phase.BETWEEN ? index + 1 : index) + 1);
    }

    /** Teilnehmer (auch wer schon aufgegeben hat), null wenn nicht dabei oder gegangen. */
    public Racer racer(UUID uuid) {
        for (Racer r : racers) {
            if (r.uuid().equals(uuid) && !r.left()) {
                return r;
            }
        }
        return null;
    }

    public boolean watching(UUID uuid) {
        return spectators.contains(uuid);
    }

    /** Wartet der Wettkampf gerade auf diese Person (Verbindung verloren, Platz noch frei)? */
    public boolean waitingFor(UUID uuid) {
        return comeback.waitingFor(uuid);
    }

    public List<Racer> active() {
        return racers.stream().filter(r -> r.player() != null).toList();
    }

    void start() {
        for (Racer r : racers) {
            Player p = r.player();
            if (p == null) {
                continue;
            }
            plugin.store().pin(r.uuid());
            plugin.data(p);
            tellPlaying(p, true);
            p.sendMessage(Texts.msg(plugin.lang(p), "comp.bib", Placeholder.unparsed("n", String.valueOf(r.number()))));
        }
        nextStage();
    }

    /** Leibchen mit Startnummer anziehen, Schnellleiste je nach Lage. */
    void dress(Player p, Racer r, boolean racing) {
        String lang = plugin.lang(p);
        Village.reset(p);
        p.setInvulnerable(true);
        ItemStack bib = Items.named(Material.LEATHER_CHESTPLATE, 1, Texts.text(lang, "item.bib",
                Placeholder.unparsed("n", String.valueOf(r.number()))), null);
        bib.editMeta(LeatherArmorMeta.class, m -> {
            m.setColor(r.colour());
            m.setUnbreakable(true);
        });
        p.getInventory().setChestplate(bib);
        if (racing) {
            p.getInventory().setItem(SLOT_RESET, Items.kitTag(Items.named(Material.FEATHER, 1, Texts.text(lang, "item.reset"),
                    Texts.lines(lang, "item.reset.lore")), "reset"));
        }
        p.getInventory().setItem(SLOT_GIVE_UP, Items.kitTag(Items.named(Material.RED_DYE, 1, Texts.text(lang, "item.giveup"),
                Texts.lines(lang, "item.giveup.lore")), "giveup"));
        p.getInventory().setHeldItemSlot(4);
    }

    private void nextStage() {
        index++;
        if (index >= program.disciplines().size()) {
            ceremony();
            return;
        }
        Discipline d = program.disciplines().get(index);
        stage = switch (d) {
            case ALMLAUF -> new AlmRace(this);
            case BOGEN -> new ArcheryStage(this);
            case REGATTA -> new RegattaRace(this);
            case SEIFENKISTE -> new KartRace(this);
        };
        phase = Phase.STAGE;
        for (Player p : everyone()) {
            String lang = plugin.lang(p);
            Ui.title(p, Texts.text(lang, "comp.next.title", Placeholder.unparsed("discipline", Texts.plain(lang, "discipline." + d.id()))),
                    Texts.text(lang, "comp.next.sub", Placeholder.unparsed("n", String.valueOf(index + 1)),
                            Placeholder.unparsed("total", String.valueOf(program.disciplines().size()))), 2500);
            Ui.sound(p, "minecraft:block.bell.use", 1.2f);
        }
        stage.start();
        for (UUID u : spectators) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                p.teleport(stage.view());
            }
        }
        plugin.stateChanged();
    }

    /** Alle Teilnehmer (online) und Zuschauer. */
    List<Player> everyone() {
        List<Player> out = new ArrayList<>();
        for (Racer r : racers) {
            Player p = r.player();
            if (p != null) {
                out.add(p);
            }
        }
        for (UUID u : spectators) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    void tick(long tick) {
        nowTick = tick;
        if (phase == Phase.STAGE && stage != null) {
            stage.tick(tick);
            if (stage.done()) {
                endStage();
            }
        }
    }

    void second() {
        if (!over && comeback.count() > 0) {
            for (UUID u : comeback.expired(nowTick)) {
                tooLate(u);
            }
        }
        switch (phase) {
            case STAGE -> {
                if (stage != null) {
                    stage.second();
                }
            }
            case BETWEEN -> {
                if (--timer <= 0) {
                    nextStage();
                }
            }
            case CEREMONY -> {
                if (timer % FIREWORKS_EVERY == 0) {
                    fireworks();
                }
                if (--timer <= 0) {
                    finish();
                    return;
                }
            }
            default -> {
            }
        }
        if (!over) {
            sidebars();
        }
        if (!over && active().isEmpty() && comeback.count() == 0) {
            abort();
        }
    }

    private void endStage() {
        Discipline d = stage.discipline();
        List<Result> results = stage.results();
        Map<String, Result> byId = new HashMap<>();
        List<Ranking.Entry> entries = new ArrayList<>();
        for (Result res : results) {
            byId.put(res.id(), res);
            entries.add(new Ranking.Entry(res.id(), res.value(d), res.finished()));
        }
        List<Ranking.Placed> ranked = Ranking.rank(entries, d.scored());
        placed.put(d, ranked);
        lastResults.clear();
        lastResults.putAll(byId);
        boolean training = entries.size() < Ranking.MEDALS_FROM;
        for (Ranking.Placed pl : ranked) {
            Result res = byId.get(pl.entry().id());
            for (Racer r : res.members()) {
                record(r, d, res, pl);
            }
        }
        announce(d, ranked, byId, training);
        stage.cleanup();
        plugin.saveBoard();
        if (index + 1 < program.disciplines().size()) {
            phase = Phase.BETWEEN;
            timer = plugin.settings().pauseSeconds();
            Discipline next = program.disciplines().get(index + 1);
            for (Player p : everyone()) {
                String lang = plugin.lang(p);
                p.sendMessage(Texts.msg(lang, "comp.between", Placeholder.unparsed("discipline", Texts.plain(lang, "discipline." + next.id()))));
            }
        } else {
            ceremony();
        }
    }

    /** Ergebnis einer Person verbuchen: Spiegel, Spielerdatei, Bestenliste, Netzwerk. */
    private void record(Racer r, Discipline d, Result res, Ranking.Placed pl) {
        table.add(r.id(), pl.medal(), pl.points());
        PlayerData data = plugin.store().get(r.uuid(), r.name());
        long best = !res.finished() ? -1 : d.scored() ? res.rings() : res.timeMs();
        data.addRace(d, best, pl.medal(), pl.points());
        if (d == Discipline.BOGEN) {
            data.addArchery(res.rings(), res.centres());
        }
        plugin.store().save(data);
        if (res.finished() && best > 0) {
            plugin.board().submit(d, r.uuid(), r.name(), best);
        }
        if (pl.medal() != null || pl.points() > 0) {
            plugin.board().medal(r.uuid(), r.name(), pl.medal(), pl.points());
        }
        Map<String, Long> add = new LinkedHashMap<>();
        add.put("races", 1L);
        add.put("points", (long) pl.points());
        if (pl.medal() != null) {
            add.put(pl.medal().id(), 1L);
        }
        Map<String, Long> times = new LinkedHashMap<>();
        Map<String, Long> bests = new LinkedHashMap<>();
        if (d == Discipline.BOGEN) {
            add.put("bogen_rings", (long) res.rings());
            add.put("bogen_centres", (long) res.centres());
            bests.put("bogen_best", (long) res.rings());
        } else if (res.finished()) {
            times.put("time_" + d.id() + "_ms", res.timeMs());
        }
        if (d == Discipline.SEIFENKISTE && res.missed() > 0) {
            add.put("kiste_crashes", (long) res.missed());
        }
        plugin.net().stats(r.uuid(), add, times, bests);
        Player p = r.player();
        if (p != null && pl.medal() != null) {
            String lang = plugin.lang(p);
            String disc = Texts.plain(lang, "discipline." + d.id());
            p.sendMessage(Texts.msg(lang, "comp.medal." + pl.medal().id(), Placeholder.unparsed("discipline", disc)));
            Ui.title(p, Texts.text(lang, "comp.medal.title." + pl.medal().id()), Texts.text(lang, "race.finish.sub",
                    Placeholder.unparsed("time", show(d, res, lang))), 3000);
            Ui.sound(p, pl.medal() == Medal.GOLD ? "minecraft:ui.toast.challenge_complete" : "minecraft:entity.player.levelup", 1f);
        }
    }

    private void announce(Discipline d, List<Ranking.Placed> ranked, Map<String, Result> byId, boolean training) {
        for (Player p : everyone()) {
            String lang = plugin.lang(p);
            String disc = Texts.plain(lang, "discipline." + d.id());
            p.sendMessage(Texts.parse(Texts.raw(lang, "comp.result.header"), Placeholder.unparsed("discipline", disc)));
            for (Ranking.Placed pl : ranked) {
                Result res = byId.get(pl.entry().id());
                if (!pl.finished()) {
                    p.sendMessage(Texts.parse(Texts.raw(lang, "comp.result.dnf"), Placeholder.unparsed("name", res.names())));
                    continue;
                }
                String extra = "";
                if (d == Discipline.REGATTA && res.missed() > 0) {
                    extra = Texts.raw(lang, "comp.result.penalty").replace("<n>", String.valueOf(res.missed()));
                } else if (d == Discipline.SEIFENKISTE && res.missed() > 0) {
                    extra = Texts.raw(lang, "comp.result.crashes").replace("<n>", String.valueOf(res.missed()));
                } else if (d == Discipline.BOGEN && res.centres() > 0) {
                    extra = Texts.raw(lang, "comp.result.centres").replace("<n>", String.valueOf(res.centres()));
                }
                p.sendMessage(Texts.parse(Texts.raw(lang, "comp.result.row"), medalTag(pl.medal()),
                        Placeholder.unparsed("place", String.valueOf(pl.place())), Placeholder.unparsed("name", res.names()),
                        Placeholder.unparsed("time", show(d, res, lang)), Placeholder.parsed("extra", extra)));
            }
            if (training) {
                p.sendMessage(Texts.msg(lang, "comp.training"));
            }
            if (program.full()) {
                standings(p, lang);
            }
        }
    }

    /** Zeit oder Ringe, wie es die Ergebnisliste zeigt. */
    public static String show(Discipline d, Result res, String lang) {
        return d.scored() ? Texts.raw(lang, "unit.rings").replace("<n>", String.valueOf(res.rings()))
                : TimeFormat.format(res.timeMs(), lang);
    }

    private static TagResolver medalTag(Medal m) {
        String s = m == null ? "<gray>·</gray>" : switch (m) {
            case GOLD -> "<au>●</au>";
            case SILVER -> "<si>●</si>";
            case BRONZE -> "<bz>●</bz>";
        };
        return Placeholder.parsed("medal", s);
    }

    /** Medaillenspiegel in den Chat. */
    private void standings(Player p, String lang) {
        p.sendMessage(Texts.parse(Texts.raw(lang, "comp.standings.header")));
        for (MedalTable.Row row : table.overall()) {
            p.sendMessage(Texts.parse(Texts.raw(lang, "comp.standings.row"), rowTags(row)));
        }
    }

    TagResolver[] rowTags(MedalTable.Row row) {
        return new TagResolver[] {Placeholder.unparsed("place", String.valueOf(row.place())),
            Placeholder.unparsed("name", nameOf(row.id())), Placeholder.unparsed("ng", String.valueOf(row.gold())),
            Placeholder.unparsed("ns", String.valueOf(row.silver())), Placeholder.unparsed("nb", String.valueOf(row.bronze())),
            Placeholder.unparsed("points", String.valueOf(row.points()))};
    }

    private String nameOf(String id) {
        for (Racer r : racers) {
            if (r.id().equals(id)) {
                return r.name();
            }
        }
        return "?";
    }

    // ---------- Siegerehrung ----------

    private void ceremony() {
        phase = Phase.CEREMONY;
        releaseAll();
        timer = plugin.settings().ceremonySeconds();
        stage = null;
        List<List<Racer>> podium = new ArrayList<>();
        List<Integer> places = new ArrayList<>();
        if (program.full() || placed.isEmpty()) {
            for (MedalTable.Row row : table.overall()) {
                Racer r = racer(UUID.fromString(row.id()));
                if (r != null) {
                    podium.add(List.of(r));
                    places.add(row.place());
                }
            }
        } else {
            for (Ranking.Placed pl : placed.values().iterator().next()) {
                Result res = lastResults.get(pl.entry().id());
                List<Racer> here = res.members().stream().filter(r -> r.player() != null).toList();
                if (!here.isEmpty()) {
                    podium.add(here);
                    places.add(pl.place());
                }
            }
        }
        int others = 0;
        for (int i = 0; i < podium.size(); i++) {
            int place = places.get(i);
            List<Racer> team = podium.get(i);
            for (int k = 0; k < team.size(); k++) {
                Player p = team.get(k).player();
                if (p == null) {
                    continue;
                }
                Village.reset(p);
                p.setInvulnerable(true);
                Location at = place >= 1 && place <= 3 && placesOk(places, i) ? podiumSpot(place, k, team.size())
                        : crowdSpot(others++);
                p.teleport(at);
            }
        }
        for (UUID u : spectators) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                p.teleport(crowdSpot(others++));
            }
        }
        String winner = podium.isEmpty() ? "" : podium.get(0).stream().map(Racer::name).reduce((a, b) -> a + " & " + b).orElse("");
        int winnerPoints = program.full() && !table.overall().isEmpty() ? table.overall().get(0).points() : 0;
        for (Player p : everyone()) {
            String lang = plugin.lang(p);
            Ui.title(p, Texts.text(lang, "comp.over.title"), Texts.text(lang, "comp.over.sub"), 4000);
            Ui.sound(p, "minecraft:ui.toast.challenge_complete", 1f);
            if (program.full() && racers.size() >= Ranking.MEDALS_FROM && !winner.isEmpty()) {
                p.sendMessage(Texts.msg(lang, "comp.winner", Placeholder.unparsed("name", winner),
                        Placeholder.unparsed("points", String.valueOf(winnerPoints))));
            }
        }
        fireworks();
    }

    /** Nur die ersten drei Plätze aufs Treppchen (geteilte Plätze teilen die Stufe). */
    private static boolean placesOk(List<Integer> places, int i) {
        return places.get(i) <= 3;
    }

    private Location podiumSpot(int place, int k, int of) {
        double[][] spots = {{0.5, 4.07}, {-3.5, 3.07}, {4.5, 2.07}};
        double[] s = spots[place - 1];
        double dx = of > 1 ? (k - (of - 1) / 2.0) * 0.8 : 0;
        return new Location(plugin.world(), s[0] + dx, Terrain.FLOOR_Y + s[1], Places.PODIUM[1] + 0.5, 0f, 0f);
    }

    private Location crowdSpot(int i) {
        double x = -5.5 + (i % 8) * 1.6;
        double z = Places.PODIUM[1] + 6.5 + (i / 8) * 1.6;
        return new Location(plugin.world(), x, Terrain.FLOOR_Y + 1, z, 180f, -8f);
    }

    private void fireworks() {
        Location base = new Location(plugin.world(), Places.PODIUM[0] + 0.5, Terrain.FLOOR_Y + 9, Places.PODIUM[1] - 1.5);
        Color[][] colours = {{Color.fromRGB(0xF2B632), Color.WHITE}, {Color.fromRGB(0xFF7A3D), Color.fromRGB(0xFFE066)},
            {Color.fromRGB(0x2BB673), Color.fromRGB(0xF2B632)}};
        for (int i = -1; i <= 1; i++) {
            Location at = base.clone().add(i * 6, 0, 0);
            Color[] c = colours[Math.floorMod(i + timer, colours.length)];
            plugin.world().spawn(at, Firework.class, fw -> {
                var meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(c).withFade(Color.WHITE)
                        .trail(true).flicker(true).build());
                meta.setPower(0);
                fw.setFireworkMeta(meta);
                fw.setPersistent(false);
            });
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.world().getEntitiesByClass(Firework.class).forEach(Firework::detonate), 8L);
    }

    private void finish() {
        over = true;
        List<MedalTable.Row> overall = table.overall();
        String winnerId = program.full() && racers.size() >= Ranking.MEDALS_FROM && !overall.isEmpty()
                && (overall.size() < 2 || overall.get(1).place() > 1) ? overall.get(0).id() : null;
        for (Racer r : racers) {
            PlayerData data = plugin.store().get(r.uuid(), r.name());
            boolean won = r.id().equals(winnerId);
            data.addEvent(won);
            plugin.store().save(data);
            Map<String, Long> add = new LinkedHashMap<>();
            add.put("events", 1L);
            if (won) {
                add.put("wins", 1L);
            }
            Map<String, Long> best = program.full() ? Map.of("best_points", (long) table.row(r.id()).points()) : Map.of();
            plugin.net().stats(r.uuid(), add, Map.of(), best);
            Player p = r.player();
            plugin.store().unpin(r.uuid(), p != null);
            if (p != null) {
                p.sendMessage(Texts.msg(plugin.lang(p), "comp.back"));
                plugin.village().enter(p);
            }
        }
        for (UUID u : spectators) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                plugin.village().enter(p);
            }
        }
        spectators.clear();
        plugin.saveBoard();
        plugin.games().ended(this);
    }

    /** Abbruch (Server stoppt, Admin, alle weg): aufräumen, alle ins Dorf, nichts mehr werten. */
    void abort() {
        if (over) {
            return;
        }
        over = true;
        releaseAll();
        if (stage != null) {
            stage.cleanup();
        }
        for (Racer r : racers) {
            Player p = r.player();
            plugin.store().unpin(r.uuid(), p != null);
            if (p != null) {
                plugin.village().enter(p);
            }
        }
        for (UUID u : spectators) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                plugin.village().enter(p);
            }
        }
        spectators.clear();
        plugin.games().ended(this);
    }

    // ---------- Zuschauen, Verlassen ----------

    public void spectate(Player p) {
        spectators.add(p.getUniqueId());
        Village.reset(p);
        p.setInvulnerable(true);
        String lang = plugin.lang(p);
        p.getInventory().setItem(SLOT_GIVE_UP, Items.kitTag(Items.named(Material.RED_DYE, 1, Texts.text(lang, "item.leave"),
                Texts.lines(lang, "item.leave.lore")), "leave"));
        p.teleport(stage != null ? stage.view() : crowdSpot(spectators.size()));
    }

    /** Zuschauen beenden oder den Wettkampf verlassen (ins Dorf). */
    public void leave(Player p) {
        if (spectators.remove(p.getUniqueId())) {
            plugin.village().enter(p);
            return;
        }
        Racer r = racer(p.getUniqueId());
        if (r != null) {
            tellPlaying(p, false);
            quit(r);
            plugin.store().unpin(r.uuid(), true);
            plugin.village().enter(p);
        }
    }

    /** Teilnehmer ist weg (Server verlassen oder /verlassen). */
    public void quit(Racer r) {
        if (r.left()) {
            return;
        }
        if (stage != null) {
            stage.quit(r);
        }
        r.leave();
        for (Player p : everyone()) {
            p.sendMessage(Texts.msg(plugin.lang(p), "comp.left", Placeholder.unparsed("name", r.name())));
        }
    }

    /**
     * Verbindung weg. Wer im Wettkampf ist, behält seinen Platz {@code rueckkehr-sekunden} lang – Disziplin, Lauf,
     * Zeiten – und macht nach dem Wiederkommen genau dort weiter ({@link #rejoin}); so lange bietet die Lobby „Zurück ins
     * Spiel“ an. Sein Lauf wartet, die Uhr läuft weiter. Ohne Rückkehr-Zeit oder bei der Siegerehrung wie bisher.
     */
    public void quit(Player p) {
        UUID u = p.getUniqueId();
        spectators.remove(u);
        // Das Netzwerk vergisst „spielt gerade“, sobald jemand den Server verlässt.
        told.remove(u);
        Racer r = racer(u);
        if (r == null) {
            return;
        }
        int seconds = plugin.settings().comebackSeconds();
        if (Comeback.holds(seconds, !over && phase != Phase.CEREMONY, true, false)) {
            comeback.away(u, nowTick, seconds);
            plugin.net().holdSlot(u, seconds);
            if (stage != null && phase == Phase.STAGE) {
                stage.away(r);
            }
            for (Player o : everyone()) {
                if (!o.equals(p)) {
                    o.sendMessage(Texts.msg(plugin.lang(o), "comp.away", Placeholder.unparsed("name", r.name()),
                            Placeholder.unparsed("secs", String.valueOf(seconds))));
                }
            }
            return;
        }
        quit(r);
        plugin.store().unpin(r.uuid(), false);
    }

    /**
     * Wiederkommen: Wartet der Wettkampf noch auf diese Person, geht es dort weiter, wo sie war (true). Zu spät: sie hat
     * den Wettkampf verlassen wie bisher (false – sie kommt ins Dorf).
     */
    public boolean rejoin(Player p) {
        UUID u = p.getUniqueId();
        Racer r = racer(u);
        if (r == null || !comeback.waitingFor(u)) {
            return false;
        }
        if (!comeback.back(u, nowTick) || over || phase == Phase.CEREMONY) {
            tooLate(u);
            return false;
        }
        plugin.net().holdSlot(u, 0);
        tellPlaying(p, true);
        String lang = plugin.lang(p);
        if (phase == Phase.STAGE && stage != null) {
            stage.rejoin(r, p);
        } else {
            dress(p, r, false);
            p.teleport(stage != null ? stage.view() : crowdSpot(0));
        }
        Ui.title(p, Texts.text(lang, "comp.welcome-back.title"), Texts.text(lang, "comp.welcome-back.sub"), 2500);
        Ui.sound(p, "minecraft:entity.player.levelup", 1.2f);
        p.sendMessage(Texts.msg(lang, "comp.welcome-back"));
        for (Player o : everyone()) {
            if (!o.equals(p)) {
                o.sendMessage(Texts.msg(plugin.lang(o), "comp.back.all", Placeholder.unparsed("name", r.name())));
            }
        }
        sidebars();
        return true;
    }

    /** Nicht rechtzeitig zurück: hat den Wettkampf verlassen wie bisher (sein Lauf ist zu Ende, die Zeiten bleiben). */
    private void tooLate(UUID u) {
        plugin.net().holdSlot(u, 0);
        Racer r = racer(u);
        if (r == null || r.left()) {
            return;
        }
        if (stage != null && phase == Phase.STAGE) {
            stage.quit(r);
        }
        r.leave();
        plugin.store().unpin(u, Bukkit.getPlayer(u) != null);
        for (Player o : everyone()) {
            o.sendMessage(Texts.msg(plugin.lang(o), "comp.away.late", Placeholder.unparsed("name", r.name())));
        }
    }

    /** Siegerehrung oder Abbruch: niemand muss mehr zurück („Zurück ins Spiel“ zurücknehmen), niemand spielt mehr. */
    private void releaseAll() {
        for (UUID u : comeback.clear()) {
            plugin.net().holdSlot(u, 0);
        }
        for (UUID u : new ArrayList<>(told)) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                tellPlaying(p, false);
            }
        }
        told.clear();
    }

    /** Dem Netzwerk sagen, ob jemand gerade spielt (dann fragt „/lobby“ erst nach) – nur bei Änderungen. */
    private void tellPlaying(Player p, boolean playing) {
        if (playing ? told.add(p.getUniqueId()) : told.remove(p.getUniqueId())) {
            plugin.net().playing(p, playing);
        }
    }

    public void giveUp(Player p) {
        Racer r = racer(p.getUniqueId());
        if (r != null && stage != null && phase == Phase.STAGE) {
            stage.giveUp(r);
            p.sendMessage(Texts.msg(plugin.lang(p), "comp.giveup"));
        }
    }

    // ---------- Leiste ----------

    private void sidebars() {
        List<MedalTable.Row> rows = table.overall();
        for (Player p : everyone()) {
            String lang = plugin.lang(p);
            List<Component> lines = new ArrayList<>();
            Discipline d = discipline();
            lines.add(Texts.text(lang, "sidebar.discipline", Placeholder.unparsed("discipline", Texts.plain(lang, "discipline." + d.id())),
                    Placeholder.unparsed("n", String.valueOf(disciplineNumber())),
                    Placeholder.unparsed("total", String.valueOf(program.disciplines().size()))));
            Racer me = racer(p.getUniqueId());
            if (stage != null && phase == Phase.STAGE) {
                String s = stage.status(me, lang);
                if (s != null && !s.isEmpty()) {
                    lines.add(Texts.parse(s));
                }
            }
            if (me != null) {
                lines.add(Texts.text(lang, "sidebar.number", Placeholder.unparsed("n", String.valueOf(me.number()))));
            }
            lines.add(Component.empty());
            lines.add(Texts.text(lang, "sidebar.table"));
            for (int i = 0; i < Math.min(TABLE_LINES, rows.size()); i++) {
                lines.add(Texts.text(lang, "sidebar.row", rowTags(rows.get(i))));
            }
            lines.add(Component.text(" "));
            lines.add(Texts.text(lang, "sidebar.address"));
            plugin.boards().sidebar().show(p, Texts.text(lang, "sidebar.title"), lines);
        }
    }

    /** Für /sommeradmin status. */
    public List<String> debug() {
        List<String> out = new ArrayList<>();
        out.add("wettkampf=" + program.id() + " phase=" + phase + " disziplin=" + discipline().id() + " index=" + index
                + " timer=" + timer);
        for (Racer r : racers) {
            MedalTable.Row row = table.row(r.id());
            out.add("r=" + r.name() + " nr=" + r.number() + " left=" + r.left() + " gold=" + row.gold() + " silver="
                    + row.silver() + " bronze=" + row.bronze() + " points=" + row.points());
        }
        if (stage != null && phase == Phase.STAGE) {
            out.addAll(stage.debug());
        }
        return out;
    }
}

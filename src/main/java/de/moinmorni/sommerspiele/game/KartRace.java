package de.moinmorni.sommerspiele.game;

import de.moinmorni.sommerspiele.Texts;
import de.moinmorni.sommerspiele.Ui;
import de.moinmorni.sommerspiele.SommerspielePlugin;
import de.moinmorni.sommerspiele.course.Kiste;
import de.moinmorni.sommerspiele.course.Path;
import de.moinmorni.sommerspiele.course.Places;
import de.moinmorni.sommerspiele.course.Terrain;
import de.moinmorni.sommerspiele.logic.Discipline;
import de.moinmorni.sommerspiele.logic.Heats;
import de.moinmorni.sommerspiele.logic.Kart;
import de.moinmorni.sommerspiele.logic.RaceClock;
import de.moinmorni.sommerspiele.logic.TimeFormat;
import io.papermc.paper.entity.TeleportFlag;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.RideableMinecart;

/**
 * Seifenkistenrennen: Läufe mit bis zu vier Loren nebeneinander (je eine eigene Schienenstrecke den Kistenberg hinab).
 * Die Lore fährt nach der eigenen Fahrphysik ({@link Kart}) die Schienen entlang: bergab wird sie schneller,
 * <b>vorwärts</b> schubst an (nur langsam), <b>rückwärts</b> bremst. Wer zu schnell in eine Kurve fährt, entgleist und
 * verliert Zeit. An der Weiche wählt man mit <b>vorwärts</b> das Steilstück, mit <b>rechts</b> die Kehren. Geht mit
 * Java und Bedrock gleich (die Eingaben kommen über Minecraft selbst).
 */
public final class KartRace implements Stage {

    private enum HeatPhase { PREP, COUNT, RUN, AFTER }

    private static final int PREP_TICKS = 50;
    private static final int COUNT_SECONDS = 3;
    private static final int AFTER_TICKS = 50;
    private static final int RELEASE_TICKS = 50;
    private static final int SEAT_DELAY = 5;
    private static final int TICKS = 20;
    /** Lore liegt so hoch über der Schiene. */
    private static final double CART_Y = 0.0625;
    /** Ab so vielen Blöcken vor der Weiche wird gefragt. */
    private static final double FORK_ASK = 30;
    /** Selbstfahrer: Spielraum unter dem Kurventempo. */
    private static final double PILOT_MARGIN = 0.04;

    /** Selbstfahrer für Tests (/sommeradmin fahren). */
    public enum Pilot { NONE, AUTO, FULL }

    /** Eine Lore. */
    final class Team {
        final Racer racer;
        final int heat;
        int lane;
        RideableMinecart cart;
        Kart kart;
        boolean steep;
        boolean switched;
        boolean finished;
        boolean dnf;
        long timeMs;
        boolean resetting;
        long releaseAt = -1;
        boolean released;
        Pilot pilot = Pilot.NONE;
        Boolean pilotSteep;

        Team(Racer racer, int heat) {
            this.racer = racer;
            this.heat = heat;
        }

        boolean active() {
            return !finished && !dnf;
        }

        Path path() {
            return Kiste.path(lane, steep && switched);
        }
    }

    private final Competition comp;
    private final SommerspielePlugin plugin;
    private final List<Team> teams = new ArrayList<>();
    private final List<List<Team>> heats = new ArrayList<>();
    private int heat = -1;
    private HeatPhase phase = HeatPhase.PREP;
    private int phaseTicks;
    private RaceClock clock;
    private long goTick;
    private long nowTick;
    private boolean done;

    KartRace(Competition comp) {
        this.comp = comp;
        this.plugin = comp.plugin();
        List<Racer> starters = new ArrayList<>();
        for (List<Racer> party : comp.parties()) {
            for (Racer r : party) {
                // Wer gerade die Verbindung verloren hat und zurückkommen darf, bekommt seinen Platz in den Läufen.
                if (r.player() != null || comp.waitingFor(r.uuid())) {
                    starters.add(r);
                }
            }
        }
        List<List<Racer>> split = Heats.split(starters, Kiste.lanes());
        for (int h = 0; h < split.size(); h++) {
            List<Team> list = new ArrayList<>();
            for (int i = 0; i < split.get(h).size(); i++) {
                Team t = new Team(split.get(h).get(i), h);
                t.lane = i;
                t.kart = newKart(t.lane, false, Kiste.startS(t.lane));
                list.add(t);
                teams.add(t);
            }
            heats.add(list);
        }
    }

    private static Kart newKart(int lane, boolean steep, double s) {
        Path p = Kiste.path(lane, steep);
        return new Kart(x -> Kiste.railY(p.at(x)), Kiste.curves(lane, steep), s, Kiste.finishS(lane, steep));
    }

    @Override
    public Discipline discipline() {
        return Discipline.SEIFENKISTE;
    }

    @Override
    public void start() {
        for (Team t : teams) {
            Player p = t.racer.player();
            if (p == null) {
                continue;
            }
            String lang = plugin.lang(p);
            p.sendMessage(Texts.msg(lang, "kiste.intro"));
            if (plugin.settings().fork()) {
                p.sendMessage(Texts.msg(lang, "kiste.fork.intro"));
            }
            if (heats.size() > 1 && t.heat > 0) {
                p.sendMessage(Texts.msg(lang, "heat.wait", Placeholder.unparsed("n", String.valueOf(t.heat + 1))));
            }
            toStand(p, t.racer);
        }
        startHeat(0);
    }

    private void startHeat(int h) {
        heat = h;
        phase = HeatPhase.PREP;
        phaseTicks = 0;
        for (Team t : heats.get(h)) {
            if (gone(t)) {
                t.dnf = true;
                continue;
            }
            place(t);
            Player p = t.racer.player();
            if (p != null) {
                p.sendMessage(Texts.msg(plugin.lang(p), "heat.your-turn", Placeholder.unparsed("lane", String.valueOf(t.lane + 1))));
            }
        }
        for (Player p : comp.everyone()) {
            String lang = plugin.lang(p);
            if (heats.size() > 1) {
                Ui.title(p, Texts.text(lang, "heat.title", Placeholder.unparsed("n", String.valueOf(h + 1)),
                        Placeholder.unparsed("total", String.valueOf(heats.size()))), Texts.text(lang, "count.sub"), 1500);
            }
        }
    }

    /** Wo die Lore an der Stelle s des Wegs steht. */
    private Location at(Team t) {
        Path p = t.path();
        Path.Sample s = p.at(Math.min(t.kart.s(), p.length()));
        return new Location(plugin.world(), s.x(), Terrain.FLOOR_Y + Kiste.railY(s) + CART_Y, s.z(), s.yaw(), 0f);
    }

    /** Neue Lore an die jetzige Stelle (Start oder Zurücksetzen), Fahrer hinein. */
    private void place(Team t) {
        t.resetting = true;
        removeCart(t);
        Location at = at(t);
        Player p = t.racer.player();
        if (p != null) {
            if (p.getVehicle() != null) {
                p.leaveVehicle();
            }
            comp.dress(p, t.racer, true);
            p.teleport(at);
        }
        t.cart = plugin.world().spawn(at, RideableMinecart.class, c -> {
            c.setPersistent(false);
            c.setGravity(false);
            c.setInvulnerable(true);
            c.setMaxSpeed(0);
            c.setSlowWhenEmpty(true);
            c.addScoreboardTag("sommerspiele_kiste");
        });
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            seat(t);
            t.resetting = false;
        }, SEAT_DELAY);
    }

    private void seat(Team t) {
        if (t.cart == null || !t.cart.isValid()) {
            return;
        }
        Player p = t.racer.player();
        if (p != null && !t.cart.getPassengers().contains(p)) {
            if (p.getVehicle() != null) {
                p.leaveVehicle();
            }
            p.teleport(t.cart.getLocation());
            t.cart.addPassenger(p);
        }
    }

    private boolean seated(Team t) {
        Player p = t.racer.player();
        return t.cart != null && t.cart.isValid() && (p == null || t.cart.getPassengers().contains(p));
    }

    private void removeCart(Team t) {
        if (t.cart != null) {
            for (Entity e : new ArrayList<>(t.cart.getPassengers())) {
                t.cart.removePassenger(e);
            }
            t.cart.remove();
            t.cart = null;
        }
    }

    @Override
    public void tick(long tick) {
        nowTick = tick;
        if (done || heat < 0) {
            return;
        }
        phaseTicks++;
        List<Team> now = heats.get(heat);
        switch (phase) {
            case PREP -> {
                if (phaseTicks % 10 == 0) {
                    now.stream().filter(t -> !t.resetting && t.active()).forEach(this::seat);
                }
                if (phaseTicks >= PREP_TICKS) {
                    phase = HeatPhase.COUNT;
                    phaseTicks = 0;
                }
            }
            case COUNT -> {
                if (phaseTicks % TICKS == 1) {
                    int n = COUNT_SECONDS - phaseTicks / TICKS;
                    for (Team t : now) {
                        Player p = t.racer.player();
                        if (p != null) {
                            String lang = plugin.lang(p);
                            Ui.title(p, Texts.text(lang, "count.number", Placeholder.unparsed("n", String.valueOf(n))),
                                    Texts.text(lang, "kiste.count.sub"), 900);
                            Ui.sound(p, "minecraft:block.note_block.bell", 1f);
                        }
                    }
                    now.stream().filter(t -> !t.resetting && t.active() && !seated(t)).forEach(this::seat);
                }
                if (phaseTicks >= COUNT_SECONDS * TICKS) {
                    go(tick, now);
                }
            }
            case RUN -> run(tick, now);
            case AFTER -> {
                now.forEach(this::rollOut);
                releaseDue(now, true);
                if (phaseTicks >= AFTER_TICKS) {
                    if (heat + 1 < heats.size()) {
                        startHeat(heat + 1);
                    } else {
                        done = true;
                    }
                }
            }
            default -> {
            }
        }
    }

    private void go(long tick, List<Team> now) {
        phase = HeatPhase.RUN;
        phaseTicks = 0;
        goTick = tick;
        clock = new RaceClock(tick);
        for (Team t : now) {
            t.kart.go();
            Player p = t.racer.player();
            if (p != null) {
                String lang = plugin.lang(p);
                Ui.title(p, Texts.text(lang, "count.go"), Component.empty(), 700);
                Ui.sound(p, "minecraft:entity.firework_rocket.launch", 1.2f);
            }
        }
    }

    private void run(long tick, List<Team> now) {
        long limit = plugin.settings().kisteLimit() * (long) TICKS;
        for (Team t : now) {
            if (!t.active()) {
                rollOut(t);
                continue;
            }
            if (gone(t)) {
                t.dnf = true;
                removeCart(t);
                continue;
            }
            Player p = t.racer.player();
            if (p == null) {
                // Verbindung verloren, Platz noch frei: die Lore wartet, die Uhr läuft weiter (Zeitlimit gilt).
                if (tick - goTick >= limit) {
                    t.dnf = true;
                    release(t);
                }
                continue;
            }
            drive(t, p, tick);
            if (t.active() && tick - goTick >= limit) {
                t.dnf = true;
                p.sendMessage(Texts.msg(plugin.lang(p), "race.dnf"));
                release(t);
            }
        }
        releaseDue(now, false);
        if (now.stream().noneMatch(Team::active)) {
            phase = HeatPhase.AFTER;
            phaseTicks = 0;
        }
    }

    /** Ein Tick Fahren: Eingaben lesen, Physik, Weiche, Lore setzen, Ziel. */
    private void drive(Team t, Player p, long tick) {
        if (t.resetting) {
            return;
        }
        if (!seated(t)) {
            seat(t);
        }
        boolean push;
        boolean brake;
        if (t.pilot != Pilot.NONE) {
            boolean[] in = t.kart.pilot(PILOT_MARGIN);
            push = in[0];
            brake = t.pilot == Pilot.AUTO && in[1];
            if (t.pilotSteep != null) {
                t.steep = t.pilotSteep;
            }
        } else {
            Input in = p.getCurrentInput();
            push = in.isForward();
            brake = in.isBackward();
            if (plugin.settings().fork() && !t.switched && nearFork(t)) {
                if (in.isForward()) {
                    t.steep = true;
                } else if (in.isRight() || in.isLeft()) {
                    t.steep = false;
                }
            }
        }
        if (plugin.settings().fork() && !t.switched && t.kart.s() >= Kiste.forkS(t.lane)) {
            t.switched = true;
            if (t.steep) {
                t.kart = t.kart.onto(x -> Kiste.railY(Kiste.path(t.lane, true).at(x)), Kiste.curves(t.lane, true),
                        Kiste.finishS(t.lane, true));
            }
            p.sendActionBar(Texts.text(plugin.lang(p), t.steep ? "kiste.fork.steep" : "kiste.fork.bends"));
            Ui.sound(p, "minecraft:block.iron_trapdoor.close", 1.4f);
        }
        Kart.Event e = t.kart.tick(push, brake);
        moveCart(t);
        if (brake && t.kart.v() > 0.2 && tick % 3 == 0) {
            plugin.world().spawnParticle(Particle.SMOKE, t.cart.getLocation().add(0, 0.2, 0), 3, 0.2, 0.05, 0.2, 0.01);
        }
        if (e == Kart.Event.CRASH) {
            String lang = plugin.lang(p);
            Ui.title(p, Texts.text(lang, "kiste.crash.title"), Texts.text(lang, "kiste.crash.sub"), 1600);
            Ui.sound(p, "minecraft:entity.minecart.inside", 0.6f);
            Ui.sound(p, "minecraft:block.anvil.land", 1.4f);
            plugin.world().spawnParticle(Particle.CRIT, t.cart.getLocation().add(0, 0.5, 0), 25, 0.5, 0.3, 0.5, 0.2);
        } else if (e == Kart.Event.FINISH) {
            finish(t, clock.at(tick, t.kart.finishFraction()));
        }
    }

    private boolean nearFork(Team t) {
        double d = Kiste.forkS(t.lane) - t.kart.s();
        return d > 0 && d <= FORK_ASK;
    }

    /** Lore an die Stelle setzen, an der die Physik sie hat (mit dem Fahrer darin). */
    private void moveCart(Team t) {
        if (t.cart == null || !t.cart.isValid()) {
            return;
        }
        t.cart.teleport(at(t), TeleportFlag.EntityState.RETAIN_PASSENGERS);
        t.cart.setVelocity(new org.bukkit.util.Vector());
    }

    /** Nach dem Ziel: weiter bremsen, bis die Lore steht. */
    private void rollOut(Team t) {
        if (!t.finished || t.released || t.cart == null || !t.cart.isValid() || t.kart.v() <= 0) {
            return;
        }
        if (t.kart.s() < t.path().length() - 2) {
            t.kart.tick(false, true);
            moveCart(t);
        }
    }

    /** Niemand mehr da und niemand, der zurückkommen darf? */
    private boolean gone(Team t) {
        return t.racer.player() == null && !comp.waitingFor(t.racer.uuid());
    }

    private void releaseDue(List<Team> now, boolean all) {
        for (Team t : now) {
            if (!t.released && (t.finished || t.dnf) && (all || (t.releaseAt >= 0 && nowTick >= t.releaseAt))) {
                release(t);
            }
        }
    }

    private void finish(Team t, long ms) {
        t.finished = true;
        t.timeMs = ms;
        t.releaseAt = nowTick + RELEASE_TICKS;
        Player p = t.racer.player();
        if (p != null) {
            String lang = plugin.lang(p);
            String time = TimeFormat.format(ms, lang);
            String extra = t.kart.crashes() > 0 ? Texts.raw(lang, "kiste.finish.crashes")
                    .replace("<n>", String.valueOf(t.kart.crashes())) : "";
            Ui.title(p, Texts.text(lang, "race.finish.title"), Texts.text(lang, "race.finish.sub", Placeholder.unparsed("time", time)), 2000);
            p.sendMessage(Texts.msg(lang, "race.finish.msg", Placeholder.unparsed("time", time), Placeholder.parsed("extra", extra)));
            Ui.sound(p, "minecraft:entity.player.levelup", 1.4f);
        }
        if (t.cart != null && t.cart.isValid()) {
            plugin.world().spawnParticle(Particle.FIREWORK, t.cart.getLocation().add(0, 1, 0), 30, 0.6, 0.6, 0.6, 0.05);
        }
    }

    /** Lore weg, Fahrer auf die Tribüne. */
    private void release(Team t) {
        t.released = true;
        t.resetting = true;
        removeCart(t);
        Player p = t.racer.player();
        if (p != null) {
            toStand(p, t.racer);
        }
    }

    private void toStand(Player p, Racer r) {
        if (p.getVehicle() != null) {
            p.leaveVehicle();
        }
        comp.dress(p, r, false);
        p.teleport(view());
    }

    /** Zurück zum letzten Kontrollpunkt (aus dem Stand, die Zeit läuft weiter). */
    private void reset(Team t) {
        if (!t.active() || t.resetting || phase != HeatPhase.RUN) {
            return;
        }
        double back = Kiste.checkpoints(t.lane, t.steep && t.switched).get(0);
        for (double c : Kiste.checkpoints(t.lane, t.steep && t.switched)) {
            if (c <= t.kart.s()) {
                back = c;
            }
        }
        t.kart.resetTo(back);
        place(t);
        Player p = t.racer.player();
        if (p != null) {
            p.sendMessage(Texts.msg(plugin.lang(p), "race.reset"));
        }
    }

    private Team teamOf(Player p) {
        for (Team t : teams) {
            if (t.racer.uuid().equals(p.getUniqueId())) {
                return t;
            }
        }
        return null;
    }

    private Team teamOf(Racer r) {
        for (Team t : teams) {
            if (t.racer == r) {
                return t;
            }
        }
        return null;
    }

    /** Zum Testen: Selbstfahrer an (klug bremsen oder Vollgas) und an der Weiche steil oder Kehren (null = egal). */
    public boolean pilot(Player p, Pilot mode, Boolean steep) {
        Team t = teamOf(p);
        if (t == null || !t.active()) {
            return false;
        }
        t.pilot = mode;
        t.pilotSteep = steep;
        return true;
    }

    /** Aussteigen geht während des Laufs nicht (aufgeben mit dem roten Farbstoff). */
    public boolean holds(Player p) {
        Team t = teamOf(p);
        return t != null && t.active() && !t.resetting && heat == t.heat && phase != HeatPhase.AFTER;
    }

    /** Gehört diese Lore zu einem Lauf? */
    public boolean ours(Entity cart) {
        return teams.stream().anyMatch(t -> t.cart == cart);
    }

    @Override
    public void second() {
        if (heat < 0) {
            return;
        }
        for (Team t : heats.get(heat)) {
            if (!t.active() || phase != HeatPhase.RUN) {
                continue;
            }
            Player p = t.racer.player();
            if (p == null) {
                continue;
            }
            String lang = plugin.lang(p);
            String time = TimeFormat.format(clock.now(nowTick), lang);
            String speed = String.valueOf(Math.round(t.kart.v() * 72));
            String key = t.kart.stopped() ? "kiste.bar.crash" : plugin.settings().fork() && !t.switched && nearFork(t)
                    ? (t.steep ? "kiste.bar.fork.steep" : "kiste.bar.fork.bends") : "kiste.bar";
            p.sendActionBar(Texts.text(lang, key, Placeholder.unparsed("time", time), Placeholder.unparsed("speed", speed)));
        }
        long left = heats.get(heat).stream().filter(Team::active).count();
        for (Player p : comp.everyone()) {
            Team t = teamOf(p);
            if (t != null && t.heat == heat && t.active()) {
                continue;
            }
            String lang = plugin.lang(p);
            p.sendActionBar(Texts.text(lang, "race.bar.watch", Placeholder.unparsed("n", String.valueOf(heat + 1)),
                    Placeholder.unparsed("total", String.valueOf(heats.size())), Placeholder.unparsed("left", String.valueOf(left))));
        }
        plugin.boards().live(Discipline.SEIFENKISTE, results(), heat + 1, heats.size());
    }

    @Override
    public boolean done() {
        return done;
    }

    @Override
    public List<Result> results() {
        List<Result> out = new ArrayList<>();
        for (Team t : teams) {
            out.add(new Result(List.of(t.racer), t.finished ? t.timeMs : 0, t.finished, t.kart.crashes(), 0, 0));
        }
        return out;
    }

    @Override
    public void quit(Racer r) {
        Team t = teamOf(r);
        if (t == null) {
            return;
        }
        Player p = r.player();
        if (p != null && p.getVehicle() != null) {
            t.resetting = true;
            p.leaveVehicle();
        }
        if (t.active()) {
            t.dnf = true;
        }
        removeCart(t);
    }

    /** Verbindung weg: aus der Lore, ohne dass das als Zurücksetzen zählt; die Lore wartet an ihrer Stelle. */
    @Override
    public void away(Racer r) {
        Team t = teamOf(r);
        Player p = r.player();
        if (t != null && p != null && p.getVehicle() != null) {
            boolean was = t.resetting;
            t.resetting = true;
            p.leaveVehicle();
            t.resetting = was;
        }
    }

    /**
     * Rechtzeitig zurück: Läuft sein Lauf gerade, sitzt er gleich wieder in der Lore – an der Stelle, an der sie stand
     * (aus dem Stand, die Uhr ist weitergelaufen). Wartet sein Lauf noch, sitzt er am Start; sonst auf die Tribüne.
     */
    @Override
    public void rejoin(Racer r, Player p) {
        Team t = teamOf(p);
        if (t == null || !t.active() || t.heat != heat || phase == HeatPhase.AFTER) {
            toStand(p, r);
            return;
        }
        comp.dress(p, r, true);
        if (phase == HeatPhase.RUN) {
            t.kart.resetTo(t.kart.s());
        }
        t.resetting = false;
        place(t);
    }

    @Override
    public void giveUp(Racer r) {
        Team t = teamOf(r);
        if (t != null && t.active()) {
            t.dnf = true;
            release(t);
        }
    }

    @Override
    public void cleanup() {
        for (Team t : teams) {
            t.resetting = true;
            removeCart(t);
        }
        plugin.boards().live(Discipline.SEIFENKISTE, List.of(), 0, 0);
    }

    @Override
    public Location view() {
        int[] s = Places.STAND_KISTE;
        return new Location(plugin.world(), s[0] + 0.5, Terrain.FLOOR_Y + 4, s[1] + 4.5, 180f, -14f);
    }

    @Override
    public String status(Racer r, String lang) {
        if (heat < 0) {
            return "";
        }
        return Texts.raw(lang, "sidebar.heat").replace("<n>", String.valueOf(heat + 1)).replace("<total>", String.valueOf(heats.size()));
    }

    @Override
    public void reset(Player p) {
        Team t = teamOf(p);
        if (t != null && t.heat == heat) {
            reset(t);
        }
    }

    @Override
    public List<String> debug() {
        List<String> out = new ArrayList<>();
        out.add("stage=seifenkiste heat=" + (heat + 1) + "/" + heats.size() + " phase=" + phase + " done=" + done);
        for (Team t : teams) {
            Location l = t.cart != null && t.cart.isValid() ? t.cart.getLocation() : null;
            Player p = t.racer.player();
            out.add("k=" + t.racer.name() + " heat=" + (t.heat + 1) + " lane=" + (t.lane + 1) + " state="
                    + (t.finished ? "finished" : t.dnf ? "dnf" : heat == t.heat ? phase.name().toLowerCase(Locale.ROOT) : "waiting")
                    + String.format(Locale.ROOT, " s=%.1f v=%.2f", t.kart.s(), t.kart.v()) + " crashes=" + t.kart.crashes()
                    + " steep=" + t.steep + " switched=" + t.switched + " time=" + t.timeMs + " cart=" + (l != null)
                    + " seated=" + (p != null && t.cart != null && t.cart.getPassengers().contains(p))
                    + (l == null ? "" : String.format(Locale.ROOT, " x=%.1f y=%.1f z=%.1f", l.getX(), l.getY(), l.getZ())));
        }
        return out;
    }
}

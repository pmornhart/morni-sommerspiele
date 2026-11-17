package de.moinmorni.sommerspiele.game;

import de.moinmorni.sommerspiele.Texts;
import de.moinmorni.sommerspiele.Ui;
import de.moinmorni.sommerspiele.SommerspielePlugin;
import de.moinmorni.sommerspiele.course.Alm;
import de.moinmorni.sommerspiele.course.Path;
import de.moinmorni.sommerspiele.course.Places;
import de.moinmorni.sommerspiele.course.Terrain;
import de.moinmorni.sommerspiele.logic.Discipline;
import de.moinmorni.sommerspiele.logic.Line;
import de.moinmorni.sommerspiele.logic.RaceClock;
import de.moinmorni.sommerspiele.logic.RunProgress;
import de.moinmorni.sommerspiele.logic.TimeFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Almlauf im Massenstart: alle laufen zusammen los, über die Hindernisse des Rundkurses, die Kontrollpunkte der Reihe
 * nach, über die Ziellinie. Wer zuerst nach der letzten Runde über die Linie kommt, gewinnt. Mit der Feder geht es zum
 * letzten Kontrollpunkt zurück (die Zeit läuft weiter). Auf der Kuhweide laufen echte Kühe herum.
 */
public final class AlmRace implements Stage {

    private static final int COUNT_SECONDS = 3;
    private static final int PREP_TICKS = 40;
    private static final int TICKS = 20;
    private static final double PILOT_STEP = 0.55;
    private static final int COWS = 7;
    /** Nach einer ganzen Runde noch so viele Abtastpunkte weiter (über die Linie hinaus). */
    private static final int PILOT_EXTRA = 40;

    private enum Phase { PREP, COUNT, RUN, OVER }

    /** Eine Läuferin / ein Läufer. */
    final class Runner {
        final Racer racer;
        final int slot;
        final RunProgress progress;
        double px;
        double pz;
        boolean finished;
        boolean dnf;
        long timeMs;
        boolean placed;
        final Deque<double[]> pilot = new ArrayDeque<>();

        Runner(Racer racer, int slot) {
            this.racer = racer;
            this.slot = slot;
            this.progress = new RunProgress(plugin.settings().almLaps(), Alm.checkpoints().size());
        }

        boolean active() {
            return !finished && !dnf;
        }
    }

    private final Competition comp;
    private final SommerspielePlugin plugin;
    private final List<Runner> runners = new ArrayList<>();
    private final List<Cow> cows = new ArrayList<>();
    private Phase phase = Phase.PREP;
    private int phaseTicks;
    private RaceClock clock;
    private long goTick;
    private long nowTick;
    private boolean done;

    AlmRace(Competition comp) {
        this.comp = comp;
        this.plugin = comp.plugin();
        int slot = 0;
        for (Racer r : comp.racers()) {
            if ((r.player() != null || comp.waitingFor(r.uuid())) && !r.left()) {
                runners.add(new Runner(r, slot++));
            }
        }
    }

    @Override
    public Discipline discipline() {
        return Discipline.ALMLAUF;
    }

    @Override
    public void start() {
        spawnCows();
        for (Runner run : runners) {
            Player p = run.racer.player();
            if (p == null) {
                continue;
            }
            comp.dress(p, run.racer, true);
            toGrid(run, p);
            freeze(p, true);
            p.sendMessage(Texts.msg(plugin.lang(p), "alm.intro", Placeholder.unparsed("laps",
                    String.valueOf(plugin.settings().almLaps()))));
        }
    }

    private void toGrid(Runner run, Player p) {
        double[] g = Alm.grid(run.slot);
        p.teleport(new Location(plugin.world(), g[0], Terrain.FLOOR_Y + 1, g[1], 180f, 0f));
        run.px = g[0];
        run.pz = g[1];
        run.placed = true;
    }

    /** Vor dem Startschuss: wer losläuft, steht gleich wieder auf seinem Startplatz (auch ohne Tempo-Sperre im Client). */
    private void hold() {
        for (Runner run : runners) {
            Player p = run.racer.player();
            if (p == null || !run.placed) {
                continue;
            }
            double[] g = Alm.grid(run.slot);
            Location l = p.getLocation();
            if (Math.hypot(l.getX() - g[0], l.getZ() - g[1]) > 0.4) {
                p.teleport(new Location(plugin.world(), g[0], Terrain.FLOOR_Y + 1, g[1], l.getYaw(), l.getPitch()));
            }
        }
    }

    private void freeze(Player p, boolean frozen) {
        var speed = p.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(frozen ? 0 : speed.getDefaultValue());
        }
        var jump = p.getAttribute(Attribute.JUMP_STRENGTH);
        if (jump != null) {
            jump.setBaseValue(frozen ? 0 : jump.getDefaultValue());
        }
    }

    /** Kühe auf die Weide (sie laufen frei herum, bleiben aber auf der Weide). */
    private void spawnCows() {
        removeCows();
        int[] w = Alm.PASTURE;
        for (int i = 0; i < COWS; i++) {
            double x = w[0] + 3 + (w[2] - w[0] - 6) * (i + 0.5) / COWS;
            double z = w[1] + 1 + (i % 3) * 2.5;
            cows.add(plugin.world().spawn(new Location(plugin.world(), x, Terrain.FLOOR_Y + 1, z, i * 50f, 0f), Cow.class, c -> {
                c.setPersistent(false);
                c.setInvulnerable(true);
                c.setRemoveWhenFarAway(false);
                c.addScoreboardTag("sommerspiele_cow");
            }));
        }
    }

    private void removeCows() {
        cows.forEach(c -> c.remove());
        cows.clear();
    }

    /** Ausreißer zurück auf die Weide. */
    private void herd() {
        int[] w = Alm.PASTURE;
        for (Cow c : cows) {
            Location l = c.getLocation();
            if (!c.isValid()) {
                continue;
            }
            if (l.getX() < w[0] + 1 || l.getX() > w[2] || l.getZ() < w[1] || l.getZ() > w[3] + 1) {
                c.teleport(new Location(plugin.world(), (w[0] + w[2]) / 2.0, Terrain.FLOOR_Y + 1, (w[1] + w[3]) / 2.0 + 0.5));
            }
        }
    }

    @Override
    public void tick(long tick) {
        nowTick = tick;
        if (done) {
            return;
        }
        phaseTicks++;
        switch (phase) {
            case PREP -> {
                hold();
                if (phaseTicks >= PREP_TICKS) {
                    phase = Phase.COUNT;
                    phaseTicks = 0;
                }
            }
            case COUNT -> {
                hold();
                if (phaseTicks % TICKS == 1) {
                    int n = COUNT_SECONDS - phaseTicks / TICKS;
                    for (Runner run : runners) {
                        Player p = run.racer.player();
                        if (p != null) {
                            String lang = plugin.lang(p);
                            Ui.title(p, Texts.text(lang, "count.number", Placeholder.unparsed("n", String.valueOf(n))),
                                    Texts.text(lang, "count.sub"), 900);
                            Ui.sound(p, "minecraft:block.note_block.bell", 1f);
                        }
                    }
                }
                if (phaseTicks >= COUNT_SECONDS * TICKS) {
                    go(tick);
                }
            }
            case RUN -> run(tick);
            default -> {
            }
        }
    }

    private void speed(Player p) {
        int amp = plugin.settings().runSpeed() - 1;
        if (amp >= 0) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, amp, false, false, true));
        }
    }

    private void go(long tick) {
        phase = Phase.RUN;
        phaseTicks = 0;
        goTick = tick;
        clock = new RaceClock(tick);
        for (Runner run : runners) {
            Player p = run.racer.player();
            if (p == null) {
                continue;
            }
            freeze(p, false);
            speed(p);
            String lang = plugin.lang(p);
            Ui.title(p, Texts.text(lang, "count.go"), Component.empty(), 700);
            Ui.sound(p, "minecraft:entity.firework_rocket.launch", 1.2f);
        }
    }

    private void run(long tick) {
        long limit = plugin.settings().almLimit() * (long) TICKS;
        if (tick % 20 == 0) {
            herd();
        }
        for (Runner run : runners) {
            if (!run.active()) {
                continue;
            }
            Player p = run.racer.player();
            if (p == null) {
                // Verbindung verloren, Platz noch frei: Stand und Uhr bleiben, das Zeitlimit gilt weiter.
                if (!comp.waitingFor(run.racer.uuid()) || tick - goTick >= limit) {
                    run.dnf = true;
                }
                continue;
            }
            move(run, p, tick);
            if (run.active() && tick - goTick >= limit) {
                run.dnf = true;
                p.sendMessage(Texts.msg(plugin.lang(p), "race.dnf"));
                toStand(run, p);
            }
        }
        if (runners.stream().noneMatch(Runner::active)) {
            phase = Phase.OVER;
            done = true;
        }
    }

    /** Laufen: Kontrollpunkte und Ziellinie prüfen. */
    private void move(Runner run, Player p, long tick) {
        walk(run, p);
        Location l = p.getLocation();
        double qx = l.getX();
        double qz = l.getZ();
        double px = run.px;
        double pz = run.pz;
        run.px = qx;
        run.pz = qz;
        if (l.getY() < Terrain.FLOOR_Y - 3 || !Alm.onTrack(qx, qz) && Alm.loop().distance(qx, qz) > Alm.FENCE + 3) {
            back(run, p);
            return;
        }
        RunProgress pr = run.progress;
        List<Line> cps = Alm.checkpoints();
        for (int i = 0; i < cps.size(); i++) {
            if (cps.get(i).cross(px, pz, qx, qz) != null && pr.checkpoint(i)) {
                Ui.sound(p, "minecraft:block.note_block.hat", 1.5f);
            }
        }
        Line.Hit hit = Alm.lapLine().cross(px, pz, qx, qz);
        if (hit == null) {
            return;
        }
        switch (pr.lapLine()) {
            case LAP -> p.sendMessage(Texts.msg(plugin.lang(p), "alm.lap", Placeholder.unparsed("n", String.valueOf(pr.lap())),
                    Placeholder.unparsed("total", String.valueOf(pr.laps()))));
            case FINISH -> finish(run, p, clock.at(tick, hit.t()));
            default -> {
            }
        }
    }

    /** Zurück an den letzten Kontrollpunkt (oder den Start). */
    private void back(Runner run, Player p) {
        run.pilot.clear();
        Path.Sample s = Alm.loop().at(Alm.checkpointS(run.progress.lastCheckpoint()) + 1);
        double y = Terrain.FLOOR_Y + Alm.standY(s.x(), s.z());
        p.teleport(new Location(plugin.world(), s.x(), y, s.z(), s.yaw(), 0f));
        p.setFallDistance(0);
        run.px = s.x();
        run.pz = s.z();
        p.sendMessage(Texts.msg(plugin.lang(p), "race.reset"));
    }

    private void finish(Runner run, Player p, long ms) {
        run.finished = true;
        run.timeMs = ms;
        String lang = plugin.lang(p);
        String time = TimeFormat.format(ms, lang);
        Ui.title(p, Texts.text(lang, "race.finish.title"), Texts.text(lang, "race.finish.sub", Placeholder.unparsed("time", time)), 2000);
        p.sendMessage(Texts.msg(lang, "race.finish.msg", Placeholder.unparsed("time", time), Placeholder.parsed("extra", "")));
        Ui.sound(p, "minecraft:entity.player.levelup", 1.4f);
        plugin.world().spawnParticle(Particle.FIREWORK, p.getLocation().add(0, 1, 0), 30, 0.6, 0.6, 0.6, 0.05);
        toStand(run, p);
    }

    private void toStand(Runner run, Player p) {
        p.removePotionEffect(PotionEffectType.SPEED);
        freeze(p, false);
        comp.dress(p, run.racer, false);
        p.teleport(view());
    }

    private Runner runnerOf(Player p) {
        for (Runner run : runners) {
            if (run.racer.uuid().equals(p.getUniqueId())) {
                return run;
            }
        }
        return null;
    }

    private Runner runnerOf(Racer r) {
        for (Runner run : runners) {
            if (run.racer == r) {
                return run;
            }
        }
        return null;
    }

    /** Läuft man gerade? */
    public boolean running(Player p) {
        Runner run = runnerOf(p);
        return run != null && run.active();
    }

    @Override
    public void second() {
        if (phase != Phase.RUN) {
            return;
        }
        for (Runner run : runners) {
            if (!run.active()) {
                continue;
            }
            Player p = run.racer.player();
            if (p == null) {
                continue;
            }
            String lang = plugin.lang(p);
            RunProgress pr = run.progress;
            p.sendActionBar(Texts.text(lang, "alm.bar", Placeholder.unparsed("time", TimeFormat.format(clock.now(nowTick), lang)),
                    Placeholder.unparsed("n", String.valueOf(Math.min(pr.laps(), pr.lap() + 1))),
                    Placeholder.unparsed("total", String.valueOf(pr.laps())),
                    Placeholder.unparsed("cp", String.valueOf(pr.nextCheckpoint())),
                    Placeholder.unparsed("cps", String.valueOf(Alm.checkpoints().size()))));
        }
        plugin.boards().live(Discipline.ALMLAUF, results(), 1, 1);
    }

    @Override
    public boolean done() {
        return done;
    }

    @Override
    public List<Result> results() {
        List<Result> out = new ArrayList<>();
        for (Runner run : runners) {
            out.add(new Result(List.of(run.racer), run.finished ? run.timeMs : 0, run.finished, 0, 0, 0));
        }
        return out;
    }

    @Override
    public void quit(Racer r) {
        Runner run = runnerOf(r);
        if (run != null && run.active()) {
            run.dnf = true;
            Player p = r.player();
            if (p != null) {
                p.removePotionEffect(PotionEffectType.SPEED);
                freeze(p, false);
            }
        }
    }

    /** Verbindung weg: der Stand (Runde, Kontrollpunkte) bleibt. */
    @Override
    public void away(Racer r) {
        Runner run = runnerOf(r);
        if (run != null) {
            run.pilot.clear();
        }
    }

    /** Rechtzeitig zurück: vor dem Start in die Startaufstellung, im Rennen genau dort weiter, wo man war. */
    @Override
    public void rejoin(Racer r, Player p) {
        Runner run = runnerOf(r);
        if (run == null || !run.active() || phase == Phase.OVER) {
            comp.dress(p, r, false);
            p.teleport(view());
            return;
        }
        comp.dress(p, r, true);
        if (phase != Phase.RUN || !run.placed) {
            toGrid(run, p);
            if (phase != Phase.RUN) {
                freeze(p, true);
                return;
            }
        }
        freeze(p, false);
        speed(p);
        Location l = p.getLocation();
        run.px = l.getX();
        run.pz = l.getZ();
    }

    @Override
    public void giveUp(Racer r) {
        Runner run = runnerOf(r);
        if (run != null && run.active()) {
            run.dnf = true;
            Player p = r.player();
            if (p != null) {
                toStand(run, p);
            }
        }
    }

    @Override
    public void cleanup() {
        removeCows();
        for (Runner run : runners) {
            Player p = run.racer.player();
            if (p != null) {
                p.removePotionEffect(PotionEffectType.SPEED);
                freeze(p, false);
            }
        }
        plugin.boards().live(Discipline.ALMLAUF, List.of(), 0, 0);
    }

    @Override
    public Location view() {
        int[] s = Places.STAND_ALM;
        return new Location(plugin.world(), s[0] + 0.5, Terrain.FLOOR_Y + 4, s[1] + 0.5, -90f, 6f);
    }

    @Override
    public String status(Racer r, String lang) {
        Runner run = r == null ? null : runnerOf(r);
        if (run == null || !run.active()) {
            return "";
        }
        return Texts.raw(lang, "sidebar.time").replace("<time>", TimeFormat.format(clock == null ? 0 : clock.now(nowTick), lang));
    }

    @Override
    public void reset(Player p) {
        Runner run = runnerOf(p);
        if (run != null && run.active() && phase == Phase.RUN) {
            back(run, p);
        }
    }

    /** Zum Testen: Wegpunkte einer ganzen Runde ab der jetzigen Stelle. */
    public List<double[]> lapPoints(Player p) {
        List<double[]> out = new ArrayList<>();
        Runner run = runnerOf(p);
        if (run == null) {
            return out;
        }
        Path path = Alm.loop();
        Location l = p.getLocation();
        int from = path.nearest(l.getX(), l.getZ(), -1);
        int n = path.samples().size();
        for (int k = 0; k <= n + PILOT_EXTRA; k += 2) {
            Path.Sample s = path.samples().get((from + k) % n);
            out.add(new double[] {s.x(), s.z()});
        }
        return out;
    }

    /** Zum Testen: Wegpunkte setzen (je Tick höchstens {@link #PILOT_STEP} Blöcke). */
    public boolean walk(Player p, List<double[]> points) {
        Runner run = runnerOf(p);
        if (run == null || !run.active() || phase != Phase.RUN) {
            return false;
        }
        run.pilot.clear();
        run.pilot.addAll(points);
        return true;
    }

    private void walk(Runner run, Player p) {
        if (run.pilot.isEmpty()) {
            return;
        }
        Location l = p.getLocation();
        double budget = PILOT_STEP;
        double x = l.getX();
        double z = l.getZ();
        while (budget > 0 && !run.pilot.isEmpty()) {
            double[] w = run.pilot.peekFirst();
            double d = Math.hypot(w[0] - x, w[1] - z);
            if (d <= budget) {
                x = w[0];
                z = w[1];
                budget -= d;
                run.pilot.pollFirst();
            } else {
                x += (w[0] - x) / d * budget;
                z += (w[1] - z) / d * budget;
                budget = 0;
            }
        }
        double y = Terrain.FLOOR_Y + Alm.standY(x, z);
        p.teleport(new Location(l.getWorld(), x, y, z, Path.yaw(x - l.getX(), z - l.getZ()), 0f));
    }

    @Override
    public List<String> debug() {
        List<String> out = new ArrayList<>();
        out.add("stage=almlauf phase=" + phase + " done=" + done + " cows=" + cows.stream().filter(c -> c.isValid()).count());
        for (Runner run : runners) {
            RunProgress pr = run.progress;
            Location l = run.racer.player() != null ? run.racer.player().getLocation() : null;
            out.add("l=" + run.racer.name() + " state=" + (run.finished ? "finished" : run.dnf ? "dnf" : phase.name().toLowerCase(Locale.ROOT))
                    + " lap=" + pr.lap() + " cp=" + pr.nextCheckpoint() + " time=" + run.timeMs
                    + (l == null ? "" : String.format(Locale.ROOT, " x=%.1f y=%.1f z=%.1f", l.getX(), l.getY(), l.getZ())));
        }
        return out;
    }
}

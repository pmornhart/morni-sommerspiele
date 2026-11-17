package de.moinmorni.sommerspiele.game;

import de.moinmorni.sommerspiele.Items;
import de.moinmorni.sommerspiele.Texts;
import de.moinmorni.sommerspiele.Ui;
import de.moinmorni.sommerspiele.SommerspielePlugin;
import de.moinmorni.sommerspiele.course.Places;
import de.moinmorni.sommerspiele.course.Range;
import de.moinmorni.sommerspiele.course.Terrain;
import de.moinmorni.sommerspiele.logic.Archery;
import de.moinmorni.sommerspiele.logic.Discipline;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Bogenschießen: alle gleichzeitig, jede Person an ihrer eigenen Bahn. Drei Durchgänge (Scheibe in 20, 35 und 50
 * Blöcken), je {@code bogen.pfeile} Pfeile in {@code bogen.zeit-sekunden}. Die Pfeile fliegen wie in Minecraft (sie
 * fallen), dazu weht je Durchgang ein Wind, der sie zur Seite treibt. Gewertet werden die Ringe (innen 10, außen 1),
 * bei Gleichstand zählen die Treffer genau in die Mitte.
 */
public final class ArcheryStage implements Stage {

    private enum Phase { SETUP, SHOOT, BETWEEN, OVER }

    private static final int SETUP_TICKS = 60;
    private static final int BETWEEN_TICKS = 70;
    private static final int TICKS = 20;
    private static final int ARROW_LIFE = 100;
    private static final int AFTER_LAST_SHOT = 25;
    private static final double MAT_LEASH = 0.8;
    private static final int SLOT_BOW = 0;
    private static final int SLOT_ARROWS = 1;
    private static final String[] RINGS = {"yellow_concrete", "red_concrete", "light_blue_concrete"};

    /** Eine Schützin / ein Schütze. */
    final class Shooter {
        final Racer racer;
        final int lane;
        final int[] roundRings = new int[Range.DISTANCES.length];
        int rings;
        int centres;
        int fired;
        int landed;
        long lastShot = -1;
        boolean dnf;
        boolean placed;

        Shooter(Racer racer, int lane) {
            this.racer = racer;
            this.lane = lane;
        }

        boolean active() {
            return !dnf;
        }

        boolean doneShooting() {
            return fired >= plugin.settings().arrows() && landed >= fired;
        }
    }

    private record Shot(Shooter shooter, long tick) {
    }

    private final Competition comp;
    private final SommerspielePlugin plugin;
    private final List<Shooter> shooters = new ArrayList<>();
    private final Map<UUID, Shot> arrows = new HashMap<>();
    private final List<UUID> stuck = new ArrayList<>();
    private final Random random = new Random();
    private Phase phase = Phase.SETUP;
    private int round = -1;
    private int phaseTicks;
    private long roundEnd;
    private long nowTick;
    private int wind;
    private boolean done;

    ArcheryStage(Competition comp) {
        this.comp = comp;
        this.plugin = comp.plugin();
        int lane = 0;
        for (Racer r : comp.racers()) {
            if ((r.player() != null || comp.waitingFor(r.uuid())) && !r.left() && lane < Range.LANES) {
                shooters.add(new Shooter(r, lane++));
            }
        }
    }

    @Override
    public Discipline discipline() {
        return Discipline.BOGEN;
    }

    @Override
    public void start() {
        for (Shooter s : shooters) {
            Player p = s.racer.player();
            if (p != null) {
                p.sendMessage(Texts.msg(plugin.lang(p), "bogen.intro", Placeholder.unparsed("n",
                        String.valueOf(plugin.settings().arrows()))));
            }
        }
        nextRound();
    }

    /** Wind des Durchgangs: zufällig bis zur Stärke aus der config.yml, Richtung West oder Ost. */
    public int wind() {
        return wind;
    }

    /** Zum Testen: Wind festlegen. */
    public void wind(int w) {
        wind = Math.max(-Archery.MAX_WIND, Math.min(Archery.MAX_WIND, w));
    }

    public int round() {
        return round;
    }

    private void nextRound() {
        round++;
        if (round >= Range.DISTANCES.length) {
            phase = Phase.OVER;
            done = true;
            return;
        }
        phase = Phase.SETUP;
        phaseTicks = 0;
        int max = plugin.settings().wind();
        wind = max == 0 ? 0 : (random.nextBoolean() ? 1 : -1) * random.nextInt(max + 1);
        targets(round);
        for (Shooter s : shooters) {
            s.fired = 0;
            s.landed = 0;
            s.lastShot = -1;
            Player p = s.racer.player();
            if (p == null || !s.active()) {
                continue;
            }
            comp.dress(p, s.racer, false);
            p.teleport(mat(s));
            s.placed = true;
            String lang = plugin.lang(p);
            Ui.title(p, Texts.text(lang, "bogen.round.title", Placeholder.unparsed("n", String.valueOf(round + 1)),
                    Placeholder.unparsed("total", String.valueOf(Range.DISTANCES.length))),
                    Texts.text(lang, "bogen.round.sub", Placeholder.unparsed("d", String.valueOf(Range.DISTANCES[round])),
                            Placeholder.parsed("wind", windText(lang))), 2500);
            Ui.sound(p, "minecraft:block.bell.use", 1.4f);
        }
    }

    private String windText(String lang) {
        if (wind == 0) {
            return Texts.raw(lang, "bogen.wind.none");
        }
        String arrowsDir = (wind > 0 ? "→" : "←").repeat(Math.abs(wind));
        return Texts.raw(lang, wind > 0 ? "bogen.wind.east" : "bogen.wind.west").replace("<arrows>", arrowsDir)
                .replace("<n>", String.valueOf(Math.abs(wind)));
    }

    /** Scheiben für den Durchgang aufstellen (die anderen Entfernungen räumen). */
    private void targets(int r) {
        for (int i = 0; i < Range.DISTANCES.length; i++) {
            for (int lane = 0; lane < Range.LANES; lane++) {
                face(lane, Range.targetZ(i), i == r);
            }
        }
    }

    private void face(int lane, int z, boolean show) {
        int cx = Range.laneX(lane);
        int y0 = Terrain.FLOOR_Y + 1;
        for (int dx = -Range.FACE_HALF; dx <= Range.FACE_HALF; dx++) {
            for (int dy = 0; dy < 2 * Range.FACE_HALF + 1; dy++) {
                int ring = Math.max(Math.abs(dx), Math.abs(dy - Range.FACE_HALF));
                Block front = plugin.world().getBlockAt(cx + dx, y0 + dy, z);
                Block back = plugin.world().getBlockAt(cx + dx, y0 + dy, z + 1);
                front.setType(show ? Material.matchMaterial(RINGS[Math.min(ring, RINGS.length - 1)]) : Material.AIR, false);
                back.setType(show ? Material.HAY_BLOCK : Material.AIR, false);
            }
        }
    }

    private Location mat(Shooter s) {
        double[] m = Range.mat(s.lane);
        return new Location(plugin.world(), m[0], Terrain.FLOOR_Y + 1.07, m[1], 0f, 0f);
    }

    private void giveBow(Shooter s, Player p) {
        String lang = plugin.lang(p);
        ItemStack bow = Items.kitTag(Items.named(Material.BOW, 1, Texts.text(lang, "item.bow"), Texts.lines(lang, "item.bow.lore")), "bow");
        bow.editMeta(m -> m.setUnbreakable(true));
        p.getInventory().setItem(SLOT_BOW, bow);
        int left = plugin.settings().arrows() - s.fired;
        p.getInventory().setItem(SLOT_ARROWS, left > 0 ? new ItemStack(Material.ARROW, left) : null);
        p.getInventory().setHeldItemSlot(SLOT_BOW);
    }

    private void takeBow(Player p) {
        p.getInventory().setItem(SLOT_BOW, null);
        p.getInventory().remove(Material.ARROW);
    }

    @Override
    public void tick(long tick) {
        nowTick = tick;
        if (done) {
            return;
        }
        phaseTicks++;
        switch (phase) {
            case SETUP -> {
                hold();
                if (phaseTicks >= SETUP_TICKS) {
                    phase = Phase.SHOOT;
                    phaseTicks = 0;
                    roundEnd = tick + plugin.settings().bowSeconds() * (long) TICKS;
                    for (Shooter s : shooters) {
                        Player p = s.racer.player();
                        if (p != null && s.active()) {
                            giveBow(s, p);
                            Ui.title(p, Texts.text(plugin.lang(p), "bogen.go"), net.kyori.adventure.text.Component.empty(), 800);
                            Ui.sound(p, "minecraft:block.note_block.pling", 1.4f);
                        }
                    }
                }
            }
            case SHOOT -> {
                hold();
                pushArrows(tick);
                expireArrows(tick);
                boolean all = shooters.stream().filter(Shooter::active)
                        .allMatch(s -> s.doneShooting() && (s.lastShot < 0 || tick - s.lastShot >= AFTER_LAST_SHOT)
                                || s.racer.player() == null && !comp.waitingFor(s.racer.uuid()));
                if (all || tick >= roundEnd) {
                    endRound();
                }
            }
            case BETWEEN -> {
                if (phaseTicks >= BETWEEN_TICKS) {
                    nextRound();
                }
            }
            default -> {
            }
        }
    }

    /** Auf der Matte bleiben (umschauen geht). */
    private void hold() {
        for (Shooter s : shooters) {
            Player p = s.racer.player();
            if (p == null || !s.active()) {
                continue;
            }
            Location m = mat(s);
            Location l = p.getLocation();
            if (l.getWorld() != m.getWorld() || Math.hypot(l.getX() - m.getX(), l.getZ() - m.getZ()) > MAT_LEASH) {
                m.setYaw(l.getYaw());
                m.setPitch(l.getPitch());
                p.teleport(m);
            }
        }
    }

    /** Wind schiebt die Pfeile in der Luft zur Seite. */
    private void pushArrows(long tick) {
        if (wind == 0) {
            return;
        }
        for (UUID id : arrows.keySet()) {
            Entity e = plugin.getServer().getEntity(id);
            if (e instanceof AbstractArrow a && !a.isInBlock()) {
                a.setVelocity(a.getVelocity().add(new Vector(wind * Archery.WIND_PUSH, 0, 0)));
            }
        }
        if (tick % 10 == 0) {
            for (int lane = 0; lane < Range.LANES; lane += 2) {
                double x = Range.laneX(lane) + 0.5 - wind * 2;
                plugin.world().spawnParticle(Particle.CLOUD, x, Terrain.FLOOR_Y + 6, Range.MAT_Z + 6, 0,
                        wind * 0.15, 0, 0, 1);
            }
        }
    }

    private void endRound() {
        for (UUID id : arrows.keySet()) {
            Shot s = arrows.get(id);
            s.shooter.landed++;
        }
        arrows.clear();
        for (Shooter s : shooters) {
            Player p = s.racer.player();
            if (p == null || !s.active()) {
                continue;
            }
            takeBow(p);
            String lang = plugin.lang(p);
            p.sendMessage(Texts.msg(lang, "bogen.round.result", Placeholder.unparsed("n", String.valueOf(round + 1)),
                    Placeholder.unparsed("rings", String.valueOf(s.roundRings[round])),
                    Placeholder.unparsed("total", String.valueOf(s.rings))));
        }
        board();
        phase = Phase.BETWEEN;
        phaseTicks = 0;
        if (round + 1 >= Range.DISTANCES.length) {
            phaseTicks = BETWEEN_TICKS - 30;
        }
    }

    private void board() {
        plugin.boards().live(Discipline.BOGEN, results(), round + 1, Range.DISTANCES.length);
    }

    // ---------- Pfeile ----------

    /** Ein Pfeil wurde abgeschossen. @return true, wenn er zu einer Bahn gehört */
    public boolean shot(Player p, AbstractArrow arrow) {
        Shooter s = shooterOf(p);
        if (s == null || !s.active() || phase != Phase.SHOOT || s.fired >= plugin.settings().arrows()) {
            return false;
        }
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.setPersistent(false);
        arrow.addScoreboardTag("sommerspiele_arrow");
        if (arrow instanceof Arrow a) {
            a.setCritical(false);
        }
        arrows.put(arrow.getUniqueId(), new Shot(s, nowTick));
        s.fired++;
        s.lastShot = nowTick;
        return true;
    }

    public boolean isOurs(AbstractArrow arrow) {
        return arrows.containsKey(arrow.getUniqueId());
    }

    /** Ein Pfeil hat etwas getroffen. */
    public void hit(AbstractArrow arrow, Block block) {
        Shot shot = arrows.remove(arrow.getUniqueId());
        if (shot == null) {
            return;
        }
        Shooter s = shot.shooter;
        s.landed++;
        stuck.add(arrow.getUniqueId());
        int targetZ = Range.targetZ(round);
        int score = 0;
        double r = -1;
        boolean own = block != null && block.getZ() == targetZ && Math.abs(block.getX() - Range.laneX(s.lane)) <= Range.FACE_HALF
                && block.getY() >= Terrain.FLOOR_Y + 1 && block.getY() <= Terrain.FLOOR_Y + 1 + 2 * Range.FACE_HALF;
        if (own) {
            Location l = arrow.getLocation();
            Vector v = arrow.getVelocity();
            // Zurück auf die Vorderseite der Scheibe rechnen (der Pfeil steckt schon ein Stück drin).
            double back = v.getZ() > 1e-6 ? (l.getZ() - targetZ) / v.getZ() : 0;
            double hx = l.getX() - v.getX() * Math.max(0, back);
            double hy = l.getY() - v.getY() * Math.max(0, back);
            double[] c = Range.center(s.lane);
            r = Math.hypot(hx - c[0], hy - (Terrain.FLOOR_Y + c[1]));
            score = Archery.score(r);
        }
        s.rings += score;
        s.roundRings[round] += score;
        if (Archery.centre(r)) {
            s.centres++;
        }
        Player p = s.racer.player();
        if (p != null) {
            String lang = plugin.lang(p);
            String key = score == 10 && Archery.centre(r) ? "bogen.hit.centre" : score > 0 ? "bogen.hit" : "bogen.miss";
            p.sendActionBar(Texts.text(lang, key, Placeholder.unparsed("n", String.valueOf(score))));
            Ui.sound(p, score >= 9 ? "minecraft:block.note_block.bell" : score > 0 ? "minecraft:block.note_block.chime"
                    : "minecraft:block.note_block.didgeridoo", score > 0 ? 1.0f + score * 0.08f : 0.8f);
        }
        if (score > 0) {
            plugin.world().spawnParticle(score >= 9 ? Particle.HAPPY_VILLAGER : Particle.CRIT, arrow.getLocation(), 10,
                    0.15, 0.15, 0.05, 0.02);
        }
        board();
    }

    private void expireArrows(long tick) {
        List<UUID> old = new ArrayList<>();
        arrows.forEach((id, s) -> {
            if (tick - s.tick >= ARROW_LIFE) {
                old.add(id);
            }
        });
        for (UUID id : old) {
            Shot s = arrows.remove(id);
            Entity e = plugin.getServer().getEntity(id);
            if (e != null) {
                e.remove();
            }
            s.shooter.landed++;
        }
    }

    /**
     * Zum Testen: einen Pfeil genau auf die Mitte schießen (Schwerkraft und Wind mitgerechnet), um {@code side} Blöcke
     * daneben (+ = Osten).
     */
    public boolean autoShot(Player p, double side) {
        Shooter s = shooterOf(p);
        if (s == null || phase != Phase.SHOOT || s.fired >= plugin.settings().arrows()) {
            return false;
        }
        Location eye = p.getEyeLocation();
        double[] c = Range.center(s.lane);
        double dist = Range.targetZ(round) - eye.getZ();
        double rise = Terrain.FLOOR_Y + c[1] - eye.getY();
        double[] aim = Archery.aim(dist, rise, Archery.ARROW_SPEED, wind);
        double yaw = Math.toRadians(aim[1]) + Math.atan2(c[0] + side - eye.getX(), dist);
        double pitch = Math.toRadians(aim[0]);
        Vector v = new Vector(Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch))
                .multiply(Archery.ARROW_SPEED);
        Arrow arrow = p.launchProjectile(Arrow.class, v);
        arrow.setVelocity(v);
        p.getInventory().removeItem(new ItemStack(Material.ARROW, 1));
        return shot(p, arrow);
    }

    private Shooter shooterOf(Player p) {
        for (Shooter s : shooters) {
            if (s.racer.uuid().equals(p.getUniqueId())) {
                return s;
            }
        }
        return null;
    }

    private Shooter shooterOf(Racer r) {
        for (Shooter s : shooters) {
            if (s.racer == r) {
                return s;
            }
        }
        return null;
    }

    @Override
    public void second() {
        if (phase != Phase.SHOOT && phase != Phase.SETUP) {
            return;
        }
        for (Shooter s : shooters) {
            Player p = s.racer.player();
            if (p == null || !s.active()) {
                continue;
            }
            String lang = plugin.lang(p);
            long secs = phase == Phase.SHOOT ? Math.max(0, (roundEnd - nowTick) / TICKS) : plugin.settings().bowSeconds();
            p.sendActionBar(Texts.text(lang, "bogen.bar", Placeholder.unparsed("n", String.valueOf(round + 1)),
                    Placeholder.unparsed("total", String.valueOf(Range.DISTANCES.length)),
                    Placeholder.unparsed("arrow", String.valueOf(Math.min(plugin.settings().arrows(), s.fired + 1))),
                    Placeholder.unparsed("arrows", String.valueOf(plugin.settings().arrows())),
                    Placeholder.unparsed("rings", String.valueOf(s.rings)), Placeholder.parsed("wind", windText(lang)),
                    Placeholder.unparsed("secs", String.valueOf(secs))));
        }
    }

    @Override
    public boolean done() {
        return done;
    }

    @Override
    public List<Result> results() {
        List<Result> out = new ArrayList<>();
        for (Shooter s : shooters) {
            out.add(new Result(List.of(s.racer), 0, !s.dnf, 0, s.rings, s.centres));
        }
        return out;
    }

    @Override
    public void quit(Racer r) {
        Shooter s = shooterOf(r);
        if (s != null) {
            s.dnf = true;
            arrows.values().removeIf(x -> x.shooter == s);
        }
    }

    /** Verbindung weg: Ringe bleiben; Pfeile in der Luft zählen nicht mehr. */
    @Override
    public void away(Racer r) {
        Shooter s = shooterOf(r);
        if (s != null) {
            arrows.entrySet().removeIf(e -> {
                if (e.getValue().shooter == s) {
                    s.landed++;
                    return true;
                }
                return false;
            });
        }
    }

    /** Rechtzeitig zurück: auf die eigene Matte, im laufenden Durchgang mit den übrigen Pfeilen. */
    @Override
    public void rejoin(Racer r, Player p) {
        Shooter s = shooterOf(r);
        comp.dress(p, r, false);
        if (s == null || !s.active() || phase == Phase.OVER) {
            p.teleport(view());
            return;
        }
        p.teleport(mat(s));
        s.placed = true;
        if (phase == Phase.SHOOT) {
            giveBow(s, p);
        }
    }

    @Override
    public void giveUp(Racer r) {
        Shooter s = shooterOf(r);
        if (s != null && s.active()) {
            s.dnf = true;
            Player p = r.player();
            if (p != null) {
                takeBow(p);
                comp.dress(p, r, false);
                p.teleport(view());
            }
        }
    }

    @Override
    public void cleanup() {
        for (UUID id : arrows.keySet()) {
            Entity e = plugin.getServer().getEntity(id);
            if (e != null) {
                e.remove();
            }
        }
        for (UUID id : stuck) {
            Entity e = plugin.getServer().getEntity(id);
            if (e != null) {
                e.remove();
            }
        }
        arrows.clear();
        stuck.clear();
        for (Shooter s : shooters) {
            Player p = s.racer.player();
            if (p != null) {
                takeBow(p);
            }
        }
        targets(0);
        plugin.boards().live(Discipline.BOGEN, List.of(), 0, 0);
    }

    @Override
    public Location view() {
        int[] s = Places.STAND_BOGEN;
        return new Location(plugin.world(), s[0] + 0.5, Terrain.FLOOR_Y + 4, s[1] + 0.5, 0f, 8f);
    }

    @Override
    public String status(Racer r, String lang) {
        Shooter s = r == null ? null : shooterOf(r);
        String base = Texts.raw(lang, "sidebar.round").replace("<n>", String.valueOf(Math.max(1, round + 1)))
                .replace("<total>", String.valueOf(Range.DISTANCES.length));
        return s == null ? base : base + "  " + Texts.raw(lang, "sidebar.rings").replace("<n>", String.valueOf(s.rings));
    }

    @Override
    public void reset(Player p) {
        Shooter s = shooterOf(p);
        if (s != null && s.active()) {
            p.teleport(mat(s));
        }
    }

    @Override
    public List<String> debug() {
        List<String> out = new ArrayList<>();
        out.add("stage=bogen round=" + (round + 1) + " phase=" + phase.name().toLowerCase(Locale.ROOT) + " wind=" + wind
                + " done=" + done);
        for (Shooter s : shooters) {
            out.add("a=" + s.racer.name() + " lane=" + (s.lane + 1) + " state=" + (s.dnf ? "dnf" : phase.name().toLowerCase(Locale.ROOT))
                    + " rings=" + s.rings + " centres=" + s.centres + " fired=" + s.fired + " landed=" + s.landed
                    + " round=" + s.roundRings[Math.max(0, Math.min(round, s.roundRings.length - 1))]);
        }
        return out;
    }
}

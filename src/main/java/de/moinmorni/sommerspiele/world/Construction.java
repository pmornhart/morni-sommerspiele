package de.moinmorni.sommerspiele.world;

import de.moinmorni.sommerspiele.course.Places;
import de.moinmorni.sommerspiele.course.Terrain;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Baut das Sommerdorf mit allen Strecken aus den Bauplänen – nur wenn sich die Bau-Version geändert hat (gemerkt in
 * {@code bau.yml}). Erst sofort das Dorf (damit Ankommende Boden unter den Füßen haben), dann Stück für Stück Boden,
 * Wald, Seifenkisten-Bahn, See, Schießplatz, Almlauf (höchstens {@link #BUDGET_NANOS} pro Tick). Bei einem Neubau wird
 * vorher alles abgeräumt.
 */
public final class Construction {

    /** Bau-Version: erhöhen, wenn sich Dorf oder Strecken ändern – dann wird beim nächsten Start neu gebaut. */
    public static final int VERSION = 1;
    private static final long BUDGET_NANOS = 25_000_000L;
    private static final int CHECK_EVERY = 256;
    /** Abgeräumt wird von hier bis so weit über dem Tal. */
    private static final int CLEAR_BELOW = 12;
    private static final int CLEAR_ABOVE = 110;

    private interface Job {
        boolean step(long deadline);
    }

    private final Plugin plugin;
    private final Logger log;
    private final World world;
    private final Path stateFile;
    private final Deque<Job> jobs = new ArrayDeque<>();
    private BukkitTask task;
    private long placed;

    public Construction(Plugin plugin, World world) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.world = world;
        this.stateFile = plugin.getDataFolder().toPath().resolve("bau.yml");
    }

    public boolean running() {
        return task != null;
    }

    /** Ist schon einmal gebaut worden (irgendeine Version)? */
    public boolean builtBefore() {
        return readState().contains("version");
    }

    /** Baut, falls nötig; {@code done} läuft auf dem Hauptthread, sobald alles steht. */
    public void ensure(boolean force, Runnable done) {
        YamlConfiguration state = readState();
        if (!force && state.getInt("version") == VERSION) {
            done.run();
            return;
        }
        log.info("Das Sommerdorf mit allen Strecken wird gebaut (Bau-Version " + VERSION + ") …");
        long start = System.currentTimeMillis();
        boolean clear = state.contains("version");
        // Erst das Dorf mit seinem Boden – sofort, damit niemand ins Leere fällt.
        jobs.add(placeJob(() -> Ground.area(-60, -40, 60, 60)));
        jobs.add(placeJob(VillagePlan::build));
        runNow();
        if (clear) {
            jobs.add(clearJob());
        }
        for (Supplier<Plan> s : Ground.strips()) {
            jobs.add(placeJob(s));
        }
        for (Supplier<Plan> s : Forest.strips()) {
            jobs.add(placeJob(s));
        }
        List<Supplier<Plan>> parts = new ArrayList<>(List.of(KistePlan::build, LakePlan::build, RangePlan::build,
                AlmPlan::build, VillagePlan::build));
        parts.forEach(s -> jobs.add(placeJob(s)));
        jobs.add(deadline -> {
            YamlConfiguration fresh = new YamlConfiguration();
            fresh.set("version", VERSION);
            writeState(fresh);
            log.info("Fertig gebaut: " + placed + " Blöcke in " + (System.currentTimeMillis() - start) / 1000 + " s.");
            done.run();
            return true;
        });
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void runNow() {
        while (!jobs.isEmpty()) {
            Job job = jobs.peekFirst();
            if (job.step(Long.MAX_VALUE)) {
                jobs.pollFirst();
            }
        }
    }

    private void tick() {
        long deadline = System.nanoTime() + BUDGET_NANOS;
        while (!jobs.isEmpty() && System.nanoTime() < deadline) {
            Job job = jobs.peekFirst();
            boolean finished;
            try {
                finished = job.step(deadline);
            } catch (RuntimeException e) {
                log.log(Level.WARNING, "Bauschritt fehlgeschlagen – wird übersprungen", e);
                finished = true;
            }
            if (finished) {
                jobs.pollFirst();
            }
        }
        if (jobs.isEmpty() && task != null) {
            task.cancel();
            task = null;
        }
    }

    /** Bauplan erst erzeugen, wenn er dran ist (spart Speicher), dann Block für Block setzen. */
    private Job placeJob(Supplier<Plan> source) {
        List<Long> keys = new ArrayList<>();
        List<BlockData> data = new ArrayList<>();
        List<Plan.Banner> banners = new ArrayList<>();
        int[] index = {-1};
        return deadline -> {
            if (index[0] < 0) {
                Plan plan = source.get();
                plan.connect();
                plan.forEach((k, d) -> {
                    keys.add(k);
                    data.add(d);
                });
                banners.addAll(plan.banners());
                index[0] = 0;
                if (System.nanoTime() > deadline) {
                    return false;
                }
            }
            int i = index[0];
            int n = 0;
            int oy = Terrain.FLOOR_Y;
            while (i < keys.size()) {
                long k = keys.get(i);
                world.getBlockAt(Plan.keyX(k), oy + Plan.keyY(k), Plan.keyZ(k)).setBlockData(data.get(i), false);
                i++;
                placed++;
                if (++n % CHECK_EVERY == 0 && System.nanoTime() > deadline) {
                    index[0] = i;
                    return false;
                }
            }
            index[0] = i;
            for (Plan.Banner b : banners) {
                Block block = world.getBlockAt(b.x(), oy + b.y(), b.z());
                BlockState st = block.getState();
                if (st instanceof org.bukkit.block.Banner banner) {
                    banner.setPatterns(b.patterns());
                    banner.update(true, false);
                }
            }
            keys.clear();
            data.clear();
            return true;
        };
    }

    /** Alles über dem alten Bau abräumen (außer Spielern), damit kein Rest eines früheren Baus stehen bleibt. */
    private Job clearJob() {
        int[] cursor = {Places.MIN_X, Terrain.FLOOR_Y - CLEAR_BELOW, Places.MIN_Z};
        boolean[] entitiesGone = {false};
        int y1 = Terrain.FLOOR_Y - CLEAR_BELOW;
        int y2 = Terrain.FLOOR_Y + CLEAR_ABOVE;
        return deadline -> {
            if (!entitiesGone[0]) {
                for (Entity e : world.getEntities()) {
                    if (!(e instanceof Player)) {
                        e.remove();
                    }
                }
                entitiesGone[0] = true;
            }
            int n = 0;
            while (cursor[0] <= Places.MAX_X) {
                Block block = world.getBlockAt(cursor[0], cursor[1], cursor[2]);
                if (block.getType() != Material.AIR) {
                    block.setType(Material.AIR, false);
                }
                if (++cursor[1] > y2) {
                    cursor[1] = y1;
                    if (++cursor[2] > Places.MAX_Z) {
                        cursor[2] = Places.MIN_Z;
                        cursor[0]++;
                    }
                }
                if (++n % CHECK_EVERY == 0 && System.nanoTime() > deadline) {
                    return false;
                }
            }
            return true;
        };
    }

    private YamlConfiguration readState() {
        YamlConfiguration y = new YamlConfiguration();
        if (Files.isRegularFile(stateFile)) {
            try {
                y.loadFromString(Files.readString(stateFile, StandardCharsets.UTF_8));
            } catch (IOException | InvalidConfigurationException e) {
                log.warning("bau.yml ließ sich nicht lesen – es wird neu gebaut: " + e.getMessage());
            }
        }
        return y;
    }

    private void writeState(YamlConfiguration y) {
        try {
            Files.createDirectories(stateFile.getParent());
            Files.writeString(stateFile, y.saveToString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.log(Level.WARNING, "bau.yml ließ sich nicht schreiben", e);
        }
    }
}

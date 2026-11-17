package de.moinmorni.sommerspiele;

import de.moinmorni.sommerspiele.course.Places;
import de.moinmorni.sommerspiele.course.Terrain;
import de.moinmorni.sommerspiele.data.Leaderboard;
import de.moinmorni.sommerspiele.data.PlayerData;
import de.moinmorni.sommerspiele.data.Store;
import de.moinmorni.sommerspiele.game.Games;
import de.moinmorni.sommerspiele.listen.GuardListener;
import de.moinmorni.sommerspiele.listen.PlayerListener;
import de.moinmorni.sommerspiele.listen.RaceListener;
import de.moinmorni.sommerspiele.menu.MenuListener;
import de.moinmorni.sommerspiele.net.Network;
import de.moinmorni.sommerspiele.village.Village;
import de.moinmorni.sommerspiele.world.Construction;
import de.moinmorni.sommerspiele.world.VoidGenerator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Difficulty;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Morni Sommerspiele: Sommerspiele im Dorf am Bergsee – Almlauf über Heuballen, Bach und Kuhweide, Bogenschießen mit
 * Wind, Ruderregatta durch Bojen-Tore, Seifenkistenrennen die Schienen hinab. Medaillen, Medaillenspiegel,
 * Gesamtwertung. Das Gegenstück zu Wintersport: offen, wenn dort Pause ist. Spricht mit dem MoinMorni-Netzwerk, wenn es
 * da ist, und läuft sonst allein.
 */
public final class SommerspielePlugin extends JavaPlugin {

    private static final String ADMIN = "moinmorni.sommerspiele.admin";
    private static final long SECOND = 20L;
    private static final long LANGUAGE_CHECK = 40L;
    private static final long STATE_REFRESH = 20L * 60;
    private static final long BOARD_REFRESH = 20L * 30;
    private static final long DAY = 6000L;
    private static final long EVENING = 12600L;
    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static SommerspielePlugin instance;

    private Settings settings;
    private Network net;
    private Store store;
    private Leaderboard board;
    private World world;
    private Construction construction;
    private Village village;
    private Games games;
    private Boards boards;
    private Pollen pollen;
    private volatile boolean ready;
    private long tick;
    private LocalDate testDate;
    private boolean stateQueued;
    private String lastState = "";
    private int lastPlayers = -1;
    private final Map<UUID, String> lastLang = new HashMap<>();

    public static SommerspielePlugin instance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        settings = Settings.load(getConfig());
        Items.init(this);
        net = Network.connect(this);
        getLogger().info(net.connected()
                ? "Mit dem MoinMorni-Netzwerk verbunden (Server „" + net.serverName() + "“)."
                : "Kein Netzwerk-Plugin: Sommerspiele laufen allein (deutsche Texte, Statistik nur lokal).");
        store = new Store(getLogger(), getDataFolder().toPath());
        board = loadBoard();
        world = createWorld(settings.world());
        boards = new Boards(this);
        village = new Village(this);
        games = new Games(this);
        pollen = new Pollen(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new MenuListener(), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new RaceListener(this), this);
        pm.registerEvents(new GuardListener(this), this);
        Commands commands = new Commands(this);
        for (String name : List.of("sommeradmin", "abstimmen", "bereit", "zuschauen", "verlassen", "stats", "top")) {
            PluginCommand c = getCommand(name);
            if (c != null) {
                c.setExecutor(commands);
                c.setTabCompleter(commands);
            }
        }

        construction = new Construction(this, world);
        construction.ensure(false, this::built);

        var scheduler = getServer().getScheduler();
        scheduler.runTaskTimer(this, this::everyTick, 1L, 1L);
        scheduler.runTaskTimer(this, this::everySecond, SECOND, SECOND);
        scheduler.runTaskTimer(this, this::checkLanguages, LANGUAGE_CHECK, LANGUAGE_CHECK);
        scheduler.runTaskTimer(this, () -> reportState(true), STATE_REFRESH, STATE_REFRESH);
        scheduler.runTaskTimer(this, () -> {
            if (ready) {
                boards.refreshVillage();
            }
        }, BOARD_REFRESH, BOARD_REFRESH);

        for (Player p : getServer().getOnlinePlayers()) {
            data(p);
            village.enter(p);
        }
        stateChanged();
    }

    private void built() {
        keepLoaded();
        boards.spawnAll();
        ready = true;
        getLogger().info("Sommerspiele sind bereit: Almlauf, Bogenschießen, Regatta und Seifenkiste.");
        for (Player p : getServer().getOnlinePlayers()) {
            if (village.inVillage(p.getUniqueId())) {
                Holo.refresh(this, p, lang(p));
            }
        }
    }

    @Override
    public void onDisable() {
        if (games != null) {
            games.abort();
        }
        Holo.removeAll();
        if (store != null) {
            if (board != null) {
                store.write(boardFile(), board.toYaml());
            }
            store.close();
        }
        instance = null;
    }

    /** Dorf, Ziele, Schießplatz, Starthäuser und Weide bleiben geladen – sonst verschwinden Schrift und Scheiben. */
    private void keepLoaded() {
        java.util.Set<Long> keys = new java.util.HashSet<>();
        int[][] boxes = {{-60, -40, 60, 60}, {-176, -44, -100, -4}, {-176, -192, -100, -176}, {72, -34, 140, 170},
            {-134, 20, -54, 100}, {-40, 70, 64, 176}};
        for (int[] b : boxes) {
            for (int cx = b[0] >> 4; cx <= b[2] >> 4; cx++) {
                for (int cz = b[1] >> 4; cz <= b[3] >> 4; cz++) {
                    keys.add(((long) cx << 32) ^ (cz & 0xffffffffL));
                }
            }
        }
        for (long k : keys) {
            world.getChunkAt((int) (k >> 32), (int) k).addPluginChunkTicket(this);
        }
    }

    private World createWorld(String name) {
        World w = getServer().getWorld(name);
        if (w == null) {
            w = new WorldCreator(name).generator(new VoidGenerator()).environment(World.Environment.NORMAL)
                    .generateStructures(false).createWorld();
        }
        if (w == null) {
            throw new IllegalStateException("Welt " + name + " ließ sich nicht anlegen");
        }
        w.setGameRule(GameRules.ADVANCE_TIME, false);
        w.setGameRule(GameRules.ADVANCE_WEATHER, false);
        w.setGameRule(GameRules.SPAWN_MOBS, false);
        w.setGameRule(GameRules.SPAWN_MONSTERS, false);
        w.setGameRule(GameRules.SPAWN_PHANTOMS, false);
        w.setGameRule(GameRules.SPAWN_PATROLS, false);
        w.setGameRule(GameRules.SPAWN_WANDERING_TRADERS, false);
        w.setGameRule(GameRules.SHOW_DEATH_MESSAGES, false);
        w.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
        w.setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
        w.setGameRule(GameRules.MOB_GRIEFING, false);
        w.setGameRule(GameRules.BLOCK_DROPS, false);
        w.setGameRule(GameRules.ENTITY_DROPS, false);
        w.setGameRule(GameRules.FALL_DAMAGE, false);
        w.setTime(settings.evening() ? EVENING : DAY);
        w.setStorm(false);
        w.setThundering(false);
        w.setDifficulty(Difficulty.PEACEFUL);
        w.setSpawnLocation(spawn());
        return w;
    }

    private void everyTick() {
        tick++;
        if (!ready) {
            return;
        }
        games.tick(tick);
        pollen.tick(tick);
    }

    private void everySecond() {
        village.tick();
        games.second();
        boards.villageSidebars();
        stateChanged();
    }

    private Path boardFile() {
        return getDataFolder().toPath().resolve("bestenliste.yml");
    }

    private Leaderboard loadBoard() {
        Path f = boardFile();
        if (Files.isRegularFile(f)) {
            try {
                return Leaderboard.fromYaml(Files.readString(f, StandardCharsets.UTF_8));
            } catch (IOException | IllegalArgumentException e) {
                getLogger().warning("bestenliste.yml ließ sich nicht lesen: " + e.getMessage());
            }
        }
        return new Leaderboard();
    }

    public void saveBoard() {
        store.write(boardFile(), board.toYaml());
        boards.refreshVillage();
    }

    private void checkLanguages() {
        for (Player p : getServer().getOnlinePlayers()) {
            String lang = lang(p);
            String before = lastLang.put(p.getUniqueId(), lang);
            if (before != null && !before.equals(lang)) {
                Holo.refresh(this, p, lang);
                if (village.inVillage(p.getUniqueId())) {
                    village.items(p, lang);
                }
            }
        }
    }

    void forgetLanguage(UUID uuid) {
        lastLang.remove(uuid);
    }

    // ---------- Zugriff ----------

    public Settings settings() {
        return settings;
    }

    public Network net() {
        return net;
    }

    public Store store() {
        return store;
    }

    public Leaderboard board() {
        return board;
    }

    public World world() {
        return world;
    }

    public Village village() {
        return village;
    }

    public Games games() {
        return games;
    }

    public Boards boards() {
        return boards;
    }

    public boolean ready() {
        return ready;
    }

    public long tick() {
        return tick;
    }

    public String lang(Player p) {
        return net.language(p.getUniqueId());
    }

    public PlayerData data(Player p) {
        return store.get(p.getUniqueId(), p.getName());
    }

    public boolean builder(Player p) {
        return p.getGameMode() == org.bukkit.GameMode.CREATIVE && p.hasPermission(ADMIN);
    }

    public Location spawn() {
        return new Location(world, Places.SPAWN[0], Terrain.FLOOR_Y + Places.SPAWN[1], Places.SPAWN[2], Places.SPAWN_YAW, 0f);
    }

    /** Heute (Berliner Zeit) – zum Testen der Saison umstellbar. */
    public LocalDate today() {
        return testDate != null ? testDate : LocalDate.now(ZONE);
    }

    void testDate(LocalDate d) {
        testDate = d;
    }

    /** Ist gerade Saison? */
    public boolean open() {
        return settings.season().open(today());
    }

    /** Wann es wieder losgeht, als Text („1. April“ / „April 1“). */
    public String reopening(String lang) {
        LocalDate d = settings.season().nextOpening(today());
        java.time.format.DateTimeFormatter f = "en".equals(lang)
                ? java.time.format.DateTimeFormatter.ofPattern("MMMM d", java.util.Locale.ENGLISH)
                : java.time.format.DateTimeFormatter.ofPattern("d. MMMM", java.util.Locale.GERMAN);
        return d.format(f);
    }

    public void stateChanged() {
        if (stateQueued) {
            return;
        }
        stateQueued = true;
        getServer().getScheduler().runTask(this, () -> {
            stateQueued = false;
            reportState(false);
        });
    }

    private void reportState(boolean force) {
        String state = !open() ? "closed" : games.running() ? "running" : "waiting";
        int players = getServer().getOnlinePlayers().size();
        if (force || !state.equals(lastState) || players != lastPlayers) {
            lastState = state;
            lastPlayers = players;
            net.state(state, players);
        }
    }

    void rebuild(Runnable done) {
        if (construction.running()) {
            return;
        }
        ready = false;
        games.abort();
        boards.removeAll();
        construction.ensure(true, () -> {
            built();
            done.run();
        });
        for (Player p : getServer().getOnlinePlayers()) {
            village.enter(p);
        }
    }

    void reloadSettings() {
        reloadConfig();
        settings = Settings.load(getConfig());
        world.setTime(settings.evening() ? EVENING : DAY);
        stateChanged();
    }
}

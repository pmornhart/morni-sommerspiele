package de.moinmorni.fake;

import de.moinmorni.api.MoinMorni;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Test-Ersatz für das Netzwerk-Plugin: stellt die Schnittstelle {@link MoinMorni} bereit. Sprache und Partys kommen
 * aus {@code plugins/MoinMorni/fake.properties} (bei jedem Aufruf neu gelesen), alle Meldungen (Statistik, Zustand,
 * Lobby, Nachfrage, „spielt gerade“, Platz freihalten) landen als JSON-Zeilen in {@code plugins/MoinMorni/calls.log}.
 * Wie das echte Netzwerk fragt es bei {@link #askToLobby} nur nach, solange der Modus „spielt gerade“ gemeldet hat
 * (dann bleibt der Spieler da), und vergisst den Stand, wenn jemand den Server verlässt.
 *
 * <pre>
 * lang.Bert=en
 * party.Anna=Anna,Bert      # Anführerin Anna, Mitglieder Anna und Bert
 * </pre>
 */
public final class FakeNetwork extends JavaPlugin implements MoinMorni, Listener {

    private final Set<UUID> playing = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        getServer().getServicesManager().register(MoinMorni.class, this, this, ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        playing.remove(e.getPlayer().getUniqueId());
    }

    private Properties props() {
        Properties p = new Properties();
        Path file = getDataFolder().toPath().resolve("fake.properties");
        if (Files.isRegularFile(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                p.load(r);
            } catch (IOException e) {
                getLogger().warning("fake.properties: " + e.getMessage());
            }
        }
        return p;
    }

    private synchronized void log(String json) {
        try {
            Files.writeString(getDataFolder().toPath().resolve("calls.log"), json + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            getLogger().warning("calls.log: " + e.getMessage());
        }
    }

    private static String nameOf(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        return p != null ? p.getName() : Bukkit.getOfflinePlayer(uuid).getName();
    }

    private static UUID uuidOf(String name) {
        Player p = Bukkit.getPlayerExact(name);
        return p != null ? p.getUniqueId()
                : UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private static String json(Map<String, Long> m) {
        if (m == null) {
            return "{}";
        }
        return m.entrySet().stream().map(e -> "\"" + e.getKey() + "\":" + e.getValue())
                .collect(Collectors.joining(",", "{", "}"));
    }

    @Override
    public String serverName() {
        return "sommerspiele";
    }

    @Override
    public boolean online() {
        return true;
    }

    @Override
    public String language(UUID player) {
        String name = nameOf(player);
        return name == null ? "de" : props().getProperty("lang." + name, "de");
    }

    @Override
    public Rank rank(UUID player) {
        return new Rank("player", "", "#aaaaaa", 99);
    }

    @Override
    public boolean hasRight(UUID player, String right) {
        return false;
    }

    @Override
    public CompletableFuture<Optional<Party>> party(UUID player) {
        String name = nameOf(player);
        Optional<Party> result = Optional.empty();
        Properties p = props();
        for (String key : p.stringPropertyNames()) {
            if (!key.startsWith("party.")) {
                continue;
            }
            List<String> names = List.of(p.getProperty(key).split(","));
            if (name != null && names.stream().anyMatch(n -> n.trim().equalsIgnoreCase(name))) {
                List<Party.Member> members = new ArrayList<>();
                for (String n : names) {
                    String m = n.trim();
                    members.add(new Party.Member(uuidOf(m), m, Bukkit.getPlayerExact(m) != null ? "sommerspiele" : null));
                }
                result = Optional.of(new Party(uuidOf(key.substring("party.".length())), List.copyOf(members)));
            }
        }
        // Wie das echte Netzwerk: die Antwort kommt auf einem anderen Thread.
        CompletableFuture<Optional<Party>> future = new CompletableFuture<>();
        Optional<Party> answer = result;
        Thread t = new Thread(() -> future.complete(answer), "fake-network-party");
        t.setDaemon(true);
        t.start();
        return future;
    }

    @Override
    public void stats(String mode, UUID player, Map<String, Long> add, Map<String, Long> best, boolean lowerIsBetter) {
        log("{\"t\":\"stats\",\"mode\":\"" + mode + "\",\"name\":\"" + nameOf(player) + "\",\"uuid\":\"" + player
                + "\",\"add\":" + json(add) + ",\"best\":" + json(best) + ",\"lower\":" + lowerIsBetter + "}");
    }

    @Override
    public void state(String mode, String state, int players) {
        log("{\"t\":\"state\",\"mode\":\"" + mode + "\",\"state\":\"" + state + "\",\"players\":" + players + "}");
    }

    @Override
    public void toLobby(Player player) {
        log("{\"t\":\"to-lobby\",\"name\":\"" + player.getName() + "\"}");
        player.kick(Component.text("Test-Netzwerk: ab in die Lobby"));
    }

    @Override
    public void askToLobby(Player player) {
        boolean asks = playing.contains(player.getUniqueId());
        log("{\"t\":\"ask-lobby\",\"name\":\"" + player.getName() + "\",\"asked\":" + asks + "}");
        if (asks) {
            player.sendMessage(Component.text("Test-Netzwerk: Willst du wirklich zur Lobby? (Nachfrage)"));
        } else {
            player.kick(Component.text("Test-Netzwerk: ab in die Lobby"));
        }
    }

    @Override
    public void playing(Player player, boolean on) {
        if (on) {
            playing.add(player.getUniqueId());
        } else {
            playing.remove(player.getUniqueId());
        }
        log("{\"t\":\"playing\",\"name\":\"" + player.getName() + "\",\"playing\":" + on + "}");
    }

    @Override
    public void holdSlot(UUID player, int seconds) {
        log("{\"t\":\"hold-slot\",\"name\":\"" + nameOf(player) + "\",\"uuid\":\"" + player + "\",\"seconds\":"
                + seconds + "}");
    }
}

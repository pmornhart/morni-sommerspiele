package de.moinmorni.sommerspiele.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spielerdaten als YAML-Dateien ({@code spieler/<uuid>.yml}). Gelesen wird beim Betreten (kleine Datei), geschrieben
 * im Hintergrund – nacheinander, jede Datei erst als Kopie und dann umbenannt (nie halb geschrieben).
 */
public final class Store {

    private static final long CLOSE_WAIT_SECONDS = 10;

    private final Logger log;
    private final Path dir;
    private final Map<UUID, PlayerData> loaded = new HashMap<>();
    private final java.util.Set<UUID> pinned = new java.util.HashSet<>();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Morni-Sommerspiele-Speichern");
        t.setDaemon(true);
        return t;
    });

    public Store(Logger log, Path dataFolder) {
        this.log = log;
        this.dir = dataFolder.resolve("spieler");
    }

    /** Daten eines Spielers (lädt sie beim ersten Mal). */
    public PlayerData get(UUID uuid, String name) {
        PlayerData d = loaded.computeIfAbsent(uuid, u -> read(u).orElseGet(() -> new PlayerData(u, name)));
        d.name(name);
        return d;
    }

    /** Nur wenn schon geladen (Spieler ist da). */
    public Optional<PlayerData> loaded(UUID uuid) {
        return Optional.ofNullable(loaded.get(uuid));
    }

    /** Daten von der Platte lesen, ohne sie zu behalten (z. B. /stats für jemanden, der nicht da ist). */
    public Optional<PlayerData> read(UUID uuid) {
        PlayerData online = loaded.get(uuid);
        if (online != null) {
            return Optional.of(online);
        }
        Path file = file(uuid);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(PlayerData.fromYaml(uuid, Files.readString(file, StandardCharsets.UTF_8)));
        } catch (IOException | IllegalArgumentException e) {
            log.log(Level.WARNING, "Spielerdatei " + file.getFileName() + " ließ sich nicht lesen", e);
            return Optional.empty();
        }
    }

    public void save(PlayerData d) {
        write(file(d.uuid()), d.toYaml());
    }

    /** Spieler ist weg: speichern und aus dem Speicher nehmen (außer er steckt noch in einem Spiel). */
    public void unload(UUID uuid) {
        PlayerData d = pinned.contains(uuid) ? loaded.get(uuid) : loaded.remove(uuid);
        if (d != null) {
            save(d);
        }
    }

    /**
     * Daten festhalten, solange ein Spiel läuft: verlässt jemand mitten im Spiel den Server und kommt wieder, bevor das
     * Spiel vorbei ist, arbeiten Spiel und Spieler mit denselben Daten.
     */
    public void pin(UUID uuid) {
        pinned.add(uuid);
    }

    public void unpin(UUID uuid, boolean online) {
        pinned.remove(uuid);
        if (!online) {
            loaded.remove(uuid);
        }
    }

    /** Beliebige Datei im Hintergrund schreiben (gleiche Reihenfolge wie die Aufrufe). */
    public void write(Path file, String text) {
        writer.execute(() -> {
            try {
                Files.createDirectories(file.getParent());
                Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
                Files.writeString(tmp, text, StandardCharsets.UTF_8);
                try {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                log.log(Level.WARNING, "Konnte " + file.getFileName() + " nicht speichern", e);
            }
        });
    }

    /** Beim Abschalten: alles speichern und warten, bis es auf der Platte ist. */
    public void close() {
        loaded.values().forEach(this::save);
        loaded.clear();
        writer.shutdown();
        try {
            if (!writer.awaitTermination(CLOSE_WAIT_SECONDS, TimeUnit.SECONDS)) {
                log.warning("Speichern dauert zu lange – einige Werte könnten fehlen.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Path file(UUID uuid) {
        return dir.resolve(uuid + ".yml");
    }
}

package de.moinmorni.sommerspiele.net;

import de.moinmorni.api.MoinMorni;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Sommerspiele im Netzwerk: alles über die Schnittstelle des Netzwerk-Plugins. Die Schnittstelle wird bei jedem Aufruf neu
 * geholt – lädt jemand das Netzwerk-Plugin neu, stimmt sie trotzdem.
 *
 * <p>Nachfrage vor der Lobby, „spielt gerade“ und „Platz freihalten“ gibt es erst ab Version 2 der Schnittstelle. Ein
 * älteres Netzwerk-Plugin kennt sie nicht ({@link LinkageError}): dann geht „zur Lobby“ sofort und der Rest entfällt –
 * alles andere läuft wie bisher.
 */
final class Connected implements Network {

    private static final String GERMAN = "de";

    private final Logger log;
    private volatile boolean olderNetwork;

    private Connected(Logger log) {
        this.log = log;
    }

    /** null, wenn das Netzwerk-Plugin keine Schnittstelle anbietet. */
    static Network create(Logger log) {
        return MoinMorni.get().isPresent() ? new Connected(log) : null;
    }

    private static Optional<MoinMorni> api() {
        return MoinMorni.get();
    }

    @Override
    public boolean connected() {
        return true;
    }

    @Override
    public String language(UUID player) {
        String lang = api().map(a -> a.language(player)).orElse(GERMAN);
        return "en".equals(lang) ? "en" : GERMAN;
    }

    @Override
    public void stats(UUID player, Map<String, Long> add, Map<String, Long> bestTimes, Map<String, Long> bestHigh) {
        api().ifPresent(a -> {
            a.stats(MODE, player, add, bestHigh, false);
            if (!bestTimes.isEmpty()) {
                a.stats(MODE, player, Map.of(), bestTimes, true);
            }
        });
    }

    @Override
    public void state(String state, int players) {
        api().ifPresent(a -> a.state(MODE, state, players));
    }

    @Override
    public void toLobby(Player player, Component message) {
        api().ifPresentOrElse(a -> a.toLobby(player), () -> player.kick(message));
    }

    @Override
    public void askToLobby(Player player, Component message) {
        Optional<MoinMorni> api = api();
        if (api.isEmpty()) {
            player.kick(message);
            return;
        }
        if (!olderNetwork) {
            try {
                api.get().askToLobby(player);
                return;
            } catch (LinkageError e) {
                older(e);
            }
        }
        api.get().toLobby(player);
    }

    @Override
    public void playing(Player player, boolean playing) {
        if (olderNetwork) {
            return;
        }
        api().ifPresent(a -> {
            try {
                a.playing(player, playing);
            } catch (LinkageError e) {
                older(e);
            }
        });
    }

    @Override
    public void holdSlot(UUID player, int seconds) {
        if (olderNetwork) {
            return;
        }
        api().ifPresent(a -> {
            try {
                a.holdSlot(player, seconds);
            } catch (LinkageError e) {
                older(e);
            }
        });
    }

    /** Älteres Netzwerk-Plugin (Schnittstelle 1): einmal melden, danach die neuen Methoden nicht mehr versuchen. */
    private void older(LinkageError e) {
        if (!olderNetwork) {
            olderNetwork = true;
            log.warning("Das Netzwerk-Plugin ist älter (" + e + "): keine Nachfrage vor der Lobby und kein „Zurück ins "
                    + "Spiel“ – alles andere läuft wie bisher.");
        }
    }

    @Override
    public CompletableFuture<Optional<Party>> party(UUID player) {
        Optional<MoinMorni> api = api();
        if (api.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return api.get().party(player).thenApply(p -> p.map(Connected::convert))
                .exceptionally(e -> Optional.empty());
    }

    private static Party convert(MoinMorni.Party p) {
        return new Party(p.leader(), p.members().stream()
                .map(m -> new Party.Member(m.uuid(), m.name(), m.server()))
                .toList());
    }

    @Override
    public String serverName() {
        return api().map(MoinMorni::serverName).orElse("");
    }
}

package de.moinmorni.api;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Schnittstelle des MoinMorni-Netzwerks für Spielmodi (Duels, Morni Party …). Das Netzwerk-Plugin („MoinMorni“) stellt
 * sie als Bukkit-Dienst bereit; ein Modus-Plugin holt sie mit {@link #get()} und braucht in seiner plugin.yml
 * {@code depend: [MoinMorni]} (oder {@code softdepend}, wenn es auch allein laufen soll).
 *
 * <p>Ändert sich diese Datei, müssen die Kopien in den Modus-Repos gleich bleiben (Paket {@code de.moinmorni.api},
 * dort nur zum Übersetzen, nicht in die Jar). Methoden ab Version 2 fehlen in älteren Netzwerk-Plugins: Modi rufen
 * sie so auf, dass ein {@link LinkageError} abgefangen wird, und laufen dann wie bisher weiter.
 */
public interface MoinMorni {

    /** Version dieser Schnittstelle; neue Methoden erhöhen sie. */
    int API_VERSION = 2;

    /** Das Netzwerk, falls das Netzwerk-Plugin läuft. */
    static Optional<MoinMorni> get() {
        RegisteredServiceProvider<MoinMorni> r = Bukkit.getServicesManager().getRegistration(MoinMorni.class);
        return r == null ? Optional.empty() : Optional.of(r.getProvider());
    }

    /** Name dieses Servers im Netzwerk, z. B. „duels“. */
    String serverName();

    /** Ist der Hub gerade verbunden? Ohne Hub läuft der Modus allein weiter (Statistiken gehen dann verloren). */
    boolean online();

    /** Sprache des Spielers: „de“ oder „en“ (aus dem Client oder /sprache). */
    String language(UUID player);

    /** Rang des Spielers (Owner, Moderator, VIP, Spieler …). */
    Rank rank(UUID player);

    /** Hat der Rang dieses Recht (z. B. „build“, „teleport“, „bigger-party“)? */
    boolean hasRight(UUID player, String right);

    /** Party des Spielers – leer ohne Party (oder ohne Hub). */
    CompletableFuture<Optional<Party>> party(UUID player);

    /**
     * Statistiken melden (nur für den eigenen Modus, den Rest lehnt der Hub ab). Werte: kleine Buchstaben, Ziffern,
     * Unterstrich, z. B. „wins“, „kills“, „best_time_ms“.
     *
     * @param add  hochzählen (Siege, Kills …)
     * @param best Bestwerte: übernommen nur, wenn besser ({@code lowerIsBetter} für Zeiten)
     */
    void stats(String mode, UUID player, Map<String, Long> add, Map<String, Long> best, boolean lowerIsBetter);

    /** Zustand melden (Lobby-Figur, Webseite): z. B. „waiting“, „running“, „full“. */
    void state(String mode, String state, int players);

    /** Spieler zurück in die Lobby schicken – sofort, ohne Nachfrage (z. B. am Ende einer Runde). */
    void toLobby(Player player);

    /**
     * Der Spieler will selbst in die Lobby (Knopf oder Gegenstand des Modus). Spielt er gerade ({@link #playing}),
     * fragt das Netzwerk erst nach, ob er wirklich gehen will; sonst geht es sofort. Seit Version 2.
     */
    void askToLobby(Player player);

    /**
     * Spielt der Spieler gerade eine Runde ({@code true}) oder wartet, schaut zu oder ist ausgeschieden
     * ({@code false})? Solange er spielt, fragt „/lobby“ erst nach. Der Modus meldet jede Änderung; beim Verlassen
     * des Servers vergisst das Netzwerk den Stand. Seit Version 2.
     */
    void playing(Player player, boolean playing);

    /**
     * Platz im laufenden Spiel freihalten: Der Spieler hat mitten im Spiel die Verbindung verloren, der Modus wartet
     * so viele Sekunden auf ihn. So lange bietet die Lobby „Zurück ins Spiel“ an und schickt ihn auf diesen Server
     * zurück. {@code 0} nimmt das Angebot zurück (Runde vorbei, Platz vergeben). Seit Version 2.
     */
    void holdSlot(UUID player, int seconds);

    /** Rang eines Spielers. Farbe als „#rrggbb“; Symbol kann leer sein. */
    record Rank(String id, String symbol, String colour, int position) {
    }

    /** Party: Anführer und alle Mitglieder (mit ihrem Server, null = gerade beim Wechseln). */
    record Party(UUID leader, java.util.List<Member> members) {
        public record Member(UUID uuid, String name, String server) {
        }
    }
}

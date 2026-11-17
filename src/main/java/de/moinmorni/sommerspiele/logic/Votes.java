package de.moinmorni.sommerspiele.logic;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Abstimmung im Dorf (Wettkampf oder eine Disziplin). Jede Person hat eine Stimme; es gewinnt die Wahl mit den meisten
 * Stimmen. Gleichstand oder keine Stimmen: was am längsten nicht gespielt wurde (der Reihe nach).
 *
 * @param <M> Auswahl (Aufzählung)
 */
public final class Votes<M extends Enum<M>> {

    private final Class<M> type;
    private final Map<UUID, M> votes = new HashMap<>();
    private final Map<M, Long> lastPlayed;

    public Votes(Class<M> type) {
        this.type = type;
        this.lastPlayed = new EnumMap<>(type);
    }

    public void vote(UUID player, M map) {
        votes.put(player, map);
    }

    public M voteOf(UUID player) {
        return votes.get(player);
    }

    public void forget(UUID player) {
        votes.remove(player);
    }

    /** Nur diese Leute zählen (wer gegangen ist, fällt raus). */
    public void retain(Collection<UUID> players) {
        votes.keySet().retainAll(Set.copyOf(players));
    }

    public int count(M map) {
        return (int) votes.values().stream().filter(m -> m == map).count();
    }

    public void played(M map, long when) {
        lastPlayed.put(map, when);
    }

    /** Gewinnerin unter den freien Karten (null, wenn keine frei ist). */
    public M winner(Set<M> free) {
        M best = null;
        for (M m : type.getEnumConstants()) {
            if (!free.contains(m)) {
                continue;
            }
            if (best == null || count(m) > count(best)
                    || (count(m) == count(best) && lastPlayed.getOrDefault(m, 0L) < lastPlayed.getOrDefault(best, 0L))) {
                best = m;
            }
        }
        return best;
    }

    /** Für eine neue Runde: Stimmen der Mitspieler löschen. */
    public void clear(Collection<UUID> players) {
        players.forEach(votes::remove);
    }
}

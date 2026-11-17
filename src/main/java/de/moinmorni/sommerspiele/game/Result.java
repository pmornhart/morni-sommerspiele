package de.moinmorni.sommerspiele.game;

import de.moinmorni.sommerspiele.logic.Archery;
import de.moinmorni.sommerspiele.logic.Discipline;
import java.util.List;

/**
 * Ergebnis eines Starters in einer Disziplin: Zeit samt Strafen bzw. Ringe, gewertet? Dazu was die Ergebnisliste zeigt
 * (verpasste Tore, Entgleisungen, Mitten).
 *
 * @param missed verpasste Tore (Regatta) bzw. Entgleisungen (Seifenkiste)
 * @param rings  Ringe (Bogenschießen)
 * @param centres Treffer genau in die Mitte (Bogenschießen, zählt bei Gleichstand)
 */
public record Result(List<Racer> members, long timeMs, boolean finished, int missed, int rings, int centres) {

    public String id() {
        return members.stream().map(Racer::id).reduce((a, b) -> a + "+" + b).orElse("");
    }

    public String names() {
        return members.stream().map(Racer::name).reduce((a, b) -> a + " & " + b).orElse("");
    }

    /** Wert für die Wertung: Zeit oder (beim Bogenschießen) Ringe mit Mitten. */
    public long value(Discipline d) {
        return d.scored() ? Archery.rankValue(rings, centres) : timeMs;
    }
}

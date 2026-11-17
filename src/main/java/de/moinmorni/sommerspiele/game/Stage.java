package de.moinmorni.sommerspiele.game;

import de.moinmorni.sommerspiele.logic.Discipline;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Eine laufende Disziplin im Wettkampf (Almlauf, Bogenschießen, Regatta oder Seifenkiste). */
public interface Stage {

    Discipline discipline();

    void start();

    /** Jeden Tick. */
    void tick(long tick);

    /** Jede Sekunde. */
    void second();

    boolean done();

    /** Ergebnis je Starter (Einzel oder Zweierboot). */
    List<Result> results();

    /** Jemand hat den Server oder den Wettkampf verlassen. */
    void quit(Racer r);

    /**
     * Jemand hat gerade die Verbindung verloren, sein Platz bleibt frei (Zurück ins Spiel): nichts beenden, nur
     * aufräumen, was ohne ihn stört. Sein Lauf wartet, die Uhr läuft weiter.
     */
    void away(Racer r);

    /** Rechtzeitig zurück: in den eigenen Lauf zurück (oder auf die Tribüne, wenn er vorbei ist oder noch kommt). */
    void rejoin(Racer r, Player p);

    /** Jemand gibt diese Disziplin auf (bleibt im Wettkampf). */
    void giveUp(Racer r);

    /** Aufräumen (Boote und Loren weg, Scheiben zurück). */
    void cleanup();

    /** Wo Zuschauer stehen. */
    Location view();

    /** Zeile für die Leiste („Lauf 2 von 3“, „Durchgang 1/3“ …). */
    String status(Racer r, String lang);

    /** Zurück auf die Strecke (Gegenstand). */
    void reset(Player p);

    /** Für /sommeradmin status: eine Zeile je Starter. */
    List<String> debug();
}

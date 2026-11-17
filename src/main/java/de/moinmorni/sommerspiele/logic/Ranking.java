package de.moinmorni.sommerspiele.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Wertung einer Disziplin: bei Zeiten die kleinste vorn, bei Ringen (Bogenschießen) die größte. Gleicher Wert = gleicher
 * Platz (und gleiche Medaille), wer nicht ins Ziel kam bzw. aufgegeben hat, steht hinten ohne Platz. Medaillen gibt es
 * nur, wenn mindestens {@link #MEDALS_FROM} Starter dabei waren (allein ist es ein Training).
 */
public final class Ranking {

    /** Ab so vielen Startern gibt es Medaillen. */
    public static final int MEDALS_FROM = 2;
    /** Punkte für die Gesamtwertung nach Platz (ab Platz 8 je 1 Punkt, ohne Ziel 0). */
    private static final int[] POINTS = {10, 8, 6, 5, 4, 3, 2};
    private static final int LATER_POINTS = 1;

    private Ranking() {
    }

    /**
     * Ein Starter: Kennung (Einzel oder Boot zu zweit), Wert (Zeit in ms oder Wertung beim Bogenschießen), gewertet?
     */
    public record Entry(String id, long value, boolean finished) {
    }

    /** Platzierter Starter: Platz 0 = nicht im Ziel. */
    public record Placed(Entry entry, int place, Medal medal, int points) {
        public boolean finished() {
            return entry.finished();
        }
    }

    /** Wertung nach Zeit (kleiner ist besser). */
    public static List<Placed> rank(List<Entry> entries) {
        return rank(entries, false);
    }

    public static List<Placed> rank(List<Entry> entries, boolean higherBetter) {
        List<Entry> sorted = new ArrayList<>(entries);
        Comparator<Entry> byValue = Comparator.comparingLong(Entry::value);
        sorted.sort(Comparator.comparing((Entry e) -> !e.finished()).thenComparing(higherBetter ? byValue.reversed() : byValue));
        boolean medals = entries.size() >= MEDALS_FROM;
        List<Placed> out = new ArrayList<>();
        int place = 0;
        Long last = null;
        for (int i = 0; i < sorted.size(); i++) {
            Entry e = sorted.get(i);
            if (!e.finished()) {
                out.add(new Placed(e, 0, null, 0));
                continue;
            }
            if (last == null || e.value() != last) {
                place = i + 1;
                last = e.value();
            }
            out.add(new Placed(e, place, medals ? Medal.forPlace(place) : null, points(place)));
        }
        return out;
    }

    /** Punkte für einen Platz (0 = nicht im Ziel). */
    public static int points(int place) {
        if (place <= 0) {
            return 0;
        }
        return place <= POINTS.length ? POINTS[place - 1] : LATER_POINTS;
    }
}

package de.moinmorni.sommerspiele.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Medaillenspiegel eines Wettkampfs (oder für immer): je Person Gold, Silber, Bronze und Punkte. Zwei Sortierungen:
 * {@link #medals()} wie bei echten Winterspielen (Gold, dann Silber, dann Bronze) und {@link #overall()} für den
 * Gesamtsieg (Punkte, bei Gleichstand entscheiden die Medaillen).
 */
public final class MedalTable {

    /** Eine Zeile. {@code place} wird beim Sortieren gesetzt (gleiche Werte = gleicher Platz). */
    public record Row(String id, int gold, int silver, int bronze, int points, int place) {
        public int total() {
            return gold + silver + bronze;
        }
    }

    private static final class Tally {
        int gold;
        int silver;
        int bronze;
        int points;
    }

    private final Map<String, Tally> tallies = new LinkedHashMap<>();

    /** Jemand ist dabei (auch ohne Medaille – er steht dann mit 0 im Spiegel). */
    public void join(String id) {
        tallies.computeIfAbsent(id, k -> new Tally());
    }

    public void add(String id, Medal medal, int points) {
        Tally t = tallies.computeIfAbsent(id, k -> new Tally());
        if (medal == Medal.GOLD) {
            t.gold++;
        } else if (medal == Medal.SILVER) {
            t.silver++;
        } else if (medal == Medal.BRONZE) {
            t.bronze++;
        }
        t.points += Math.max(0, points);
    }

    /** Werte direkt setzen (z. B. aus der Datei für den ewigen Spiegel). */
    public void set(String id, int gold, int silver, int bronze, int points) {
        Tally t = tallies.computeIfAbsent(id, k -> new Tally());
        t.gold = gold;
        t.silver = silver;
        t.bronze = bronze;
        t.points = points;
    }

    public boolean contains(String id) {
        return tallies.containsKey(id);
    }

    public int size() {
        return tallies.size();
    }

    public Row row(String id) {
        Tally t = tallies.get(id);
        return t == null ? new Row(id, 0, 0, 0, 0, 0) : new Row(id, t.gold, t.silver, t.bronze, t.points, 0);
    }

    private static final Comparator<Row> BY_MEDALS = Comparator.comparingInt(Row::gold).reversed()
            .thenComparing(Comparator.comparingInt(Row::silver).reversed())
            .thenComparing(Comparator.comparingInt(Row::bronze).reversed());

    private static final Comparator<Row> BY_POINTS = Comparator.comparingInt(Row::points).reversed()
            .thenComparing(BY_MEDALS);

    /** Medaillenspiegel: Gold vor Silber vor Bronze, dann Punkte. */
    public List<Row> medals() {
        return sorted(BY_MEDALS.thenComparing(Comparator.comparingInt(Row::points).reversed()));
    }

    /** Gesamtwertung: Punkte, bei Gleichstand Gold, Silber, Bronze. */
    public List<Row> overall() {
        return sorted(BY_POINTS);
    }

    private List<Row> sorted(Comparator<Row> order) {
        List<Row> rows = new ArrayList<>();
        tallies.forEach((id, t) -> rows.add(new Row(id, t.gold, t.silver, t.bronze, t.points, 0)));
        rows.sort(order);
        List<Row> out = new ArrayList<>();
        Row prev = null;
        int place = 0;
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            if (prev == null || order.compare(prev, r) != 0) {
                place = i + 1;
            }
            out.add(new Row(r.id(), r.gold(), r.silver(), r.bronze(), r.points(), place));
            prev = r;
        }
        return out;
    }
}

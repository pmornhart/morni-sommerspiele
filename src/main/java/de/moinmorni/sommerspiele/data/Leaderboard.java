package de.moinmorni.sommerspiele.data;

import de.moinmorni.sommerspiele.logic.Discipline;
import de.moinmorni.sommerspiele.logic.Medal;
import de.moinmorni.sommerspiele.logic.MedalTable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Bestenlisten für die Tafeln im Dorf: die zehn besten Zeiten je Disziplin (beim Bogenschießen die meisten Ringe;
 * jede Person nur einmal, mit ihrem Bestwert) und der ewige Medaillenspiegel.
 */
public final class Leaderboard {

    public static final int KEEP = 10;

    /** Eintrag: Zeit in ms bzw. (Bogenschießen) Ringe. */
    public record Time(UUID uuid, String name, long ms) {
    }

    private final Map<Discipline, List<Time>> times = new EnumMap<>(Discipline.class);
    private final MedalTable medals = new MedalTable();
    private final Map<String, String> names = new HashMap<>();

    /**
     * Neue Zeit eintragen.
     *
     * @return Platz in der Liste (1–10) oder 0, wenn sie nicht hineinkommt bzw. nicht besser ist als die eigene
     */
    public int submit(Discipline d, UUID uuid, String name, long ms) {
        if (ms <= 0) {
            return 0;
        }
        List<Time> list = times.computeIfAbsent(d, k -> new ArrayList<>());
        for (Time t : list) {
            if (t.uuid().equals(uuid) && (d.scored() ? t.ms() >= ms : t.ms() <= ms)) {
                return 0;
            }
        }
        list.removeIf(t -> t.uuid().equals(uuid));
        Time mine = new Time(uuid, name, ms);
        list.add(mine);
        Comparator<Time> order = Comparator.comparingLong(Time::ms);
        list.sort(d.scored() ? order.reversed() : order);
        while (list.size() > KEEP) {
            list.remove(list.size() - 1);
        }
        int idx = list.indexOf(mine);
        return idx < 0 ? 0 : idx + 1;
    }

    public List<Time> top(Discipline d) {
        return List.copyOf(times.getOrDefault(d, List.of()));
    }

    /** Medaille oder Punkte in den ewigen Spiegel. */
    public void medal(UUID uuid, String name, Medal m, int points) {
        names.put(uuid.toString(), name);
        medals.add(uuid.toString(), m, points);
    }

    /** Ewiger Medaillenspiegel (nur wer mindestens eine Medaille hat). */
    public List<MedalTable.Row> medalTable() {
        return medals.medals().stream().filter(r -> r.total() > 0).toList();
    }

    public String nameOf(String id) {
        return names.getOrDefault(id, "?");
    }

    public String toYaml() {
        YamlConfiguration y = new YamlConfiguration();
        times.forEach((d, list) -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Time t : list) {
                rows.add(Map.of("uuid", t.uuid().toString(), "name", t.name(), "ms", t.ms()));
            }
            y.set("zeiten." + d.id(), rows);
        });
        for (MedalTable.Row r : medals.medals()) {
            String base = "medaillen." + r.id() + ".";
            y.set(base + "name", nameOf(r.id()));
            y.set(base + "gold", r.gold());
            y.set(base + "silber", r.silver());
            y.set(base + "bronze", r.bronze());
            y.set(base + "punkte", r.points());
        }
        return y.saveToString();
    }

    public static Leaderboard fromYaml(String text) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        Leaderboard b = new Leaderboard();
        for (Discipline d : Discipline.values()) {
            for (Map<?, ?> row : y.getMapList("zeiten." + d.id())) {
                try {
                    UUID u = UUID.fromString(String.valueOf(row.get("uuid")));
                    long ms = Long.parseLong(String.valueOf(row.get("ms")));
                    b.submit(d, u, String.valueOf(row.get("name")), ms);
                } catch (IllegalArgumentException e) {
                    // kaputte Zeile überspringen
                }
            }
        }
        ConfigurationSection m = y.getConfigurationSection("medaillen");
        if (m != null) {
            for (String id : m.getKeys(false)) {
                b.names.put(id, m.getString(id + ".name", "?"));
                b.medals.set(id, m.getInt(id + ".gold"), m.getInt(id + ".silber"), m.getInt(id + ".bronze"),
                        m.getInt(id + ".punkte"));
            }
        }
        return b;
    }
}

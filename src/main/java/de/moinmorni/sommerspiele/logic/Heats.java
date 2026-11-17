package de.moinmorni.sommerspiele.logic;

import java.util.ArrayList;
import java.util.List;

/**
 * Läufe: Bei Regatta und Seifenkiste fahren höchstens so viele gleichzeitig, wie es Bahnen gibt. Die Starter werden in
 * Startreihenfolge auf möglichst gleich große Läufe verteilt (vorne höchstens einer mehr).
 */
public final class Heats {

    private Heats() {
    }

    public static <T> List<List<T>> split(List<T> starters, int lanes) {
        List<List<T>> out = new ArrayList<>();
        if (starters.isEmpty()) {
            return out;
        }
        int lanesOk = Math.max(1, lanes);
        int heats = (starters.size() + lanesOk - 1) / lanesOk;
        int base = starters.size() / heats;
        int extra = starters.size() % heats;
        int i = 0;
        for (int h = 0; h < heats; h++) {
            int size = base + (h < extra ? 1 : 0);
            out.add(new ArrayList<>(starters.subList(i, i + size)));
            i += size;
        }
        return out;
    }
}

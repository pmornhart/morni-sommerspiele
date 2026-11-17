package de.moinmorni.sommerspiele.logic;

import java.util.ArrayList;
import java.util.List;

/**
 * Zweier-Ruderboot: Wer zusammen in einer Party ist, rudert zu zweit in einem Boot (der Erste lenkt). Bleibt in einer Party
 * jemand übrig, fährt er allein – Fremde werden nicht zusammengewürfelt.
 */
public final class Teams {

    private Teams() {
    }

    /**
     * @param groups Gruppen in Startreihenfolge (eine Party = eine Gruppe, wer allein ist = Gruppe aus einem)
     * @param pairs  Zweierboot erlaubt? Sonst rudert jeder allein.
     * @return Boote in Startreihenfolge: je einer oder zwei Leute
     */
    public static <T> List<List<T>> bobs(List<List<T>> groups, boolean pairs) {
        List<List<T>> out = new ArrayList<>();
        for (List<T> g : groups) {
            if (!pairs) {
                g.forEach(t -> out.add(List.of(t)));
                continue;
            }
            for (int i = 0; i < g.size(); i += 2) {
                out.add(i + 1 < g.size() ? List.of(g.get(i), g.get(i + 1)) : List.of(g.get(i)));
            }
        }
        return out;
    }
}

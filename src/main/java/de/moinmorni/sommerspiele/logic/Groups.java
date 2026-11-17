package de.moinmorni.sommerspiele.logic;

import java.util.ArrayList;
import java.util.List;

/** Wer kommt in den Wettkampf? Partys bleiben zusammen; passt eine Party nicht mehr hinein, wartet sie ganz. */
public final class Groups {

    private Groups() {
    }

    /**
     * @param groups Gruppen in Wartereihenfolge (je eine Party oder ein Einzelner)
     * @param max    höchstens so viele Leute
     */
    public static <T> List<T> pick(List<List<T>> groups, int max) {
        List<T> out = new ArrayList<>();
        for (List<T> g : groups) {
            if (out.size() + g.size() <= max) {
                out.addAll(g);
            } else if (out.isEmpty()) {
                out.addAll(g.subList(0, Math.min(max, g.size())));
            }
        }
        return out;
    }
}

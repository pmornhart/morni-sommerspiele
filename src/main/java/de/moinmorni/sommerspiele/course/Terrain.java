package de.moinmorni.sommerspiele.course;

/**
 * Höhe des Kistenbergs (ohne Minecraft): Das Tal liegt auf Höhe 0 (Welt-y {@link #FLOOR_Y}). Nördlich vom Bergfuß
 * ({@link #FOOT_Z}) steigt der Berg in Terrassen an – je {@link #PERIOD} Blöcke eine Stufe aus {@link #STEPS} Absätzen zu
 * je einem Block. Die Schienen der Seifenkiste gehen auf den Absätzen schräg hinab (eine Schiene pro Block), die Kurven
 * liegen auf den flachen Stücken. Weil die Höhe nur von z abhängt, passen alle Bahnen nebeneinander von selbst.
 */
public final class Terrain {

    /** Welt-y der Talsohle (oberster Block); man steht auf {@code FLOOR_Y + 1}. */
    public static final int FLOOR_Y = 100;
    /** Letzte Reihe des Tals; nördlich davon (kleineres z) beginnt der Berg. */
    public static final int FOOT_Z = -40;
    /** Länge einer Terrasse samt Absätzen. */
    public static final int PERIOD = 16;
    /** Absätze am Anfang jeder Terrasse (je ein Block hoch). */
    public static final int STEPS = 3;
    public static final int TERRACES = 8;
    /** Höhe des Gipfelplateaus (Starthäuser). */
    public static final int TOP = STEPS * TERRACES;
    /** Ab hier (nach Norden) liegt das Plateau. */
    public static final int PLATEAU_Z = FOOT_Z - PERIOD * TERRACES;
    /** Mitte der flachen Stücke: so weit hinter dem ersten Absatz einer Terrasse. */
    private static final int MIDDLE = 10;

    private Terrain() {
    }

    /** Höhe des Bodens (oberster Block, relativ zum Tal) in der Reihe z. */
    public static int height(int z) {
        int d = FOOT_Z - z;
        if (d <= 0) {
            return 0;
        }
        if (d > PERIOD * TERRACES) {
            return TOP;
        }
        int i = (d - 1) / PERIOD;
        int r = (d - 1) % PERIOD;
        return STEPS * i + Math.min(r + 1, STEPS);
    }

    public static int height(double z) {
        return height((int) Math.floor(z));
    }

    /** Welt-y, auf dem man an dieser Stelle steht. */
    public static double standY(double z) {
        return FLOOR_Y + height(z) + 1;
    }

    /** Steigt in dieser Reihe eine Schiene nach Norden an (der nördliche Nachbar liegt einen Block höher)? */
    public static boolean ascending(int z) {
        return height(z - 1) == height(z) + 1;
    }

    /**
     * Höhe der Schiene (relativ zum Tal, wo die Lore fährt) an der Stelle z – auf ansteigenden Schienen gleitend: an der
     * Südkante {@code h + 1}, an der Nordkante {@code h + 2}.
     */
    public static double railY(double z) {
        int zb = (int) Math.floor(z);
        int h = height(zb);
        if (!ascending(zb)) {
            return h + 1;
        }
        double toNorth = zb + 1 - z;
        return h + 1 + toNorth;
    }

    /** z-Mitte der flachen Terrasse i (0 = unterste) – hier liegen Kurven und Weichen. */
    public static double terraceMiddle(int i) {
        return FOOT_Z - (PERIOD * i + MIDDLE) + 0.5;
    }
}

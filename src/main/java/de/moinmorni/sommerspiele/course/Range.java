package de.moinmorni.sommerspiele.course;

/**
 * Bogenschießplatz am Waldrand: zwölf Bahnen nebeneinander, alle schießen gleichzeitig von der Schießlinie nach Süden.
 * Je Durchgang steht die Scheibe auf einer anderen Entfernung ({@link #DISTANCES}); die Scheiben sind 5 × 5 Blöcke groß
 * (Mitte gold, dann rot, dann blau) und werden für jeden Durchgang an die richtige Stelle gestellt.
 */
public final class Range {

    public static final int LANES = 12;
    /** x der Matte von Bahn 0; die Bahnen liegen {@link #SPACING} Blöcke auseinander. */
    public static final int FIRST_X = -129;
    public static final int SPACING = 6;
    /** Reihe der Matten (man steht in ihrer Mitte und schaut nach Süden). */
    public static final int MAT_Z = 40;
    /** Entfernungen der Durchgänge. */
    public static final int[] DISTANCES = {20, 35, 50};
    /** Halbe Größe der Scheibe (5 × 5 Blöcke). */
    public static final int FACE_HALF = 2;
    /** Mitte der Scheibe über dem Boden (Planhöhe; man steht auf 1). */
    public static final double CENTER_Y = 3.5;
    /** Hinter der letzten Scheibe: Strohwand und Waldrand. */
    public static final int BACKSTOP_Z = MAT_Z + DISTANCES[DISTANCES.length - 1] + 4;

    private Range() {
    }

    public static int laneX(int lane) {
        return FIRST_X + lane * SPACING;
    }

    /** Mitte der Matte (x, z). */
    public static double[] mat(int lane) {
        return new double[] {laneX(lane) + 0.5, MAT_Z + 0.5};
    }

    /** Reihe (z) der Scheibe im Durchgang r; ihre Vorderseite liegt bei genau diesem z. */
    public static int targetZ(int round) {
        return MAT_Z + DISTANCES[Math.max(0, Math.min(DISTANCES.length - 1, round))];
    }

    /** Mitte der Scheibe (x, Planhöhe y). */
    public static double[] center(int lane) {
        return new double[] {laneX(lane) + 0.5, CENTER_Y};
    }
}

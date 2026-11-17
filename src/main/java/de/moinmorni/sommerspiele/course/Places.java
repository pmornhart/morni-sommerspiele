package de.moinmorni.sommerspiele.course;

/**
 * Wo im Sommerdorf was steht (Blockkoordinaten relativ zur Talsohle, y = 0 ist der Boden): Dorfplatz mit Maibaum,
 * Siegertreppchen, Tafeln, Biergarten, Badesteg und die Zuschauertribünen an den Strecken.
 */
public final class Places {

    /** Ankunft im Dorf (Blick nach Norden zum Maibaum und zum Berg). */
    public static final double[] SPAWN = {0.5, 1, 30.5};
    public static final float SPAWN_YAW = 180f;
    /** Dorfplatz (Mitte) und Maibaum. */
    public static final int PLAZA_Z = 8;
    public static final int PLAZA_R = 15;
    public static final int[] MAYPOLE = {0, 6};
    /** Siegertreppchen: Mitte, Blick der Sieger nach Süden auf den Platz. */
    public static final int[] PODIUM = {0, -9};
    /** Tafeln links und rechts vom Treppchen. */
    public static final double[] BOARD_MEDALS = {-11.5, 4.2, -7.5};
    public static final double[] BOARD_TIMES = {12.5, 4.2, -7.5};
    /** Biergarten. */
    public static final int[] BEERGARDEN = {26, 24};
    /** Tribünen (Mitte, z der vordersten Reihe): Seifenkiste (Blick nach Norden), Regatta (nach Norden). */
    public static final int[] STAND_KISTE = {-134, -15};
    public static final int[] STAND_REGATTA = {105, 160};
    /** Zuschauer am Schießstand (hinter der Linie, Blick nach Süden) und am Almlauf (Blick nach Osten). */
    public static final int[] STAND_BOGEN = {-94, 28};
    public static final int[] STAND_ALM = {-44, 120};
    /** Grenzen der ganzen Welt (darüber hinaus unsichtbare Wand). */
    public static final int MIN_X = -192;
    public static final int MAX_X = 176;
    public static final int MIN_Z = -200;
    public static final int MAX_Z = 196;

    private Places() {
    }

    /** Welt-y für Planhöhe y. */
    public static double worldY(double y) {
        return Terrain.FLOOR_Y + y;
    }
}

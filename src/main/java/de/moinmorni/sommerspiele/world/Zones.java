package de.moinmorni.sommerspiele.world;

import de.moinmorni.sommerspiele.course.Alm;
import de.moinmorni.sommerspiele.course.Kiste;
import de.moinmorni.sommerspiele.course.Lake;
import de.moinmorni.sommerspiele.course.Path;
import de.moinmorni.sommerspiele.course.Places;
import de.moinmorni.sommerspiele.course.Range;
import java.util.BitSet;

/**
 * Was wo frei bleiben muss (keine Bäume, keine Blumen): Schienen, See, Schießplatz, Almlauf, Dorf und die Zielbereiche.
 * Einmal für die ganze Welt als Raster ausgerechnet.
 */
final class Zones {

    private static final int W = Places.MAX_X - Places.MIN_X + 1;
    private static final int H = Places.MAX_Z - Places.MIN_Z + 1;
    private static BitSet reserved;
    private static BitSet tracks;

    private Zones() {
    }

    private static int idx(int x, int z) {
        return (x - Places.MIN_X) * H + (z - Places.MIN_Z);
    }

    private static boolean inside(int x, int z) {
        return x >= Places.MIN_X && x <= Places.MAX_X && z >= Places.MIN_Z && z <= Places.MAX_Z;
    }

    /** Gebaut oder Weg: hier keine Bäume und keine Blumen. */
    static synchronized boolean reserved(int x, int z) {
        init();
        return inside(x, z) && reserved.get(idx(x, z));
    }

    /** Schiene, Wasser oder Laufweg einer Strecke (genauer als {@link #reserved}). */
    static synchronized boolean track(int x, int z) {
        init();
        return inside(x, z) && tracks.get(idx(x, z));
    }

    private static void box(BitSet set, int x1, int z1, int x2, int z2) {
        for (int x = Math.max(Places.MIN_X, x1); x <= Math.min(Places.MAX_X, x2); x++) {
            for (int z = Math.max(Places.MIN_Z, z1); z <= Math.min(Places.MAX_Z, z2); z++) {
                set.set(idx(x, z));
            }
        }
    }

    private static void around(BitSet set, Path path, double radius) {
        int r = (int) Math.ceil(radius) + 1;
        for (Path.Sample s : path.samples()) {
            int cx = (int) Math.floor(s.x());
            int cz = (int) Math.floor(s.z());
            for (int x = cx - r; x <= cx + r; x++) {
                for (int z = cz - r; z <= cz + r; z++) {
                    if (inside(x, z) && Math.hypot(x + 0.5 - s.x(), z + 0.5 - s.z()) <= radius) {
                        set.set(idx(x, z));
                    }
                }
            }
        }
    }

    private static void init() {
        if (reserved != null) {
            return;
        }
        BitSet res = new BitSet(W * H);
        BitSet tr = new BitSet(W * H);
        for (int lane = 0; lane < Kiste.lanes(); lane++) {
            for (boolean steep : new boolean[] {false, true}) {
                around(tr, Kiste.path(lane, steep), 1.5);
                around(res, Kiste.path(lane, steep), 4);
            }
            box(res, Kiste.LANE_X[lane] - 4, (int) Kiste.TOP_Z - 6, Kiste.LANE_X[lane] + 4, (int) Kiste.START_Z + 4);
        }
        box(tr, Lake.X1, Lake.Z1, Lake.X2, Lake.Z2);
        box(res, Lake.X1 - 5, Lake.Z1 - 5, Lake.X2 + 5, Lake.Z2 + 22);
        box(res, Range.laneX(0) - 6, Range.MAT_Z - 12, Range.laneX(Range.LANES - 1) + 6, Range.BACKSTOP_Z + 3);
        around(tr, Alm.loop(), Alm.HALF + 0.5);
        around(res, Alm.loop(), Alm.FENCE + 2);
        box(res, Alm.PASTURE[0] - 2, Alm.PASTURE[1] - 8, Alm.PASTURE[2] + 2, Alm.PASTURE[3]);
        // Zielbereiche mit Tribünen, Dorf
        box(res, -178, -44, -98, -4);
        box(res, Places.STAND_ALM[0] - 6, Places.STAND_ALM[1] - 8, Places.STAND_ALM[0] + 4, Places.STAND_ALM[1] + 8);
        box(res, -56, -30, 56, 62);
        reserved = res;
        tracks = tr;
        reserved.or(tracks);
        // Wege vom Dorf zu den Strecken
        box(reserved, -100, -20, -40, -12);
        box(reserved, 40, -16, 80, -8);
        box(reserved, -90, 14, -40, 22);
        box(reserved, -6, 56, 6, 80);
    }
}

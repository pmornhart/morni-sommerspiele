package de.moinmorni.sommerspiele.world;

import de.moinmorni.sommerspiele.course.Places;
import de.moinmorni.sommerspiele.course.Range;

/**
 * Der Bogenschießplatz: überdachte Schießlinie mit zwölf Matten, weiße Linien zwischen den Bahnen, Entfernungs-
 * markierungen bei 20, 35 und 50 Blöcken, hinten eine Strohwand vor dem Waldrand, dahinter die Zuschauer. Die Scheiben
 * stellt das Spiel je Durchgang selbst auf.
 */
final class RangePlan {

    private RangePlan() {
    }

    static Plan build() {
        Plan p = new Plan();
        int x1 = Range.laneX(0) - 3;
        int x2 = Range.laneX(Range.LANES - 1) + 3;
        int z0 = Range.MAT_Z;
        for (int x = x1; x <= x2; x++) {
            for (int z = z0 - 3; z <= Range.BACKSTOP_Z; z++) {
                p.set(x, 0, z, "grass_block[snowy=false]");
                p.air(x, 1, z);
            }
            // Holzboden und Dach der Schießlinie
            for (int z = z0 - 3; z <= z0 + 1; z++) {
                p.set(x, 0, z, "spruce_planks");
                p.set(x, 6, z, z == z0 + 1 || z == z0 - 3 ? "spruce_slab[type=bottom]" : "dark_oak_planks");
            }
            // Strohwand hinten
            for (int y = 1; y <= 8; y++) {
                p.set(x, y, Range.BACKSTOP_Z, y == 8 ? "hay_block[axis=x]" : "hay_block[axis=y]");
            }
        }
        for (int x = x1; x <= x2; x += 6) {
            for (int y = 1; y <= 5; y++) {
                p.set(x, y, z0 + 1, "spruce_log[axis=y]");
                p.set(x, y, z0 - 3, "spruce_log[axis=y]");
            }
            p.set(x, 5, z0 - 1, "lantern[hanging=true]");
        }
        for (int lane = 0; lane < Range.LANES; lane++) {
            int lx = Range.laneX(lane);
            p.set(lx, 1, z0, lane % 2 == 0 ? "red_carpet" : "orange_carpet");
            // Linien zwischen den Bahnen
            int bx = lx + Range.SPACING / 2;
            if (lane + 1 < Range.LANES) {
                for (int z = z0 + 2; z < Range.BACKSTOP_Z; z++) {
                    p.set(bx, 0, z, "white_concrete");
                }
            }
            for (int d : Range.DISTANCES) {
                p.set(lx - 3, 0, z0 + d - 1, "yellow_concrete");
            }
        }
        int[] s = Places.STAND_BOGEN;
        Stands.alongX(p, s[0] - 20, s[0] + 20, s[1] - 2, 4, false);
        return p;
    }
}

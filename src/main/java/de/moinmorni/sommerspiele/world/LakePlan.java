package de.moinmorni.sommerspiele.world;

import de.moinmorni.sommerspiele.course.Lake;
import de.moinmorni.sommerspiele.course.Places;
import java.util.Random;

/**
 * Der Bergsee mit der Regattastrecke: vier Bahnen, dazwischen Holzstege mit Laternen, am Nordende der Startsteg, Bojen
 * (rot links, gelb rechts, mit Laterne), Strudel (Magma am Grund zieht hinab, Seerosen warnen), blaue Fliesen an den
 * Stegen bei den Strömungen, der Zielbogen und die Tribüne am Südufer, ein Bootshaus.
 */
final class LakePlan {

    private LakePlan() {
    }

    static Plan build() {
        Plan p = new Plan();
        Random r = new Random(4711);
        for (int x = Lake.X1 - 3; x <= Lake.X2 + 3; x++) {
            for (int z = Lake.Z1 - 3; z <= Lake.Z2 + 3; z++) {
                if (Lake.water(x, z)) {
                    for (int y = 0; y > -Lake.DEPTH; y--) {
                        p.set(x, y, z, "water");
                    }
                    p.set(x, -Lake.DEPTH, z, r.nextInt(5) == 0 ? "gravel" : r.nextInt(3) == 0 ? "clay" : "sand");
                    p.air(x, 1, z);
                    p.air(x, 2, z);
                } else if (Lake.dock(x, z)) {
                    p.set(x, 0, z, "spruce_planks");
                    p.set(x, -1, z, "spruce_log[axis=y]");
                    p.air(x, 1, z);
                    p.air(x, 2, z);
                } else if (x >= Lake.X1 - 3 && x <= Lake.X2 + 3) {
                    // Ufer: Sand und Kies
                    p.set(x, 0, z, r.nextInt(3) == 0 ? "gravel" : "sand");
                    p.air(x, 1, z);
                }
            }
        }
        for (int lane = 0; lane < Lake.lanes(); lane++) {
            int cx = Lake.LANE_X[lane];
            for (int g = 0; g < Lake.GATES; g++) {
                int z = Lake.gateRow(g);
                for (int bx : Lake.buoys(lane, g)) {
                    p.set(bx, 0, z, Lake.leftGate(g) ? "red_wool" : "yellow_wool");
                    p.set(bx, 1, z, "lantern[hanging=false]");
                }
            }
            for (int[] w : Lake.whirlpools(lane)) {
                p.set(w[0], -Lake.DEPTH, w[1], "magma_block");
                for (int y = -Lake.DEPTH + 1; y <= 0; y++) {
                    p.set(w[0], y, w[1], "bubble_column[drag=true]");
                }
                for (int[] o : new int[][] {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
                    if (Lake.inLane(lane, w[0] + o[0], w[1] + o[1])) {
                        p.set(w[0] + o[0], 1, w[1] + o[1], "lily_pad");
                    }
                }
            }
            // Laternen auf dem Steg östlich der Bahn
            int sx = cx + Lake.HALF + 2;
            for (int z = Lake.START_DOCK_Z + 4; z < Lake.FINISH_Z; z += 12) {
                p.set(sx, 1, z, "spruce_fence");
                p.set(sx, 2, z, "lantern[hanging=false]");
            }
            // Strömungen: blaue Fliesen auf den Stegen links und rechts
            for (Lake.Current c : Lake.CURRENTS) {
                for (int z = c.z1(); z <= c.z2(); z++) {
                    String face = c.dir() > 0 ? "east" : "west";
                    p.set(cx - Lake.HALF - 1, 0, z, "light_blue_glazed_terracotta[facing=" + face + "]");
                    p.set(cx + Lake.HALF + 1, 0, z, "light_blue_glazed_terracotta[facing=" + face + "]");
                }
            }
            // Startlinie auf dem Steg markiert
            p.set(cx - Lake.HALF - 1, 0, (int) Math.floor(Lake.START_Z), "white_concrete");
            p.set(cx + Lake.HALF + 1, 0, (int) Math.floor(Lake.START_Z), "white_concrete");
            Stands.finishArch(p, cx - Lake.HALF - 1, cx + Lake.HALF + 1, (int) Math.floor(Lake.FINISH_Z), 6);
            p.banner(cx, 3, Lake.START_DOCK_Z - 2, "light_blue_banner[rotation=8]", Decor.lozenges());
        }
        int[] s = Places.STAND_REGATTA;
        Stands.alongX(p, s[0] - 20, s[0] + 20, s[1], 5, true);
        // Bootshaus am Nordwestufer
        Decor.chalet(p, Lake.X1 - 14, Lake.Z1 + 2, Lake.X1 - 6, Lake.Z1 + 10, 0, false, r);
        return p;
    }
}

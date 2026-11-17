package de.moinmorni.sommerspiele.world;

import de.moinmorni.sommerspiele.course.Alm;
import de.moinmorni.sommerspiele.course.Path;
import de.moinmorni.sommerspiele.course.Places;
import java.util.Random;

/**
 * Der Almlauf: Trampelpfad mit Zaun links und rechts, Heuballen-Hürden, Baumstamm, Bach mit Trittsteinen, Moor mit
 * Bohlen, Gatter an der Kuhweide (mit Tränke und Heustadel), Holzstöße im Slalom, Heutreppe, Start- und Zielbogen und
 * eine Tribüne an der Weststrecke.
 */
final class AlmPlan {

    private AlmPlan() {
    }

    static Plan build() {
        Plan p = new Plan();
        Random r = new Random(1312);
        Path loop = Alm.loop();
        int x1 = Alm.WEST_X - Alm.FENCE - 2;
        int x2 = Alm.EAST_X + Alm.FENCE + 2;
        int z1 = Alm.NORTH_Z - Alm.FENCE - 2;
        int z2 = Alm.SOUTH_Z + Alm.FENCE + 2;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                double d = loop.distance(x + 0.5, z + 0.5);
                if (d <= Alm.HALF + 0.5) {
                    p.set(x, 0, z, r.nextInt(4) == 0 ? "coarse_dirt" : "dirt_path");
                    p.air(x, 1, z);
                    p.air(x, 2, z);
                } else if (d <= Alm.HALF + 1.5) {
                    p.set(x, 0, z, "grass_block[snowy=false]");
                    p.set(x, 1, z, "oak_fence");
                    p.air(x, 2, z);
                }
            }
        }
        for (Alm.Obstacle o : Alm.obstacles()) {
            for (int x = o.x1(); x <= o.x2(); x++) {
                for (int z = o.z1(); z <= o.z2(); z++) {
                    obstacle(p, x, z, o.kind());
                }
            }
        }
        // Bach läuft unter dem Zaun weiter (hübscher): links und rechts ein Stück Wasser
        for (int z = 104; z <= 108; z++) {
            for (int x = Alm.EAST_X - Alm.FENCE - 6; x <= Alm.EAST_X + Alm.FENCE + 6; x++) {
                if (Math.abs(x - Alm.EAST_X) > Alm.FENCE) {
                    p.set(x, -1, z, "gravel");
                    p.set(x, 0, z, "water");
                    p.air(x, 1, z);
                }
            }
        }
        // Start- und Zielbogen
        Stands.finishArch(p, Alm.WEST_X - Alm.FENCE, Alm.WEST_X + Alm.FENCE, (int) Math.floor(Alm.LAP_Z), 6);
        // Kuhweide: Tränke und Heustadel südlich am Zaun
        int[] w = Alm.PASTURE;
        p.set(w[0] + 8, 1, w[3] + 3, "cauldron");
        p.set(w[0] + 9, 1, w[3] + 3, "water_cauldron[level=3]");
        Decor.chalet(p, w[0] + 14, w[3] + 3, w[0] + 22, w[3] + 9, 0, false, r);
        int[] s = Places.STAND_ALM;
        Stands.alongZ(p, s[1] - 6, s[1] + 6, s[0] + 2, 4);
        return p;
    }

    private static void obstacle(Plan p, int x, int z, Alm.Kind k) {
        switch (k) {
            case HAY, STEP1 -> p.set(x, 1, z, "hay_block[axis=y]");
            case STEP2 -> {
                p.set(x, 1, z, "hay_block[axis=y]");
                p.set(x, 2, z, "hay_block[axis=y]");
            }
            case LOG -> p.set(x, 1, z, "stripped_oak_log[axis=z]");
            case GATE -> p.set(x, 1, z, "spruce_log[axis=z]");
            case STREAM -> {
                p.set(x, -1, z, "gravel");
                p.set(x, 0, z, "water");
            }
            case STONE -> p.set(x, 0, z, "mossy_cobblestone");
            case MOOR -> {
                p.set(x, -1, z, "mud");
                p.set(x, 0, z, "soul_sand");
            }
            case PLANK -> p.set(x, 0, z, "spruce_planks");
            case PILE -> {
                p.set(x, 1, z, "oak_log[axis=x]");
                p.set(x, 2, z, "oak_log[axis=x]");
            }
            default -> {
            }
        }
    }
}

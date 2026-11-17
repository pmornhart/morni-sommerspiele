package de.moinmorni.sommerspiele.world;

import de.moinmorni.sommerspiele.course.Places;
import java.util.Random;

/**
 * Das Sommerdorf (Wartebereich): Dorfplatz mit weiß-blauem Maibaum, Siegertreppchen mit Tafeln, Almhütten mit
 * Geranien, Biergarten unter Kastanien, Blumenbeete, Wege zu allen Strecken, Laternen.
 */
final class VillagePlan {

    private static final int POLE_HEIGHT = 22;

    private VillagePlan() {
    }

    static Plan build() {
        Plan p = new Plan();
        Random r = new Random(2412);
        paths(p, r);
        plaza(p, r);
        maypole(p);
        podium(p);
        boards(p);
        Decor.chalet(p, -34, -24, -24, -16, 0, true, r);
        Decor.chalet(p, 24, -24, 34, -16, 0, true, r);
        Decor.chalet(p, 30, -4, 38, 6, 0, false, r);
        Decor.chalet(p, 28, 32, 36, 42, 0, false, r);
        Decor.chalet(p, -14, 36, -6, 44, 0, false, r);
        Decor.chalet(p, 6, 36, 14, 44, 0, false, r);
        Decor.chalet(p, -40, 34, -32, 42, 0, false, r);
        Decor.chalet(p, -44, -2, -36, 6, 0, true, r);
        beerGarden(p, r);
        trees(p, r);
        flowerBed(p, -22, 20, r);
        flowerBed(p, 18, 6, r);
        flowerBed(p, -8, 28, r);
        return p;
    }

    /** Gepflasterter Weg (Breite 3) von a nach b, mit Laternen alle zehn Blöcke. */
    static void path(Plan p, int x1, int z1, int x2, int z2, Random r) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : i / (double) steps;
            int cx = (int) Math.round(x1 + (x2 - x1) * t);
            int cz = (int) Math.round(z1 + (z2 - z1) * t);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int x = cx + dx;
                    int z = cz + dz;
                    p.set(x, 0, z, r.nextInt(5) == 0 ? "cobblestone" : r.nextInt(3) == 0 ? "gravel" : "dirt_path");
                    p.air(x, 1, z);
                }
            }
            if (i % 10 == 5) {
                int ox = Math.abs(z2 - z1) > Math.abs(x2 - x1) ? 2 : 0;
                int oz = ox == 0 ? 2 : 0;
                Decor.lanternPost(p, cx + ox, 1, cz + oz);
            }
        }
    }

    private static void paths(Plan p, Random r) {
        // zur Seifenkiste (Ziel und Tribüne)
        path(p, -14, 2, -48, -14, r);
        path(p, -48, -14, -120, -14, r);
        // zum See (Startsteg und Tribüne)
        path(p, 14, 2, 46, -10, r);
        path(p, 46, -10, 72, -28, r);
        path(p, 46, -10, 66, 40, r);
        path(p, 66, 40, 70, 150, r);
        path(p, 70, 150, 84, 168, r);
        // zum Schießplatz
        path(p, -14, 16, -60, 18, r);
        path(p, -60, 18, -94, 24, r);
        // zum Almlauf (Tribüne im Westen)
        path(p, 0, 22, 0, 40, r);
        path(p, 0, 40, -40, 66, r);
        path(p, -40, 66, -46, 110, r);
    }

    private static void plaza(Plan p, Random r) {
        int cz = Places.PLAZA_Z;
        int rad = Places.PLAZA_R;
        for (int x = -rad - 1; x <= rad + 1; x++) {
            for (int z = cz - rad - 1; z <= cz + rad + 1; z++) {
                double d = Math.hypot(x, z - cz);
                if (d > rad + 0.5) {
                    continue;
                }
                String block;
                if (d > rad - 2) {
                    block = r.nextInt(4) == 0 ? "mossy_stone_bricks" : "stone_bricks";
                } else if (d > rad - 3) {
                    block = "polished_andesite";
                } else {
                    block = Decor.lozenge(x, z, 2) ? "stone_bricks" : "polished_diorite";
                }
                p.set(x, 0, z, block);
                p.air(x, 1, z);
            }
        }
        // Bänke und Laternen rund um den Platz
        for (int a = 0; a < 360; a += 45) {
            double rad2 = Math.toRadians(a + 22.5);
            int x = (int) Math.round(Math.sin(rad2) * (rad - 2));
            int z = cz + (int) Math.round(Math.cos(rad2) * (rad - 2));
            Decor.lanternPost(p, x, 1, z);
        }
        Decor.bench(p, -4, cz + 11, 4, cz + 11, 1, "south");
        Decor.bench(p, -11, cz - 3, -11, cz + 3, 1, "west");
        Decor.bench(p, 11, cz - 3, 11, cz + 3, 1, "east");
    }

    /**
     * Maibaum: weiß-blau gewundener Stamm, oben ein grüner Kranz mit Bändern und eine Fahne, an den Seiten die
     * Zunftzeichen (eigene Sinnbilder der vier Disziplinen: Lore, Boot, Scheibe, Heuballen).
     */
    private static void maypole(Plan p) {
        int tx = Places.MAYPOLE[0];
        int tz = Places.MAYPOLE[1];
        int top = POLE_HEIGHT;
        p.set(tx, 0, tz, "stone_bricks");
        for (int y = 1; y <= top; y++) {
            p.set(tx, y, tz, Math.floorMod(y, 4) < 2 ? "white_concrete" : "light_blue_concrete");
        }
        // Kranz
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                double d = Math.hypot(dx, dz);
                if (d > 1.2 && d < 2.6) {
                    p.set(tx + dx, top - 2, tz + dz, "azalea_leaves[persistent=true]");
                }
            }
        }
        for (int[] o : new int[][] {{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) {
            for (int y = 1; y <= 3; y++) {
                p.set(tx + o[0], top - 2 - y, tz + o[1], y % 2 == 0 ? "light_blue_wool" : "white_wool");
            }
        }
        p.set(tx, top + 1, tz, "spruce_fence");
        p.set(tx, top + 2, tz, "spruce_fence");
        p.set(tx, top + 3, tz, "gold_block");
        // Zunftzeichen: Querbalken mit Sinnbildern
        String[] signs = {"oak_planks", "light_blue_concrete", "red_concrete", "hay_block"};
        for (int i = 0; i < 4; i++) {
            int y = top - 6 - i * 4;
            int ox = i % 2 == 0 ? 1 : 0;
            int oz = i % 2 == 0 ? 0 : 1;
            for (int k = 1; k <= 3; k++) {
                p.set(tx + ox * k, y, tz + oz * k, "spruce_fence");
                p.set(tx - ox * k, y, tz - oz * k, "spruce_fence");
            }
            p.set(tx + ox * 4, y, tz + oz * 4, signs[i]);
            p.set(tx - ox * 4, y, tz - oz * 4, signs[i]);
        }
        // Blumen um den Fuß
        String[] flowers = {"red_tulip", "poppy", "cornflower", "oxeye_daisy", "dandelion", "allium", "pink_tulip"};
        int i = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if ((dx != 0 || dz != 0) && Math.abs(dx) + Math.abs(dz) <= 3) {
                    p.set(tx + dx, 0, tz + dz, "grass_block[snowy=false]");
                    p.set(tx + dx, 1, tz + dz, flowers[i++ % flowers.length]);
                }
            }
        }
    }

    /** Siegertreppchen mit Rückwand in Weiß-Blau. */
    private static void podium(Plan p) {
        int cx = Places.PODIUM[0];
        int cz = Places.PODIUM[1];
        int[][] steps = {{cx - 1, 3, 1}, {cx - 5, 2, 2}, {cx + 3, 1, 3}};
        String[] carpets = {"yellow_carpet", "light_gray_carpet", "orange_carpet"};
        for (int[] s : steps) {
            for (int x = s[0]; x <= s[0] + 2; x++) {
                for (int z = cz - 1; z <= cz + 1; z++) {
                    for (int y = 1; y <= s[1]; y++) {
                        p.set(x, y, z, y == s[1] ? "smooth_quartz" : "quartz_bricks");
                    }
                    p.set(x, s[1] + 1, z, carpets[s[2] - 1]);
                }
            }
        }
        for (int x = cx - 7; x <= cx + 7; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                p.set(x, 0, z, "polished_andesite");
            }
            for (int y = 1; y <= 7; y++) {
                boolean frame = Math.abs(x - cx) == 7 || y == 7;
                p.set(x, y, cz - 3, frame ? "stripped_spruce_log[axis=y]"
                        : Decor.lozenge(x, y, 2) ? "light_blue_concrete" : "white_concrete");
            }
            p.set(x, 8, cz - 3, "spruce_slab[type=bottom]");
            if (Math.floorMod(x, 3) == 0) {
                p.set(x, 9, cz - 3, "potted_red_tulip");
            }
        }
        p.banner(cx - 4, 6, cz - 2, "green_wall_banner[facing=south]", Decor.emerald());
        p.banner(cx + 4, 6, cz - 2, "yellow_wall_banner[facing=south]", Decor.gold());
        p.set(cx - 7, 8, cz - 3, "lantern[hanging=false]");
        p.set(cx + 7, 8, cz - 3, "lantern[hanging=false]");
    }

    /** Holzrahmen für die beiden Tafeln (Medaillenspiegel und Bestzeiten). */
    private static void boards(Plan p) {
        for (double[] b : new double[][] {Places.BOARD_MEDALS, Places.BOARD_TIMES}) {
            int bx = (int) Math.floor(b[0]);
            int bz = (int) Math.floor(b[2]) - 2;
            for (int x = bx - 4; x <= bx + 4; x++) {
                for (int y = 1; y <= 7; y++) {
                    boolean frame = Math.abs(x - bx) == 4 || y == 7 || y == 1;
                    p.set(x, y, bz, frame ? "spruce_log[axis=y]" : "dark_oak_planks");
                }
                p.set(x, 8, bz, "spruce_slab[type=bottom]");
            }
        }
    }

    /** Linden, Birken und ein paar Fichten am Dorfrand und zwischen den Hütten. */
    private static void trees(Plan p, Random r) {
        int[][] spots = {{-22, -6}, {20, -6}, {-48, 14}, {44, 18}, {-24, 44}, {22, 48}, {-6, -24}, {12, -26},
            {-50, 30}, {46, -2}, {40, 46}, {-18, 54}, {4, 54}, {-46, -18}, {44, -22}};
        for (int i = 0; i < spots.length; i++) {
            int[] s = spots[i];
            if (i % 4 == 3) {
                Decor.spruce(p, s[0], 1, s[1], 7 + r.nextInt(4), r);
            } else {
                Decor.broadleaf(p, s[0], 1, s[1], 4 + r.nextInt(3), i % 3 == 0, r);
            }
        }
    }

    /** Rundes Blumenbeet mit Holzrand. */
    private static void flowerBed(Plan p, int cx, int cz, Random r) {
        String[] flowers = {"red_tulip", "poppy", "cornflower", "oxeye_daisy", "dandelion", "allium", "pink_tulip",
            "azure_bluet", "orange_tulip"};
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                double d = Math.hypot(dx, dz);
                if (d > 3.3) {
                    continue;
                }
                if (d > 2.4) {
                    p.set(cx + dx, 1, cz + dz, "spruce_slab[type=bottom]");
                } else {
                    p.set(cx + dx, 0, cz + dz, "grass_block[snowy=false]");
                    p.set(cx + dx, 1, cz + dz, flowers[r.nextInt(flowers.length)]);
                }
            }
        }
        p.set(cx, 1, cz, "rose_bush[half=lower]");
        p.set(cx, 2, cz, "rose_bush[half=upper]");
    }

    /**
     * Biergarten unter Kastanien: Holzboden aus Kies, lange Tische (Zaun mit Platte) mit Bänken, ein Ausschank mit
     * Fässern und rot-weißem Dach, Lichterkette.
     */
    private static void beerGarden(Plan p, Random r) {
        int cx = Places.BEERGARDEN[0];
        int cz = Places.BEERGARDEN[1];
        for (int x = cx - 9; x <= cx + 9; x++) {
            for (int z = cz - 7; z <= cz + 7; z++) {
                p.set(x, 0, z, r.nextInt(4) == 0 ? "coarse_dirt" : "gravel");
                p.air(x, 1, z);
            }
        }
        // Tische mit Bänken
        for (int tz : new int[] {cz - 4, cz, cz + 4}) {
            for (int x = cx - 6; x <= cx + 1; x++) {
                p.set(x, 1, tz, "spruce_fence");
                p.set(x, 2, tz, "spruce_pressure_plate");
                p.set(x, 1, tz - 1, "spruce_stairs[facing=north,half=bottom,shape=straight]");
                p.set(x, 1, tz + 1, "spruce_stairs[facing=south,half=bottom,shape=straight]");
            }
            p.set(cx - 4, 3, tz, "decorated_pot[facing=south]");
        }
        // Ausschank
        int bx = cx + 6;
        for (int z = cz - 3; z <= cz + 3; z++) {
            p.set(bx, 1, z, "spruce_planks");
            p.set(bx, 2, z, "spruce_slab[type=bottom]");
            p.set(bx + 2, 1, z, "barrel[facing=west,open=false]");
            p.set(bx + 2, 2, z, Math.floorMod(z, 2) == 0 ? "barrel[facing=up,open=false]" : "spruce_planks");
            for (int x = bx - 1; x <= bx + 3; x++) {
                p.set(x, 5, z, Math.floorMod(x + z, 2) == 0 ? "red_wool" : "white_wool");
            }
        }
        for (int[] q : new int[][] {{bx - 1, cz - 3}, {bx - 1, cz + 3}, {bx + 3, cz - 3}, {bx + 3, cz + 3}}) {
            for (int y = 1; y <= 4; y++) {
                p.set(q[0], y, q[1], "spruce_fence");
            }
        }
        p.set(bx + 1, 1, cz, "cauldron");
        // Kastanien (Laubbäume) an den Ecken
        Decor.broadleaf(p, cx - 9, 1, cz - 7, 5, false, r);
        Decor.broadleaf(p, cx - 9, 1, cz + 7, 5, false, r);
        // Lichterkette quer über den Garten
        for (int x = cx - 8; x <= cx + 4; x++) {
            p.set(x, 6, cz, "iron_chain[axis=x]");
            if (Math.floorMod(x, 3) == 0) {
                p.set(x, 5, cz, "lantern[hanging=true]");
            }
        }
        p.set(cx - 9, 6, cz, "oak_log[axis=y]");
        p.set(cx + 5, 6, cz, "spruce_fence");
        for (int y = 1; y <= 5; y++) {
            p.set(cx + 5, y, cz, "spruce_fence");
        }
    }
}

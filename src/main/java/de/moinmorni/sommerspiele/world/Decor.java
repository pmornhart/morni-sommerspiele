package de.moinmorni.sommerspiele.world;

import java.util.List;
import java.util.Random;
import org.bukkit.DyeColor;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;

/**
 * Bausteine für das Sommerdorf: Laubbäume und Fichten, Almhütten mit Holzschindeldach und Geranien, Laternen, Bänke,
 * Blumenwiesen, Banner in unseren Farben. Alles eigene Entwürfe; feste Zufallszahlen, damit jeder Neubau gleich aussieht.
 */
final class Decor {

    static final String GRASS = "grass_block[snowy=false]";
    static final String LEAVES = "spruce_leaves[persistent=true]";
    static final String OAK_LEAVES = "oak_leaves[persistent=true]";
    static final String BIRCH_LEAVES = "birch_leaves[persistent=true]";
    private static final String[] FLOWERS = {"poppy", "dandelion", "cornflower", "oxeye_daisy", "azure_bluet",
        "allium", "red_tulip", "orange_tulip", "white_tulip", "pink_tulip", "lily_of_the_valley"};

    private Decor() {
    }

    static String pick(Random r, String... options) {
        return options[r.nextInt(options.length)];
    }

    /** Fels mit etwas Abwechslung. */
    static String rock(Random r) {
        int roll = r.nextInt(100);
        return roll < 50 ? "stone" : roll < 68 ? "andesite" : roll < 82 ? "tuff" : roll < 94 ? "cobblestone" : "gravel";
    }

    /** Blume oder Gras für die Wiese (oder nichts). */
    static String meadow(Random r, int density) {
        int roll = r.nextInt(100);
        if (roll < density / 3) {
            return FLOWERS[r.nextInt(FLOWERS.length)];
        }
        if (roll < density) {
            return r.nextInt(5) == 0 ? "fern" : "short_grass";
        }
        return null;
    }

    /** Fichte: Stamm, kegelförmige Krone aus Ringen (unten breit). {@code y} = Höhe, auf der der Stamm steht. */
    static void spruce(Plan p, int x, int y, int z, int height, Random r) {
        int trunk = Math.max(2, height / 4);
        for (int i = 0; i < height - 1; i++) {
            p.set(x, y + i, z, "spruce_log[axis=y]");
        }
        int top = y + height;
        p.set(x, top - 1, z, LEAVES);
        p.set(x, top, z, LEAVES);
        int maxR = Math.max(2, Math.min(4, height / 3));
        for (int ly = top - 1; ly >= y + trunk; ly--) {
            int fromTop = top - ly;
            int rad = Math.min(maxR, 1 + fromTop / 2);
            if (fromTop % 2 == 1) {
                rad = Math.max(1, rad - 1);
            }
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    double d = Math.hypot(dx, dz);
                    if (d > rad + 0.35 || (d > rad - 0.4 && r.nextInt(4) == 0)) {
                        continue;
                    }
                    p.setIfEmpty(x + dx, ly, z + dz, LEAVES);
                }
            }
        }
    }

    /** Laubbaum (Eiche oder Birke): Stamm und runde Krone. */
    static void broadleaf(Plan p, int x, int y, int z, int height, boolean birch, Random r) {
        String log = birch ? "birch_log[axis=y]" : "oak_log[axis=y]";
        String leaves = birch ? BIRCH_LEAVES : OAK_LEAVES;
        for (int i = 0; i < height; i++) {
            p.set(x, y + i, z, log);
        }
        int cy = y + height - 1;
        double rad = birch ? 2.2 : 2.8 + r.nextDouble() * 0.6;
        int ir = (int) Math.ceil(rad);
        for (int dx = -ir; dx <= ir; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -ir; dz <= ir; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy * 1.8 + dz * dz);
                    if (d <= rad && !(d > rad - 0.6 && r.nextInt(3) == 0)) {
                        p.setIfEmpty(x + dx, cy + dy, z + dz, leaves);
                    }
                }
            }
        }
        p.setIfEmpty(x, cy + 3, z, leaves);
    }

    /** Laternenpfahl: drei Zaunpfosten, oben eine Laterne. */
    static void lanternPost(Plan p, int x, int y, int z) {
        for (int i = 0; i < 3; i++) {
            p.set(x, y + i, z, "spruce_fence");
        }
        p.set(x, y + 3, z, "lantern[hanging=false]");
    }

    /** Hängende Laterne an einer Kette. */
    static void hangingLantern(Plan p, int x, int y, int z, int chain) {
        for (int i = 0; i < chain; i++) {
            p.set(x, y - i, z, "iron_chain[axis=y]");
        }
        p.set(x, y - chain, z, "lantern[hanging=true]");
    }

    /** Bank aus Treppen; {@code facing} = wohin die Lehne zeigt (wer sitzt, schaut in die Gegenrichtung). */
    static void bench(Plan p, int x1, int z1, int x2, int z2, int y, String facing) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                p.set(x, y, z, "spruce_stairs[facing=" + facing + ",half=bottom,shape=straight]");
            }
        }
    }

    /**
     * Almhütte: Steinsockel, Erdgeschoss weiß verputzt, Obergeschoss aus Holz mit Blumenbalkon, Satteldach aus dunklen
     * Holzschindeln, Fensterläden mit Geranien, Kamin, Lichter im Fenster.
     * Grundfläche x1…x2, z1…x2 auf der Höhe {@code y} (Boden). {@code doorSouth}: Tür nach Süden, sonst Norden.
     */
    static void chalet(Plan p, int x1, int z1, int x2, int z2, int y, boolean doorSouth, Random r) {
        String plaster = pick(r, "white_terracotta", "calcite", "smooth_quartz", "white_concrete");
        String wood = pick(r, "spruce", "dark_oak", "spruce");
        int h1 = 3;
        int h2 = 3;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
                p.set(x, y, z, edge ? "stone_bricks" : wood + "_planks");
                for (int dy = 1; dy <= h1 + h2; dy++) {
                    if (!edge) {
                        p.air(x, y + dy, z);
                        continue;
                    }
                    String block;
                    if (corner) {
                        block = wood + "_log[axis=y]";
                    } else if (dy == 1) {
                        block = "stone_bricks";
                    } else if (dy <= h1) {
                        block = plaster;
                    } else {
                        block = wood + "_planks";
                    }
                    p.set(x, y + dy, z, block);
                }
                if (!edge) {
                    p.set(x, y + h1 + 1, z, wood + "_planks");
                }
            }
        }
        // Stube: Ofen, Tisch, Licht – durch die Fenster sieht man es leuchten.
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        p.set(x1 + 1, y + 1, z1 + 1, "furnace[facing=south,lit=true]");
        p.set(cx, y + 1, cz, "spruce_fence");
        p.set(cx, y + 2, cz, "spruce_pressure_plate");
        p.set(x2 - 1, y + 1, z2 - 1, "barrel[facing=up,open=false]");
        p.set(cx, y + h1, cz, "lantern[hanging=true]");
        // Fenster mit Läden und Blumenkasten (im Winter: Tannenzweige)
        for (int dy : new int[] {2, h1 + 2}) {
            for (int x = x1 + 2; x <= x2 - 2; x += 2) {
                window(p, x, y + dy, z1, "north", wood);
                window(p, x, y + dy, z2, "south", wood);
            }
            for (int z = z1 + 2; z <= z2 - 2; z += 2) {
                p.set(x1, y + dy, z, "glass_pane");
                p.set(x2, y + dy, z, "glass_pane");
            }
        }
        // Tür mit Laterne
        int dz = doorSouth ? z2 : z1;
        String face = doorSouth ? "south" : "north";
        int out = doorSouth ? 1 : -1;
        p.set(cx, y + 1, dz, wood + "_door[facing=" + face + ",half=lower,hinge=left,open=false]");
        p.set(cx, y + 2, dz, wood + "_door[facing=" + face + ",half=upper,hinge=left,open=false]");
        p.set(cx + 1, y + 3, dz + out, "lantern[hanging=false]");
        p.set(cx + 1, y + 2, dz + out, wood + "_fence");
        p.air(cx, y + 1, dz + out);
        // Balkon über der Tür
        int bz = dz + out;
        for (int x = x1 + 1; x <= x2 - 1; x++) {
            p.set(x, y + h1 + 1, bz, wood + "_slab[type=top]");
            p.set(x, y + h1 + 2, bz, wood + "_fence");
            if ((x - x1) % 2 == 0) {
                p.set(x, y + h1 + 3, bz, x % 4 == 0 ? "potted_red_tulip" : "potted_poppy");
            }
        }
        // Satteldach: First in z-Richtung, Holzrand und dunkle Schindeln
        int top = y + h1 + h2 + 1;
        for (int i = 0;; i++) {
            int left = x1 - 1 + i;
            int right = x2 + 1 - i;
            int ry = top + i - 1;
            if (left > right) {
                break;
            }
            for (int z = z1 - 1; z <= z2 + 1; z++) {
                boolean rim = z == z1 - 1 || z == z2 + 1;
                if (left == right) {
                    p.set(left, ry, z, rim ? wood + "_slab[type=bottom]" : "dark_oak_slab[type=bottom]");
                    continue;
                }
                String stairs = rim ? wood + "_stairs" : "dark_oak_stairs";
                p.set(left, ry, z, stairs + "[facing=east,half=bottom,shape=straight]");
                p.set(right, ry, z, stairs + "[facing=west,half=bottom,shape=straight]");
                if (z == z1 || z == z2) {
                    for (int x = left + 1; x < right; x++) {
                        p.set(x, ry, z, wood + "_planks");
                    }
                }
            }
            // Holzrand unter der Traufe
            if (i == 0) {
                for (int z = z1 - 1; z <= z2 + 1; z++) {
                    p.set(left, ry - 1, z, wood + "_trapdoor[facing=east,half=top,open=false]");
                    p.set(right, ry - 1, z, wood + "_trapdoor[facing=west,half=top,open=false]");
                }
            }
        }
        // Kamin mit Rauch
        int chimneyX = x2 - 1;
        for (int cy = top; cy <= top + 4; cy++) {
            p.set(chimneyX, cy, z1 + 1, "bricks");
        }
        p.set(chimneyX, top + 5, z1 + 1, "campfire[lit=true,signal_fire=false,facing=north]");
    }

    private static void window(Plan p, int x, int y, int z, String face, String wood) {
        p.set(x, y, z, "glass_pane");
        int out = "north".equals(face) ? -1 : 1;
        p.set(x, y - 1, z + out, wood + "_trapdoor[facing=" + face + ",half=top,open=false]");
        p.set(x, y, z + out, Math.floorMod(x, 3) == 0 ? "potted_red_tulip" : "potted_poppy");
        p.set(x - 1, y, z + out, wood + "_trapdoor[facing=" + face + ",half=bottom,open=true]");
        p.set(x + 1, y, z + out, wood + "_trapdoor[facing=" + face + ",half=bottom,open=true]");
    }

    /** Rautenmuster wie die weiß-blaue Fahne: schräg gestelltes Schachbrett. */
    static boolean lozenge(int x, int z, int size) {
        return Math.floorMod(Math.floorDiv(x + z, size) + Math.floorDiv(x - z, size), 2) == 0;
    }

    // ---------- Banner in unseren Farben (eigene Muster) ----------

    /** Weiß-blaue Rauten. */
    static List<Pattern> lozenges() {
        return List.of(new Pattern(DyeColor.LIGHT_BLUE, PatternType.RHOMBUS),
                new Pattern(DyeColor.LIGHT_BLUE, PatternType.TRIANGLES_TOP),
                new Pattern(DyeColor.LIGHT_BLUE, PatternType.TRIANGLES_BOTTOM));
    }

    /** Sonne: gelber Kreis auf Himmelblau. */
    static List<Pattern> sun() {
        return List.of(new Pattern(DyeColor.YELLOW, PatternType.CIRCLE),
                new Pattern(DyeColor.ORANGE, PatternType.FLOWER),
                new Pattern(DyeColor.LIGHT_BLUE, PatternType.BORDER));
    }

    /** Rot mit weißer Mitte (Torflagge, Start). */
    static List<Pattern> redWhite() {
        return List.of(new Pattern(DyeColor.WHITE, PatternType.STRIPE_CENTER),
                new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE),
                new Pattern(DyeColor.RED, PatternType.BORDER));
    }

    /** Gold-Banner (Ziel, Siegertreppchen). */
    static List<Pattern> gold() {
        return List.of(new Pattern(DyeColor.ORANGE, PatternType.GRADIENT_UP),
                new Pattern(DyeColor.YELLOW, PatternType.STRIPE_MIDDLE),
                new Pattern(DyeColor.ORANGE, PatternType.CURLY_BORDER));
    }

    /** Smaragd: grün mit hellgrüner Raute und goldenem Rand. */
    static List<Pattern> emerald() {
        return List.of(new Pattern(DyeColor.LIME, PatternType.RHOMBUS),
                new Pattern(DyeColor.GREEN, PatternType.CIRCLE),
                new Pattern(DyeColor.YELLOW, PatternType.BORDER));
    }
}

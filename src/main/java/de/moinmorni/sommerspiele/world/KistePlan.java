package de.moinmorni.sommerspiele.world;

import de.moinmorni.sommerspiele.course.Kiste;
import de.moinmorni.sommerspiele.course.Path;
import de.moinmorni.sommerspiele.course.Places;
import de.moinmorni.sommerspiele.course.Terrain;
import java.util.ArrayList;
import java.util.List;

/**
 * Die Seifenkisten-Bahn: Schienen auf einem Schotterbett den Berg hinab (gerade, schräg auf den Absätzen, Kurven auf
 * den Terrassen), Strohballen außen in jeder Kurve, an der Weiche ein Wegweiser, oben ein Starthaus je Bahn, unten der
 * Zielbogen, Prellböcke und die Tribüne.
 */
final class KistePlan {

    private KistePlan() {
    }

    static Plan build() {
        Plan p = new Plan();
        for (int lane = 0; lane < Kiste.lanes(); lane++) {
            rails(p, lane, false);
            rails(p, lane, true);
            startHouse(p, lane);
            int fx = (int) Math.floor(Kiste.finishX(lane));
            Stands.finishArch(p, fx - 3, fx + 3, (int) Math.floor(Kiste.FINISH_Z), 6);
            // Prellbock am Ende
            int ez = (int) Math.floor(Kiste.END_Z) + 1;
            p.set(fx, 1, ez, "oak_log[axis=x]");
            p.set(fx - 1, 1, ez, "hay_block[axis=y]");
            p.set(fx + 1, 1, ez, "hay_block[axis=y]");
            p.set(fx, 2, ez, "hay_block[axis=y]");
            fork(p, lane);
        }
        int[] s = Places.STAND_KISTE;
        Stands.alongX(p, s[0] - 22, s[0] + 22, s[1], 5, true);
        return p;
    }

    /** Schienen eines Wegs; die Kehren zuerst, das Steilstück nur, wo noch nichts liegt (Weiche). */
    private static void rails(Plan p, int lane, boolean steep) {
        Path path = Kiste.path(lane, steep);
        List<int[]> cells = new ArrayList<>();
        for (Path.Sample s : path.samples()) {
            int x = (int) Math.floor(s.x());
            int z = (int) Math.floor(s.z());
            if (cells.isEmpty() || cells.get(cells.size() - 1)[0] != x || cells.get(cells.size() - 1)[1] != z) {
                cells.add(new int[] {x, z});
            }
        }
        for (int i = 0; i < cells.size(); i++) {
            int[] c = cells.get(i);
            int[] prev = i > 0 ? cells.get(i - 1) : null;
            int[] next = i + 1 < cells.size() ? cells.get(i + 1) : null;
            String shape = shape(c, prev, next);
            int h = Terrain.height(c[1]);
            if (steep && p.has(c[0], h + 1, c[1])) {
                continue;
            }
            p.set(c[0], h, c[1], "gravel");
            p.set(c[0], h + 1, c[1], "rail[shape=" + shape + "]");
            p.air(c[0], h + 2, c[1]);
            p.air(c[0], h + 3, c[1]);
            // Schwellen: Holz links und rechts neben dem Schotter
            for (int d : new int[] {-1, 1}) {
                boolean alongZ = shape.startsWith("north_south") || shape.startsWith("ascending_n")
                        || shape.startsWith("ascending_s");
                int sx = alongZ ? c[0] + d : c[0];
                int sz = alongZ ? c[1] : c[1] + d;
                if (!p.has(sx, h + 1, sz)) {
                    p.setIfEmpty(sx, Terrain.height(sz), sz, "coarse_dirt");
                }
            }
        }
        // Strohballen außen in jeder Kurve
        for (double cs : Kiste.curves(lane, steep)) {
            Path.Sample a = path.at(cs - 1.5);
            Path.Sample b = path.at(cs + 1.5);
            double ox = (a.x() + b.x()) / 2 - path.at(cs).x();
            double oz = (a.z() + b.z()) / 2 - path.at(cs).z();
            double len = Math.hypot(ox, oz);
            if (len < 1e-6) {
                continue;
            }
            Path.Sample m = path.at(cs);
            for (int k = 2; k <= 3; k++) {
                int x = (int) Math.floor(m.x() - ox / len * k);
                int z = (int) Math.floor(m.z() - oz / len * k);
                int h = Terrain.height(z);
                if (!p.has(x, h + 1, z)) {
                    p.set(x, h + 1, z, "hay_block[axis=y]");
                    p.set(x, h + 2, z, "hay_block[axis=y]");
                }
            }
        }
    }

    /** Schienenform aus Vorgänger und Nachfolger (Minecraft-Namen). */
    static String shape(int[] c, int[] prev, int[] next) {
        List<String> dirs = new ArrayList<>();
        for (int[] o : new int[][] {prev, next}) {
            if (o == null) {
                continue;
            }
            if (o[1] < c[1]) {
                dirs.add("north");
            } else if (o[1] > c[1]) {
                dirs.add("south");
            } else if (o[0] > c[0]) {
                dirs.add("east");
            } else if (o[0] < c[0]) {
                dirs.add("west");
            }
        }
        boolean ns = dirs.stream().allMatch(d -> d.equals("north") || d.equals("south"));
        boolean ew = dirs.stream().allMatch(d -> d.equals("east") || d.equals("west"));
        if (ns) {
            return Terrain.ascending(c[1]) ? "ascending_north" : "north_south";
        }
        if (ew) {
            return "east_west";
        }
        boolean n = dirs.contains("north");
        boolean s = dirs.contains("south");
        boolean e = dirs.contains("east");
        return (n ? "north" : s ? "south" : "north") + "_" + (e ? "east" : "west");
    }

    /** Starthaus oben auf dem Plateau: Holzdach über dem Start, Banner, Laternen. */
    private static void startHouse(Plan p, int lane) {
        int x = Kiste.LANE_X[lane];
        int z0 = (int) Math.floor(Kiste.TOP_Z) - 1;
        int z1 = (int) Math.floor(Kiste.START_Z) + 2;
        int h = Terrain.TOP;
        for (int z = z0; z <= z1; z++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (Math.abs(dx) == 2 && (z == z0 || z == z1)) {
                    for (int y = 1; y <= 4; y++) {
                        p.set(x + dx, h + y, z, "spruce_log[axis=y]");
                    }
                }
                p.set(x + dx, h + 5, z, Math.abs(dx) == 2 ? "spruce_slab[type=bottom]" : "spruce_planks");
            }
        }
        p.banner(x - 2, h + 3, z1 + 1, "orange_wall_banner[facing=south]", Decor.sun());
        p.set(x + 2, h + 3, z1 + 1, "lantern[hanging=false]");
    }

    /** Wegweiser an der Weiche: links Schild „Steil“ (rot), rechts „Kehre“ (grün) als farbige Tafeln. */
    private static void fork(Plan p, int lane) {
        int side = Kiste.forkSide();
        int x = (int) Math.floor(Kiste.x(lane, side));
        double z = Terrain.terraceMiddle(Kiste.FORK);
        int zb = (int) Math.floor(z) - 6;
        int h = Terrain.height(zb);
        int px = side == 0 ? x - 2 : x + 2;
        for (int y = 1; y <= 3; y++) {
            p.set(px, h + y, zb, "spruce_fence");
        }
        p.set(px, h + 4, zb, "red_concrete");
        p.set(px + (side == 0 ? -1 : 1), h + 4, zb, "lime_concrete");
    }
}

package de.moinmorni.sommerspiele.world;

import de.moinmorni.sommerspiele.course.Places;
import de.moinmorni.sommerspiele.course.Terrain;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Der Boden der ganzen Welt: grünes Tal mit Blumenwiesen, der Kistenberg in Terrassen, ringsum Berge (im Norden hohe
 * Gipfel mit Felswänden und Schneeresten, an den Seiten und im Süden Almhänge, die das Tal abschließen). Nur eine Kruste
 * von ein paar Blöcken – von unten sieht es niemand. Gebaut in Streifen, damit der Server dabei flüssig bleibt.
 */
final class Ground {

    private static final int STRIP = 48;
    private static final int CRUST = 4;
    /** So hoch ist die unsichtbare Wand am Rand der Welt. */
    private static final int BORDER_HEIGHT = 10;

    private Ground() {
    }

    /** Baupläne (Streifen von West nach Ost), erst beim Bauen erzeugt. */
    static List<Supplier<Plan>> strips() {
        List<Supplier<Plan>> out = new ArrayList<>();
        for (int x = Places.MIN_X; x <= Places.MAX_X; x += STRIP) {
            int from = x;
            int to = Math.min(Places.MAX_X, x + STRIP - 1);
            out.add(() -> strip(from, to));
        }
        return out;
    }

    /** Boden für einen Ausschnitt (das Dorf wird zuerst gebaut). */
    static Plan area(int x1, int z1, int x2, int z2) {
        Plan all = strip(x1, x2);
        Plan out = new Plan();
        all.forEach((k, d) -> {
            int z = Plan.keyZ(k);
            if (z >= z1 && z <= z2) {
                out.set(Plan.keyX(k), Plan.keyY(k), z, d);
            }
        });
        return out;
    }

    private static double noise(int x, int z) {
        return Math.sin(x * 0.11 + 1.3) * Math.cos(z * 0.13 - 0.7) + 0.5 * Math.sin(x * 0.27 + z * 0.19)
                + 0.25 * Math.cos(x * 0.53 - z * 0.41);
    }

    /** Höhe der Oberfläche (über dem Tal) samt Bergen am Rand. */
    static int surface(int x, int z) {
        int h = Terrain.height(z);
        double extra = 0;
        if (x < Places.MIN_X + 14) {
            extra = Math.max(extra, (Places.MIN_X + 14 - x) * 1.5);
        }
        if (x > Places.MAX_X - 14) {
            extra = Math.max(extra, (x - Places.MAX_X + 14) * 1.5);
        }
        if (z > Places.MAX_Z - 14) {
            extra = Math.max(extra, (z - Places.MAX_Z + 14) * 1.2);
        }
        if (z < Places.MIN_Z + 6) {
            extra = Math.max(extra, (Places.MIN_Z + 6 - z) * 2.6);
        }
        if (extra > 0) {
            extra += (noise(x, z) + 1) * 3.0 * Math.min(1, extra / 6);
        }
        return h + (int) Math.round(extra);
    }

    private static Plan strip(int x1, int x2) {
        Plan p = new Plan();
        Random r = new Random(7331L * x1 + 17);
        for (int x = x1; x <= x2; x++) {
            for (int z = Places.MIN_Z; z <= Places.MAX_Z; z++) {
                int h = surface(x, z);
                int low = Math.min(Math.min(surface(x - 1, z), surface(x + 1, z)), Math.min(surface(x, z - 1), surface(x, z + 1)));
                int steep = h - low;
                boolean rocky = steep >= 3 && h > Terrain.TOP;
                boolean peak = h > Terrain.TOP + 22;
                int depth = Math.max(CRUST, steep + 3);
                for (int y = h - depth; y <= h; y++) {
                    String block;
                    if (y == h) {
                        block = peak && !rocky ? "snow_block" : rocky ? Decor.rock(r) : Decor.GRASS;
                    } else if (y >= h - 2 && !rocky) {
                        block = "dirt";
                    } else {
                        block = Decor.rock(r);
                    }
                    p.set(x, y, z, block);
                }
                boolean free = !Zones.reserved(x, z);
                if (free && !rocky && !peak) {
                    // Blumenwiese: dichter, wo das Rauschen hoch ist.
                    int density = noise(x * 2, z * 2) > 0.6 ? 45 : 18;
                    String plant = Decor.meadow(r, density);
                    if (plant != null) {
                        p.set(x, h + 1, z, plant);
                    }
                }
                if (x == Places.MIN_X || x == Places.MAX_X || z == Places.MIN_Z || z == Places.MAX_Z) {
                    for (int y = h + 1; y <= h + BORDER_HEIGHT; y++) {
                        p.set(x, y, z, "barrier");
                    }
                }
            }
        }
        return p;
    }
}

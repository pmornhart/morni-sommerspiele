package de.moinmorni.sommerspiele.world;

/** Zuschauertribünen aus Holz: Sitzreihen aus Treppen, die nach hinten ansteigen, mit Geländer und Fahnen. */
final class Stands {

    private Stands() {
    }

    /**
     * Tribüne mit Blick nach Norden (vorderste Reihe bei {@code zFront}, die Reihen steigen nach Süden an) oder mit
     * Blick nach Süden ({@code north = false}, Reihen steigen nach Norden an).
     */
    static void alongX(Plan p, int x1, int x2, int zFront, int rows, boolean north) {
        int dir = north ? 1 : -1;
        String facing = north ? "south" : "north";
        for (int i = 0; i < rows; i++) {
            int z = zFront + dir * i;
            for (int x = x1; x <= x2; x++) {
                for (int y = 1; y <= i; y++) {
                    p.set(x, y, z, "spruce_planks");
                }
                p.set(x, i + 1, z, "spruce_stairs[facing=" + facing + ",half=bottom,shape=straight]");
                p.air(x, i + 2, z);
                p.air(x, i + 3, z);
            }
        }
        int back = zFront + dir * rows;
        for (int x = x1 - 1; x <= x2 + 1; x++) {
            for (int y = 1; y <= rows + 1; y++) {
                p.set(x, y, back, "spruce_planks");
            }
            p.set(x, rows + 2, back, "spruce_fence");
        }
        for (int i = 0; i <= rows; i++) {
            int z = zFront + dir * i;
            for (int y = 1; y <= i + 2; y++) {
                p.set(x1 - 1, y, z, y <= i + 1 ? "spruce_planks" : "spruce_fence");
                p.set(x2 + 1, y, z, y <= i + 1 ? "spruce_planks" : "spruce_fence");
            }
        }
        for (int x = x1; x <= x2; x += 6) {
            for (int y = rows + 2; y <= rows + 6; y++) {
                p.set(x, y, back, "spruce_fence");
            }
            p.set(x, rows + 6, back + dir, Math.floorMod(x, 12) == 0 ? "light_blue_wool" : "white_wool");
            p.set(x, rows + 5, back + dir, Math.floorMod(x, 12) == 0 ? "white_wool" : "light_blue_wool");
        }
    }

    /** Tribüne mit Blick nach Osten: Reihen steigen nach Westen an (vorderste Reihe bei {@code xFront}). */
    static void alongZ(Plan p, int z1, int z2, int xFront, int rows) {
        for (int i = 0; i < rows; i++) {
            int x = xFront - i;
            for (int z = z1; z <= z2; z++) {
                for (int y = 1; y <= i; y++) {
                    p.set(x, y, z, "spruce_planks");
                }
                p.set(x, i + 1, z, "spruce_stairs[facing=west,half=bottom,shape=straight]");
                p.air(x, i + 2, z);
                p.air(x, i + 3, z);
            }
        }
        int back = xFront - rows;
        for (int z = z1 - 1; z <= z2 + 1; z++) {
            for (int y = 1; y <= rows + 1; y++) {
                p.set(back, y, z, "spruce_planks");
            }
            p.set(back, rows + 2, z, "spruce_fence");
        }
        for (int i = 0; i <= rows; i++) {
            int x = xFront - i;
            for (int y = 1; y <= i + 2; y++) {
                p.set(x, y, z1 - 1, y <= i + 1 ? "spruce_planks" : "spruce_fence");
                p.set(x, y, z2 + 1, y <= i + 1 ? "spruce_planks" : "spruce_fence");
            }
        }
    }

    /** Zielbogen quer über die Strecke: zwei Pfosten, oben ein Schachbrett-Balken. */
    static void finishArch(Plan p, int x1, int x2, int z, int height) {
        for (int y = 1; y < height; y++) {
            p.set(x1, y, z, "stripped_spruce_log[axis=y]");
            p.set(x2, y, z, "stripped_spruce_log[axis=y]");
        }
        for (int x = x1; x <= x2; x++) {
            p.set(x, height, z, Math.floorMod(x, 2) == 0 ? "black_wool" : "white_wool");
            p.set(x, height + 1, z, Math.floorMod(x, 2) == 0 ? "white_wool" : "black_wool");
        }
        p.set(x1, height + 2, z, "lantern[hanging=false]");
        p.set(x2, height + 2, z, "lantern[hanging=false]");
    }

    /** Zielbogen quer über eine Strecke in z-Richtung (Pfosten bei z1 und z2, Balken bei x). */
    static void finishArchZ(Plan p, int x, int z1, int z2, int height) {
        for (int y = 1; y < height; y++) {
            p.set(x, y, z1, "stripped_spruce_log[axis=y]");
            p.set(x, y, z2, "stripped_spruce_log[axis=y]");
        }
        for (int z = z1; z <= z2; z++) {
            p.set(x, height, z, Math.floorMod(z, 2) == 0 ? "black_wool" : "white_wool");
            p.set(x, height + 1, z, Math.floorMod(z, 2) == 0 ? "white_wool" : "black_wool");
        }
    }
}

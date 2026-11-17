package de.moinmorni.sommerspiele.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.Wall;

/**
 * Bauplan: welche Blöcke wohin (relativ zum Mittelpunkt, y = 0 ist die oberste Bodenschicht, man steht auf y = 1). Wird im Code
 * erzeugt – Dorf und Strecken entstehen bei jedem Neubau genau gleich (feste Zufallszahlen).
 */
public final class Plan {

    /** Banner mit Muster (Muster brauchen den fertigen Block). */
    public record Banner(int x, int y, int z, List<Pattern> patterns) {
    }

    private static final Map<String, BlockData> CACHE = new HashMap<>();
    private static final int OFFSET = 2048;
    private static final int BITS = 16;
    private static final int MASK = 0xFFFF;
    private static final BlockFace[] SIDES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    private final Map<Long, BlockData> blocks = new LinkedHashMap<>();
    private final List<Banner> banners = new ArrayList<>();
    private int minX = Integer.MAX_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private int maxZ = Integer.MIN_VALUE;

    /** Blockzustand aus Text wie {@code "spruce_stairs[facing=north]"} (zwischengespeichert, nicht verändern!). */
    public static BlockData data(String spec) {
        return CACHE.computeIfAbsent(spec, s -> Bukkit.createBlockData(s.indexOf(':') >= 0 ? s : "minecraft:" + s));
    }

    static long key(int x, int y, int z) {
        return ((long) (x + OFFSET) << (2 * BITS)) | ((long) (y + OFFSET) << BITS) | (z + OFFSET);
    }

    static int keyX(long k) {
        return (int) ((k >>> (2 * BITS)) & MASK) - OFFSET;
    }

    static int keyY(long k) {
        return (int) ((k >>> BITS) & MASK) - OFFSET;
    }

    static int keyZ(long k) {
        return (int) (k & MASK) - OFFSET;
    }

    public void set(int x, int y, int z, String spec) {
        set(x, y, z, data(spec));
    }

    public void set(int x, int y, int z, BlockData d) {
        if (d.getMaterial() == Material.AIR) {
            blocks.remove(key(x, y, z));
            return;
        }
        put(x, y, z, d);
    }

    /** Ausdrücklich Luft setzen (räumt beim Bauen weg, was ein früherer Bauplan dort hingestellt hat). */
    public void air(int x, int y, int z) {
        put(x, y, z, data("air"));
    }

    private void put(int x, int y, int z, BlockData d) {
        blocks.put(key(x, y, z), d);
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        minZ = Math.min(minZ, z);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
        maxZ = Math.max(maxZ, z);
    }

    /** Nur setzen, wenn dort noch nichts ist. */
    public void setIfEmpty(int x, int y, int z, String spec) {
        if (!blocks.containsKey(key(x, y, z))) {
            set(x, y, z, spec);
        }
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, String spec) {
        BlockData d = data(spec);
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, d);
                }
            }
        }
    }

    public void clear(int x, int y, int z) {
        blocks.remove(key(x, y, z));
    }

    public BlockData get(int x, int y, int z) {
        return blocks.get(key(x, y, z));
    }

    /** Ist an dieser Stelle etwas, auf dem man nicht stehen kann (fester Block)? */
    public boolean solid(int x, int y, int z) {
        BlockData d = blocks.get(key(x, y, z));
        return d != null && d.getMaterial().isSolid();
    }

    /** Ist an dieser Stelle schon etwas (auch ausdrücklich Luft)? */
    public boolean has(int x, int y, int z) {
        return blocks.containsKey(key(x, y, z));
    }

    /** Erste freie Höhe über dem Boden (für Startpunkte): von y = 1 aufwärts, bis zwei Blöcke frei sind. */
    public int standY(int x, int z) {
        int y = 1;
        while (y < 40 && (solid(x, y, z) || solid(x, y + 1, z))) {
            y++;
        }
        return y;
    }

    public void banner(int x, int y, int z, String blockSpec, List<Pattern> patterns) {
        set(x, y, z, blockSpec);
        banners.add(new Banner(x, y, z, List.copyOf(patterns)));
    }

    public List<Banner> banners() {
        return banners;
    }

    public void forEach(BiConsumer<Long, BlockData> action) {
        blocks.forEach(action);
    }

    public int size() {
        return blocks.size();
    }

    public int[] bounds() {
        return new int[] {minX, minY, minZ, maxX, maxY, maxZ};
    }

    /**
     * Zäune, Mauern, Scheiben und Gitter mit ihren Nachbarn verbinden – gebaut wird ohne Physik, sonst blieben sie
     * einzelne Pfosten.
     */
    public void connect() {
        Map<Long, BlockData> changed = new HashMap<>();
        blocks.forEach((k, d) -> {
            Material m = d.getMaterial();
            int x = keyX(k);
            int y = keyY(k);
            int z = keyZ(k);
            if (d instanceof Wall wall) {
                Wall w = (Wall) wall.clone();
                for (BlockFace f : SIDES) {
                    BlockData n = get(x + f.getModX(), y, z + f.getModZ());
                    w.setHeight(f, n != null && n.getMaterial() != Material.BARRIER
                            && (n instanceof Wall || n.getMaterial().isOccluding()) ? Wall.Height.LOW : Wall.Height.NONE);
                }
                BlockData above = get(x, y + 1, z);
                boolean straightX = w.getHeight(BlockFace.EAST) != Wall.Height.NONE
                        && w.getHeight(BlockFace.WEST) != Wall.Height.NONE
                        && w.getHeight(BlockFace.NORTH) == Wall.Height.NONE
                        && w.getHeight(BlockFace.SOUTH) == Wall.Height.NONE;
                boolean straightZ = w.getHeight(BlockFace.NORTH) != Wall.Height.NONE
                        && w.getHeight(BlockFace.SOUTH) != Wall.Height.NONE
                        && w.getHeight(BlockFace.EAST) == Wall.Height.NONE
                        && w.getHeight(BlockFace.WEST) == Wall.Height.NONE;
                w.setUp(above != null || !(straightX || straightZ));
                changed.put(k, w);
            } else if (d instanceof MultipleFacing mf && connectable(m)) {
                MultipleFacing c = (MultipleFacing) mf.clone();
                for (BlockFace f : SIDES) {
                    if (!c.getAllowedFaces().contains(f)) {
                        continue;
                    }
                    BlockData n = get(x + f.getModX(), y, z + f.getModZ());
                    c.setFace(f, n != null && joins(m, n.getMaterial()));
                }
                changed.put(k, c);
            }
        });
        blocks.putAll(changed);
    }

    private static boolean connectable(Material m) {
        return Tag.FENCES.isTagged(m) || m == Material.IRON_BARS || m.name().endsWith("GLASS_PANE");
    }

    /** Verbindet sich ein Zaun/eine Scheibe mit diesem Nachbarn? Unsichtbare Wände (Barrieren) nie. */
    private static boolean joins(Material self, Material other) {
        if (other == Material.BARRIER) {
            return false;
        }
        if (other.isOccluding()) {
            return true;
        }
        if (Tag.FENCES.isTagged(self)) {
            return Tag.FENCES.isTagged(other) || Tag.FENCE_GATES.isTagged(other);
        }
        return other == Material.IRON_BARS || other.name().endsWith("GLASS_PANE") || Tag.WALLS.isTagged(other);
    }
}

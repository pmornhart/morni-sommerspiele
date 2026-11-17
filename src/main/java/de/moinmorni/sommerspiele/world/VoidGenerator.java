package de.moinmorni.sommerspiele.world;

import de.moinmorni.sommerspiele.course.Places;
import de.moinmorni.sommerspiele.course.Terrain;
import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

/** Leere Welt (nur Himmel) mit Wiesen-Biom: sattes Grün, blaues Wasser. Das Sommerdorf baut das Plugin selbst. */
public final class VoidGenerator extends ChunkGenerator {

    private static final BiomeProvider SUMMER = new BiomeProvider() {
        @Override
        public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
            return Biome.PLAINS;
        }

        @Override
        public List<Biome> getBiomes(WorldInfo worldInfo) {
            return List.of(Biome.PLAINS);
        }
    };

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return SUMMER;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, Places.SPAWN[0], Terrain.FLOOR_Y + Places.SPAWN[1], Places.SPAWN[2], Places.SPAWN_YAW, 0f);
    }
}

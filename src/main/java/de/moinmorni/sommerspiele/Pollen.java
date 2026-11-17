package de.moinmorni.sommerspiele;

import org.bukkit.Particle;
import org.bukkit.entity.Player;

/** Leise schwebender Blütenstaub um jeden Spieler in der Sommerwelt (nur Anzeige, abschaltbar). */
final class Pollen {

    private static final int EVERY = 10;
    private static final int COUNT = 4;
    private static final double SPREAD = 7;

    private final SommerspielePlugin plugin;

    Pollen(SommerspielePlugin plugin) {
        this.plugin = plugin;
    }

    void tick(long tick) {
        if (!plugin.settings().pollen() || tick % EVERY != 0) {
            return;
        }
        for (Player p : plugin.world().getPlayers()) {
            p.spawnParticle(Particle.SPORE_BLOSSOM_AIR, p.getLocation().add(0, 2.5, 0), COUNT, SPREAD, 2.5, SPREAD, 0);
        }
    }
}

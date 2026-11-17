package de.moinmorni.sommerspiele.listen;

import de.moinmorni.sommerspiele.SommerspielePlugin;
import de.moinmorni.sommerspiele.game.ArcheryStage;
import de.moinmorni.sommerspiele.game.Competition;
import de.moinmorni.sommerspiele.game.KartRace;
import de.moinmorni.sommerspiele.game.RegattaRace;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

/** Boote (aussteigen = zurück an das letzte Tor), Loren (aussteigen geht im Lauf nicht) und Pfeile am Schießplatz. */
public final class RaceListener implements Listener {

    private final SommerspielePlugin plugin;

    public RaceListener(SommerspielePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExit(VehicleExitEvent e) {
        if (!(e.getExited() instanceof Player p)) {
            return;
        }
        Competition c = plugin.games().playing(p.getUniqueId());
        if (c != null && c.stage() instanceof RegattaRace race) {
            race.exited(p);
        } else if (c != null && c.stage() instanceof KartRace race && race.holds(p) && p.isOnline()) {
            e.setCancelled(true);
        }
    }

    /** Boote und Loren im Wettkampf schieben sich nicht gegenseitig (jedes hat seine eigene Bahn). */
    @EventHandler(ignoreCancelled = true)
    public void onCollide(VehicleEntityCollisionEvent e) {
        var tags = e.getVehicle().getScoreboardTags();
        if (tags.contains("sommerspiele_boat") || tags.contains("sommerspiele_kiste")) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player p) || !(e.getProjectile() instanceof AbstractArrow arrow)) {
            return;
        }
        Competition c = plugin.games().playing(p.getUniqueId());
        if (c != null && c.stage() instanceof ArcheryStage stage && stage.shot(p, arrow)) {
            return;
        }
        if (p.getWorld() == plugin.world() && !plugin.builder(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        Competition c = plugin.games().current();
        if (c != null && c.stage() instanceof ArcheryStage stage && stage.isOurs(arrow)) {
            if (e.getHitEntity() != null) {
                e.setCancelled(true);
                arrow.remove();
                stage.hit(arrow, null);
                return;
            }
            stage.hit(arrow, e.getHitBlock());
        }
    }
}

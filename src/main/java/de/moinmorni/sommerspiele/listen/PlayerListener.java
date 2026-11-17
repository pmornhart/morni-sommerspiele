package de.moinmorni.sommerspiele.listen;

import de.moinmorni.sommerspiele.Items;
import de.moinmorni.sommerspiele.Texts;
import de.moinmorni.sommerspiele.Ui;
import de.moinmorni.sommerspiele.SommerspielePlugin;
import de.moinmorni.sommerspiele.game.Competition;
import de.moinmorni.sommerspiele.village.VillageMenus;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Betreten, Verlassen, Knöpfe in der Schnellleiste. */
public final class PlayerListener implements Listener {

    private final SommerspielePlugin plugin;

    public PlayerListener(SommerspielePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        e.joinMessage(null);
        plugin.data(p);
        // Zurück ins Spiel: Wartet der Wettkampf noch auf diese Person, geht es dort weiter, wo sie war.
        Competition held = plugin.games().waitingFor(p.getUniqueId());
        if (held != null && held.rejoin(p)) {
            return;
        }
        plugin.village().enter(p);
        String lang = plugin.lang(p);
        if (!plugin.ready()) {
            p.sendMessage(Texts.msg(lang, "not-ready"));
        }
        p.sendMessage(Texts.msg(lang, "village.welcome"));
        p.sendMessage(Texts.msg(lang, "village.how"));
        if (!plugin.open()) {
            String date = plugin.reopening(lang);
            p.sendMessage(Texts.msg(lang, "village.closed", Placeholder.unparsed("date", date)));
            Ui.title(p, Texts.text(lang, "village.closed.title"), Texts.text(lang, "village.closed.sub",
                    Placeholder.unparsed("date", date)), 3500);
        } else {
            Ui.title(p, Texts.text(lang, "village.title"), Texts.text(lang, "village.subtitle"), 2500);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        e.quitMessage(null);
        plugin.village().leave(p.getUniqueId());
        Competition c = plugin.games().of(p.getUniqueId());
        if (c != null) {
            c.quit(p);
        }
        plugin.boards().sidebar().forget(p.getUniqueId());
        plugin.store().unload(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        String hall = Items.hallTag(item);
        if (hall != null) {
            e.setCancelled(true);
            switch (hall) {
                case "vote" -> VillageMenus.vote(plugin, p);
                case "ready" -> plugin.village().toggleReady(p);
                case "stats" -> VillageMenus.stats(plugin, p);
                case "lobby" -> {
                    // Das Netzwerk fragt nach, falls jemand gerade spielt (im Dorf geht es sofort).
                    p.sendMessage(Texts.msg(plugin.lang(p), "lobby.going"));
                    plugin.net().askToLobby(p, Texts.text(plugin.lang(p), "lobby.kick"));
                }
                default -> {
                }
            }
            return;
        }
        String kit = Items.kitTag(item);
        if (kit == null || "bow".equals(kit)) {
            return;
        }
        Competition c = plugin.games().of(p.getUniqueId());
        if (c == null) {
            return;
        }
        e.setCancelled(true);
        switch (kit) {
            case "reset" -> {
                if (c.stage() != null) {
                    c.stage().reset(p);
                }
            }
            case "giveup" -> c.giveUp(p);
            case "leave" -> c.leave(p);
            default -> {
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (e.getPlayer().getWorld() == plugin.world() && !plugin.builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (e.getPlayer().getWorld() == plugin.world() && !plugin.builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    /** Leibchen, Bogen und Knöpfe bleiben, wo sie sind. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p && p.getWorld() == plugin.world() && !plugin.builder(p)
                && !(e.getInventory().getHolder(false) instanceof de.moinmorni.sommerspiele.menu.Menu)) {
            e.setCancelled(true);
        }
    }
}

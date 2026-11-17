package de.moinmorni.sommerspiele.menu;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Ein Truhen-Menü: jeder Platz kann eine Aktion haben. Alles wird per einfachem Klick bedient – das klappt auch für
 * Bedrock-Spieler (Geyser zeigt Truhen als Truhen).
 */
public class Menu implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, Consumer<Player>> actions = new HashMap<>();
    /** Wird jede Sekunde neu gezeichnet, solange es offen ist. */
    private Runnable refresher;

    public Menu(int rows, Component title) {
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void set(int slot, ItemStack item, Consumer<Player> action) {
        inventory.setItem(slot, item);
        if (action == null) {
            actions.remove(slot);
        } else {
            actions.put(slot, action);
        }
    }

    public void clear() {
        inventory.clear();
        actions.clear();
    }

    public void refresher(Runnable r) {
        this.refresher = r;
    }

    public void refresh() {
        if (refresher != null) {
            refresher.run();
        }
    }

    public void open(Player p) {
        p.openInventory(inventory);
    }

    /** Klick: nichts darf verschoben werden; ein Klick oben löst die Aktion aus. */
    public void click(InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getClickedInventory() != inventory || !(e.getWhoClicked() instanceof Player p)) {
            return;
        }
        Consumer<Player> action = actions.get(e.getSlot());
        if (action != null) {
            action.accept(p);
        }
    }

    public void drag(InventoryDragEvent e) {
        e.setCancelled(true);
    }

    /** Menü wurde geschlossen. */
    public void closed(Player p) {
        // nichts
    }
}

package de.moinmorni.sommerspiele;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Gegenstände bauen und markieren (Markierung im Gegenstand, nicht am Namen – Namen hängen an der Sprache). */
public final class Items {

    /** Gegenstand im Wettkampf: „reset“, „giveup“, „leave“ … */
    private static NamespacedKey kitKey;
    /** Gegenstand im Dorf: Aktion wie „vote“ oder „lobby“. */
    private static NamespacedKey hallKey;

    private Items() {
    }

    static void init(Plugin plugin) {
        kitKey = new NamespacedKey(plugin, "item");
        hallKey = new NamespacedKey(plugin, "hall");
    }

    public static ItemStack named(Material material, int amount, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        item.editMeta(meta -> {
            if (name != null) {
                meta.displayName(name);
            }
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
        return item;
    }

    public static ItemStack glow(ItemStack item, boolean glow) {
        item.editMeta(meta -> meta.setEnchantmentGlintOverride(glow ? Boolean.TRUE : null));
        return item;
    }

    public static ItemStack kitTag(ItemStack item, String id) {
        item.editMeta(meta -> meta.getPersistentDataContainer().set(kitKey, PersistentDataType.STRING, id));
        return item;
    }

    public static String kitTag(ItemStack item) {
        return read(item, kitKey);
    }

    public static ItemStack hallTag(ItemStack item, String action) {
        item.editMeta(meta -> meta.getPersistentDataContainer().set(hallKey, PersistentDataType.STRING, action));
        return item;
    }

    public static String hallTag(ItemStack item) {
        return read(item, hallKey);
    }

    private static String read(ItemStack item, NamespacedKey key) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    /** Leere Glasscheibe als Rahmen in Menüs. */
    public static ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> meta.setHideTooltip(true));
        return item;
    }
}

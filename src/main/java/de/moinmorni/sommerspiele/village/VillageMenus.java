package de.moinmorni.sommerspiele.village;

import de.moinmorni.sommerspiele.Items;
import de.moinmorni.sommerspiele.Texts;
import de.moinmorni.sommerspiele.SommerspielePlugin;
import de.moinmorni.sommerspiele.data.Leaderboard;
import de.moinmorni.sommerspiele.data.PlayerData;
import de.moinmorni.sommerspiele.game.Competition;
import de.moinmorni.sommerspiele.logic.Discipline;
import de.moinmorni.sommerspiele.logic.MedalTable;
import de.moinmorni.sommerspiele.logic.Program;
import de.moinmorni.sommerspiele.menu.Menu;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Truhen-Menüs im Dorf (gehen auch mit Bedrock): Abstimmen/Zuschauen und Medaillen/Bestzeiten. */
public final class VillageMenus {

    private static final int[] PROGRAM_SLOTS = {9, 11, 13, 15, 17};
    private static final int WATCH_SLOT = 22;
    private static final int CLOSE_SLOT = 26;

    private VillageMenus() {
    }

    public static Material icon(Program p) {
        return switch (p) {
            case WETTKAMPF -> Material.GOLD_BLOCK;
            case ALMLAUF -> Material.HAY_BLOCK;
            case BOGEN -> Material.BOW;
            case REGATTA -> Material.OAK_BOAT;
            case SEIFENKISTE -> Material.MINECART;
        };
    }

    public static void vote(SommerspielePlugin plugin, Player p) {
        String lang = plugin.lang(p);
        Menu m = new Menu(3, Texts.text(lang, "menu.vote.title"));
        Runnable draw = () -> {
            m.clear();
            for (int i = 0; i < 27; i++) {
                m.set(i, Items.pane(Material.LIME_STAINED_GLASS_PANE), null);
            }
            Program mine = plugin.village().votes().voteOf(p.getUniqueId());
            Program[] all = Program.values();
            for (int i = 0; i < all.length; i++) {
                Program prog = all[i];
                List<Component> lore = new ArrayList<>();
                if (prog == Program.WETTKAMPF) {
                    lore.addAll(Texts.lines(lang, "program.wettkampf.desc"));
                } else {
                    Discipline disc = Discipline.byId(prog.id());
                    lore.addAll(Texts.lines(lang, "discipline." + prog.id() + ".desc",
                            Placeholder.unparsed("secs", String.valueOf(plugin.settings().regattaPenaltySeconds()))));
                    List<Leaderboard.Time> top = plugin.board().top(disc);
                    if (!top.isEmpty()) {
                        lore.add(Texts.text(lang, "menu.vote.best", Placeholder.unparsed("time", Texts.value(disc, top.get(0).ms(), lang)),
                                Placeholder.unparsed("name", top.get(0).name())));
                    }
                }
                lore.add(Texts.text(lang, "menu.vote.votes", Placeholder.unparsed("n", String.valueOf(plugin.village().votes().count(prog)))));
                if (!plugin.open()) {
                    lore.add(Texts.text(lang, "menu.vote.closed", Placeholder.unparsed("date", plugin.reopening(lang))));
                } else {
                    lore.add(Texts.text(lang, prog == mine ? "menu.vote.mine" : "menu.vote.click"));
                }
                ItemStack item = Items.glow(Items.named(icon(prog), 1,
                        Texts.parse("<em><b>" + Village.name(lang, prog) + "</b></em>"), lore), prog == mine);
                m.set(PROGRAM_SLOTS[i], item, q -> {
                    if (plugin.open()) {
                        plugin.village().vote(q, prog);
                        q.closeInventory();
                    }
                });
            }
            Competition c = plugin.games().current();
            if (c != null) {
                m.set(WATCH_SLOT, Items.named(Material.SPYGLASS, 1, Texts.text(lang, "menu.vote.watch"),
                        Texts.lines(lang, "menu.vote.watch.lore", Placeholder.unparsed("discipline",
                                Texts.plain(lang, "discipline." + c.discipline().id())))), q -> {
                                    q.closeInventory();
                                    plugin.games().spectate(q);
                                });
            }
            m.set(CLOSE_SLOT, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), q -> q.closeInventory());
        };
        draw.run();
        m.refresher(draw);
        m.open(p);
    }

    public static void stats(SommerspielePlugin plugin, Player p) {
        String lang = plugin.lang(p);
        PlayerData d = plugin.data(p);
        Menu m = new Menu(3, Texts.text(lang, "menu.stats.title"));
        for (int i = 0; i < 27; i++) {
            m.set(i, Items.pane(Material.WHITE_STAINED_GLASS_PANE), null);
        }
        m.set(4, Items.named(Material.GOLD_INGOT, 1, Texts.text(lang, "menu.stats.me"), Texts.lines(lang, "menu.stats.me.lore",
                Placeholder.unparsed("ng", String.valueOf(d.gold())), Placeholder.unparsed("ns", String.valueOf(d.silver())),
                Placeholder.unparsed("nb", String.valueOf(d.bronze())), Placeholder.unparsed("events", String.valueOf(d.events())),
                Placeholder.unparsed("wins", String.valueOf(d.wins())), Placeholder.unparsed("points", String.valueOf(d.points())))), null);
        int[] slots = {10, 12, 14, 16};
        Discipline[] all = Discipline.values();
        for (int i = 0; i < all.length; i++) {
            Discipline disc = all[i];
            List<Component> lore = new ArrayList<>();
            long best = d.best(disc);
            lore.add(best > 0 ? Texts.text(lang, "menu.stats.best", Placeholder.unparsed("time", Texts.value(disc, best, lang)))
                    : Texts.text(lang, "menu.stats.none"));
            if (disc == Discipline.BOGEN) {
                lore.add(Texts.text(lang, "menu.stats.rings", Placeholder.unparsed("rings", String.valueOf(d.rings())),
                        Placeholder.unparsed("centres", String.valueOf(d.centres()))));
            }
            lore.add(Component.empty());
            List<Leaderboard.Time> top = plugin.board().top(disc);
            for (int k = 0; k < Math.min(5, top.size()); k++) {
                lore.add(Texts.text(lang, "menu.stats.top", Placeholder.unparsed("place", String.valueOf(k + 1)),
                        Placeholder.unparsed("name", top.get(k).name()), Placeholder.unparsed("time", Texts.value(disc, top.get(k).ms(), lang))));
            }
            m.set(slots[i], Items.named(icon(Program.byId(disc.id())), 1,
                    Texts.parse("<em><b>" + Texts.plain(lang, "discipline." + disc.id()) + "</b></em>"), lore), null);
        }
        List<Component> table = new ArrayList<>();
        List<MedalTable.Row> rows = plugin.board().medalTable();
        for (int k = 0; k < Math.min(8, rows.size()); k++) {
            MedalTable.Row r = rows.get(k);
            table.add(Texts.text(lang, "board.medals.row", Placeholder.unparsed("place", String.valueOf(r.place())),
                    Placeholder.unparsed("name", plugin.board().nameOf(r.id())), Placeholder.unparsed("ng", String.valueOf(r.gold())),
                    Placeholder.unparsed("ns", String.valueOf(r.silver())), Placeholder.unparsed("nb", String.valueOf(r.bronze()))));
        }
        if (table.isEmpty()) {
            table.add(Texts.text(lang, "board.empty"));
        }
        m.set(22, Items.named(Material.GOLD_BLOCK, 1, Texts.text(lang, "menu.stats.table"), table), null);
        m.set(CLOSE_SLOT, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), q -> q.closeInventory());
        m.open(p);
    }
}

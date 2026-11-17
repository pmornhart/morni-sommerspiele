package de.moinmorni.sommerspiele.village;

import de.moinmorni.sommerspiele.Holo;
import de.moinmorni.sommerspiele.Items;
import de.moinmorni.sommerspiele.Texts;
import de.moinmorni.sommerspiele.Ui;
import de.moinmorni.sommerspiele.SommerspielePlugin;
import de.moinmorni.sommerspiele.logic.Groups;
import de.moinmorni.sommerspiele.logic.Program;
import de.moinmorni.sommerspiele.logic.Votes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Das Sommerdorf als Wartebereich: hier wartet man, stimmt ab (ganzer Wettkampf oder eine Disziplin) und sagt
 * „bereit“. Ein Countdown startet den nächsten Wettkampf mit höchstens zwölf Leuten – Partys bleiben zusammen.
 */
public final class Village {

    public static final int SLOT_VOTE = 0;
    public static final int SLOT_READY = 1;
    public static final int SLOT_STATS = 4;
    public static final int SLOT_LOBBY = 8;
    private static final int ALL_READY_SECONDS = 5;

    private final SommerspielePlugin plugin;
    private final Set<UUID> waiting = new LinkedHashSet<>();
    private final Set<UUID> ready = new HashSet<>();
    private final Map<UUID, UUID> leaders = new HashMap<>();
    private final Votes<Program> votes = new Votes<>(Program.class);
    private int countdown = -1;

    public Village(SommerspielePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean inVillage(UUID uuid) {
        return waiting.contains(uuid);
    }

    public Set<UUID> waiting() {
        return waiting;
    }

    public Votes<Program> votes() {
        return votes;
    }

    public int countdown() {
        return countdown;
    }

    public boolean ready(UUID uuid) {
        return ready.contains(uuid);
    }

    /** Was als Nächstes kommt (nach den Stimmen). */
    public Program next() {
        return votes.winner(Set.of(Program.values()));
    }

    /** Ins Dorf (Betreten, nach dem Wettkampf, /verlassen). */
    public void enter(Player p) {
        String lang = plugin.lang(p);
        if (p.getVehicle() != null) {
            p.leaveVehicle();
        }
        p.teleport(plugin.spawn());
        reset(p);
        items(p, lang);
        waiting.add(p.getUniqueId());
        ready.remove(p.getUniqueId());
        Holo.refresh(plugin, p, lang);
        refreshParty(p.getUniqueId());
    }

    /** Spieler in den Grundzustand: Abenteuer, volle Leben, keine Wirkungen, normale Geschwindigkeit. */
    public static void reset(Player p) {
        p.setGameMode(GameMode.ADVENTURE);
        p.setFlying(false);
        p.setAllowFlight(false);
        p.setInvulnerable(false);
        p.clearActivePotionEffects();
        var max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20 : max.getValue());
        p.setFoodLevel(20);
        p.setSaturation(20);
        p.setFireTicks(0);
        p.setFreezeTicks(0);
        p.setExp(0);
        p.setLevel(0);
        var speed = p.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getDefaultValue());
        }
        var jump = p.getAttribute(Attribute.JUMP_STRENGTH);
        if (jump != null) {
            jump.setBaseValue(jump.getDefaultValue());
        }
        p.getInventory().clear();
    }

    public void items(Player p, String lang) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setItem(SLOT_VOTE, Items.hallTag(Items.named(Material.COMPASS, 1, Texts.text(lang, "item.vote"),
                Texts.lines(lang, "item.vote.lore")), "vote"));
        inv.setItem(SLOT_READY, readyItem(p, lang));
        inv.setItem(SLOT_STATS, Items.hallTag(Items.named(Material.GOLD_NUGGET, 1, Texts.text(lang, "item.stats"),
                Texts.lines(lang, "item.stats.lore")), "stats"));
        inv.setItem(SLOT_LOBBY, Items.hallTag(Items.named(Material.RED_BED, 1, Texts.text(lang, "item.lobby"),
                Texts.lines(lang, "item.lobby.lore")), "lobby"));
        inv.setHeldItemSlot(SLOT_VOTE);
    }

    private ItemStack readyItem(Player p, String lang) {
        boolean r = ready.contains(p.getUniqueId());
        return Items.glow(Items.hallTag(Items.named(r ? Material.LIME_DYE : Material.GRAY_DYE, 1,
                Texts.text(lang, "item.ready"), Texts.lines(lang, "item.ready.lore")), "ready"), r);
    }

    public void leave(UUID uuid) {
        waiting.remove(uuid);
        ready.remove(uuid);
        votes.forget(uuid);
        leaders.remove(uuid);
    }

    public void toggleReady(Player p) {
        if (!waiting.contains(p.getUniqueId())) {
            return;
        }
        String lang = plugin.lang(p);
        if (!plugin.open()) {
            p.sendMessage(Texts.msg(lang, "village.closed", Placeholder.unparsed("date", plugin.reopening(lang))));
            return;
        }
        if (ready.remove(p.getUniqueId())) {
            p.sendMessage(Texts.msg(lang, "village.not-ready"));
        } else {
            ready.add(p.getUniqueId());
            p.sendMessage(Texts.msg(lang, "village.ready"));
            Ui.sound(p, "minecraft:block.note_block.pling", 1.4f);
        }
        p.getInventory().setItem(SLOT_READY, readyItem(p, lang));
    }

    public void vote(Player p, Program program) {
        votes.vote(p.getUniqueId(), program);
        String lang = plugin.lang(p);
        p.sendMessage(Texts.msg(lang, "village.voted", Placeholder.unparsed("program", name(lang, program))));
        Ui.sound(p, "minecraft:item.book.page_turn", 1f);
    }

    /** Name eines Programms in der Sprache. */
    public static String name(String lang, Program program) {
        return Texts.plain(lang, program == Program.WETTKAMPF ? "program.wettkampf" : "discipline." + program.id());
    }

    /** Party-Leitung nachschlagen (kommt auf einem anderen Thread zurück). */
    public void refreshParty(UUID uuid) {
        plugin.net().party(uuid).thenAccept(party -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (party.isPresent()) {
                leaders.put(uuid, party.get().leader());
            } else {
                leaders.remove(uuid);
            }
        }));
    }

    /** Party-Leitung (oder man selbst). */
    public UUID leaderOf(UUID uuid) {
        return leaders.getOrDefault(uuid, uuid);
    }

    /** Jede Sekunde: Countdown, Start. */
    public void tick() {
        waiting.removeIf(u -> Bukkit.getPlayer(u) == null);
        ready.retainAll(waiting);
        votes.retain(waiting);
        if (!plugin.ready()) {
            return;
        }
        if (!plugin.open()) {
            countdown = -1;
            ready.clear();
            for (UUID u : waiting) {
                Player p = Bukkit.getPlayer(u);
                if (p != null) {
                    String lang = plugin.lang(p);
                    p.sendActionBar(Texts.text(lang, "village.closed.bar", Placeholder.unparsed("date", plugin.reopening(lang))));
                }
            }
            return;
        }
        boolean busy = plugin.games().running();
        boolean anyReady = !ready.isEmpty();
        boolean allReady = !waiting.isEmpty() && ready.containsAll(waiting);
        if (busy || waiting.isEmpty() || (!anyReady && waiting.size() < plugin.settings().minPlayers())) {
            countdown = -1;
            for (UUID u : waiting) {
                Player p = Bukkit.getPlayer(u);
                if (p != null) {
                    String lang = plugin.lang(p);
                    p.sendActionBar(busy ? Texts.text(lang, "village.busy")
                            : Texts.text(lang, "village.waiting", Placeholder.unparsed("n", String.valueOf(waiting.size()))));
                }
            }
            return;
        }
        if (countdown < 0) {
            countdown = plugin.settings().villageCountdown();
        }
        if (allReady && countdown > ALL_READY_SECONDS) {
            countdown = ALL_READY_SECONDS;
        }
        Program next = next();
        for (UUID u : waiting) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                String lang = plugin.lang(p);
                p.sendActionBar(Texts.text(lang, "village.countdown", Placeholder.unparsed("secs", String.valueOf(countdown)),
                        Placeholder.unparsed("program", name(lang, next))));
                if (countdown <= 3 && countdown > 0) {
                    Ui.sound(p, "minecraft:block.note_block.hat", 1.2f);
                }
            }
        }
        if (countdown-- <= 0) {
            start(next);
            countdown = -1;
        }
    }

    /** Wettkampf starten (Countdown abgelaufen oder /sommeradmin start). */
    public boolean start(Program program) {
        if (plugin.games().running()) {
            return false;
        }
        Map<UUID, List<UUID>> groups = new LinkedHashMap<>();
        for (UUID u : waiting) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) {
                continue;
            }
            UUID leader = leaderOf(u);
            if (!waiting.contains(leader)) {
                leader = u;
            }
            groups.computeIfAbsent(leader, k -> new ArrayList<>()).add(u);
        }
        List<List<UUID>> groupList = new ArrayList<>(groups.values());
        List<UUID> picked = Groups.pick(groupList, plugin.settings().maxPlayers());
        if (picked.isEmpty()) {
            return false;
        }
        List<List<Player>> parties = new ArrayList<>();
        for (List<UUID> g : groupList) {
            List<Player> members = new ArrayList<>();
            for (UUID u : g) {
                Player p = Bukkit.getPlayer(u);
                if (p != null && picked.contains(u)) {
                    members.add(p);
                }
            }
            if (!members.isEmpty()) {
                parties.add(members);
            }
        }
        int count = parties.stream().mapToInt(List::size).sum();
        parties.forEach(g -> g.forEach(p -> leave(p.getUniqueId())));
        votes.played(program, System.currentTimeMillis());
        for (List<Player> g : parties) {
            for (Player p : g) {
                String lang = plugin.lang(p);
                p.sendMessage(Texts.msg(lang, "village.starting", Placeholder.unparsed("program", name(lang, program)),
                        Placeholder.unparsed("n", String.valueOf(count))));
            }
        }
        for (UUID u : waiting) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                p.sendMessage(Texts.msg(plugin.lang(p), "village.full"));
            }
        }
        plugin.games().start(program, parties);
        return true;
    }
}

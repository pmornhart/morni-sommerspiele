package de.moinmorni.sommerspiele.game;

import de.moinmorni.sommerspiele.Texts;
import de.moinmorni.sommerspiele.SommerspielePlugin;
import de.moinmorni.sommerspiele.logic.Discipline;
import de.moinmorni.sommerspiele.logic.Program;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

/** Hält den laufenden Wettkampf (es gibt immer höchstens einen – alle Strecken gehören ihm). */
public final class Games {

    private final SommerspielePlugin plugin;
    private Competition current;

    public Games(SommerspielePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean running() {
        return current != null;
    }

    public Competition current() {
        return current;
    }

    public void start(Program program, List<List<Player>> parties) {
        current = new Competition(plugin, program, parties);
        current.start();
        plugin.stateChanged();
    }

    void ended(Competition c) {
        if (current == c) {
            current = null;
            plugin.stateChanged();
        }
    }

    public void tick(long tick) {
        if (current != null) {
            current.tick(tick);
        }
    }

    public void second() {
        if (current != null) {
            current.second();
        }
    }

    /** Ist dieser Spieler im Wettkampf (nicht ausgeschieden)? */
    public Competition playing(UUID uuid) {
        return current != null && current.racer(uuid) != null ? current : null;
    }

    /** Wettkampf, der auf jemanden wartet, der die Verbindung verloren hat (Platz noch frei) – sonst null. */
    public Competition waitingFor(UUID uuid) {
        return current != null && current.waitingFor(uuid) ? current : null;
    }

    /** Spieler ist im Wettkampf oder schaut zu. */
    public Competition of(UUID uuid) {
        return current != null && (current.racer(uuid) != null || current.watching(uuid)) ? current : null;
    }

    public void spectate(Player p) {
        if (current == null) {
            p.sendMessage(Texts.msg(plugin.lang(p), "spectate.none"));
            return;
        }
        plugin.village().leave(p.getUniqueId());
        current.spectate(p);
        p.sendMessage(Texts.msg(plugin.lang(p), "spectate.start", Placeholder.unparsed("discipline",
                Texts.plain(plugin.lang(p), "discipline." + current.discipline().id()))));
    }

    public void abort() {
        if (current != null) {
            current.abort();
            current = null;
        }
    }

    public Discipline discipline() {
        return current == null ? null : current.discipline();
    }
}

package de.moinmorni.sommerspiele;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;

/**
 * Schwebende Schrift in zwei Sprachen: zwei Text-Anzeigen an derselben Stelle, jeder sieht nur die in seiner
 * Sprache. Nicht dauerhaft (verschwinden beim Neustart von selbst).
 */
public final class Holo {

    private static final List<Holo> ALL = new ArrayList<>();

    private final TextDisplay de;
    private final TextDisplay en;

    private Holo(TextDisplay de, TextDisplay en) {
        this.de = de;
        this.en = en;
    }

    /** Neue Schrift; {@code text} liefert den Inhalt je Sprache. */
    public static Holo spawn(Plugin plugin, Location at, Function<String, Component> text, float scale) {
        TextDisplay d = make(at, text.apply("de"), scale);
        TextDisplay e = make(at, text.apply("en"), scale);
        Holo h = new Holo(d, e);
        ALL.add(h);
        for (Player p : at.getWorld().getPlayers()) {
            h.showTo(plugin, p, SommerspielePlugin.instance().lang(p));
        }
        return h;
    }

    private static TextDisplay make(Location at, Component text, float scale) {
        return at.getWorld().spawn(at, TextDisplay.class, t -> {
            t.text(text);
            t.setPersistent(false);
            t.setVisibleByDefault(false);
            t.setBillboard(Display.Billboard.CENTER);
            t.setBackgroundColor(Color.fromARGB(110, 0, 0, 0));
            t.setShadowed(true);
            t.setLineWidth(240);
            t.setTransformation(new org.bukkit.util.Transformation(new org.joml.Vector3f(),
                    new org.joml.AxisAngle4f(), new org.joml.Vector3f(scale, scale, scale), new org.joml.AxisAngle4f()));
        });
    }

    public void update(Function<String, Component> text) {
        if (de.isValid()) {
            de.text(text.apply("de"));
        }
        if (en.isValid()) {
            en.text(text.apply("en"));
        }
    }

    public void showTo(Plugin plugin, Player p, String lang) {
        boolean english = "en".equals(lang);
        if (english) {
            p.hideEntity(plugin, de);
            p.showEntity(plugin, en);
        } else {
            p.hideEntity(plugin, en);
            p.showEntity(plugin, de);
        }
    }

    /** Stehen beide Anzeigen noch? */
    public boolean valid() {
        return de.isValid() && en.isValid();
    }

    public void remove() {
        de.remove();
        en.remove();
        ALL.remove(this);
    }

    /** Alle Schriften für diesen Spieler in seiner Sprache zeigen (beim Betreten, nach Sprachwechsel). */
    public static void refresh(Plugin plugin, Player p, String lang) {
        for (Holo h : List.copyOf(ALL)) {
            if (h.de.isValid() && h.de.getWorld() == p.getWorld()) {
                h.showTo(plugin, p, lang);
            }
        }
    }

    public static void removeAll() {
        for (Holo h : List.copyOf(ALL)) {
            h.remove();
        }
    }
}

package de.moinmorni.sommerspiele;

import java.time.Duration;
import java.util.Locale;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

/** Kleine Helfer für Titel, Töne und Zahlen. */
public final class Ui {

    private static final long FADE_IN_MS = 100;
    private static final long FADE_OUT_MS = 250;

    private Ui() {
    }

    public static void title(Player p, Component main, Component sub, long stayMs) {
        p.showTitle(Title.title(main, sub, Title.Times.times(Duration.ofMillis(FADE_IN_MS), Duration.ofMillis(stayMs),
                Duration.ofMillis(FADE_OUT_MS))));
    }

    public static void sound(Player p, String key, float pitch) {
        p.playSound(Sound.sound(Key.key(key), Sound.Source.MASTER, 1f, pitch));
    }

    /** Sekunden als „1:05“. */
    public static String clock(long seconds) {
        long s = Math.max(0, seconds);
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    /** Herzen mit einer Nachkommastelle, deutsch mit Komma. */
    public static String hearts(double health, String lang) {
        String s = String.format(Locale.ROOT, "%.1f", Math.max(0, health) / 2.0);
        return "en".equals(lang) ? s : s.replace('.', ',');
    }

    /** Fortschrittsbalken aus ▮ und ▯. */
    public static String bar(double fraction, int width) {
        int full = (int) Math.round(Math.max(0, Math.min(1, fraction)) * width);
        return "▮".repeat(full) + "▯".repeat(width - full);
    }

    /** Sekunden mit einer Nachkommastelle. */
    public static String seconds(long millis, String lang) {
        String s = String.format(Locale.ROOT, "%.1f", Math.max(0, millis) / 1000.0);
        return "en".equals(lang) ? s : s.replace('.', ',');
    }
}

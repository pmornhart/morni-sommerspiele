package de.moinmorni.sommerspiele.game;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;

/** Eine Person im Wettkampf: Startnummer, Farbe des Leibchens, ob sie noch dabei ist. */
public final class Racer {

    private static final Color[] COLOURS = {Color.fromRGB(0xD7263D), Color.fromRGB(0x1B6CD1), Color.fromRGB(0x2BB673),
        Color.fromRGB(0xF2B632), Color.fromRGB(0xF46036), Color.fromRGB(0x8E44AD), Color.fromRGB(0x16BAC5),
        Color.fromRGB(0xF49AC1), Color.fromRGB(0xF2F2F2), Color.fromRGB(0x2E2E2E), Color.fromRGB(0x8D5A3B),
        Color.fromRGB(0x9BE564)};

    private final UUID uuid;
    private final String name;
    private final int number;
    private boolean left;

    public Racer(UUID uuid, String name, int number) {
        this.uuid = uuid;
        this.name = name;
        this.number = number;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public int number() {
        return number;
    }

    public Color colour() {
        return COLOURS[(number - 1) % COLOURS.length];
    }

    public boolean left() {
        return left;
    }

    void leave() {
        left = true;
    }

    /** Online und noch im Wettkampf? */
    public Player player() {
        if (left) {
            return null;
        }
        Player p = Bukkit.getPlayer(uuid);
        return p != null && p.isOnline() ? p : null;
    }

    public String id() {
        return uuid.toString();
    }
}

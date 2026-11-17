package de.moinmorni.sommerspiele;

import de.moinmorni.sommerspiele.logic.Season;
import org.bukkit.configuration.file.FileConfiguration;

/** Werte aus der config.yml (Erklärungen stehen dort). */
public record Settings(
        String world,
        Season season,
        boolean evening,
        boolean pollen,
        int villageCountdown,
        int minPlayers,
        int maxPlayers,
        int pauseSeconds,
        int ceremonySeconds,
        int almLaps,
        int almLimit,
        int runSpeed,
        int arrows,
        int bowSeconds,
        int wind,
        boolean pairs,
        int regattaPenaltySeconds,
        int regattaLimit,
        int current,
        int kisteLimit,
        boolean fork,
        int comebackSeconds) {

    /** Höchstens so lange wartet ein Wettkampf auf jemanden, der die Verbindung verloren hat. */
    static final int MAX_COMEBACK_SECONDS = 600;

    static Settings load(FileConfiguration c) {
        return new Settings(
                c.getString("welt", "sommerspiele"),
                Season.parse(c.getString("saison", "sommer"), clamp(c.getInt("saison-von", Season.DEFAULT_FROM), 1, 12),
                        clamp(c.getInt("saison-bis", Season.DEFAULT_TO), 1, 12)),
                "abend".equalsIgnoreCase(c.getString("tageszeit", "tag").trim()),
                c.getBoolean("bluetenstaub", true),
                clamp(c.getInt("dorf.countdown-sekunden", 30), 3, 300),
                clamp(c.getInt("dorf.mindestens-spieler", 2), 1, 12),
                clamp(c.getInt("wettkampf.hoechstens-spieler", 12), 1, 12),
                clamp(c.getInt("wettkampf.pause-sekunden", 8), 2, 120),
                clamp(c.getInt("wettkampf.siegerehrung-sekunden", 12), 3, 120),
                clamp(c.getInt("almlauf.runden", 1), 1, 5),
                clamp(c.getInt("almlauf.zeitlimit-sekunden", 240), 30, 1800),
                clamp(c.getInt("almlauf.tempo", 1), 0, 3),
                clamp(c.getInt("bogen.pfeile", 5), 1, 10),
                clamp(c.getInt("bogen.zeit-sekunden", 45), 10, 300),
                clamp(c.getInt("bogen.wind", 2), 0, 3),
                c.getBoolean("regatta.zweier", true),
                clamp(c.getInt("regatta.strafe-sekunden", 4), 0, 60),
                clamp(c.getInt("regatta.zeitlimit-sekunden", 150), 20, 600),
                clamp(c.getInt("regatta.stroemung", 2), 0, 3),
                clamp(c.getInt("seifenkiste.zeitlimit-sekunden", 120), 20, 600),
                c.getBoolean("seifenkiste.weiche", true),
                clamp(c.getInt("rueckkehr-sekunden", 90), 0, MAX_COMEBACK_SECONDS));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}

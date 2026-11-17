package de.moinmorni.sommerspiele.logic;

import java.util.Locale;

/** Zeiten wie auf der Anzeigetafel: „0:14,32“ (deutsch mit Komma, englisch mit Punkt), auf Hundertstel. */
public final class TimeFormat {

    private static final long MS_PER_MINUTE = 60_000;
    private static final long MS_PER_SECOND = 1000;
    private static final long MS_PER_HUNDREDTH = 10;

    private TimeFormat() {
    }

    public static String format(long ms, String lang) {
        long v = Math.max(0, ms);
        long minutes = v / MS_PER_MINUTE;
        long seconds = (v % MS_PER_MINUTE) / MS_PER_SECOND;
        long hundredths = (v % MS_PER_SECOND) / MS_PER_HUNDREDTH;
        String mark = "en".equals(lang) ? "." : ",";
        return String.format(Locale.ROOT, "%d:%02d%s%02d", minutes, seconds, mark, hundredths);
    }

    /** Abstand zur Bestzeit: „+1,23“. */
    public static String gap(long ms, String lang) {
        long v = Math.max(0, ms);
        String s = String.format(Locale.ROOT, "+%d.%02d", v / MS_PER_SECOND, (v % MS_PER_SECOND) / MS_PER_HUNDREDTH);
        return "en".equals(lang) ? s : s.replace('.', ',');
    }

    /** Ganze Sekunden einer Strafe: „+3 s“. */
    public static String penalty(long ms) {
        return "+" + Math.round(ms / (double) MS_PER_SECOND) + " s";
    }
}

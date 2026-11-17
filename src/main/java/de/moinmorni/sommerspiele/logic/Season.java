package de.moinmorni.sommerspiele.logic;

import java.time.LocalDate;

/**
 * Saison aus der config.yml: {@code immer} offen oder nur im {@code sommer} – vom ersten Tag des Monats
 * {@code from} bis zum letzten Tag des Monats {@code to}. Vorgabe April bis November: genau dann, wenn Wintersport
 * (1. Dezember bis 31. März) Pause hat – so ist immer einer der beiden Modi offen. Geht die Spanne über den
 * Jahreswechsel (z. B. 11 bis 2), zählt sie über Silvester. Außerhalb ist der Modus „geschlossen“: das Dorf bleibt zum
 * Anschauen offen, Wettkämpfe gibt es keine.
 */
public record Season(boolean always, int from, int to) {

    /** Vorgabe: erster Monat der Sommersaison (April). */
    public static final int DEFAULT_FROM = 4;
    /** Vorgabe: letzter Monat der Sommersaison (November). */
    public static final int DEFAULT_TO = 11;
    public static final Season ALWAYS = new Season(true, 1, 12);

    public Season {
        from = clampMonth(from);
        to = clampMonth(to);
    }

    private static int clampMonth(int m) {
        return Math.max(1, Math.min(12, m));
    }

    /** Aus der config.yml; unbekannt → immer. */
    public static Season parse(String s, int from, int to) {
        if (s != null) {
            String v = s.trim().toLowerCase(java.util.Locale.ROOT);
            if (v.equals("sommer") || v.equals("summer") || v.equals("saison") || v.equals("season")) {
                return new Season(false, from, to);
            }
        }
        return ALWAYS;
    }

    public String id() {
        return always ? "immer" : "sommer";
    }

    private boolean inMonth(int m) {
        return from <= to ? m >= from && m <= to : m >= from || m <= to;
    }

    public boolean open(LocalDate day) {
        return always || inMonth(day.getMonthValue());
    }

    /** Wann es (wieder) losgeht: heute, wenn offen, sonst der nächste Monatserste von {@code from}. */
    public LocalDate nextOpening(LocalDate day) {
        if (open(day)) {
            return day;
        }
        LocalDate d = LocalDate.of(day.getYear(), from, 1);
        return d.isAfter(day) ? d : d.plusYears(1);
    }

    /** Letzter Tag der laufenden Saison (null bei „immer“ oder außerhalb der Saison). */
    public LocalDate closing(LocalDate day) {
        if (always || !open(day)) {
            return null;
        }
        LocalDate d = day;
        while (d.getMonthValue() != to) {
            d = d.plusMonths(1).withDayOfMonth(1);
        }
        return d.withDayOfMonth(d.lengthOfMonth());
    }
}

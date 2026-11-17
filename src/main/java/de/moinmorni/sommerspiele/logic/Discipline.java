package de.moinmorni.sommerspiele.logic;

/**
 * Die vier Disziplinen. Gewertet wird nach Zeit (kleiner ist besser) – nur beim Bogenschießen nach Ringen (größer ist
 * besser).
 */
public enum Discipline {
    /** Almlauf: Hindernislauf zu Fuß über Heuballen, Zäune, den Bach und die Kuhweide. */
    ALMLAUF("almlauf"),
    /** Bogenschießen: drei Durchgänge auf Scheiben in 20, 35 und 50 Blöcken, mit Wind. */
    BOGEN("bogen"),
    /** Ruderregatta: im Boot über den Bergsee durch Bojen-Tore, Strömungen und Strudel. */
    REGATTA("regatta"),
    /** Seifenkistenrennen: in der Lore die Schienen den Berg hinab, mit Bremse und Weiche. */
    SEIFENKISTE("seifenkiste");

    private final String id;

    Discipline(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** Nach Ringen gewertet (größer ist besser) statt nach Zeit? */
    public boolean scored() {
        return this == BOGEN;
    }

    /** Fahren mehrere Läufe mit Bahnen nebeneinander (Regatta, Seifenkiste)? */
    public boolean lanes() {
        return this == REGATTA || this == SEIFENKISTE;
    }

    public static Discipline byId(String id) {
        if (id == null) {
            return null;
        }
        for (Discipline d : values()) {
            if (d.id.equalsIgnoreCase(id)) {
                return d;
            }
        }
        return switch (id.toLowerCase(java.util.Locale.ROOT)) {
            case "kiste", "soapbox", "lore" -> SEIFENKISTE;
            case "bogenschiessen", "archery", "bow" -> BOGEN;
            case "rudern", "rowing", "boot" -> REGATTA;
            case "lauf", "run", "hindernislauf" -> ALMLAUF;
            default -> null;
        };
    }
}

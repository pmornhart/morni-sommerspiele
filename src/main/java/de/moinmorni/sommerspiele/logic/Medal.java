package de.moinmorni.sommerspiele.logic;

/** Medaillen für die Plätze 1 bis 3 (geteilte Plätze teilen die Medaille). */
public enum Medal {
    GOLD("gold"), SILVER("silver"), BRONZE("bronze");

    private final String id;

    Medal(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** Medaille für einen Platz (1–3), sonst null. */
    public static Medal forPlace(int place) {
        return switch (place) {
            case 1 -> GOLD;
            case 2 -> SILVER;
            case 3 -> BRONZE;
            default -> null;
        };
    }
}

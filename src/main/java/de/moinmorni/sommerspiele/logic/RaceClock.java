package de.moinmorni.sommerspiele.logic;

/**
 * Stoppuhr in Server-Ticks (50 ms) mit Bruchteilen: Wann genau innerhalb eines Ticks jemand über die Ziellinie fährt,
 * rechnet die Zeitnahme aus der Bewegung zwischen zwei Ticks nach – so gibt es Hundertstel statt nur 50-ms-Schritten.
 */
public final class RaceClock {

    public static final double MS_PER_TICK = 50.0;

    private final long startTick;

    public RaceClock(long startTick) {
        this.startTick = startTick;
    }

    /** Zeit bis zu einem Ereignis im Tick {@code tick}, das beim Anteil {@code fraction} (0…1) der Bewegung lag. */
    public long at(long tick, double fraction) {
        double f = Double.isNaN(fraction) ? 1.0 : Math.max(0, Math.min(1, fraction));
        double ticks = (tick - 1 - startTick) + f;
        return Math.max(0, Math.round(ticks * MS_PER_TICK));
    }

    /** Laufende Anzeige. */
    public long now(long tick) {
        return Math.max(0, Math.round((tick - startTick) * MS_PER_TICK));
    }

    public long startTick() {
        return startTick;
    }
}

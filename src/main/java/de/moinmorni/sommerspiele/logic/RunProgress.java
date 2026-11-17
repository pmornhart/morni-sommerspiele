package de.moinmorni.sommerspiele.logic;

/**
 * Stand eines Läufers im Almlauf (ohne Minecraft): Kontrollpunkte der Reihe nach, dann über die Rundenlinie. Eine Runde
 * zählt nur, wenn alle Kontrollpunkte dazwischen passiert wurden – Abkürzen bringt nichts, und die Startlinie gleich
 * nach dem Start zählt nicht.
 */
public final class RunProgress {

    /** Was das Kreuzen der Rundenlinie bewirkt hat. */
    public enum Lap { NONE, LAP, FINISH }

    private final int laps;
    private final int checkpoints;
    private int lap;
    private int next;
    private boolean finished;

    public RunProgress(int laps, int checkpoints) {
        this.laps = Math.max(1, laps);
        this.checkpoints = Math.max(0, checkpoints);
    }

    public int laps() {
        return laps;
    }

    /** Fertige Runden. */
    public int lap() {
        return lap;
    }

    /** Nächster Kontrollpunkt (0 …). */
    public int nextCheckpoint() {
        return next;
    }

    /** Letzter passierter Kontrollpunkt dieser Runde (−1 = keiner). */
    public int lastCheckpoint() {
        return next - 1;
    }

    public boolean finished() {
        return finished;
    }

    /** Kontrollpunkt i gekreuzt: zählt nur, wenn er der nächste ist. */
    public boolean checkpoint(int i) {
        if (finished || i != next) {
            return false;
        }
        next++;
        return true;
    }

    /** Rundenlinie gekreuzt. */
    public Lap lapLine() {
        if (finished || next < checkpoints) {
            return Lap.NONE;
        }
        lap++;
        next = 0;
        if (lap >= laps) {
            finished = true;
            return Lap.FINISH;
        }
        return Lap.LAP;
    }
}

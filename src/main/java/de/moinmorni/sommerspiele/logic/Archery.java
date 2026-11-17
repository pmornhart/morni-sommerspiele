package de.moinmorni.sommerspiele.logic;

/**
 * Bogenschießen ohne Minecraft: Ringe aus dem Treffpunkt, Wind, Pfeilflug wie in Minecraft (je Tick: weiter fliegen,
 * Luftwiderstand ×0,99, Schwerkraft −0,05) und das Zielen dazu (für den Selbstschuss im Test).
 *
 * <p>Ringe: Die Scheibe hat einen Halbmesser von {@link #FACE_RADIUS} Blöcken in zehn gleich breiten Ringen – innen
 * 10, außen 1, daneben 0. Wer genau in die Mitte trifft (innerhalb {@link #X_RADIUS}), hat eine „Mitte“ (zählt bei
 * Gleichstand).
 */
public final class Archery {

    public static final double FACE_RADIUS = 2.5;
    public static final double RING = FACE_RADIUS / 10;
    public static final double X_RADIUS = RING / 2;
    /** So viel schiebt Wind der Stärke 1 einen Pfeil je Tick zur Seite (Blöcke/Tick²). */
    public static final double WIND_PUSH = 0.0035;
    public static final int MAX_WIND = 3;
    /** Pfeiltempo bei voll gespanntem Bogen (Blöcke/Tick). */
    public static final double ARROW_SPEED = 3.0;
    private static final double DRAG = 0.99;
    private static final double GRAVITY = 0.05;
    private static final int MAX_TICKS = 200;

    private Archery() {
    }

    /** Ringe für einen Treffer im Abstand r (Blöcke) von der Mitte. */
    public static int score(double r) {
        if (r < 0 || r >= FACE_RADIUS) {
            return 0;
        }
        return 10 - (int) Math.floor(r / RING);
    }

    public static boolean centre(double r) {
        return r >= 0 && r < X_RADIUS;
    }

    /** Wertung zum Sortieren: Ringe, bei Gleichstand mehr Mitten. */
    public static long rankValue(int rings, int centres) {
        return rings * 100L + centres;
    }

    /** Ein Pfeil im Flug (für die Prüfung und das Zielen). */
    public record Flight(double side, double drop, int ticks) {
    }

    /**
     * Pfeilflug bis zur Scheibe in {@code distance} Blöcken (geradeaus), Start mit {@code speed} unter dem Winkel
     * {@code pitchUp} (Grad nach oben) und {@code yawSide} (Grad zur Seite, + = nach Osten), Wind {@code wind}
     * (−3 … 3, + = nach Osten).
     *
     * @return seitliche Abweichung und Höhe gegenüber dem Abschuss an der Scheibe
     */
    public static Flight fly(double distance, double speed, double pitchUp, double yawSide, int wind) {
        double p = Math.toRadians(pitchUp);
        double yw = Math.toRadians(yawSide);
        double vz = speed * Math.cos(p) * Math.cos(yw);
        double vx = speed * Math.cos(p) * Math.sin(yw);
        double vy = speed * Math.sin(p);
        double x = 0;
        double y = 0;
        double z = 0;
        for (int t = 1; t <= MAX_TICKS; t++) {
            double nz = z + vz;
            if (nz >= distance) {
                double f = (distance - z) / vz;
                return new Flight(x + vx * f, y + vy * f, t);
            }
            x += vx;
            y += vy;
            z = nz;
            vx = vx * DRAG + wind * WIND_PUSH;
            vy = vy * DRAG - GRAVITY;
            vz = vz * DRAG;
        }
        return new Flight(x, y, MAX_TICKS);
    }

    /**
     * Zielen: Winkel nach oben und zur Seite, damit ein Pfeil mit {@code speed} auf {@code distance} Blöcke um
     * {@code rise} höher landet (Mitte der Scheibe über dem Auge) und der Wind ausgeglichen ist.
     *
     * @return {pitchUp, yawSide} in Grad
     */
    public static double[] aim(double distance, double rise, double speed, int wind) {
        double pitch = 0;
        double yaw = 0;
        for (int i = 0; i < 40; i++) {
            Flight f = fly(distance, speed, pitch, yaw, wind);
            pitch += Math.toDegrees(Math.atan2(rise - f.drop(), distance)) * 0.8;
            yaw -= Math.toDegrees(Math.atan2(f.side(), distance)) * 0.8;
        }
        return new double[] {pitch, yaw};
    }
}

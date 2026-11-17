package de.moinmorni.sommerspiele.logic;

/**
 * Eine Linie auf dem Boden (von oben gesehen: x und z) – Tor, Ziel oder Kontrollpunkt. Geprüft wird, ob die Bewegung
 * von einem Tick zum nächsten sie kreuzt; so zählt sie auch bei Höchstgeschwindigkeit (mehrere Blöcke pro Tick).
 */
public record Line(double ax, double az, double bx, double bz) {

    /** Wo die Bewegung die Linie kreuzt: {@code t} = Anteil der Bewegung (0…1), {@code u} = Stelle auf der Linie (0…1). */
    public record Hit(double t, double u) {
    }

    /** Waagrechte Linie (gleiches z) von x1 bis x2. */
    public static Line acrossZ(double z, double x1, double x2) {
        return new Line(x1, z, x2, z);
    }

    /** Senkrechte Linie (gleiches x) von z1 bis z2. */
    public static Line acrossX(double x, double z1, double z2) {
        return new Line(x, z1, x, z2);
    }

    /**
     * Kreuzt die Bewegung von (px, pz) nach (qx, qz) diese Linie? Berührt die Bewegung sie nur am Anfang, zählt das
     * nicht (sonst würde ein Spieler, der genau auf der Linie steht, sie zweimal kreuzen).
     *
     * @return der Schnittpunkt oder null
     */
    public Hit cross(double px, double pz, double qx, double qz) {
        double rx = qx - px;
        double rz = qz - pz;
        double sx = bx - ax;
        double sz = bz - az;
        double denom = rx * sz - rz * sx;
        if (Math.abs(denom) < 1e-12) {
            return null;
        }
        double wx = ax - px;
        double wz = az - pz;
        double t = (wx * sz - wz * sx) / denom;
        double u = (wx * rz - wz * rx) / denom;
        if (t <= 0 || t > 1 || u < 0 || u > 1) {
            return null;
        }
        return new Hit(t, u);
    }

    /** Punkt auf der Linie an der Stelle {@code u}. */
    public double x(double u) {
        return ax + (bx - ax) * u;
    }

    public double z(double u) {
        return az + (bz - az) * u;
    }

    public double length() {
        return Math.hypot(bx - ax, bz - az);
    }
}

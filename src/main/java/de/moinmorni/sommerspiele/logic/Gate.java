package de.moinmorni.sommerspiele.logic;

/**
 * Ein Bojen-Tor: eine Linie quer über die ganze Bahn, durchfahren werden muss sie zwischen den beiden Bojen
 * ({@code from} … {@code to} als Stellen auf der Linie, 0…1). Wer die Linie außerhalb kreuzt, hat das Tor verpasst.
 *
 * @param red rote Stangen (sonst blaue) – die Farben wechseln sich ab
 */
public record Gate(Line line, double from, double to, boolean red) {

    /** Ergebnis beim Kreuzen der Linie. */
    public enum Pass { NONE, PASSED, MISSED }

    public Pass check(double px, double pz, double qx, double qz) {
        Line.Hit hit = line.cross(px, pz, qx, qz);
        if (hit == null) {
            return Pass.NONE;
        }
        return hit.u() >= from && hit.u() <= to ? Pass.PASSED : Pass.MISSED;
    }

    /** Stelle der Stangen (x, z) – erste und zweite. */
    public double[] poles() {
        return new double[] {line.x(from), line.z(from), line.x(to), line.z(to)};
    }

    /** Mitte der Durchfahrt. */
    public double[] middle() {
        double u = (from + to) / 2;
        return new double[] {line.x(u), line.z(u)};
    }
}

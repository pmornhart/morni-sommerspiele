package de.moinmorni.sommerspiele.course;

import java.util.ArrayList;
import java.util.List;

/**
 * Eine Strecke von oben gesehen: Eckpunkte mit runden Kurven (Radius {@code radius}), fein abgetastet. Dient der
 * Seifenkisten-Bahn und dem Almlauf: Was näher als die halbe Breite an der Mittellinie liegt, ist Strecke.
 */
public final class Path {

    /** Abtastpunkt: Lage, Richtung (Yaw wie in Minecraft: 0 = Süden, 90 = Westen) und zurückgelegte Strecke. */
    public record Sample(double x, double z, float yaw, double s) {
    }

    private static final double STEP = 0.25;
    private final List<Sample> samples = new ArrayList<>();

    /**
     * @param corners Eckpunkte {x, z}
     * @param radius  Kurvenradius an jeder Ecke
     */
    public Path(List<double[]> corners, double radius) {
        List<double[]> pts = new ArrayList<>();
        pts.add(corners.get(0));
        for (int i = 1; i < corners.size() - 1; i++) {
            double[] a = corners.get(i - 1);
            double[] b = corners.get(i);
            double[] c = corners.get(i + 1);
            double[] u = unit(b[0] - a[0], b[1] - a[1]);
            double[] v = unit(c[0] - b[0], c[1] - b[1]);
            double lenIn = Math.hypot(b[0] - a[0], b[1] - a[1]);
            double lenOut = Math.hypot(c[0] - b[0], c[1] - b[1]);
            double cos = u[0] * v[0] + u[1] * v[1];
            if (cos > 0.9999) {
                pts.add(b);
                continue;
            }
            double angle = Math.acos(Math.max(-1, Math.min(1, cos)));
            double cut = Math.min(radius * Math.tan(angle / 2), Math.min(lenIn, lenOut) / 2);
            double r = cut / Math.tan(angle / 2);
            double[] p = {b[0] - u[0] * cut, b[1] - u[1] * cut};
            double[] q = {b[0] + v[0] * cut, b[1] + v[1] * cut};
            // Mittelpunkt des Bogens: von p aus senkrecht zu u, auf der Seite der Kurve.
            double cross = u[0] * v[1] - u[1] * v[0];
            double[] n = cross > 0 ? new double[] {-u[1], u[0]} : new double[] {u[1], -u[0]};
            double[] m = {p[0] + n[0] * r, p[1] + n[1] * r};
            double a0 = Math.atan2(p[1] - m[1], p[0] - m[0]);
            double a1 = Math.atan2(q[1] - m[1], q[0] - m[0]);
            double da = a1 - a0;
            while (da > Math.PI) {
                da -= 2 * Math.PI;
            }
            while (da < -Math.PI) {
                da += 2 * Math.PI;
            }
            int steps = Math.max(2, (int) Math.ceil(Math.abs(da) * r / STEP));
            for (int k = 0; k <= steps; k++) {
                double ang = a0 + da * k / steps;
                pts.add(new double[] {m[0] + Math.cos(ang) * r, m[1] + Math.sin(ang) * r});
            }
        }
        pts.add(corners.get(corners.size() - 1));
        double s = 0;
        for (int i = 0; i < pts.size() - 1; i++) {
            double[] a = pts.get(i);
            double[] b = pts.get(i + 1);
            double len = Math.hypot(b[0] - a[0], b[1] - a[1]);
            float yaw = yaw(b[0] - a[0], b[1] - a[1]);
            int n = Math.max(1, (int) Math.ceil(len / STEP));
            for (int k = 0; k < n; k++) {
                double t = k / (double) n;
                samples.add(new Sample(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, yaw, s + len * t));
            }
            s += len;
        }
        double[] last = pts.get(pts.size() - 1);
        Sample prev = samples.get(samples.size() - 1);
        samples.add(new Sample(last[0], last[1], prev.yaw(), s));
    }

    private static double[] unit(double x, double z) {
        double l = Math.hypot(x, z);
        return new double[] {x / l, z / l};
    }

    /** Minecraft-Yaw für eine Richtung (0 = +z/Süden, 90 = −x/Westen, −90 = +x/Osten, 180 = Norden). */
    public static float yaw(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    /** Drehung von Richtung a nach b in Grad (−180 … 180). */
    public static float turn(float a, float b) {
        float d = b - a;
        while (d > 180f) {
            d -= 360f;
        }
        while (d < -180f) {
            d += 360f;
        }
        return d;
    }

    public List<Sample> samples() {
        return samples;
    }

    public double length() {
        return samples.get(samples.size() - 1).s();
    }

    /** Nächster Abtastpunkt (Index) – sucht um {@code hint} herum, bei −1 überall. */
    public int nearest(double x, double z, int hint) {
        int from = 0;
        int to = samples.size() - 1;
        if (hint >= 0) {
            from = Math.max(0, hint - 80);
            to = Math.min(samples.size() - 1, hint + 160);
        }
        int best = from;
        double bd = Double.MAX_VALUE;
        for (int i = from; i <= to; i++) {
            Sample p = samples.get(i);
            double d = (p.x() - x) * (p.x() - x) + (p.z() - z) * (p.z() - z);
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        return best;
    }

    /** Abstand zur Mittellinie (für Bau und Prüfung). */
    public double distance(double x, double z) {
        Sample p = samples.get(nearest(x, z, -1));
        return Math.hypot(p.x() - x, p.z() - z);
    }

    /** Abtastpunkt bei Strecke s. */
    public Sample at(double s) {
        int i = (int) Math.round(s / STEP);
        // Die Abtastung ist nicht ganz gleichmäßig (Ecken) – darum genau suchen.
        int lo = Math.max(0, Math.min(samples.size() - 1, i - 20));
        int hi = Math.min(samples.size() - 1, i + 20);
        Sample best = samples.get(lo);
        for (int k = lo; k <= hi; k++) {
            if (Math.abs(samples.get(k).s() - s) < Math.abs(best.s() - s)) {
                best = samples.get(k);
            }
        }
        if (Math.abs(best.s() - s) > 1) {
            for (Sample p : samples) {
                if (Math.abs(p.s() - s) < Math.abs(best.s() - s)) {
                    best = p;
                }
            }
        }
        return best;
    }
}

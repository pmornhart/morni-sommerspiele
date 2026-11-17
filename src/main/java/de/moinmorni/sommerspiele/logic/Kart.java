package de.moinmorni.sommerspiele.logic;

import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * Fahrphysik der Seifenkiste (ohne Minecraft), Tick für Tick entlang der Schienen: Bergab wird die Lore schneller
 * (Lageenergie wird Tempo), Rollwiderstand und Fahrtwind bremsen. Mit <b>vorwärts</b> schubst man sich an, solange man
 * langsam ist, mit <b>rückwärts</b> bremst man. Wer schneller als {@link #CURVE_LIMIT} durch eine Kurve fährt,
 * <b>entgleist</b>: die Lore steht kurz und fährt dann vor der Kurve aus dem Stand weiter – das kostet Zeit.
 *
 * <p>Einheiten: Strecke in Blöcken, Tempo in Blöcken pro Tick (1 Block/Tick = 72 km/h).
 */
public final class Kart {

    /** Schwerkraft: so viel v² gewinnt man je Block Höhe (×2). */
    public static final double GRAVITY = 0.05;
    /** Rollwiderstand je Tick. */
    public static final double ROLL = 0.001;
    /** Fahrtwind: Bremse ∝ v². */
    public static final double DRAG = 0.0016;
    /** Anschubsen (vorwärts): so viel mehr Tempo je Tick, aber nur bis {@link #PUSH_MAX}. */
    public static final double PUSH = 0.02;
    public static final double PUSH_MAX = 0.32;
    /** Bremse (rückwärts) je Tick. */
    public static final double BRAKE = 0.04;
    /** Höchsttempo (mehr schafft keine Kiste). */
    public static final double MAX = 1.6;
    /** Schneller als das durch eine Kurve = entgleist. */
    public static final double CURVE_LIMIT = 0.72;
    /** So viele Ticks steht man nach dem Entgleisen. */
    public static final int CRASH_TICKS = 40;
    /** So weit vor der Kurve geht es danach weiter. */
    public static final double CRASH_BACK = 2.5;
    /** Tempo beim Startschuss (der Helfer schubst an). */
    public static final double START_PUSH = 0.18;

    /** Was in einem Tick passiert ist. */
    public enum Event { NONE, CRASH, FINISH }

    private final DoubleUnaryOperator height;
    private final List<Double> curves;
    private final double finishS;
    private double s;
    private double v;
    private int stopped;
    private int crashes;
    /** Anteil des Ticks, an dem die Ziellinie gekreuzt wurde. */
    private double finishFraction = 1;

    /**
     * @param height  Schienenhöhe zur Strecke s
     * @param curves  Strecke s der Kurven (aufsteigend)
     * @param startS  Start
     * @param finishS Ziellinie
     */
    public Kart(DoubleUnaryOperator height, List<Double> curves, double startS, double finishS) {
        this.height = height;
        this.curves = List.copyOf(curves);
        this.s = startS;
        this.finishS = finishS;
    }

    public double s() {
        return s;
    }

    public double v() {
        return v;
    }

    public int crashes() {
        return crashes;
    }

    /** Steht die Lore nach dem Entgleisen noch? */
    public boolean stopped() {
        return stopped > 0;
    }

    public double finishFraction() {
        return finishFraction;
    }

    /** Startschuss. */
    public void go() {
        v = Math.max(v, START_PUSH);
    }

    /** Auf einen anderen Weg wechseln (Weiche) – Strecke und Tempo bleiben. */
    public Kart onto(DoubleUnaryOperator h, List<Double> c, double finish) {
        Kart k = new Kart(h, c, s, finish);
        k.v = v;
        k.stopped = stopped;
        k.crashes = crashes;
        return k;
    }

    /** Zurück an eine Stelle (Zurücksetzen): aus dem Stand. */
    public void resetTo(double at) {
        s = at;
        v = 0;
        stopped = 0;
    }

    /** Nächste Kurve ab der jetzigen Stelle (oder NaN). */
    public double nextCurve() {
        for (double c : curves) {
            if (c > s) {
                return c;
            }
        }
        return Double.NaN;
    }

    /** Ein Tick. */
    public Event tick(boolean push, boolean brake) {
        if (stopped > 0) {
            stopped--;
            return Event.NONE;
        }
        double next = s + v;
        double drop = height.applyAsDouble(s) - height.applyAsDouble(next);
        double v2 = v * v + 2 * GRAVITY * drop;
        double nv = Math.sqrt(Math.max(0, v2));
        nv -= ROLL + DRAG * nv * nv;
        if (push && nv < PUSH_MAX) {
            nv = Math.min(PUSH_MAX, nv + PUSH);
        }
        if (brake) {
            nv -= BRAKE;
        }
        nv = Math.max(0, Math.min(MAX, nv));
        double from = s;
        double to = s + nv;
        for (double c : curves) {
            if (c > from && c <= to && nv > CURVE_LIMIT) {
                crashes++;
                s = Math.max(0, c - CRASH_BACK);
                v = 0;
                stopped = CRASH_TICKS;
                return Event.CRASH;
            }
        }
        v = nv;
        s = to;
        if (from < finishS && to >= finishS) {
            finishFraction = nv <= 0 ? 1 : (finishS - from) / nv;
            return Event.FINISH;
        }
        return Event.NONE;
    }

    /**
     * Kluges Bremsen (Selbstfahrer für Tests und Prüfung): bremst gerade so, dass man mit {@code limit} in die nächste
     * Kurve kommt, und schubst an, wenn man langsam ist.
     *
     * @return {push, brake}
     */
    public boolean[] pilot(double margin) {
        double c = nextCurve();
        boolean brake = false;
        if (!Double.isNaN(c)) {
            double target = CURVE_LIMIT - margin;
            double dist = c - s;
            // Weg, den man zum Abbremsen auf target braucht (plus der nächste Tick).
            double need = v > target ? (v * v - target * target) / (2 * BRAKE) + 2 * v : 0;
            brake = v > target && dist <= need;
        }
        return new boolean[] {!brake && v < PUSH_MAX, brake};
    }
}

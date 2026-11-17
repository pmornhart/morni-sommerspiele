package de.moinmorni.sommerspiele.course;

import de.moinmorni.sommerspiele.logic.Gate;
import de.moinmorni.sommerspiele.logic.Line;
import java.util.ArrayList;
import java.util.List;

/**
 * Ruderregatta auf dem Bergsee: vier Bahnen nebeneinander, von Norden (Startsteg) nach Süden (Ziel vor der Tribüne).
 * Jede Bahn ist elf Blöcke breit, zwischen den Bahnen liegen Holzstege. Acht Bojen-Tore: je zwei Bojen, abwechselnd
 * links und rechts der Bahnmitte – zwischen den Bojen durch, sonst Strafe. Dazu Strömungen (der Wind treibt das Boot
 * seitlich) und Strudel (wer darüber rudert, wird hinabgezogen und muss neu ansetzen).
 */
public final class Lake {

    /** Mittlere Blockspalte jeder Bahn. */
    public static final int[] LANE_X = {84, 98, 112, 126};
    /** Wasser bis Mitte ± 5 (elf Blöcke), dann ein Steg. */
    public static final int HALF = 5;
    /** Ausdehnung des Sees (Wasser). */
    public static final int X1 = LANE_X[0] - HALF - 3;
    public static final int X2 = LANE_X[LANE_X.length - 1] + HALF + 3;
    public static final int Z1 = -30;
    public static final int Z2 = 150;
    /** Tiefe (Wasser von y = −DEPTH+1 bis y = 0). */
    public static final int DEPTH = 3;
    /** Startsteg (Nordende) und Startlinie. */
    public static final double START_Z = -22.5;
    public static final int START_DOCK_Z = -26;
    public static final double FINISH_Z = 132.0;
    public static final int GATES = 8;
    private static final int FIRST_GATE_Z = -4;
    private static final int GATE_EVERY = 17;
    /** Halbe Breite der Toröffnung und Versatz der Tore von der Bahnmitte. */
    private static final double GATE_HALF = 2.0;
    private static final double GATE_OFFSET = 2.5;
    private static final double LINE_HALF = HALF + 0.5;

    /** Eine Strömung: z-Bereich, Richtung (+1 = nach Osten, −1 = nach Westen). */
    public record Current(int z1, int z2, int dir) {
    }

    /** Strömungen in jeder Bahn gleich. */
    public static final List<Current> CURRENTS = List.of(new Current(4, 18, 1), new Current(55, 69, -1),
            new Current(96, 110, 1));

    private Lake() {
    }

    public static int lanes() {
        return LANE_X.length;
    }

    public static double centerX(int lane) {
        return LANE_X[lane] + 0.5;
    }

    /** Reihe (z) des Tors g. */
    public static int gateRow(int g) {
        return FIRST_GATE_Z + GATE_EVERY * g;
    }

    /** Linkes Tor (Westen) für gerade Nummern, rechtes für ungerade. */
    public static boolean leftGate(int g) {
        return g % 2 == 0;
    }

    /** Mitte der Toröffnung (x) in einer Bahn. */
    public static double gateMiddle(int lane, int g) {
        return centerX(lane) + (leftGate(g) ? -GATE_OFFSET : GATE_OFFSET);
    }

    public static List<Gate> gates(int lane) {
        double cx = centerX(lane);
        List<Gate> out = new ArrayList<>();
        for (int g = 0; g < GATES; g++) {
            double z = gateRow(g) + 0.5;
            Line line = Line.acrossZ(z, cx - LINE_HALF, cx + LINE_HALF);
            double m = gateMiddle(lane, g);
            double a = m - GATE_HALF;
            double b = m + GATE_HALF;
            out.add(new Gate(line, (a - line.ax()) / line.length(), (b - line.ax()) / line.length(), leftGate(g)));
        }
        return out;
    }

    /** Bojen (Blockspalten x) eines Tors: links und rechts der Öffnung. */
    public static int[] buoys(int lane, int g) {
        double m = gateMiddle(lane, g);
        return new int[] {(int) Math.floor(m - GATE_HALF - 0.5), (int) Math.floor(m + GATE_HALF + 0.5)};
    }

    /**
     * Strudel einer Bahn (x, z): zwischen den Toren auf der Linie, die man ohne Nachdenken fahren würde – darum herum
     * muss man steuern.
     */
    public static List<int[]> whirlpools(int lane) {
        List<int[]> out = new ArrayList<>();
        int[] after = {1, 3, 5};
        for (int g : after) {
            int z = gateRow(g) + GATE_EVERY / 2;
            double mid = (gateMiddle(lane, g) + gateMiddle(lane, g + 1)) / 2;
            out.add(new int[] {(int) Math.floor(mid), z});
        }
        return out;
    }

    public static Line finish(int lane) {
        double cx = centerX(lane);
        return Line.acrossZ(FINISH_Z, cx - LINE_HALF, cx + LINE_HALF);
    }

    /** Ist (x, z) Wasser der Bahn? */
    public static boolean inLane(int lane, int x, int z) {
        return Math.abs(x - LANE_X[lane]) <= HALF && z >= Z1 && z <= Z2;
    }

    /** Ist (x, z) überhaupt Wasser? Bahnen ab dem Startsteg, hinter dem Ziel ist der See offen. */
    public static boolean water(int x, int z) {
        if (z <= START_DOCK_Z || z > Z2 || x < X1 || x > X2) {
            return false;
        }
        if (z > FINISH_Z + 3) {
            return true;
        }
        for (int lane = 0; lane < lanes(); lane++) {
            if (inLane(lane, x, z)) {
                return true;
            }
        }
        return false;
    }

    /** Ist (x, z) Steg (zwischen und neben den Bahnen, am Start)? */
    public static boolean dock(int x, int z) {
        return x >= X1 && x <= X2 && z >= Z1 && z <= Z2 && !water(x, z);
    }

    /** Strömung an dieser Stelle (+1 Osten, −1 Westen, 0 keine). */
    public static int current(double z) {
        for (Current c : CURRENTS) {
            if (z >= c.z1() && z <= c.z2() + 1) {
                return c.dir();
            }
        }
        return 0;
    }

    /** Wohin man nach dem Tor g zurückgesetzt wird (−1 = Start). */
    public static double[] resetAfter(int lane, int g) {
        if (g < 0) {
            return new double[] {centerX(lane), START_Z};
        }
        return new double[] {gateMiddle(lane, g), gateRow(g) + 3.5};
    }
}

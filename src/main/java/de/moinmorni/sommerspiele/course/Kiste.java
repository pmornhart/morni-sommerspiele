package de.moinmorni.sommerspiele.course;

import de.moinmorni.sommerspiele.logic.Line;
import java.util.ArrayList;
import java.util.List;

/**
 * Seifenkisten-Bahn: vier gleiche Schienenstrecken nebeneinander den Kistenberg hinab. Jede schlängelt sich in Kehren
 * über die Terrassen (Kurven auf den flachen Stücken, schräge Schienen auf den Absätzen). Auf Terrasse {@link #FORK}
 * liegt eine <b>Weiche</b>: rechts in die zwei Kehren (sicher) oder geradeaus das <b>Steilstück</b> über zwei
 * Terrassen hinab (kürzer, aber man kommt schnell an die nächste Kurve). Danach treffen sich beide Wege wieder.
 *
 * <p>Die Mittellinie folgt genau den Schienen: gerade Stücke längs der Blockmitten, jede Kurve ist eine einzige
 * Kurvenschiene (Viertelkreis mit Radius einem halben Block).
 */
public final class Kiste {

    /** Westlichste Spalte jeder Bahn (die Kehren gehen {@link #SWING} Blöcke nach Osten und zurück). */
    public static final int[] LANE_X = {-170, -154, -138, -122};
    public static final int SWING = 10;
    /** Kurvenradius der Mittellinie (eine Kurvenschiene). */
    public static final double RADIUS = 0.5;
    /** Erste Reihe der Bahn (im Starthaus). */
    public static final double TOP_Z = -184.5;
    /** Wo die Lore vor dem Start steht. */
    public static final double START_Z = -181.5;
    public static final double FINISH_Z = -34.0;
    /** Bis hier Schienen zum Ausrollen, dann ein Prellbock. */
    public static final double END_Z = -24.5;
    /** Terrasse mit der Weiche (das Steilstück geht über diese und die nächste Terrasse). */
    public static final int FORK = 4;
    /** Kontrollpunkte (zum Zurücksetzen) alle so viele Blöcke Strecke. */
    private static final double CHECK_EVERY = 14;

    private static final Path[][] PATHS = new Path[LANE_X.length][2];

    private Kiste() {
    }

    public static int lanes() {
        return LANE_X.length;
    }

    /** x-Mitte der Schiene bei Querversatz {@code rel} (0 oder {@link #SWING}). */
    public static double x(int lane, int rel) {
        return LANE_X[lane] + rel + 0.5;
    }

    /**
     * Mittellinie der Bahn.
     *
     * @param steep true = an der Weiche geradeaus das Steilstück, false = rechts in die Kehren
     */
    public static synchronized Path path(int lane, boolean steep) {
        int v = steep ? 1 : 0;
        if (PATHS[lane][v] == null) {
            PATHS[lane][v] = new Path(corners(lane, steep), RADIUS);
        }
        return PATHS[lane][v];
    }

    static List<double[]> corners(int lane, boolean steep) {
        List<double[]> c = new ArrayList<>();
        c.add(new double[] {x(lane, 0), TOP_Z});
        int side = 0;
        for (int i = Terrain.TERRACES - 1; i >= 0; i--) {
            double z = Terrain.terraceMiddle(i);
            if (steep && (i == FORK || i == FORK - 1)) {
                // Steilstück: ohne Kehre geradeaus; die Seite bleibt (zwei Kehren hin und zurück entfallen).
                continue;
            }
            c.add(new double[] {x(lane, side), z});
            side = side == 0 ? SWING : 0;
            c.add(new double[] {x(lane, side), z});
        }
        c.add(new double[] {x(lane, side), END_Z});
        return c;
    }

    /** Wo die Weiche ist (Strecke s auf beiden Wegen gleich): an der ersten Kurve der Terrasse {@link #FORK}. */
    public static double forkS(int lane) {
        Path p = path(lane, false);
        int i = p.nearest(x(lane, forkSide()), Terrain.terraceMiddle(FORK), -1);
        return p.samples().get(i).s() - 1.0;
    }

    /** Seite (0 oder {@link #SWING}), auf der man an der Weiche ankommt. */
    public static int forkSide() {
        return (Terrain.TERRACES - 1 - FORK) % 2 == 0 ? 0 : SWING;
    }

    /** x-Mitte am Ziel. */
    public static double finishX(int lane) {
        return x(lane, Terrain.TERRACES % 2 == 0 ? 0 : SWING);
    }

    public static Line finish(int lane) {
        double cx = finishX(lane);
        return Line.acrossZ(FINISH_Z, cx - 3, cx + 3);
    }

    /** Höhe der Schiene (relativ zum Tal) an einem Punkt der Strecke. */
    public static double railY(Path.Sample s) {
        return Terrain.railY(s.z());
    }

    /** Stellen zum Zurücksetzen auf einem Weg: Strecke s, alle {@link #CHECK_EVERY} Blöcke, ab dem Start. */
    public static List<Double> checkpoints(int lane, boolean steep) {
        Path p = path(lane, steep);
        List<Double> out = new ArrayList<>();
        double startS = p.samples().get(p.nearest(x(lane, 0), START_Z, -1)).s();
        out.add(startS);
        for (double s = startS + CHECK_EVERY; s < p.length() - 16; s += CHECK_EVERY) {
            out.add(s);
        }
        return out;
    }

    /** Strecke s, an der die Lore vor dem Start steht. */
    public static double startS(int lane) {
        Path p = path(lane, false);
        return p.samples().get(p.nearest(x(lane, 0), START_Z, -1)).s();
    }

    /** Strecke s der Ziellinie auf einem Weg. */
    public static double finishS(int lane, boolean steep) {
        Path p = path(lane, steep);
        return p.samples().get(p.nearest(finishX(lane), FINISH_Z, -1)).s();
    }

    /**
     * Kurven eines Wegs als Strecke s (Mitte der Kurvenschiene) – hier gilt das Kurventempo.
     */
    public static List<Double> curves(int lane, boolean steep) {
        List<Double> out = new ArrayList<>();
        Path p = path(lane, steep);
        List<Path.Sample> ss = p.samples();
        for (int i = 1; i < ss.size() - 1; i++) {
            float a = ss.get(i - 1).yaw();
            float b = ss.get(i + 1).yaw();
            if (Math.abs(Path.turn(a, b)) > 1f) {
                double s = ss.get(i).s();
                if (out.isEmpty() || s - out.get(out.size() - 1) > 1.2) {
                    out.add(s);
                } else {
                    out.set(out.size() - 1, (out.get(out.size() - 1) + s) / 2);
                }
            }
        }
        return out;
    }
}

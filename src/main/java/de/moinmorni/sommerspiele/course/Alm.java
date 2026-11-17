package de.moinmorni.sommerspiele.course;

import de.moinmorni.sommerspiele.logic.Line;
import java.util.ArrayList;
import java.util.List;

/**
 * Almlauf: ein Rundkurs über die Almwiese südlich vom Dorf, sieben Blöcke breit, links und rechts ein Zaun. Gelaufen
 * wird im Uhrzeigersinn: vom Start nach Norden, oben über die Heuballen-Hürden und einen Baumstamm, an der Ostseite
 * über den Bach (Trittsteine!) und durchs Moor (Bohlen!), im Süden quer über die Kuhweide mit zwei Gattern, im Westen
 * im Slalom um die Holzstöße und über die Heutreppe zurück zur Ziellinie.
 */
public final class Alm {

    /** Halbe Breite des Wegs (Mitte ± 3). */
    public static final int HALF = 3;
    /** Zaun in diesem Abstand von der Mitte. */
    public static final int FENCE = HALF + 1;
    public static final double RADIUS = 6;
    public static final int WEST_X = -28;
    public static final int EAST_X = 52;
    public static final int NORTH_Z = 82;
    public static final int SOUTH_Z = 164;
    /** Start- und Ziellinie quer über die Weststrecke (gelaufen wird nach Norden). */
    public static final double LAP_Z = 100.0;
    /** Kuhweide (die Kühe bleiben darin). */
    public static final int[] PASTURE = {0, SOUTH_Z - HALF, 30, SOUTH_Z + HALF};
    private static final int CHECKPOINTS = 8;

    /** Hindernis: Rechteck (Blöcke, einschließlich) und Art. */
    public record Obstacle(int x1, int z1, int x2, int z2, Kind kind) {
        public boolean contains(int x, int z) {
            return x >= x1 && x <= x2 && z >= z1 && z <= z2;
        }
    }

    /** Arten von Hindernissen. */
    public enum Kind {
        /** Heuballen-Reihe, einen Block hoch (drüberspringen). */
        HAY(1),
        /** Baumstamm quer über den Weg (drüberspringen). */
        LOG(1),
        /** Gatter an der Kuhweide (drüberspringen). */
        GATE(1),
        /** Bachwasser (man watet langsam). */
        STREAM(0),
        /** Trittstein im Bach (auf Bodenhöhe – wer ihn trifft, bleibt trocken). */
        STONE(0),
        /** Moor (Seelensand bremst stark). */
        MOOR(0),
        /** Bohle durchs Moor. */
        PLANK(0),
        /** Holzstoß, zwei hoch (außen herum laufen). */
        PILE(2),
        /** Heutreppe, erste Stufe. */
        STEP1(1),
        /** Heutreppe, zweite Stufe. */
        STEP2(2);

        private final int height;

        Kind(int height) {
            this.height = height;
        }

        public int height() {
            return height;
        }
    }

    private static Path loop;
    private static List<Obstacle> obstacles;

    private Alm() {
    }

    /** Der Rundkurs ab der Startlinie (nach Norden), einmal herum bis wieder zur Linie. */
    public static synchronized Path loop() {
        if (loop == null) {
            List<double[]> c = new ArrayList<>();
            c.add(new double[] {WEST_X + 0.5, LAP_Z + 0.5});
            c.add(new double[] {WEST_X + 0.5, NORTH_Z + 0.5});
            c.add(new double[] {EAST_X + 0.5, NORTH_Z + 0.5});
            c.add(new double[] {EAST_X + 0.5, SOUTH_Z + 0.5});
            c.add(new double[] {WEST_X + 0.5, SOUTH_Z + 0.5});
            c.add(new double[] {WEST_X + 0.5, LAP_Z + 0.5});
            loop = new Path(c, RADIUS);
        }
        return loop;
    }

    /** Alle Hindernisse (fest, gleich bei jedem Bau). */
    public static synchronized List<Obstacle> obstacles() {
        if (obstacles == null) {
            List<Obstacle> o = new ArrayList<>();
            int w1 = WEST_X - HALF;
            int w2 = WEST_X + HALF;
            int e1 = EAST_X - HALF;
            int e2 = EAST_X + HALF;
            int n1 = NORTH_Z - HALF;
            int n2 = NORTH_Z + HALF;
            int s1 = SOUTH_Z - HALF;
            int s2 = SOUTH_Z + HALF;
            // Norden: drei Heuballen-Reihen und ein Baumstamm
            for (int x : new int[] {-10, 2, 14}) {
                o.add(new Obstacle(x, n1, x, n2, Kind.HAY));
            }
            o.add(new Obstacle(30, n1, 30, n2, Kind.LOG));
            // Osten: Bach mit Trittsteinen, dann das Moor mit Bohlen
            o.add(new Obstacle(e1, 104, e2, 108, Kind.STREAM));
            int[][] stones = {{EAST_X - 2, 104}, {EAST_X - 1, 105}, {EAST_X, 106}, {EAST_X + 1, 107}, {EAST_X + 2, 108},
                {EAST_X + 2, 104}, {EAST_X + 3, 106}};
            for (int[] s : stones) {
                o.add(new Obstacle(s[0], s[1], s[0], s[1], Kind.STONE));
            }
            o.add(new Obstacle(e1, 124, e2, 132, Kind.MOOR));
            o.add(new Obstacle(EAST_X + 1, 124, EAST_X + 1, 127, Kind.PLANK));
            o.add(new Obstacle(EAST_X - 1, 127, EAST_X + 1, 127, Kind.PLANK));
            o.add(new Obstacle(EAST_X - 1, 128, EAST_X - 1, 132, Kind.PLANK));
            // Süden: Gatter am Ein- und Ausgang der Kuhweide
            o.add(new Obstacle(PASTURE[2], s1, PASTURE[2], s2, Kind.GATE));
            o.add(new Obstacle(PASTURE[0], s1, PASTURE[0], s2, Kind.GATE));
            // Westen (nach Norden): Holzstöße im Slalom, dann die Heutreppe
            int side = 0;
            for (int z : new int[] {150, 142, 134, 126}) {
                if (side == 0) {
                    o.add(new Obstacle(w1, z, WEST_X, z + 1, Kind.PILE));
                } else {
                    o.add(new Obstacle(WEST_X, z, w2, z + 1, Kind.PILE));
                }
                side = 1 - side;
            }
            o.add(new Obstacle(w1, 116, w2, 116, Kind.STEP1));
            o.add(new Obstacle(w1, 115, w2, 115, Kind.STEP2));
            o.add(new Obstacle(w1, 114, w2, 114, Kind.STEP1));
            obstacles = List.copyOf(o);
        }
        return obstacles;
    }

    /** Hindernis an dieser Stelle (das zuletzt eingetragene gewinnt: Trittsteine liegen im Bach) oder null. */
    public static Obstacle at(int x, int z) {
        Obstacle found = null;
        for (Obstacle o : obstacles()) {
            if (o.contains(x, z)) {
                found = o;
            }
        }
        return found;
    }

    /** Planhöhe, auf der man an dieser Stelle steht (oben auf einem Hindernis). */
    public static int standY(double x, double z) {
        Obstacle o = at((int) Math.floor(x), (int) Math.floor(z));
        return 1 + (o == null ? 0 : o.kind().height());
    }

    /** Liegt (x, z) auf dem Weg (innerhalb der Zäune)? */
    public static boolean onTrack(double x, double z) {
        return loop().distance(x, z) <= HALF + 0.5;
    }

    /** Startlinie / Ziellinie. */
    public static Line lapLine() {
        return Line.acrossZ(LAP_Z, WEST_X - HALF - 0.5, WEST_X + HALF + 1.5);
    }

    /** Kontrollpunkte (Linien quer über den Weg), gleichmäßig verteilt, in Laufrichtung. */
    public static List<Line> checkpoints() {
        List<Line> out = new ArrayList<>();
        Path p = loop();
        double len = p.length();
        for (int i = 1; i <= CHECKPOINTS; i++) {
            Path.Sample s = p.at(len * i / (CHECKPOINTS + 1));
            double a = Math.toRadians(s.yaw());
            // Richtung (Minecraft-Yaw): dx = −sin, dz = cos – quer dazu die Linie.
            double nx = Math.cos(a);
            double nz = Math.sin(a);
            double r = HALF + 1.5;
            out.add(new Line(s.x() - nx * r, s.z() - nz * r, s.x() + nx * r, s.z() + nz * r));
        }
        return out;
    }

    /** Wo ein Kontrollpunkt liegt (zum Zurücksetzen): Strecke s; −1 = Start. */
    public static double checkpointS(int index) {
        Path p = loop();
        return index < 0 ? 0 : p.length() * (index + 1) / (CHECKPOINTS + 1);
    }

    /** Startplätze hinter der Linie (drei nebeneinander, vier Reihen). */
    public static double[] grid(int i) {
        int col = i % 3;
        int row = i / 3;
        return new double[] {WEST_X + 0.5 + (col - 1) * 2, LAP_Z + 2.5 + row * 2};
    }
}

package de.moinmorni.sommerspiele.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.sommerspiele.logic.Archery;
import de.moinmorni.sommerspiele.logic.Gate;
import de.moinmorni.sommerspiele.logic.Kart;
import de.moinmorni.sommerspiele.logic.Line;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Alle Strecken ohne Server: fahrbar, lösbar, nichts überschneidet sich. */
class CourseTest {

    @Test
    void terrainRisesInTerracesOfSingleSteps() {
        assertEquals(0, Terrain.height(-40));
        assertEquals(0, Terrain.height(50));
        assertEquals(1, Terrain.height(-41));
        assertEquals(3, Terrain.height(-43));
        assertEquals(3, Terrain.height(-56));
        assertEquals(4, Terrain.height(-57));
        assertEquals(Terrain.TOP, Terrain.height(-200));
        for (int z = -200; z < 0; z++) {
            int diff = Terrain.height(z) - Terrain.height(z + 1);
            assertTrue(diff == 0 || diff == 1, "nach Süden nie mehr als ein Block auf einmal: z=" + z);
        }
    }

    /** Schienen: gerade Stücke in Blockmitte, schräg nur geradeaus, Kurven nur auf ebenem Boden. */
    @Test
    void railsCanBeLaid() {
        for (int lane = 0; lane < Kiste.lanes(); lane++) {
            for (boolean steep : new boolean[] {false, true}) {
                Path p = Kiste.path(lane, steep);
                for (Path.Sample s : p.samples()) {
                    double fx = s.x() - Math.floor(s.x());
                    double fz = s.z() - Math.floor(s.z());
                    boolean onLineX = Math.abs(fx - 0.5) < 1e-6;
                    boolean onLineZ = Math.abs(fz - 0.5) < 1e-6;
                    if (!onLineX && !onLineZ) {
                        // Kurve: in diesem Block und den Nachbarn muss es eben sein
                        int z = (int) Math.floor(s.z());
                        assertFalse(Terrain.ascending(z), "Kurve auf einem Absatz: lane " + lane + " z=" + z);
                    }
                }
                for (double c : Kiste.curves(lane, steep)) {
                    int z = (int) Math.floor(p.at(c).z());
                    assertFalse(Terrain.ascending(z) || Terrain.ascending(z + 1), "Kurve am Absatz z=" + z);
                }
                assertTrue(Kiste.curves(lane, steep).size() >= 10);
            }
            assertEquals(Kiste.curves(lane, false).size() - 4, Kiste.curves(lane, true).size(), "Steilstück spart vier Kurven");
        }
    }

    /** Vor der Weiche sind beide Wege genau gleich, danach treffen sie sich wieder. */
    @Test
    void forkSharesTheWayUntilTheSwitch() {
        for (int lane = 0; lane < Kiste.lanes(); lane++) {
            Path a = Kiste.path(lane, false);
            Path b = Kiste.path(lane, true);
            double fork = Kiste.forkS(lane);
            for (double s = 0; s < fork; s += 0.5) {
                assertEquals(a.at(s).x(), b.at(s).x(), 1e-6);
                assertEquals(a.at(s).z(), b.at(s).z(), 1e-6);
            }
            assertEquals(Kiste.finishX(lane), a.at(Kiste.finishS(lane, false)).x(), 1e-6);
            assertEquals(Kiste.finishX(lane), b.at(Kiste.finishS(lane, true)).x(), 1e-6);
        }
    }

    /** Bahnen der Seifenkiste überschneiden sich nicht (auch nicht mit dem Steilstück). */
    @Test
    void kisteLanesDontTouch() {
        for (int lane = 0; lane + 1 < Kiste.lanes(); lane++) {
            for (Path.Sample s : Kiste.path(lane, false).samples()) {
                assertTrue(Kiste.path(lane + 1, true).distance(s.x(), s.z()) > 3, "Bahn " + lane);
            }
        }
    }

    private static int[] drive(int lane, boolean steep, double margin, boolean brake) {
        Path p = Kiste.path(lane, steep);
        Kart k = new Kart(s -> Kiste.railY(p.at(s)), Kiste.curves(lane, steep), Kiste.startS(lane), Kiste.finishS(lane, steep));
        k.go();
        for (int t = 1; t < 6000; t++) {
            boolean[] in = k.pilot(margin);
            if (k.tick(in[0], brake && in[1]) == Kart.Event.FINISH) {
                return new int[] {t, k.crashes()};
            }
        }
        return new int[] {-1, k.crashes()};
    }

    /** Fahrbar: wer klug bremst, kommt ohne Entgleisen ins Ziel; ohne Bremse entgleist man und ist langsamer. */
    @Test
    void soapboxIsDrivableAndBrakingPays() {
        for (int lane = 0; lane < Kiste.lanes(); lane++) {
            for (boolean steep : new boolean[] {false, true}) {
                int[] good = drive(lane, steep, 0.02, true);
                int[] careful = drive(lane, steep, 0.15, true);
                int[] wild = drive(lane, steep, 0, false);
                assertTrue(good[0] > 0 && good[1] == 0, "klug gebremst ins Ziel");
                assertTrue(good[0] < careful[0], "spät bremsen ist schneller als früh bremsen");
                assertTrue(wild[1] > 0, "ohne Bremse entgleist man");
                assertTrue(wild[0] > good[0], "Entgleisen kostet Zeit");
                assertTrue(good[0] > 15 * 20 && good[0] < 40 * 20, "eine Fahrt dauert 15–40 s: " + good[0]);
            }
            assertTrue(drive(lane, true, 0.02, true)[0] < drive(lane, false, 0.02, true)[0], "Steilstück ist (gut gefahren) schneller");
        }
    }

    /** Regatta: Tore liegen im Wasser der Bahn, Bojen auch, Strudel nicht in einer Toröffnung. */
    @Test
    void regattaGatesAndWhirlpoolsFit() {
        for (int lane = 0; lane < Lake.lanes(); lane++) {
            List<Gate> gates = Lake.gates(lane);
            assertEquals(Lake.GATES, gates.size());
            for (int g = 0; g < Lake.GATES; g++) {
                for (int bx : Lake.buoys(lane, g)) {
                    assertTrue(Lake.inLane(lane, bx, Lake.gateRow(g)), "Boje im Wasser");
                }
                double m = Lake.gateMiddle(lane, g);
                assertTrue(Lake.water((int) Math.floor(m), Lake.gateRow(g)));
                int[] b = Lake.buoys(lane, g);
                assertEquals(4, b[1] - b[0] - 1, "vier Blöcke Durchfahrt");
                assertEquals(Gate.Pass.PASSED, gates.get(g).check(m, Lake.gateRow(g) - 1, m, Lake.gateRow(g) + 2));
                double wrong = 2 * Lake.centerX(lane) - m;
                assertEquals(Gate.Pass.MISSED, gates.get(g).check(wrong, Lake.gateRow(g) - 1, wrong, Lake.gateRow(g) + 2));
            }
            for (int[] w : Lake.whirlpools(lane)) {
                assertTrue(Lake.inLane(lane, w[0], w[1]));
                for (int g = 0; g < Lake.GATES; g++) {
                    assertTrue(Math.abs(w[1] - Lake.gateRow(g)) >= 3, "Strudel nicht im Tor");
                }
            }
            assertTrue(Lake.water((int) Lake.centerX(lane), (int) Math.floor(Lake.START_Z)), "Start im Wasser");
            assertTrue(Lake.water((int) Lake.centerX(lane), (int) Lake.FINISH_Z + 2), "Ziel im Wasser");
            // Nachbarbahnen durch einen Steg getrennt
            assertFalse(Lake.water(Lake.LANE_X[lane] + Lake.HALF + 1, 50));
        }
        assertEquals(1, Lake.current(10));
        assertEquals(-1, Lake.current(60));
        assertEquals(0, Lake.current(40));
    }

    /** Bogenschießen: Ringe, Zielen mit Schwerkraft und Wind trifft die Mitte, Wind treibt spürbar ab. */
    @Test
    void archeryRingsAimAndWind() {
        assertEquals(10, Archery.score(0));
        assertEquals(10, Archery.score(0.24));
        assertEquals(9, Archery.score(0.26));
        assertEquals(1, Archery.score(2.4));
        assertEquals(0, Archery.score(2.6));
        assertTrue(Archery.centre(0.1));
        for (int d : Range.DISTANCES) {
            for (int w = -Archery.MAX_WIND; w <= Archery.MAX_WIND; w++) {
                double[] aim = Archery.aim(d, 1.0, Archery.ARROW_SPEED, w);
                Archery.Flight f = Archery.fly(d, Archery.ARROW_SPEED, aim[0], aim[1], w);
                assertTrue(Math.hypot(f.side(), f.drop() - 1.0) < 0.05, "Mitte auf " + d + " bei Wind " + w);
            }
            Archery.Flight flat = Archery.fly(d, Archery.ARROW_SPEED, 0, 0, 0);
            assertTrue(flat.drop() < -0.2, "Pfeil fällt auf " + d + ": " + flat.drop());
        }
        Archery.Flight windy = Archery.fly(50, Archery.ARROW_SPEED, 3, 0, 3);
        assertTrue(windy.side() > 0.4 && windy.side() < 2.5, "starker Wind treibt auf 50 Blöcke ab: " + windy.side());
        assertTrue(Range.laneX(Range.LANES - 1) - Range.laneX(0) == (Range.LANES - 1) * Range.SPACING);
        assertTrue(Range.SPACING > 2 * Range.FACE_HALF + 1, "Scheiben berühren sich nicht");
    }

    /** Almlauf: Rundkurs geschlossen, Hindernisse auf dem Weg, Startplätze frei, Kontrollpunkte der Reihe nach. */
    @Test
    void almCourseIsSound() {
        Path loop = Alm.loop();
        Path.Sample first = loop.samples().get(0);
        Path.Sample last = loop.samples().get(loop.samples().size() - 1);
        assertTrue(Math.hypot(first.x() - last.x(), first.z() - last.z()) < 0.01, "geschlossen");
        assertTrue(loop.length() > 250 && loop.length() < 400, "Länge " + loop.length());
        for (Alm.Obstacle o : Alm.obstacles()) {
            assertTrue(Alm.onTrack(o.x1() + 0.5, o.z1() + 0.5) && Alm.onTrack(o.x2() + 0.5, o.z2() + 0.5), "auf dem Weg: " + o);
            assertTrue(o.kind().height() <= 2);
        }
        for (int i = 0; i < 12; i++) {
            double[] g = Alm.grid(i);
            assertTrue(Alm.onTrack(g[0], g[1]), "Startplatz " + i);
            assertEquals(1, Alm.standY(g[0], g[1]), "Startplatz frei");
            assertTrue(g[1] > Alm.LAP_Z, "hinter der Linie");
        }
        // Jede Hürde ist höchstens einen Block höher als der Schritt davor (springbar), den Weg entlang –
        // die Holzstöße (zwei hoch) stehen im Slalom und lassen jeweils drei Spalten frei.
        int prev = 1;
        for (Path.Sample s : loop.samples()) {
            Alm.Obstacle o = Alm.at((int) Math.floor(s.x()), (int) Math.floor(s.z()));
            if (o != null && o.kind() == Alm.Kind.PILE) {
                int free = 0;
                for (int x = Alm.WEST_X - Alm.HALF; x <= Alm.WEST_X + Alm.HALF; x++) {
                    if (Alm.at(x, o.z1()) == null) {
                        free++;
                    }
                }
                assertTrue(free >= 3, "Lücke am Holzstoß z=" + o.z1());
                continue;
            }
            int y = Alm.standY(s.x(), s.z());
            assertTrue(y - prev <= 1, "springbar bei " + s.x() + "/" + s.z());
            prev = y;
        }
        // Der Weg entlang kreuzt jeden Kontrollpunkt genau einmal und der Reihe nach, danach die Linie.
        List<Line> cps = Alm.checkpoints();
        int next = 0;
        boolean lap = false;
        for (int i = 1; i < loop.samples().size(); i++) {
            Path.Sample a = loop.samples().get(i - 1);
            Path.Sample b = loop.samples().get(i);
            for (int c = 0; c < cps.size(); c++) {
                if (cps.get(c).cross(a.x(), a.z(), b.x(), b.z()) != null) {
                    assertEquals(next, c, "Reihenfolge");
                    next++;
                }
            }
            if (Alm.lapLine().cross(a.x(), a.z(), b.x(), b.z()) != null && next > 0) {
                lap = true;
                assertEquals(cps.size(), next, "erst alle Posten");
            }
        }
        assertEquals(cps.size(), next);
        assertTrue(lap || Alm.lapLine().cross(last.x(), last.z() + 0.6, last.x(), last.z() - 0.6) != null);
    }

    @Test
    void venuesDontOverlap() {
        int[][] boxes = {
            {Kiste.LANE_X[0] - 3, (int) Kiste.TOP_Z - 2, Kiste.LANE_X[Kiste.lanes() - 1] + Kiste.SWING + 3, (int) Kiste.END_Z + 2},
            {Lake.X1, Lake.Z1, Lake.X2, Lake.Z2},
            {Range.laneX(0) - 3, Range.MAT_Z - 3, Range.laneX(Range.LANES - 1) + 3, Range.BACKSTOP_Z},
            {Alm.WEST_X - Alm.FENCE, Alm.NORTH_Z - Alm.FENCE, Alm.EAST_X + Alm.FENCE, Alm.SOUTH_Z + Alm.FENCE},
            {-20, -20, 20, 30}};
        for (int i = 0; i < boxes.length; i++) {
            for (int j = i + 1; j < boxes.length; j++) {
                int[] a = boxes[i];
                int[] b = boxes[j];
                boolean overlap = a[0] <= b[2] && b[0] <= a[2] && a[1] <= b[3] && b[1] <= a[3];
                assertFalse(overlap, "Bereiche " + i + " und " + j + " überlappen");
            }
        }
        for (int[] b : boxes) {
            assertTrue(b[0] > Places.MIN_X + 14 && b[2] < Places.MAX_X - 14 && b[1] > Places.MIN_Z + 5 && b[3] < Places.MAX_Z - 14,
                    "in der Welt");
        }
    }
}

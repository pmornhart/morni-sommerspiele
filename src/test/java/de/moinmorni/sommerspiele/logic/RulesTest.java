package de.moinmorni.sommerspiele.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RulesTest {

    @Test
    void heatsAreBalancedAndKeepTheOrder() {
        assertEquals(List.of(List.of(1, 2, 3)), Heats.split(List.of(1, 2, 3), 4));
        assertEquals(List.of(List.of(1, 2, 3, 4), List.of(5, 6, 7)), Heats.split(List.of(1, 2, 3, 4, 5, 6, 7), 4));
        assertEquals(List.of(List.of(1, 2, 3), List.of(4, 5)), Heats.split(List.of(1, 2, 3, 4, 5), 4));
        assertEquals(3, Heats.split(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), 4).size());
        assertEquals(List.of(), Heats.split(List.<Integer>of(), 4));
    }

    @Test
    void partiesShareABobStrangersDont() {
        var bobs = Teams.bobs(List.of(List.of("anna", "carl", "eva"), List.of("bert"), List.of("dora")), true);
        assertEquals(List.of(List.of("anna", "carl"), List.of("eva"), List.of("bert"), List.of("dora")), bobs);
        var solo = Teams.bobs(List.of(List.of("anna", "carl"), List.of("bert")), false);
        assertEquals(3, solo.size());
    }

    @Test
    void partiesStayTogetherWhenPicking() {
        assertEquals(List.of(1, 2, 3, 6), Groups.pick(List.of(List.of(1, 2, 3), List.of(4, 5), List.of(6)), 4));
        assertEquals(List.of(1, 2), Groups.pick(List.of(List.of(1, 2, 3)), 2), "Party größer als erlaubt: die Ersten");
    }

    @Test
    void votesPickTheFavouriteOtherwiseTheLongestUnplayed() {
        Votes<Program> v = new Votes<>(Program.class);
        Set<Program> all = Set.of(Program.values());
        assertEquals(Program.WETTKAMPF, v.winner(all), "ohne Stimmen: der ganze Wettkampf");
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        v.vote(a, Program.REGATTA);
        v.vote(b, Program.REGATTA);
        assertEquals(Program.REGATTA, v.winner(all));
        v.retain(List.of(a));
        assertEquals(1, v.count(Program.REGATTA));
        v.forget(a);
        v.played(Program.WETTKAMPF, 10);
        assertEquals(Program.ALMLAUF, v.winner(all));
    }

    @Test
    void programs() {
        assertEquals(4, Program.WETTKAMPF.disciplines().size());
        assertEquals(Discipline.SEIFENKISTE, Program.WETTKAMPF.disciplines().get(3), "Finale: Seifenkiste");
        assertTrue(Program.WETTKAMPF.full());
        assertFalse(Program.REGATTA.full());
        assertEquals(Program.BOGEN, Program.byId("bogen"));
        assertEquals(Program.SEIFENKISTE, Program.byId("soapbox"));
        assertEquals(Discipline.ALMLAUF, Discipline.byId("ALMLAUF"));
        assertTrue(Discipline.BOGEN.scored());
        assertFalse(Discipline.REGATTA.scored());
        assertNull(Program.byId("curling"));
    }

    @Test
    void seasonSwitch() {
        Season summer = Season.parse("sommer", Season.DEFAULT_FROM, Season.DEFAULT_TO);
        assertTrue(Season.ALWAYS.open(LocalDate.of(2026, 12, 24)));
        assertFalse(summer.open(LocalDate.of(2027, 3, 31)), "Wintersport-Saison");
        assertTrue(summer.open(LocalDate.of(2027, 4, 1)));
        assertTrue(summer.open(LocalDate.of(2027, 7, 15)));
        assertTrue(summer.open(LocalDate.of(2026, 11, 30)));
        assertFalse(summer.open(LocalDate.of(2026, 12, 1)));
        assertEquals(LocalDate.of(2027, 4, 1), summer.nextOpening(LocalDate.of(2026, 12, 24)));
        assertEquals(LocalDate.of(2027, 4, 1), summer.nextOpening(LocalDate.of(2027, 2, 2)));
        assertEquals(LocalDate.of(2026, 11, 30), summer.closing(LocalDate.of(2026, 6, 1)));
        assertNull(Season.ALWAYS.closing(LocalDate.of(2027, 2, 1)));
        assertNull(summer.closing(LocalDate.of(2027, 2, 1)));
        assertEquals("sommer", Season.parse(" Summer ", 4, 11).id());
        assertEquals(Season.ALWAYS, Season.parse("immer", 4, 11));
        assertEquals(Season.ALWAYS, Season.parse(null, 4, 11));
        // Mai bis September (wie im Auftrag vorgeschlagen) geht auch.
        Season may = Season.parse("sommer", 5, 9);
        assertFalse(may.open(LocalDate.of(2027, 4, 30)));
        assertTrue(may.open(LocalDate.of(2027, 9, 30)));
        // Über den Jahreswechsel
        Season wrap = Season.parse("sommer", 11, 2);
        assertTrue(wrap.open(LocalDate.of(2027, 1, 10)));
        assertFalse(wrap.open(LocalDate.of(2027, 6, 10)));
        assertEquals(LocalDate.of(2028, 2, 29), wrap.closing(LocalDate.of(2027, 12, 3)));
    }

    /** Sommerspiele und Wintersport (1.12.–31.3.) sind mit der Vorgabe nie zugleich zu und nie zugleich offen. */
    @Test
    void summerIsTheExactCounterpartOfWinter() {
        Season summer = Season.parse("sommer", Season.DEFAULT_FROM, Season.DEFAULT_TO);
        for (LocalDate d = LocalDate.of(2026, 1, 1); d.getYear() < 2028; d = d.plusDays(1)) {
            boolean winter = d.getMonthValue() >= 12 || d.getMonthValue() <= 3;
            assertTrue(summer.open(d) != winter, d.toString());
        }
    }

    @Test
    void runProgressNeedsAllCheckpoints() {
        RunProgress pr = new RunProgress(2, 3);
        assertEquals(RunProgress.Lap.NONE, pr.lapLine(), "Startlinie gleich nach dem Start zählt nicht");
        assertFalse(pr.checkpoint(1), "nur der Reihe nach");
        assertTrue(pr.checkpoint(0));
        assertTrue(pr.checkpoint(1));
        assertEquals(RunProgress.Lap.NONE, pr.lapLine(), "abgekürzt");
        assertTrue(pr.checkpoint(2));
        assertEquals(RunProgress.Lap.LAP, pr.lapLine());
        assertEquals(-1, pr.lastCheckpoint());
        for (int i = 0; i < 3; i++) {
            pr.checkpoint(i);
        }
        assertEquals(RunProgress.Lap.FINISH, pr.lapLine());
        assertTrue(pr.finished());
    }

    @Test
    void times() {
        assertEquals("0:14,32", TimeFormat.format(14_321, "de"));
        assertEquals("1:02.05", TimeFormat.format(62_050, "en"));
        assertEquals("+1,23", TimeFormat.gap(1_234, "de"));
        assertEquals("+3 s", TimeFormat.penalty(3_000));
        RaceClock c = new RaceClock(100);
        assertEquals(50, c.at(101, 1.0));
        assertEquals(1025, c.at(121, 0.5), "Hundertstel aus dem Bruchteil des Ticks");
        assertEquals(500, c.now(110));
    }

    @Test
    void crossingALineAndAGate() {
        Line l = Line.acrossZ(10.5, 0, 10);
        Line.Hit h = l.cross(3, 8, 3, 12);
        assertNotNull(h);
        assertEquals(0.625, h.t(), 1e-9);
        assertEquals(0.3, h.u(), 1e-9);
        assertNull(l.cross(3, 8, 3, 10), "noch nicht drüber");
        assertNull(l.cross(3, 10.5, 3, 12), "vom Strich weg zählt nicht noch einmal");
        assertNull(l.cross(12, 8, 12, 12), "neben der Linie vorbei");
        Gate g = new Gate(l, 0.2, 0.5, true);
        assertEquals(Gate.Pass.PASSED, g.check(3, 8, 3, 12));
        assertEquals(Gate.Pass.MISSED, g.check(8, 8, 8, 12));
        assertEquals(Gate.Pass.NONE, g.check(3, 1, 3, 2));
        assertEquals(Gate.Pass.PASSED, g.check(1, 9, 4, 13), "auch schräg und schnell");
    }
}

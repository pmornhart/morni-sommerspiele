package de.moinmorni.sommerspiele.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class RankingTest {

    private static Ranking.Entry e(String id, long ms) {
        return new Ranking.Entry(id, ms, true);
    }

    @Test
    void fastestFirstWithMedalsAndPoints() {
        var r = Ranking.rank(List.of(e("b", 20_000), e("a", 15_500), e("c", 31_000), e("d", 40_000),
                new Ranking.Entry("x", 0, false)));
        assertEquals(List.of("a", "b", "c", "d", "x"), r.stream().map(p -> p.entry().id()).toList());
        assertEquals(Medal.GOLD, r.get(0).medal());
        assertEquals(Medal.SILVER, r.get(1).medal());
        assertEquals(Medal.BRONZE, r.get(2).medal());
        assertNull(r.get(3).medal());
        assertEquals(List.of(10, 8, 6, 5, 0), r.stream().map(Ranking.Placed::points).toList());
        assertEquals(0, r.get(4).place(), "nicht im Ziel: kein Platz");
    }

    @Test
    void sameTimeSharesThePlaceAndTheMedal() {
        var r = Ranking.rank(List.of(e("a", 12_000), e("b", 12_000), e("c", 13_000)));
        assertEquals(1, r.get(0).place());
        assertEquals(1, r.get(1).place());
        assertEquals(3, r.get(2).place(), "nach zwei Ersten kommt der Dritte");
        assertEquals(Medal.GOLD, r.get(1).medal());
        assertEquals(Medal.BRONZE, r.get(2).medal());
    }

    @Test
    void aloneIsTrainingWithoutMedal() {
        var r = Ranking.rank(List.of(e("a", 12_000)));
        assertEquals(1, r.get(0).place());
        assertNull(r.get(0).medal());
        assertEquals(10, r.get(0).points());
    }

    @Test
    void archeryMostRingsFirstCentresBreakTies() {
        var r = Ranking.rank(List.of(new Ranking.Entry("a", Archery.rankValue(120, 1), true),
                new Ranking.Entry("b", Archery.rankValue(131, 0), true), new Ranking.Entry("c", Archery.rankValue(120, 3), true),
                new Ranking.Entry("d", Archery.rankValue(120, 3), true)), true);
        assertEquals(List.of("b", "c", "d", "a"), r.stream().map(p -> p.entry().id()).toList());
        assertEquals(2, r.get(2).place(), "gleiche Ringe und Mitten = gleicher Platz");
        assertEquals(4, r.get(3).place());
    }

    @Test
    void pointsForLaterPlaces() {
        assertEquals(2, Ranking.points(7));
        assertEquals(1, Ranking.points(8));
        assertEquals(1, Ranking.points(12));
        assertEquals(0, Ranking.points(0));
    }

    @Test
    void medalTableSortsLikeRealGames() {
        MedalTable t = new MedalTable();
        t.add("anna", Medal.SILVER, 8);
        t.add("anna", Medal.SILVER, 8);
        t.add("anna", Medal.SILVER, 8);
        t.add("bert", Medal.GOLD, 10);
        t.add("bert", null, 2);
        t.add("bert", null, 1);
        t.add("carl", Medal.BRONZE, 6);
        t.join("dora");
        var medals = t.medals();
        assertEquals(List.of("bert", "anna", "carl", "dora"), medals.stream().map(MedalTable.Row::id).toList(),
                "eine Goldmedaille schlägt drei silberne");
        var overall = t.overall();
        assertEquals(List.of("anna", "bert", "carl", "dora"), overall.stream().map(MedalTable.Row::id).toList(),
                "Gesamtsieg nach Punkten: 24 vor 13");
        assertEquals(24, overall.get(0).points());
        assertEquals(4, overall.get(3).place());
    }

    @Test
    void overallTieIsBrokenByMedalsOtherwiseShared() {
        MedalTable t = new MedalTable();
        t.add("a", Medal.GOLD, 10);
        t.add("a", null, 4);
        t.add("b", Medal.SILVER, 8);
        t.add("b", Medal.BRONZE, 6);
        t.add("c", Medal.SILVER, 8);
        t.add("c", Medal.BRONZE, 6);
        var o = t.overall();
        assertEquals("a", o.get(0).id(), "gleich viele Punkte (14) – Gold entscheidet");
        assertEquals(2, o.get(1).place());
        assertEquals(2, o.get(2).place(), "ganz gleich: geteilter Platz");
    }
}

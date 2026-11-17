package de.moinmorni.sommerspiele;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.sommerspiele.data.Leaderboard;
import de.moinmorni.sommerspiele.data.PlayerData;
import de.moinmorni.sommerspiele.logic.Discipline;
import de.moinmorni.sommerspiele.logic.Medal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DataTest {

    @Test
    void playerDataSurvivesTheFile() {
        UUID u = UUID.randomUUID();
        PlayerData d = new PlayerData(u, "Anna");
        assertTrue(d.addRace(Discipline.REGATTA, 21_000, Medal.GOLD, 10));
        assertFalse(d.addRace(Discipline.REGATTA, 25_000, Medal.SILVER, 8), "langsamer ist keine Bestzeit");
        assertTrue(d.addRace(Discipline.REGATTA, 19_500, null, 6));
        d.addRace(Discipline.SEIFENKISTE, -1, null, 0);
        d.addEvent(true);
        assertTrue(d.addRace(Discipline.BOGEN, 120, null, 0));
        assertFalse(d.addRace(Discipline.BOGEN, 90, null, 0), "weniger Ringe ist kein Bestwert");
        d.addArchery(120, 3);
        PlayerData back = PlayerData.fromYaml(u, d.toYaml());
        assertEquals("Anna", back.name());
        assertEquals(19_500, back.best(Discipline.REGATTA));
        assertEquals(-1, back.best(Discipline.SEIFENKISTE));
        assertEquals(120, back.best(Discipline.BOGEN));
        assertEquals(6, back.races());
        assertEquals(1, back.gold());
        assertEquals(1, back.silver());
        assertEquals(24, back.points());
        assertEquals(1, back.wins());
        assertEquals(120, back.rings());
        assertEquals(3, back.centres());
    }

    @Test
    void leaderboardKeepsOneBestTimePerPerson() {
        Leaderboard b = new Leaderboard();
        UUID a = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        assertEquals(1, b.submit(Discipline.SEIFENKISTE, a, "Anna", 30_000));
        assertEquals(1, b.submit(Discipline.SEIFENKISTE, c, "Carl", 25_000));
        assertEquals(0, b.submit(Discipline.SEIFENKISTE, a, "Anna", 31_000), "nicht besser");
        assertEquals(1, b.submit(Discipline.SEIFENKISTE, a, "Anna", 24_000));
        assertEquals(2, b.top(Discipline.SEIFENKISTE).size());
        for (int i = 0; i < 15; i++) {
            b.submit(Discipline.ALMLAUF, UUID.randomUUID(), "P" + i, 10_000 + i);
        }
        assertEquals(Leaderboard.KEEP, b.top(Discipline.ALMLAUF).size());
        assertEquals(1, b.submit(Discipline.BOGEN, a, "Anna", 100));
        assertEquals(1, b.submit(Discipline.BOGEN, c, "Carl", 130), "mehr Ringe = vorn");
        assertEquals(0, b.submit(Discipline.BOGEN, c, "Carl", 110), "weniger Ringe = kein Bestwert");
        assertEquals(130, b.top(Discipline.BOGEN).get(0).ms());
        b.medal(a, "Anna", Medal.GOLD, 10);
        b.medal(c, "Carl", null, 3);
        Leaderboard back = Leaderboard.fromYaml(b.toYaml());
        assertEquals(24_000, back.top(Discipline.SEIFENKISTE).get(0).ms());
        assertEquals(1, back.medalTable().size(), "ohne Medaille nicht im Spiegel");
        assertEquals("Anna", back.nameOf(back.medalTable().get(0).id()));
    }
}

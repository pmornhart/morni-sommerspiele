package de.moinmorni.sommerspiele.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ComebackTest {

    private static final UUID ANNA = UUID.nameUUIDFromBytes("Anna".getBytes());
    private static final UUID BERT = UUID.nameUUIDFromBytes("Bert".getBytes());

    @Test
    void onlyPlayersStillInARunningRoundKeepTheirSpot() {
        assertTrue(Comeback.holds(90, true, true, false));
        assertFalse(Comeback.holds(0, true, true, false), "0 = aus");
        assertFalse(Comeback.holds(90, false, true, false), "Runde schon vorbei");
        assertFalse(Comeback.holds(90, true, false, false), "schon ausgeschieden");
        assertFalse(Comeback.holds(90, true, true, true), "mitten im Kampf: scheidet aus wie bisher");
    }

    @Test
    void backInTimeKeepsTheSpotTooLateDoesNot() {
        Comeback c = new Comeback();
        c.away(ANNA, 1000, 20);
        assertTrue(c.waitingFor(ANNA));
        assertEquals(20, c.secondsLeft(ANNA, 1000));
        assertEquals(1, c.secondsLeft(ANNA, 1000 + 20 * 20 - 1));
        assertTrue(c.back(ANNA, 1000 + 20 * 20), "genau zur Frist zählt noch");
        assertFalse(c.waitingFor(ANNA));
        c.away(BERT, 1000, 20);
        assertFalse(c.back(BERT, 1000 + 20 * 20 + 1));
        assertFalse(c.waitingFor(BERT), "zu spät: Frist vergessen");
        assertFalse(c.back(ANNA, 2000), "niemand wartet auf Anna");
    }

    @Test
    void expiredInTheOrderPeopleLeft() {
        Comeback c = new Comeback();
        c.away(BERT, 0, 10);
        c.away(ANNA, 20, 10);
        assertEquals(List.of(), c.expired(200));
        assertEquals(List.of(BERT), c.expired(201));
        assertEquals(1, c.count());
        assertEquals(List.of(ANNA), c.expired(10_000));
        assertEquals(0, c.count());
        assertEquals(0, c.secondsLeft(ANNA, 10_000));
    }

    @Test
    void endOfRoundForgetsEveryone() {
        Comeback c = new Comeback();
        c.away(ANNA, 0, 90);
        c.away(BERT, 0, 90);
        assertEquals(List.of(ANNA, BERT), c.clear());
        assertEquals(0, c.count());
        assertEquals(List.of(), c.clear());
    }
}

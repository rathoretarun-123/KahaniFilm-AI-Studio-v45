package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Lessons;

import java.util.Arrays;
import org.junit.Test;

/** v47.9 (the user: "make your director, technical director, reader and editor learn things from each story creation"). */
public class LessonsTest {
    @Test public void aNewTeamChangesNothing() {
        Lessons l = new Lessons();
        assertEquals(1f, l.shotLengthScale(), 1e-6);
        assertEquals(1f, l.moveScale(), 1e-6);
        assertTrue(l.notes().isEmpty());
    }

    @Test public void theUsersFixesAndTheAuditChangeTheNextFilm() {
        Lessons l = new Lessons();
        // a film of 20 shots: the user asked for calmer on 6 and no cut on 2; the audit found 3 jolts in 1500 frames; 1 of 9 lines silent
        l.learn(Arrays.asList(Director.FIX_CALM, Director.FIX_CALM, Director.FIX_CALM, Director.FIX_CALM, Director.FIX_CALM, Director.FIX_CALM,
                Director.FIX_REMOVE, Director.FIX_REMOVE, Director.FIX_CLOSER), 20, 3, 1500, 8, 9);
        assertTrue("longer shots: " + l.shotLengthScale(), l.shotLengthScale() > 1.3f && l.shotLengthScale() <= 1.5f);
        assertTrue("gentler moves: " + l.moveScale(), l.moveScale() < 1f && l.moveScale() >= 0.7f);
        assertTrue(l.notes().toString(), l.notes().toString().contains("Technical director") && l.notes().toString().contains("Director:") && l.notes().toString().contains("Editor"));
        // kept and read back
        Lessons back = Lessons.read(l.write());
        assertEquals(l.films, back.films);
        assertEquals(l.shotLengthScale(), back.shotLengthScale(), 1e-3);
        assertEquals(l.moveScale(), back.moveScale(), 1e-3);
        // a clean film afterwards weighs in, but does not erase the lesson at once
        back.learn(Arrays.<Integer>asList(), 20, 0, 1500, 9, 9);
        assertTrue(back.shotLengthScale() > 1f && back.shotLengthScale() < l.shotLengthScale());
    }

    @Test public void gentlerMovesTakeLonger() {
        float before = Director.leastMoveTime(1f, 1f, false);
        Director.MOVE_SCALE = 0.8f;
        try { assertTrue(Director.leastMoveTime(1f, 1f, false) > before); }
        finally { Director.MOVE_SCALE = 1f; }
    }
}

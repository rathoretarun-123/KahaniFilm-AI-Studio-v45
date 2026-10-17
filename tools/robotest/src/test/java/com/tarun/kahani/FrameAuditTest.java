package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.FrameAudit;
import com.tarun.kahani.core.Story;
import org.junit.Test;

/**
 * v47.7 (the user: "no shaking or random movements, no extra lips or eyes anywhere — check every frame"): the frame
 * audit reads every character in every frame as drawn and finds a jolt, a jump, a size pop, a picture swapped inside a
 * shot and a second face — and nothing in eased motion or across a cut.
 */
public class FrameAuditTest {

    static Film film() {
        Film f = new Film();
        Film.Seg s = new Film.Seg(); s.t0 = 0; s.t1 = 10; f.segs.add(s);
        Film.Shot a = new Film.Shot(); a.t = 0; a.dur = 5; f.shots.add(a);
        Film.Shot b = new Film.Shot(); b.t = 5; b.dur = 5; f.shots.add(b);
        return f;
    }

    static Film.Actor actor() {
        Film.Actor a = new Film.Actor();
        a.c = new Story.CharacterDef(); a.c.id = "c1"; a.c.displayName = "मीना";
        return a;
    }

    @Test public void easedMotionAndCutsAreClean() {
        Film f = film(); Film.Actor a = actor();
        FrameAudit au = new FrameAudit(24);
        for (int i = 0; i < 240; i++) {
            float t = i / 24f, u = Math.min(1, t / 4f), e = u * u * (3 - 2 * u);       // an eased walk of 400 px over 4 s
            float x = t < 5 ? 100 + 400 * e : 900;                                      // and a different place after the cut at 5 s
            au.at(a, t, x, 500 + 3 * (float) Math.sin(t * 12), 300);                   // with a small step bob
            au.picture(a, t, t < 5 ? "front" : "side", false);
            au.face(a, t);
        }
        FrameAudit.Result r = au.read(f);
        assertTrue(r.text(), r.clean());
        assertEquals(240, r.frames);
    }

    @Test public void joltsJumpsPopsAndASecondFaceAreFound() {
        Film f = film(); Film.Actor a = actor();
        FrameAudit au = new FrameAudit(24);
        for (int i = 0; i < 120; i++) {
            float t = i / 24f;
            float x = 300;
            if (i == 30) x = 312;                     // a one-frame twitch: a jolt
            if (i >= 60) x = 420;                     // a teleport: a jump
            float h = i >= 90 ? 330 : 300;            // a size pop
            au.at(a, t, x, 500, h);
            au.picture(a, t, i < 100 ? "front" : "sitting", false);    // a picture swapped inside the shot
            au.face(a, t);
            if (i == 110) au.face(a, t);              // drawn twice: two faces
        }
        FrameAudit.Result r = au.read(f);
        assertEquals(r.text(), 1, r.jumps);
        assertTrue(r.text(), r.jolts >= 1);
        assertEquals(r.text(), 1, r.sizePops);
        assertEquals(r.text(), 1, r.picturePops);
        assertEquals(r.text(), 1, r.extraFaces);
    }
}

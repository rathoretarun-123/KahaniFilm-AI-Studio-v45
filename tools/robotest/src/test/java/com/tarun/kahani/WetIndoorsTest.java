package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import org.junit.Test;

/**
 * v47.12 (new stories made end to end): rain outside the window wets no one who sat inside all along — only the puppy
 * that comes in from it through the door is drawn dripping.
 */
public class WetIndoorsTest {
    @Test public void onlyTheOneFromTheRainIsWet() {
        Story st = ScriptParser.parse("Title: Rain\n\nCharacters\n\n1. Maya (9 years)\n(a girl)\n\n2. Bruno (dog)\n(a small brown puppy)\n\n"
                + "Places\n\n1. Living room\n(a sofa, a window, a door)\n\nStory\n\nScene 1\n"
                + "(Maya sits on the sofa and looks out of the window. Rain is falling outside.)\n\n"
                + "Maya: \"The rain will never stop.\" (sadly)\n\n(Someone scratches at the door. Maya runs to the door and opens it.)\n\n"
                + "Maya: \"Bruno! You are all wet!\" (in surprise)\n\n(Bruno shakes the water off.)\n\nTHE END\n");
        Director d = new Director(st, new Director.Options());
        d.prepare();
        Film film = d.direct(new Art());
        float maya = 0, bruno = 0, rain = 0;
        for (Film.Seg seg : film.segs) for (Film.Actor a : seg.actors)
            for (float t = seg.t0; t < seg.t1; t += 0.25f) {
                rain = Math.max(rain, film.wetness(t));
                if (!a.stateAt(t).visible) continue;
                float w = film.wetness(a, seg, t);
                if ("Maya".equals(a.c.shown())) maya = Math.max(maya, w);
                if ("Bruno".equals(a.c.shown())) bruno = Math.max(bruno, w);
            }
        assertTrue("it rains in this film: " + rain, rain > 0.05f);
        assertEquals("Maya was inside all along: dry", 0, maya, 0.001f);
        assertTrue("Bruno came in from the rain: wet " + bruno, bruno > 0.05f);
    }
}

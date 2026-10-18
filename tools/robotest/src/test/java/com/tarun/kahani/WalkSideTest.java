package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import org.junit.Test;

/**
 * v47.10 (the user: "no motion … leg and body movements"): a direction that walks someone across the room to one side
 * ("वृंदा कमरे में दाईं तरफ चलकर जाती है") is a walk on the stage — to the side it names — not a still picture held while
 * the sentence goes by.
 */
public class WalkSideTest {
    static String script(String direction) {
        return "शीर्षक: चाल\n\nपात्र\n\n1. वृंदा (11 साल)\n(पतली लड़की, ऊँची पोनीटेल)\n\nस्थान\n\n1. घर का कमरा\n(क्रीम दीवारें)\n\n"
                + "कहानी\n\nदृश्य 1\n(वृंदा कमरे में खड़ी है)\n\nवृंदा: \"आज तो बहुत मज़ा आया!\"\n\n(" + direction + ")\n\n"
                + "वृंदा: \"डर लग रहा है।\"\n\nसमाप्त\n";
    }

    /** How far she walks in her scene: the end of her moves less where she stood before the first (0: she never moves). */
    static float walked(String direction) throws Exception {
        Story story = ScriptParser.parse(script(direction));
        Director d = new Director(story, new Director.Options());
        d.prepare();
        Film film = d.direct(new Art());
        float best = 0;
        boolean seen = false;
        for (Film.Seg seg : film.segs) for (Film.Actor a : seg.actors) {
            if (!"वृंदा".equals(a.c.shown())) continue;
            seen = true;
            float from = Float.NaN, to = Float.NaN;
            for (int i = 1; i < a.keys.size(); i++) {
                Film.Key k = a.keys.get(i);
                if (k.moveDur > 0.3f && Math.abs(k.x - a.stateAt(k.t - 0.01f).x) > 60) {
                    if (Float.isNaN(from)) from = a.stateAt(k.t - 0.01f).x;
                    to = k.x;
                }
            }
            if (!Float.isNaN(from) && Math.abs(to - from) > Math.abs(best)) best = to - from;
        }
        assertTrue("वृंदा is on the stage", seen);
        return best;
    }

    @Test public void aWalkToTheRightGoesRight() throws Exception {
        float dx = walked("वृंदा कमरे में दाईं तरफ चलकर जाती है");
        assertTrue("she walks to the right: " + dx, dx > 150);
    }

    @Test public void aWalkToTheLeftGoesLeft() throws Exception {
        float dx = walked("वृंदा कमरे में बाईं तरफ चलकर जाती है");
        assertTrue("she walks to the left: " + dx, dx < -150);
    }

    @Test public void standingIsNoWalk() throws Exception {
        assertEquals("only standing: she stays", 0, walked("वृंदा चुपचाप खड़ी रहती है"), 0);
    }
}

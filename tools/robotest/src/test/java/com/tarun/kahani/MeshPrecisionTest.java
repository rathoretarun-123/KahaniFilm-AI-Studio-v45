package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Rig;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;
import com.tarun.kahani.jvm.MakeFilm;
import org.junit.Test;

/**
 * v47.13 (the user: "decrease meshing to the level of 0.06 px, and rigging"): how far the drawn mesh is from the exact
 * bend, on the user's own picture of the old man (standing, walking, seated, kneeling).
 */
public class MeshPrecisionTest {
    static Rig rig() {
        Story st = ScriptParser.parse("शीर्षक: जाँच\n\nपात्र\nरघुवीर जी (78 साल)\n\nकहानी\n\nदृश्य 1\nरघुवीर जी: \"हाँ।\"\n\nसमाप्त\n");
        Art art = Art.fromManifest("char|रघुवीर जी|grandpa_standing_front.jpg\n", st, new MakeFilm.AwtLoader(FaceModelTest.STILLS));
        Art.Sprite sp = art.sprites.get(st.characters.get(0).id);
        assertNotNull("the old man's picture is read", sp);
        assertNotNull("and rigged", sp.rig);
        return sp.rig;
    }

    static Rig.State pose(int k) {
        Rig.State s = new Rig.State(); s.reset();
        if (k == 1) { s.legLAng = 15; s.legRAng = -15; s.legLLift = 0.13f; s.armL = 13; s.armR = -13; }
        if (k == 2) s.sit = 1;
        if (k == 3) s.kneel = 1;
        return s;
    }

    @Test public void noTearBetweenTheLegsAndSubPixelEverywhere() {
        Rig r = rig();
        for (int k = 0; k < 4; k++) {
            float[] e = r.meshError(pose(k), 540);
            // (a seated old man's mesh used to tear 8 px open along the line between his legs)
            assertTrue("pose " + k + ": the largest error " + e[0] + " px", e[0] < 1f);
            assertTrue("pose " + k + ": 99% of the mesh within 0.06 px (" + e[1] + ")", e[1] < 0.06f);
        }
    }

    @Test public void theFineMeshIsWithinAboutSixHundredthsOfAPixel() {
        float cell = Rig.CELL_PX, src = Rig.SOURCE_CELLS; int max = Rig.MAX_CELLS;
        try {
            Rig.CELL_PX = 0.33f; Rig.SOURCE_CELLS = 3; Rig.MAX_CELLS = 3072;
            Rig r = rig();
            for (int k = 0; k < 4; k++) {
                float[] e = r.meshError(pose(k), 540);
                assertTrue("pose " + k + ": the largest error " + e[0] + " px", e[0] < 0.07f);
            }
        } finally { Rig.CELL_PX = cell; Rig.SOURCE_CELLS = src; Rig.MAX_CELLS = max; }
    }
}

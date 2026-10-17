package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Cutout;
import org.junit.Test;

/**
 * v47.9 (the user: "pixer level film"): a picture that comes cut out from a magenta ground keeps a purple cast along
 * its edge (the ponytail of the sofa story's girl turned purple 5 to 10 pixels in, and the hair's pockets stayed
 * magenta). The cast is taken off the edge; a figure whose own colour leans the same way (a lavender shirt) keeps it.
 */
public class KeyFringeTest {
    static final int KEY = 0x00F601F6;          // the see-through pixels keep the key's colour, as the user's PNGs do

    /** A 200×300 cut-out: a figure of colour body inside a 20-pixel margin, the outer 5 pixels of it cast with the key. */
    static int[] figure(int body, int cast) {
        int w = 200, h = 300;
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int i = y * w + x;
            int d = Math.min(Math.min(x - 20, 179 - x), Math.min(y - 20, 279 - y));
            if (d < 0) { px[i] = KEY; continue; }
            px[i] = 0xFF000000 | (d < 5 ? cast : body);
        }
        return px;
    }

    static int lean(int c) { int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255; return Math.min(r, b) - g; }

    @Test public void darkHairLosesThePurpleCast() {
        int hair = 0x383636, cast = 0x922F8A;   // the ponytail's edge as measured: (146, 47, 138)
        Cutout.Result r = Cutout.process(figure(hair, cast), 200, 300);
        int edge = r.px[(r.h / 2) * r.w + 4], inside = r.px[(r.h / 2) * r.w + r.w / 2];
        assertTrue("the edge no longer leans to magenta: " + Integer.toHexString(edge), lean(edge) < 20);
        assertEquals("the figure inside is unchanged", hair, inside & 0xFFFFFF);
    }

    @Test public void aLavenderShirtKeepsItsColour() {
        int lavender = 0xBEA0DC;                // leans the key's way everywhere: its own colour, not a cast
        Cutout.Result r = Cutout.process(figure(lavender, lavender), 200, 300);
        int edge = r.px[(r.h / 2) * r.w + 4];
        assertEquals("the edge keeps the shirt's colour", lavender, edge & 0xFFFFFF);
    }

    @Test public void aCleanCutOutIsLeftAsItIs() {
        int[] px = figure(0x383636, 0x383636);
        for (int i = 0; i < px.length; i++) if ((px[i] >>> 24) == 0) px[i] = 0;     // clear pixels with no colour of their own
        int[] keep = px.clone();
        Cutout.Result r = Cutout.process(px, 200, 300);
        assertEquals(keep[150 * 200 + 100], r.px[(r.h / 2) * r.w + r.w / 2]);
        assertEquals(keep[150 * 200 + 24], r.px[(r.h / 2) * r.w + 4]);
    }
}

package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Cutout;
import org.junit.Test;

/**
 * v47.10 (the user: "no lipsync"): a face turned to the side — a profile or a three-quarter picture — finds its mouth by
 * the lips' colour (redder and darker than the skin, wider than tall), not on the round blush of a cheek or a nostril;
 * a face with no lips found keeps its mouth closed (nothing is guessed). And a face seen from the front keeps its mouth
 * between its eyes: a strongly blushed cheek beyond one eye is not taken for its lips (a dark slit opened on the cheek).
 */
public class LipFinderTest {
    static final int SKIN = 0xFFE8A884, LIPS = 0xFFB8505A, BLUSH = 0xFFE88F86, NOSTRIL = 0xFFB86A60;

    /** A 400×400 face of skin, eyes at (150, 120) and (250, 120). */
    static int[] face() { int[] px = new int[400 * 400]; java.util.Arrays.fill(px, SKIN); return px; }
    static void box(int[] px, int x0, int y0, int x1, int y1, int c) { for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) px[y * 400 + x] = c; }
    static void disc(int[] px, int cx, int cy, int r, int c) { for (int y = cy - r; y <= cy + r; y++) for (int x = cx - r; x <= cx + r; x++) if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r) px[y * 400 + x] = c; }

    @Test public void theLipsAreFoundNotTheBlushOrTheNostril() {
        int[] px = face();
        disc(px, 260, 190, 26, BLUSH);              // a blush on the cheek: large and round
        box(px, 190, 168, 196, 174, NOSTRIL);         // a nostril: small
        box(px, 170, 215, 220, 225, LIPS);            // the lips: a wide flat mark below the nose
        float[] m = Cutout.sideMouth(px, 400, 400, 150, 250, 120);
        assertNotNull("the lips are found", m);
        assertEquals("across: at the lips", 195, m[0], 4);
        assertEquals("down: at the lips", 220, m[1], 4);
    }

    @Test public void aFrontFacesBlushedCheekIsNotItsMouth() {
        // a round face of skin on a clear ground, seen from the front; thin lips of the skin's own colour, a little
        // darker, under the middle of the eyes; one cheek blushed deep red beyond its eye, the other hardly at all
        int[] px = new int[400 * 400];
        disc(px, 200, 200, 150, SKIN);
        disc(px, 150, 150, 14, 0xFF3A2418);
        disc(px, 250, 150, 14, 0xFF3A2418);
        box(px, 180, 232, 220, 235, 0xFFD89A7C);
        disc(px, 92, 205, 22, 0xFFC8505A);
        Cutout.Result r = new Cutout.Result();
        r.px = px; r.w = 400; r.h = 400; r.faceFound = true;
        Art.Sprite sp = new Art.Sprite();
        sp.faceKnown = true; sp.skin = SKIN;
        sp.eyeLX = 0.375f; sp.eyeRX = 0.625f; sp.eyeLY = sp.eyeRY = 0.375f;
        sp.mouthX = 0.5f; sp.mouthY = 0.58f; sp.mouthHW = 0.05f;
        Art.mouthFromPicture(r, sp);
        assertTrue("the mouth stays between the eyes: " + sp.mouthX, sp.mouthX > sp.eyeLX && sp.mouthX < sp.eyeRX);
    }

    /** v47.11: a brown face whose mouth is a darker line of the skin's own colour (no redder): found at the reader's point. */
    @Test public void aMouthLineNoRedderThanTheSkinIsFoundAtThePoint() {
        int brown = 0xFFDC8B55, line = 0xFF8A4A30;
        int[] px = new int[400 * 400];
        java.util.Arrays.fill(px, brown);
        box(px, 180, 218, 238, 224, line);                 // the mouth: a long flat darker line
        box(px, 196, 188, 202, 194, line);                 // a nostril: small
        float[] m = Cutout.sideMouth(px, 400, 400, 150, 250, 120, 205, 222);
        assertNotNull("the mouth line is found at the point", m);
        assertEquals("across", 209, m[0], 6);
        assertEquals("down", 221, m[1], 4);
        assertNull("without the reader's point nothing is guessed", Cutout.sideMouth(px, 400, 400, 150, 250, 120));
    }

    @Test public void noLipsNoMouth() {
        int[] px = face();
        disc(px, 260, 190, 26, BLUSH);
        assertNull("only a blush: nothing is taken for the mouth", Cutout.sideMouth(px, 400, 400, 150, 250, 120));
    }
}

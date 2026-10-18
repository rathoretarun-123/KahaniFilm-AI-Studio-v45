package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Cutout;
import org.junit.Test;

/**
 * v47.10 (the user: "no lipsync"): a face turned to the side — a profile or a three-quarter picture — finds its mouth by
 * the lips' colour (redder and darker than the skin, wider than tall), not on the round blush of a cheek or a nostril;
 * a face with no lips found keeps its mouth closed (nothing is guessed).
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

    @Test public void noLipsNoMouth() {
        int[] px = face();
        disc(px, 260, 190, 26, BLUSH);
        assertNull("only a blush: nothing is taken for the mouth", Cutout.sideMouth(px, 400, 400, 150, 250, 120));
    }
}

package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Cutout;

import org.junit.Test;

/**
 * v47.13 (the user: "finding proper lips and other body parts must be perfect on all the poses"): a face read from a
 * picture of a pose is animated only where it is a face. The old man's beige kurta was taken for skin, and in the
 * pictures of him sitting and bending he blinked on his chest and spoke from his knees.
 */
public class FaceTrustTest {
    static final int W = 200, H = 300, SKIN = 0xFFC88A66, KURTA = 0xFFBEAF87, SHIRT = 0xFF3050A0;

    static Cutout.Result picture() {
        Cutout.Result r = new Cutout.Result();
        r.w = W; r.h = H; r.px = new int[W * H];
        return r;
    }

    static void disc(Cutout.Result r, float cx, float cy, float rad, int c) {
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= rad * rad) r.px[y * W + x] = c;
    }

    static void box(Cutout.Result r, int x0, int y0, int x1, int y1, int c) {
        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) r.px[y * W + x] = c;
    }

    /** Two eyes (a dark iris in a white) and a red mouth under them, at eye height ey. */
    static void face(Cutout.Result r, float ey) {
        for (int e = 0; e < 2; e++) { disc(r, 80 + 40 * e, ey, 7, 0xFFF4F4F0); disc(r, 80 + 40 * e, ey, 4, 0xFF201810); }
        box(r, 88, (int) ey + 33, 112, (int) ey + 37, 0xFFB03C3C);
    }

    static Art.Sprite marks(float ey) {
        Art.Sprite v = new Art.Sprite();
        v.faceKnown = true; v.mouthSeen = true;
        v.eyeLX = 80f / W; v.eyeRX = 120f / W; v.eyeLY = v.eyeRY = ey / H;
        v.mouthX = 100f / W; v.mouthY = (ey + 35) / H; v.mouthHW = 12f / W;
        return v;
    }

    @Test public void aFaceIsAFace() {
        Cutout.Result r = picture();
        disc(r, 100, 85, 52, SKIN);
        box(r, 50, 135, 150, 300, SHIRT);
        face(r, 80);
        assertTrue("the drawn face is taken for the face", Art.faceTrusted(r, marks(80), new Art.Sprite()));
    }

    @Test public void eyesOnTheKurtaAreNoFace() {
        Cutout.Result r = picture();
        disc(r, 100, 50, 45, SKIN);
        box(r, 30, 95, 170, 300, KURTA);
        face(r, 190);
        assertFalse("dark spots and a red patch on the beige kurta are no face", Art.faceTrusted(r, marks(190), new Art.Sprite()));
    }

    @Test public void eyesUnderAHeadAndAChestAreNoFace() {
        // (skin-coloured all the way: the arms and the chest of a bare-chested figure; the head is far above)
        Cutout.Result r = picture();
        disc(r, 100, 40, 38, SKIN);
        box(r, 30, 78, 170, 300, SKIN);
        face(r, 200);
        assertFalse("a face with a whole head and chest above its eyes is no face", Art.faceTrusted(r, marks(200), new Art.Sprite()));
    }

    @Test public void aMouthAtTheHandIsNoFace() {
        Cutout.Result r = picture();
        disc(r, 100, 85, 52, SKIN);
        box(r, 50, 135, 150, 300, SHIRT);
        face(r, 80);
        Art.Sprite v = marks(80);
        v.mouthY = (80 + 75f) / H;     // far under the eyes: a hand, a collar
        assertFalse("lips found far below the eyes are not this face's", Art.faceTrusted(r, v, new Art.Sprite()));
    }
}

package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Renderer;

import org.junit.Test;

/**
 * v47.10 (the user: "output must be pixer level"): the acting under the words and the light the people are drawn in.
 * A speaker's head takes a key pose for every phrase, swinging across from the one before and moving into it with a
 * small overshoot; blinks are shaped (down fast, up slower), never on or off; the people from the user's pictures take
 * the colour of the place's own picture, gently, and a drawn place leaves them as they are.
 */
public class ActingTest {
    @Test public void keyPosesAreTheSameEveryTimeAndSwingAcross() {
        assertEquals(Renderer.keyPose(3, 2, 0), Renderer.keyPose(3, 2, 0), 0);
        assertEquals("nothing before the line", 0, Renderer.keyTilt(3, -1), 0);
        int across = 0, n = 40;
        for (int p = 1; p <= n; p++) {
            float a = Renderer.keyTilt(7, p - 1), b = Renderer.keyTilt(7, p);
            assertTrue("a tilt is between 45% and 100%: " + b, Math.abs(b) >= 0.449f && Math.abs(b) <= 1.0001f);
            if (Math.signum(a) != Math.signum(b)) across++;
        }
        assertTrue("most phrases swing the head to the other side: " + across + " of " + n, across >= n / 2);
    }

    @Test public void movesOvershootAndSettle() {
        assertEquals(0, Renderer.backEase(0), 1e-5f);
        assertEquals(1, Renderer.backEase(1), 1e-5f);
        float peak = 0;
        for (int i = 0; i <= 100; i++) peak = Math.max(peak, Renderer.backEase(i / 100f));
        assertTrue("past the pose a little: " + peak, peak > 1.02f && peak < 1.12f);
    }

    @Test public void aBlinkIsShaped() {
        assertEquals(0, Renderer.blinkShape(-0.01f), 0);
        assertEquals(1, Renderer.blinkShape(0.1f), 0);
        assertEquals(0, Renderer.blinkShape(0.3f), 0);
        float closing = Renderer.blinkShape(0.035f), opening = Renderer.blinkShape(0.185f);
        assertTrue("half-closed on the way down: " + closing, closing > 0.2f && closing < 0.8f);
        assertTrue("half-open on the way up: " + opening, opening > 0.2f && opening < 0.8f);
    }

    @Test public void thePeopleTakeTheLightOfThePlace() {
        Art.Backdrop warm = new Art.Backdrop();
        warm.sw = 10; warm.sh = 10; warm.small = new int[100];
        java.util.Arrays.fill(warm.small, 0xFFE6C8A0);              // a room in warm evening light
        int c = Renderer.pictureLight(warm);
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        assertTrue("warmer: red over green over blue " + Integer.toHexString(c), r > g && g > b);
        assertTrue("gently: no channel below 75%", b > 190);
        Art.Backdrop night = new Art.Backdrop();
        night.sw = 10; night.sh = 10; night.small = new int[100];
        java.util.Arrays.fill(night.small, 0xFF101830);
        int n = Renderer.pictureLight(night);
        assertTrue("a night dims them, by at most a sixth", ((n >> 8) & 255) < 230 && ((n >> 8) & 255) > 150);
        assertEquals("a drawn place leaves them as they are", 0xFFFFFF, Renderer.pictureLight(new Art.Backdrop()));
        assertEquals("worked out once", c, Renderer.pictureLight(warm));
    }
}

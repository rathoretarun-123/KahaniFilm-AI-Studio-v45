package com.tarun.kahani.core;

/**
 * v47.9 (the user: "pixer level"): the volume a drawn figure's shapes are filled with while Gfx.shading is on — the same
 * numbers on the phone and the desktop. A flat colour becomes a light from above and the left: an oval is lit like a
 * ball (its highlight up and to the left, its rim in shade), any other shape like a cylinder standing up (lighter at the
 * top, darker at the bottom). Only solid colours are shaded — a shadow, a glow or a highlight (translucent) stays as it
 * was drawn, and so does a gradient.
 */
public final class Shading {
    private Shading() {}

    /** Below this opacity a colour is a shadow or a glow, never shaded. */
    public static final int MIN_ALPHA = 200;
    /** Below this size (pixels on screen, either side) a shape is a detail — a pupil, a button — and stays flat. */
    public static final float MIN_SIZE = 3f;

    /** The lit side: the colour towards white by 16% of the strength. */
    public static int lit(int c, float k) { return mix(c, 0xFFFFFFFF, 0.16f * k); }
    /** The side in shade: the colour towards a warm dark by 24% of the strength (shadows are never grey in a film). */
    public static int shade(int c, float k) { return mix(c, 0xFF1A0E14, 0.24f * k); }

    /** Where an oval's highlight is, as a share of its width and height from its centre (up and to the left). */
    public static final float BALL_DX = -0.18f, BALL_DY = -0.22f;
    /** The ball gradient's reach, as a share of the oval's larger side; its middle colour sits at this stop. */
    public static final float BALL_R = 0.78f, MID_STOP = 0.5f;

    static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = Math.round(ar + (br - ar) * t), g = Math.round(ag + (bg - ag) * t), bl = Math.round(ab + (bb - ab) * t);
        return (a & 0xFF000000) | (r << 16) | (g << 8) | bl;
    }

    /** Whether a solid fill of this colour and screen size is shaded at strength k. */
    public static boolean applies(float k, int argb, float wPx, float hPx) {
        return k > 0 && (argb >>> 24) >= MIN_ALPHA && wPx >= MIN_SIZE && hPx >= MIN_SIZE;
    }
}

package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Shading;
import com.tarun.kahani.jvm.AwtGfx;

import java.awt.image.BufferedImage;
import org.junit.Test;

/**
 * v47.9 (the user: "pixer level"): a drawn figure's flat colours get volume while Gfx.shading is on — an oval lit like
 * a ball from above and the left, any other shape lighter at the top and darker at the bottom — and nothing changes
 * with it off, for a translucent colour (a shadow) or for a tiny detail.
 */
public class ShadingTest {
    static int lum(int c) { return ((c >> 16) & 255) * 299 + ((c >> 8) & 255) * 587 + (c & 255) * 114; }

    static BufferedImage draw(float k, int colour, boolean oval) {
        BufferedImage im = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        AwtGfx g = new AwtGfx(im);
        g.shading(k);
        g.color(colour);
        if (oval) g.oval(100, 100, 80, 80); else g.rect(20, 20, 160, 160);
        return im;
    }

    @Test public void anOvalIsLitLikeABall() {
        BufferedImage im = draw(1, 0xFFB07850, true);
        int upLeft = im.getRGB(70, 65), downRight = im.getRGB(150, 150), mid = im.getRGB(100, 100);
        assertTrue("lit up and to the left", lum(upLeft) > lum(mid));
        assertTrue("in shade at the lower right rim", lum(downRight) < lum(mid));
    }

    @Test public void aShapeIsLighterAtTheTop() {
        BufferedImage im = draw(1, 0xFF4060A0, false);
        assertTrue(lum(im.getRGB(100, 25)) > lum(im.getRGB(100, 175)));
    }

    @Test public void offTranslucentOrTinyStaysFlat() {
        BufferedImage off = draw(0, 0xFF4060A0, false);
        assertEquals("off: one colour", off.getRGB(100, 25), off.getRGB(100, 175));
        BufferedImage shadow = draw(1, 0x60000000, false);
        assertEquals("a shadow stays as drawn", shadow.getRGB(100, 25), shadow.getRGB(100, 175));
        assertFalse("a 2-pixel detail is not shaded", Shading.applies(1, 0xFF000000, 2, 2));
        assertTrue(Shading.applies(1, 0xFF000000, 20, 20));
    }
}

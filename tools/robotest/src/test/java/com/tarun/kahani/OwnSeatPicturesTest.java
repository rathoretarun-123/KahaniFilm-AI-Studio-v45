package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Casting;
import com.tarun.kahani.core.PoseSense;
import com.tarun.kahani.core.Txt;
import org.junit.Test;

/**
 * v47.7 (the user: "some pictures of characters may appear with objects as per the story, use them completely, don't
 * split object from character"; "one side of bench is broken, the brick should be used to fix it"): a picture of a
 * sitter on a bench of its own is known by its shape, and the brick the script sets under the bench goes under the leg
 * the script names.
 */
public class OwnSeatPicturesTest {

    /** A cut-out picture {w, h, pixels…}: transparent, with the given opaque boxes. */
    static int[] picture(int w, int h, int[]... boxes) {
        int[] d = new int[2 + w * h];
        d[0] = w; d[1] = h;
        for (int[] b : boxes) for (int y = b[1]; y < b[3]; y++) for (int x = b[0]; x < b[2]; x++) d[2 + y * w + x] = 0xFF806040;
        return d;
    }

    @Test public void aSitterOnABenchOfTheirOwnIsKnownByItsShape() {
        // a bench (plank across the whole width, legs at both ends) with someone on it: head, body, shins down to the ground
        int[] bench = picture(200, 200,
                new int[]{80, 10, 120, 50}, new int[]{70, 50, 130, 120},            // head, body
                new int[]{0, 120, 200, 132},                                         // the plank
                new int[]{6, 132, 22, 200}, new int[]{178, 132, 194, 200},           // its legs
                new int[]{82, 132, 94, 200}, new int[]{106, 132, 118, 200});         // the sitter's shins
        float os = Art.ownSeat(bench);
        assertTrue("the plank's top, from the feet: " + os, os > 0.35f && os < 0.45f);
        // a standing figure: narrow
        assertEquals(-1f, Art.ownSeat(picture(200, 200, new int[]{85, 10, 115, 60}, new int[]{75, 60, 125, 140}, new int[]{80, 140, 95, 200}, new int[]{105, 140, 120, 200})), 0);
        // a figure bent over with its arms out wide: wide, but standing on its two feet close together
        assertEquals(-1f, Art.ownSeat(picture(200, 200, new int[]{0, 40, 200, 70}, new int[]{60, 70, 140, 140}, new int[]{70, 140, 90, 200}, new int[]{110, 140, 130, 200})), 0);
        // someone cross-legged on the floor: wide at the bottom, but nothing open under the "plank"
        assertEquals(-1f, Art.ownSeat(picture(200, 160, new int[]{80, 0, 120, 40}, new int[]{60, 40, 140, 110}, new int[]{0, 110, 200, 160})), 0);
        // a picture with its background (no cut-out) is never read so
        int[] full = picture(100, 100, new int[]{0, 0, 100, 100});
        assertEquals(-1f, Art.ownSeat(full), 0);
    }

    /**
     * v47.8: a photograph of someone sitting, with its background, shows what they sit on (the old man of बाबू के लिए on
     * his own bench, grey background and all, was lifted with his bench onto a drawn one): it brings its own seat. A
     * standing one, a face-and-shoulders one or a cut-out with no seat found does not.
     */
    @Test public void aSeatedPhotographWithItsBackgroundBringsItsSeat() {
        Art.PoseSprite photo = new Art.PoseSprite();
        photo.pose = PoseSense.SIT; photo.withBackground = true;
        assertTrue(Casting.seatOfItsOwn(photo));
        Art.PoseSprite face = new Art.PoseSprite();
        face.pose = PoseSense.SIT; face.withBackground = true; face.closeUp = true;
        assertFalse("a face-and-shoulders picture shows no seat", Casting.seatOfItsOwn(face));
        Art.PoseSprite standing = new Art.PoseSprite();
        standing.pose = PoseSense.STAND; standing.withBackground = true;
        assertFalse(Casting.seatOfItsOwn(standing));
        Art.PoseSprite cutOut = new Art.PoseSprite();
        cutOut.pose = PoseSense.SIT;
        assertFalse("a cut-out sitter with no seat found is lifted onto the drawn seat", Casting.seatOfItsOwn(cutOut));
        cutOut.ownSeat = 0.4f;
        assertTrue(Casting.seatOfItsOwn(cutOut));
    }

    @Test public void theBrickGoesUnderTheLegTheScriptNames() {
        assertEquals(1, Art.sideNear(Txt.norm("टूटी बेंच जिसके दायें पैर के नीचे आधी लाल ईंट लगी है"), Txt.norm("ईंट")));
        assertEquals(1, Art.sideNear(Txt.norm("बेंच के चार पैर हैं आगे वाला दायां पैर बीच से टूटा हुआ है"), Txt.norm("टूट")));
        assertEquals(-1, Art.sideNear(Txt.norm("a brick under its left leg"), "brick"));
        assertEquals(0, Art.sideNear(Txt.norm("एक आधी टूटी हुई लाल रंग की पुरानी ईंट"), Txt.norm("ईंट")));
    }
}

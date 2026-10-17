package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Cutout;
import com.tarun.kahani.core.Rigidity;
import org.junit.Test;

/**
 * v47.6 rules the technical director works by, checked without a film: the material class of each thing आखिरी चिट्ठी
 * lists, and a figure cut from a black sheet keeps its black pupils and hair (they came out see-through).
 */
public final class MaterialsAndCutoutTest {
    @Test public void listedThingsHaveTheirMaterialClass() {
        assertEquals(Rigidity.RIGID, Rigidity.material("दादाजी की लकड़ी की छड़ी"));
        assertEquals(Rigidity.FLEXIBLE, Rigidity.material("परी का गुलाबी स्कूल बैग"));
        assertEquals(Rigidity.FLEXIBLE, Rigidity.material("परी के लाल रिबन"));
        assertEquals(Rigidity.FLEXIBLE, Rigidity.material("पीली पड़ी मुड़ी-तुड़ी चिट्ठी"));
        assertEquals(Rigidity.RIGID, Rigidity.material("लकड़ी की टूटी बेंच"));
        assertEquals(Rigidity.RIGID, Rigidity.material("बेंच के नीचे लगी ईंट"));
        assertEquals(Rigidity.GRANULAR, Rigidity.material("जमीन पर बिखरे सूखे पत्ते और धूल"));
        assertEquals(Rigidity.LIQUID, Rigidity.material("गिलास में पानी"));
        assertEquals(Rigidity.UNKNOWN, Rigidity.material("कुछ"));
        assertTrue("scattered dust and leaves are ground cover", Rigidity.groundCover("जमीन पर बिखरे सूखे पत्ते और धूल"));
        assertFalse("a stick is not", Rigidity.groundCover("दादाजी की लकड़ी की छड़ी"));
    }

    @Test public void blackSheetKeepsDarkPupilsAndHair() {
        // a 200x300 figure on pure black: a skin-coloured body with two black pupils (12x12) inside white eyes and a
        // dark-hair cap touching nothing but the face
        int w = 200, h = 300;
        int[] px = new int[w * h];
        java.util.Arrays.fill(px, 0xFF000000);
        for (int y = 40; y < 280; y++) for (int x = 50; x < 150; x++) px[y * w + x] = 0xFFD9A074;
        for (int y = 40; y < 70; y++) for (int x = 55; x < 145; x++) px[y * w + x] = 0xFF161210;      // hair, 22 from black
        for (int[] e : new int[][]{{70, 90}, {110, 90}}) {
            for (int y = e[1]; y < e[1] + 24; y++) for (int x = e[0]; x < e[0] + 24; x++) px[y * w + x] = 0xFFFFFFFF;
            for (int y = e[1] + 6; y < e[1] + 18; y++) for (int x = e[0] + 6; x < e[0] + 18; x++) px[y * w + x] = 0xFF050505;
        }
        int[] cut = px.clone();
        Cutout.removeBackground(cut, w, h, true, 0xFF000000);
        assertEquals("the border is gone", 0, cut[5] >>> 24);
        assertEquals("a pupil stays", 255, cut[(90 + 12) * w + 70 + 12] >>> 24);
        assertEquals("the hair stays", 255, cut[55 * w + 100] >>> 24);
    }
}

package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.FaceModel;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.junit.Test;

/**
 * v47.13 (the user: "trust should be more than 95 percent … find another way to find correct positions"): the faces of
 * the user's own pictures found by the face detector (YuNet through ONNX Runtime) — the eyes on the eyes, the mouth
 * under them, inside the head; none on the back of the head.
 */
public class FaceModelTest {
    static final String STILLS = "../../tools/testdata/aakhiri_chitthi/stills/";

    static int[] argb(BufferedImage im) { return im.getRGB(0, 0, im.getWidth(), im.getHeight(), null, 0, im.getWidth()); }

    static FaceModel.Face face(String name) throws Exception {
        BufferedImage im = ImageIO.read(new File(STILLS + name));
        assertNotNull(name, im);
        return FaceModel.face(argb(im), im.getWidth(), im.getHeight());
    }

    static void aFace(String name, BufferedImage im, FaceModel.Face f) {
        assertNotNull(name + ": a face", f);
        float d = (float) Math.hypot(f.eyeRX - f.eyeLX, f.eyeRY - f.eyeLY);
        assertTrue(name + ": the eyes apart " + d, d > im.getWidth() * 0.01f);
        assertTrue(name + ": the eyes in the head (upper third)", (f.eyeLY + f.eyeRY) / 2 < im.getHeight() * 0.34f);
        float my = (f.mouthLY + f.mouthRY) / 2, ey = (f.eyeLY + f.eyeRY) / 2;
        assertTrue(name + ": the mouth under the eyes", my > ey + 0.3f * d && my < ey + 2.2f * d);
        assertTrue(name + ": the eyes and the mouth inside the face's box", f.eyeLX >= f.x && f.eyeRX <= f.x + f.w && my <= f.y + f.h);
    }

    @Test public void theOldMansFaceFrontAndTurned() throws Exception {
        assertTrue("the face detector is there (the model in the assets, ONNX Runtime on the class path)", FaceModel.available());
        for (String n : new String[]{"grandpa_standing_front.jpg", "grandpa_three_quarter_with_cane.jpg"}) {
            BufferedImage im = ImageIO.read(new File(STILLS + n));
            aFace(n, im, FaceModel.face(argb(im), im.getWidth(), im.getHeight()));
        }
    }

    @Test public void noFaceOnTheBackOfTheHead() throws Exception {
        assertTrue("the face detector is there (the model in the assets, ONNX Runtime on the class path)", FaceModel.available());
        assertNull("the back view has no face", face("grandpa_back_view.jpg"));
    }
}

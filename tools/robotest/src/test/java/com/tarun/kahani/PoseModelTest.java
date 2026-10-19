package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.PoseModel;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.junit.Test;

/**
 * v47.13 (the user: "what about movenet … skeleton check"): the body's joints in the user's own pictures, found by the
 * pose model (MoveNet Thunder through ONNX Runtime) — head over shoulders over hips over knees over ankles on a
 * standing figure, and a profile's nose ahead of its ear the way it looks.
 */
public class PoseModelTest {
    static PoseModel.Pose pose(String name) throws Exception {
        BufferedImage im = ImageIO.read(new File(FaceModelTest.STILLS + name));
        assertNotNull(name, im);
        return PoseModel.find(FaceModelTest.argb(im), im.getWidth(), im.getHeight());
    }

    @Test public void aStandingFiguresJointsAreInBodyOrder() throws Exception {
        assertTrue("the pose model is there (the model in the assets, ONNX Runtime on the class path)", PoseModel.available());
        for (String n : new String[]{"grandpa_standing_front.jpg", "pari_front_single.jpg"}) {
            PoseModel.Pose p = pose(n);
            assertNotNull(n, p);
            int[] chain = {PoseModel.NOSE, PoseModel.SHOULDER_L, PoseModel.HIP_L, PoseModel.KNEE_L, PoseModel.ANKLE_L};
            for (int j : chain) assertTrue(n + ": joint " + j + " found (" + p.score[j] + ")", p.has(j));
            for (int k = 1; k < chain.length; k++)
                assertTrue(n + ": joint " + chain[k] + " below joint " + chain[k - 1], p.y[chain[k]] > p.y[chain[k - 1]]);
            assertTrue(n + ": both hips", p.has(PoseModel.HIP_L, PoseModel.HIP_R));
            // in a front picture the person's left is on the picture's right
            assertTrue(n + ": the left shoulder on the picture's right", p.x[PoseModel.SHOULDER_L] > p.x[PoseModel.SHOULDER_R]);
        }
    }

    @Test public void aProfileLooksTheWayItsNoseIs() throws Exception {
        assertTrue(PoseModel.available());
        float l = noseAhead(pose("pari_profile_left.jpg")), r = noseAhead(pose("pari_profile_right.jpg"));
        assertTrue("the two profiles look opposite ways: " + l + " / " + r, l * r < 0);
    }

    static float noseAhead(PoseModel.Pose p) {
        assertNotNull(p);
        int ear = p.score[PoseModel.EAR_L] >= p.score[PoseModel.EAR_R] ? PoseModel.EAR_L : PoseModel.EAR_R;
        assertTrue("a nose and an ear", p.has(PoseModel.NOSE) && p.has(ear));
        return p.x[PoseModel.NOSE] - p.x[ear];
    }
}

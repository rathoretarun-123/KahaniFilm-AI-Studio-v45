package com.tarun.kahani.jvm;

import com.tarun.kahani.core.FaceMesh;
import com.tarun.kahani.core.FaceModel;
import com.tarun.kahani.core.PoseModel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * v48 (the user: "recheck that movenet and other are working"): each model run on each picture given, and what it
 * found printed — the face (YuNet), the body's 17 joints (MoveNet) and the lips and lids (FaceMesh) — with a copy of
 * the picture marked with them (pictures_checked/). Exit 1 when a model is not loaded or finds nothing at all.
 *
 * <p>Usage: ModelCheck picture... (with -Dkahani.assets=&lt;assets folder holding models/&gt;)
 */
public final class ModelCheck {
    static final String[] JOINT = {"nose", "eye L", "eye R", "ear L", "ear R", "shoulder L", "shoulder R", "elbow L", "elbow R",
            "wrist L", "wrist R", "hip L", "hip R", "knee L", "knee R", "ankle L", "ankle R"};

    public static void main(String[] a) throws Exception {
        System.out.println("YuNet face detector: " + (FaceModel.available() ? "loaded" : "NOT LOADED"));
        System.out.println("MoveNet body joints: " + (PoseModel.available() ? "loaded" : "NOT LOADED"));
        System.out.println("FaceMesh lips and lids: " + (FaceMesh.available() ? "loaded" : "NOT LOADED"));
        int faces = 0, poses = 0, meshes = 0;
        File outDir = new File("pictures_checked");
        outDir.mkdirs();
        for (String path : a) {
            BufferedImage im = ImageIO.read(new File(path));
            if (im == null) { System.out.println(path + ": not a picture"); continue; }
            int w = im.getWidth(), h = im.getHeight();
            int[] px = im.getRGB(0, 0, w, h, null, 0, w);
            System.out.println("\n" + new File(path).getName() + " (" + w + "x" + h + ")");
            BufferedImage marked = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = marked.createGraphics();
            g.drawImage(im, 0, 0, null);
            int r = Math.max(3, Math.min(w, h) / 120);
            FaceModel.Face f = FaceModel.face(px, w, h);
            if (f != null) {
                faces++;
                System.out.println(String.format(Locale.US, "  face: score %.2f at (%.0f, %.0f) %.0fx%.0f", f.score, f.x, f.y, f.w, f.h));
                g.setColor(Color.GREEN); g.drawRect((int) f.x, (int) f.y, (int) f.w, (int) f.h);
            } else System.out.println("  face: none");
            PoseModel.Pose p = PoseModel.find(px, w, h);
            if (p != null) {
                int sure = 0;
                StringBuilder b = new StringBuilder();
                for (int j = 0; j < 17; j++) {
                    if (p.has(j)) { sure++; g.setColor(Color.RED); g.fillOval((int) p.x[j] - r, (int) p.y[j] - r, 2 * r, 2 * r); }
                    b.append(String.format(Locale.US, "%s%s %.2f", j == 0 ? "" : ", ", JOINT[j], p.score[j]));
                }
                if (sure >= 5) poses++;
                System.out.println(String.format(Locale.US, "  body: %d of 17 joints sure (mean %.2f): %s", sure, p.mean(), b));
            } else System.out.println("  body: none");
            FaceMesh.Marks m = f == null ? null : FaceMesh.find(px, w, h, f);
            if (m != null) {
                meshes++;
                System.out.println(String.format(Locale.US, "  lips: centre (%.0f, %.0f), half width %.0f, open %.2f; eyes at (%.0f, %.0f) and (%.0f, %.0f)",
                        m.mouthX, m.mouthY, m.mouthHW, m.gap, m.eyeX[0], m.eyeY[0], m.eyeX[1], m.eyeY[1]));
                g.setColor(Color.CYAN);
                for (int k = 0; k < 468; k++) g.fillRect((int) m.x[k], (int) m.y[k], Math.max(1, r / 3), Math.max(1, r / 3));
            } else System.out.println("  lips and lids: none" + (f == null ? " (no face)" : " (a profile or an unsure face)"));
            g.dispose();
            ImageIO.write(marked, "jpg", new File(outDir, new File(path).getName().replaceAll("\\.[^.]+$", "") + "_checked.jpg"));
        }
        System.out.println(String.format(Locale.US, "\nRESULT: faces %d, bodies %d, lips/lids %d of %d pictures", faces, poses, meshes, a.length));
        boolean ok = FaceModel.available() && PoseModel.available() && FaceMesh.available() && poses > 0 && faces > 0;
        System.out.println(ok ? "MODELS WORK" : "MODELS NOT WORKING");
        System.exit(ok ? 0 : 1);
    }
}

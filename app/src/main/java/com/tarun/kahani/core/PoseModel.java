package com.tarun.kahani.core;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

import java.io.File;
import java.io.FileInputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * v47.13 (the user: "what about movenet … meshing of skeleton … skeleton check and meshing and rigging"): the body's
 * joints — shoulders, elbows, wrists, hips, knees, ankles, and the nose, eyes and ears — found by a trained pose model,
 * MoveNet SinglePose Thunder (Google, Apache licence 2.0; the TensorFlow.js graph of the npm package
 * {@code @vladmandic/human-models} 3.0.4, converted to ONNX at full precision, 25 MB — the user: "keep the full model"), run with ONNX
 * Runtime like the face detector. The rig puts its hips and knees where the joints are (a long kurta hides them from
 * the outline: the old man's legs folded at his belly when he sat) and bends the forearm at the elbow.
 *
 * <p>The model is read from {@link #source} (the phone's assets) or from the assets folder named by the
 * {@code kahani.assets} property (the desktop); without it, or without the runtime, nothing is found and the rig is
 * measured from the outline as before.
 */
public final class PoseModel {
    private PoseModel() {}

    public static volatile FaceModel.Source source;
    public static final String ASSET = "models/movenet_thunder.onnx";
    static final int IN = 256;
    /** A joint is taken at this score or above. */
    public static final float MIN_SCORE = 0.3f;

    public static final int NOSE = 0, EYE_L = 1, EYE_R = 2, EAR_L = 3, EAR_R = 4, SHOULDER_L = 5, SHOULDER_R = 6,
            ELBOW_L = 7, ELBOW_R = 8, WRIST_L = 9, WRIST_R = 10, HIP_L = 11, HIP_R = 12, KNEE_L = 13, KNEE_R = 14,
            ANKLE_L = 15, ANKLE_R = 16;

    private static OrtEnvironment env;
    private static OrtSession session;
    private static boolean failed;

    /** The 17 joints in the picture's pixels (the person's own left and right) and how sure the model is of each. */
    public static final class Pose {
        public final float[] x = new float[17], y = new float[17], score = new float[17];
        public boolean has(int j) { return score[j] >= MIN_SCORE; }
        public boolean has(int a, int b) { return has(a) && has(b); }
        /** The middle of two joints' heights (pixels). */
        public float midY(int a, int b) { return (y[a] + y[b]) / 2; }
        public float mean() { float s = 0; for (float v : score) s += v; return s / 17; }
    }

    private static synchronized OrtSession session() {
        if (session != null || failed) return session;
        try {
            byte[] m = source != null ? source.bytes() : null;
            if (m == null) {
                String root = System.getProperty("kahani.assets", "");
                File f = new File(root, ASSET);
                if (root.length() > 0 && f.isFile()) m = FaceModel.readAll(new FileInputStream(f));
            }
            if (m == null) { failed = true; return null; }
            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions o = new OrtSession.SessionOptions();
            o.setIntraOpNumThreads(2);
            session = env.createSession(m, o);
        } catch (Throwable e) {
            // (no runtime on this machine, or no model: the rig is measured from the outline as before)
            failed = true;
            session = null;
        }
        return session;
    }

    /** The pose model is there and loaded. */
    public static boolean available() { return session() != null; }

    /**
     * The joints of the figure in an ARGB picture (its see-through pixels on mid grey): the figure's box, squared with a
     * margin, is what the model looks at. Null when there is no model or no figure.
     */
    public static Pose find(int[] argb, int w, int h) {
        OrtSession s = session();
        if (s == null || argb == null || w < 16 || h < 16 || argb.length < w * h) return null;
        int x0 = w, y0 = h, x1 = -1, y1 = -1;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if ((argb[y * w + x] >>> 24) > 128) {
            if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y;
        }
        if (x1 < 0) { x0 = 0; y0 = 0; x1 = w - 1; y1 = h - 1; }
        float cx = (x0 + x1) / 2f, cy = (y0 + y1) / 2f, side = Math.max(x1 - x0, y1 - y0) * 1.1f + 1;
        float sc = IN / side;
        int[] in = new int[IN * IN * 3];
        for (int py = 0; py < IN; py++) {
            float fy = (py + 0.5f - IN / 2f) / sc + cy - 0.5f;
            for (int px = 0; px < IN; px++) {
                float fx = (px + 0.5f - IN / 2f) / sc + cx - 0.5f;
                float r = 0, g = 0, b = 0;
                int ix = (int) Math.floor(fx), iy = (int) Math.floor(fy);
                float tx = fx - ix, ty = fy - iy;
                for (int k = 0; k < 4; k++) {
                    int xx = ix + (k & 1), yy = iy + (k >> 1);
                    float wt = ((k & 1) == 0 ? 1 - tx : tx) * ((k >> 1) == 0 ? 1 - ty : ty);
                    int c = xx < 0 || yy < 0 || xx >= w || yy >= h ? 0 : argb[yy * w + xx];
                    float a = (c >>> 24) / 255f;
                    r += wt * (((c >> 16) & 255) * a + 128 * (1 - a));
                    g += wt * (((c >> 8) & 255) * a + 128 * (1 - a));
                    b += wt * ((c & 255) * a + 128 * (1 - a));
                }
                int i = (py * IN + px) * 3;
                in[i] = Math.round(r); in[i + 1] = Math.round(g); in[i + 2] = Math.round(b);
            }
        }
        try (OnnxTensor t = OnnxTensor.createTensor(env, IntBuffer.wrap(in), new long[]{1, IN, IN, 3})) {
            Map<String, OnnxTensor> feed = new HashMap<String, OnnxTensor>();
            feed.put(s.getInputNames().iterator().next(), t);
            try (OrtSession.Result res = s.run(feed)) {
                FloatBuffer fb = ((OnnxTensor) res.get(0)).getFloatBuffer();
                float[] o = new float[fb.remaining()];
                fb.get(o);
                if (o.length < 51) return null;
                Pose p = new Pose();
                for (int j = 0; j < 17; j++) {
                    // (y, x, score), each in the model's square from 0 to 1
                    p.y[j] = (o[3 * j] * IN - IN / 2f) / sc + cy;
                    p.x[j] = (o[3 * j + 1] * IN - IN / 2f) / sc + cx;
                    p.score[j] = o[3 * j + 2];
                }
                return p;
            }
        } catch (Throwable e) {
            return null;
        }
    }
}

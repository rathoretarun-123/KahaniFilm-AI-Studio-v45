package com.tarun.kahani.core;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

import java.io.File;
import java.io.FileInputStream;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * v47.13 (the user: "lip sync and finding proper lips and other body parts must be perfect on all the poses"; the
 * open-source survey's "Face Mesh … for lips, eyelids and brows"): 468 points of the face — the outline of the lips,
 * the opening between them, each eye's corners and lids — found by MediaPipe FaceMesh (Google, Apache licence 2.0; the
 * TensorFlow.js graph of the npm package {@code @vladmandic/human-models} 3.0.4, converted to ONNX, 3 MB) in the face
 * the face detector found. The blink's lid is sized to the eye's real corners and lids, and a mouth already open in the
 * picture (the gap between the lips) is never opened further.
 */
public final class FaceMesh {
    private FaceMesh() {}

    public static volatile FaceModel.Source source;
    public static final String ASSET = "models/facemesh.onnx";
    static final int IN = 192;
    /** The model's own "a face is here" score must reach this (a profile scores near 0: its points are not used). */
    public static final float MIN_FLAG = 0.5f;

    /** The outer lip corners, the middle of the inner upper and lower lip, and each eye's corners and lids. */
    static final int LIP_L = 61, LIP_R = 291, INNER_UP = 13, INNER_DOWN = 14;
    static final int[][] EYES = {{33, 133, 159, 145}, {362, 263, 386, 374}};      // corner, corner, upper lid, lower lid

    private static OrtEnvironment env;
    private static OrtSession session;
    private static boolean failed;
    public static volatile int asked, found;
    public static String status() { return (session != null ? "FaceMesh loaded" : failed ? "FaceMesh NOT loaded" : "FaceMesh not used") + ": " + found + " of " + asked + " faces gave lips and lids"; }

    /** The points in the picture's pixels. */
    public static final class Marks {
        public final float[] x = new float[468], y = new float[468];
        public float flag;
        /** Each eye (0: the picture's left) as {left, right, up, down} about its middle, and that middle, in pixels. */
        public final float[][] eye = new float[2][4];
        public final float[] eyeX = new float[2], eyeY = new float[2];
        /** The lips' corners' half distance, the opening's middle, and the gap between the lips over their width. */
        public float mouthHW, mouthX, mouthY, gap;
    }

    private static synchronized OrtSession session() {
        if (session != null || failed) return session;
        if (Boolean.getBoolean("kahani.noMesh")) { failed = true; return null; }
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
            // (no runtime or no model: the eyes and the mouth are read from the picture's colours as before)
            failed = true;
            session = null;
        }
        return session;
    }

    public static boolean available() { return session() != null; }

    /** The face's points in an ARGB picture, inside the face the detector found; null when there is no sure face. */
    public static Marks find(int[] argb, int w, int h, FaceModel.Face f) {
        OrtSession s = session();
        if (s == null || f == null || argb == null || argb.length < w * h) return null;
        asked++;
        float cx = f.x + f.w / 2, cy = f.y + f.h / 2, side = Math.max(f.w, f.h) * 1.5f;
        if (side < 12) return null;
        float sc = IN / side;
        float[] in = new float[IN * IN * 3];
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
                in[i] = r / 255f; in[i + 1] = g / 255f; in[i + 2] = b / 255f;
            }
        }
        try (OnnxTensor t = OnnxTensor.createTensor(env, FloatBuffer.wrap(in), new long[]{1, IN, IN, 3})) {
            Map<String, OnnxTensor> feed = new HashMap<String, OnnxTensor>();
            feed.put(s.getInputNames().iterator().next(), t);
            try (OrtSession.Result res = s.run(feed)) {
                float[] mesh = null, flag = null;
                for (Map.Entry<String, OnnxValue> e : res) {
                    if (!(e.getValue() instanceof OnnxTensor)) continue;
                    FloatBuffer fb = ((OnnxTensor) e.getValue()).getFloatBuffer();
                    float[] a = new float[fb.remaining()];
                    fb.get(a);
                    if (a.length == 1404) mesh = a; else if (a.length == 1) flag = a;
                }
                if (mesh == null || flag == null) return null;
                Marks m = new Marks();
                m.flag = flag[0];
                if (m.flag < MIN_FLAG) return null;
                found++;
                for (int j = 0; j < 468; j++) {
                    m.x[j] = (mesh[3 * j] - IN / 2f) / sc + cx;
                    m.y[j] = (mesh[3 * j + 1] - IN / 2f) / sc + cy;
                }
                // the eyes, the picture's left one first
                int first = m.x[EYES[0][0]] + m.x[EYES[0][1]] <= m.x[EYES[1][0]] + m.x[EYES[1][1]] ? 0 : 1;
                for (int k = 0; k < 2; k++) {
                    int[] e = EYES[k == 0 ? first : 1 - first];
                    float x0 = Math.min(m.x[e[0]], m.x[e[1]]), x1 = Math.max(m.x[e[0]], m.x[e[1]]);
                    float ex = (x0 + x1) / 2, ey = (m.y[e[0]] + m.y[e[1]] + m.y[e[2]] + m.y[e[3]]) / 4;
                    m.eyeX[k] = ex; m.eyeY[k] = ey;
                    m.eye[k][0] = ex - x0; m.eye[k][1] = x1 - ex;
                    m.eye[k][2] = Math.max(0, ey - m.y[e[2]]); m.eye[k][3] = Math.max(0, m.y[e[3]] - ey);
                }
                float lw = Math.abs(m.x[LIP_R] - m.x[LIP_L]);
                m.mouthHW = lw / 2;
                m.mouthX = (m.x[INNER_UP] + m.x[INNER_DOWN]) / 2;
                m.mouthY = (m.y[INNER_UP] + m.y[INNER_DOWN]) / 2;
                m.gap = lw > 0 ? Math.max(0, m.y[INNER_DOWN] - m.y[INNER_UP]) / lw : 0;
                return m;
            }
        } catch (Throwable e) {
            return null;
        }
    }
}

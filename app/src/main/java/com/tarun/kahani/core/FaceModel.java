package com.tarun.kahani.core;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * v47.13 (the user: "trust should be more than 95 percent … find another way to find correct positions"): the face's
 * eyes, nose and mouth corners found by a trained face detector — YuNet (OpenCV model zoo, MIT licence, 230 KB),
 * run with ONNX Runtime, the same on the phone and on the desktop — instead of read from the picture's colours. The
 * colour readings took the old man's beige kurta for skin and put his face on his chest and his knees; on all 102 of
 * आखिरी चिट्ठी's pictures YuNet found the faces there are (front, three-quarter, sitting, bending, crying, behind
 * glasses) and none on a back or a thing.
 *
 * <p>The model is read from {@link #source} (the phone's assets) or from the assets folder named by the
 * {@code kahani.assets} property (the desktop); without it, or without the runtime, nothing is found and the studio
 * reads faces as before.
 */
public final class FaceModel {
    private FaceModel() {}

    /** Where the model's bytes come from (set by the phone app from its assets). */
    public interface Source { byte[] bytes() throws Exception; }

    public static volatile Source source;
    public static final String ASSET = "models/face_yunet_2023mar.onnx";
    /** A face is taken at this score or above (the false ones on the old man's sleeve scored 0.51 and 0.55). */
    public static float MIN_SCORE = 0.6f;
    static final int IN = 640;

    private static OrtEnvironment env;
    private static OrtSession session;
    private static boolean failed;

    /** A face found, in the picture's pixels: its score, box, eyes and mouth corners (left and right as seen), nose. */
    public static final class Face {
        public float score, x, y, w, h;
        public float eyeLX, eyeLY, eyeRX, eyeRY, noseX, noseY, mouthLX, mouthLY, mouthRX, mouthRY;
    }

    private static synchronized OrtSession session() {
        if (session != null || failed) return session;
        try {
            byte[] m = source != null ? source.bytes() : null;
            if (m == null) {
                String root = System.getProperty("kahani.assets", "");
                File f = new File(root, ASSET);
                if (root.length() > 0 && f.isFile()) m = readAll(new FileInputStream(f));
            }
            if (m == null) { failed = true; return null; }
            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions o = new OrtSession.SessionOptions();
            o.setIntraOpNumThreads(2);
            session = env.createSession(m, o);
        } catch (Throwable e) {
            // (no runtime on this machine, or no model: the faces are read from the picture as before)
            failed = true;
            session = null;
        }
        return session;
    }

    /** The detector is there and loaded. */
    public static boolean available() { return session() != null; }

    static byte[] readAll(InputStream in) throws java.io.IOException {
        try (InputStream i = in) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[1 << 16];
            for (int n; (n = i.read(buf)) > 0; ) b.write(buf, 0, n);
            return b.toByteArray();
        }
    }

    /** The faces in an ARGB picture (its see-through pixels on mid grey), best first; empty when none or no detector. */
    public static List<Face> find(int[] argb, int w, int h) {
        List<Face> out = new ArrayList<Face>();
        OrtSession s = session();
        if (s == null || argb == null || w < 8 || h < 8) return out;
        float sc = IN / (float) Math.max(w, h);
        int sw = Math.max(1, Math.round(w * sc)), sh = Math.max(1, Math.round(h * sc));
        // the picture scaled into the top-left of a 640 x 640 input, blue-green-red planes, 0..255 (YuNet's own order)
        float[] in = new float[3 * IN * IN];
        for (int y = 0; y < sh; y++) {
            float fy = Math.min(h - 1f, (y + 0.5f) / sc - 0.5f);
            int y0 = Math.max(0, (int) fy), y1 = Math.min(h - 1, y0 + 1);
            float ty = Math.max(0, fy - y0);
            for (int x = 0; x < sw; x++) {
                float fx = Math.min(w - 1f, (x + 0.5f) / sc - 0.5f);
                int x0 = Math.max(0, (int) fx), x1 = Math.min(w - 1, x0 + 1);
                float tx = Math.max(0, fx - x0);
                float r = 0, g = 0, b = 0;
                for (int k = 0; k < 4; k++) {
                    int c = argb[(k < 2 ? y0 : y1) * w + ((k & 1) == 0 ? x0 : x1)];
                    float wt = ((k & 1) == 0 ? 1 - tx : tx) * (k < 2 ? 1 - ty : ty);
                    float a = (c >>> 24) / 255f;
                    r += wt * (((c >> 16) & 255) * a + 128 * (1 - a));
                    g += wt * (((c >> 8) & 255) * a + 128 * (1 - a));
                    b += wt * ((c & 255) * a + 128 * (1 - a));
                }
                int i = y * IN + x;
                in[i] = b; in[IN * IN + i] = g; in[2 * IN * IN + i] = r;
            }
        }
        try (OnnxTensor t = OnnxTensor.createTensor(env, FloatBuffer.wrap(in), new long[]{1, 3, IN, IN})) {
            Map<String, OnnxTensor> feed = new HashMap<String, OnnxTensor>();
            feed.put("input", t);
            try (OrtSession.Result res = s.run(feed)) {
                for (int st : new int[]{8, 16, 32}) {
                    float[] cls = flat(res, "cls_" + st), obj = flat(res, "obj_" + st), bb = flat(res, "bbox_" + st), kp = flat(res, "kps_" + st);
                    if (cls == null || obj == null || bb == null || kp == null) continue;
                    int cols = IN / st;
                    for (int i = 0; i < cls.length; i++) {
                        float score = (float) Math.sqrt(clamp01(cls[i]) * clamp01(obj[i]));
                        if (score < 0.3f) continue;
                        int r = i / cols, c = i % cols;
                        float cx = (c + bb[i * 4]) * st, cy = (r + bb[i * 4 + 1]) * st;
                        float bw = (float) Math.exp(bb[i * 4 + 2]) * st, bh = (float) Math.exp(bb[i * 4 + 3]) * st;
                        Face f = new Face();
                        f.score = score;
                        f.x = (cx - bw / 2) / sc; f.y = (cy - bh / 2) / sc; f.w = bw / sc; f.h = bh / sc;
                        float[] p = new float[10];
                        for (int n = 0; n < 5; n++) { p[2 * n] = (kp[i * 10 + 2 * n] + c) * st / sc; p[2 * n + 1] = (kp[i * 10 + 2 * n + 1] + r) * st / sc; }
                        // (YuNet: the right eye, the left eye, the nose, the right and the left corner of the mouth — the
                        // person's own right is the picture's left)
                        boolean swapE = p[0] > p[2], swapM = p[6] > p[8];
                        f.eyeLX = swapE ? p[2] : p[0]; f.eyeLY = swapE ? p[3] : p[1]; f.eyeRX = swapE ? p[0] : p[2]; f.eyeRY = swapE ? p[1] : p[3];
                        f.noseX = p[4]; f.noseY = p[5];
                        f.mouthLX = swapM ? p[8] : p[6]; f.mouthLY = swapM ? p[9] : p[7]; f.mouthRX = swapM ? p[6] : p[8]; f.mouthRY = swapM ? p[7] : p[9];
                        out.add(f);
                    }
                }
            }
        } catch (Throwable e) {
            return new ArrayList<Face>();
        }
        Collections.sort(out, new java.util.Comparator<Face>() {
            @Override public int compare(Face a, Face b) { return Float.compare(b.score, a.score); }
        });
        List<Face> keep = new ArrayList<Face>();
        for (Face f : out) {
            boolean ok = true;
            for (Face k : keep) if (iou(f, k) > 0.3f) { ok = false; break; }
            if (ok) keep.add(f);
        }
        return keep;
    }

    /**
     * The figure's face: of the faces scored MIN_SCORE or more (and not far below the best), the largest — a picture
     * is of one figure, and a hand held up beside a turned face (परी with the letter, 0.86 against her face's 0.79) or
     * the knob of a stick scores as high, never as large. Null when none.
     */
    public static Face face(int[] argb, int w, int h) {
        Face best = null;
        List<Face> all = find(argb, w, h);
        float top = all.isEmpty() ? 0 : all.get(0).score;
        for (Face f : all) {
            if (f.score < MIN_SCORE || f.score < top * 0.75f) continue;
            if (best == null || f.w * f.h > best.w * best.h) best = f;
        }
        return best;
    }

    private static float[] flat(OrtSession.Result res, String name) throws Exception {
        for (Map.Entry<String, ai.onnxruntime.OnnxValue> e : res) {
            if (!name.equals(e.getKey()) || !(e.getValue() instanceof OnnxTensor)) continue;
            FloatBuffer fb = ((OnnxTensor) e.getValue()).getFloatBuffer();
            float[] a = new float[fb.remaining()];
            fb.get(a);
            return a;
        }
        return null;
    }

    private static float clamp01(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }

    private static float iou(Face a, Face b) {
        float iw = Math.max(0, Math.min(a.x + a.w, b.x + b.w) - Math.max(a.x, b.x));
        float ih = Math.max(0, Math.min(a.y + a.h, b.y + b.h) - Math.max(a.y, b.y));
        float in = iw * ih;
        return in / Math.max(1e-6f, a.w * a.h + b.w * b.h - in);
    }
}

package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * v47.7 (the user: "check every frame visually … the animation, lipsync and movement should be very very smooth, no
 * extra lips or eyes anywhere, no shaking or random movements"): every character in every frame as the renderer really
 * drew it — where it stood (x, its ground y, its drawn height) and with which picture — read back in order once the
 * film is drawn. Inside one shot the director's motion is eased: a character never moves by much more than its walk
 * between two frames, never changes speed in one frame, never changes size in one frame, and keeps its picture (a
 * walk steps through its pictures with a cross-fade). What breaks that is reported with its frame:
 * <ul>
 * <li>a jolt — a change of speed between two frames of more than {@link #JOLT} of the character's height per frame
 * (a shake, a snap back, a stutter);</li>
 * <li>a jump — a move of more than {@link #JUMP} of its height in one frame (a teleport, a picture placed elsewhere);</li>
 * <li>a size pop — its drawn height changing by more than {@link #SIZE} in one frame;</li>
 * <li>a picture pop — one picture of it swapped for another inside a shot without a cross-fade;</li>
 * <li>more than one face drawn for one character in a frame (a second mouth or pair of eyes).</li>
 * </ul>
 * The renderers of the phone's worker threads record into one audit (frames arrive out of order); it is read once, at
 * the end. A camera shake the story asks for moves the frame, not the characters, and is checked by FinalQc.
 */
public final class FrameAudit {
    public static final float JOLT = 0.02f, JUMP = 0.12f, SIZE = 0.04f;
    private final int fps;
    private final Map<String, TreeMap<Integer, float[]>> where = new HashMap<String, TreeMap<Integer, float[]>>();
    private final Map<String, TreeMap<Integer, String>> pictures = new HashMap<String, TreeMap<Integer, String>>();
    private final Map<String, TreeMap<Integer, Integer>> faces = new HashMap<String, TreeMap<Integer, Integer>>();
    private final Map<String, String> names = new HashMap<String, String>();

    public FrameAudit(int fps) { this.fps = fps; }

    private int frame(float t) { return Math.round(t * fps); }

    /** Where a character was drawn at t: its x, its ground y and its height on the stage (last call of a frame wins). */
    public synchronized void at(Film.Actor a, float t, float x, float y, float h) {
        TreeMap<Integer, float[]> m = where.get(a.c.id);
        if (m == null) { m = new TreeMap<Integer, float[]>(); where.put(a.c.id, m); names.put(a.c.id, a.c.displayName); }
        m.put(frame(t), new float[]{x, y, h});
    }

    /** Which picture it was drawn with (cycle: stepping through a walk's pictures cross-faded, or narrowed into a turn — never a pop). */
    public synchronized void picture(Film.Actor a, float t, String key, boolean cycle) {
        TreeMap<Integer, String> m = pictures.get(a.c.id);
        if (m == null) { m = new TreeMap<Integer, String>(); pictures.put(a.c.id, m); }
        m.put(frame(t), cycle ? "~cycle" : key);
    }

    /** A face (eyes and mouth) drawn for it in this frame. */
    public synchronized void face(Film.Actor a, float t) {
        TreeMap<Integer, Integer> m = faces.get(a.c.id);
        if (m == null) { m = new TreeMap<Integer, Integer>(); faces.put(a.c.id, m); }
        Integer n = m.get(frame(t));
        m.put(frame(t), n == null ? 1 : n + 1);
    }

    /** What the audit found. */
    public static final class Result {
        public int frames, characterFrames, pairs, jolts, jumps, sizePops, picturePops, extraFaces;
        public final List<String> findings = new ArrayList<String>();

        public boolean clean() { return jolts + jumps + sizePops + picturePops + extraFaces == 0; }

        public String text() {
            StringBuilder b = new StringBuilder();
            b.append(String.format(Locale.US, "FRAME AUDIT — every character in every frame as drawn (%d frames, %d character-frames, %d frame pairs inside shots)%n",
                    frames, characterFrames, pairs));
            b.append(String.format(Locale.US, "• Jolts (a change of speed over %.0f%% of the height in one frame): %d; jumps (a move over %.0f%% of the height in one frame): %d; "
                    + "size pops (over %.0f%% in one frame): %d; picture pops inside a shot: %d; extra faces: %d%n",
                    JOLT * 100, jolts, JUMP * 100, jumps, SIZE * 100, sizePops, picturePops, extraFaces));
            int n = 0;
            for (String f : findings) { if (++n > 40) { b.append("  … and ").append(findings.size() - 40).append(" more\n"); break; } b.append("  - ").append(f).append('\n'); }
            return b.toString();
        }
    }

    /** Reads the record in order, frame pair by frame pair inside each shot of the film. */
    public synchronized Result read(Film film) {
        Result r = new Result();
        java.util.Set<Integer> all = new java.util.HashSet<Integer>();
        for (Map.Entry<String, TreeMap<Integer, float[]>> e : where.entrySet()) {
            String who = names.get(e.getKey());
            TreeMap<Integer, float[]> m = e.getValue();
            all.addAll(m.keySet());
            r.characterFrames += m.size();
            int lastJolt = -100, lastJump = -100, lastSize = -100;
            for (Map.Entry<Integer, float[]> fe : m.entrySet()) {
                int f = fe.getKey();
                float[] b = fe.getValue(), a = m.get(f - 1), c = m.get(f + 1);
                if (c == null || !sameShot(film, f, f + 1)) continue;
                r.pairs++;
                float h = Math.max(1, b[2]);
                float step = (float) Math.hypot(c[0] - b[0], c[1] - b[1]);
                if (step > JUMP * h && f - lastJump > fps) { r.jumps++; lastJump = f; r.findings.add(String.format(Locale.US, "%s jumps %.0f px (%.0f%% of the height) between frames %d and %d (%s)", who, step, step * 100 / h, f, f + 1, stamp(f))); }
                if (Math.abs(c[2] - b[2]) > SIZE * h && f - lastSize > fps) { r.sizePops++; lastSize = f; r.findings.add(String.format(Locale.US, "%s changes size by %.0f%% between frames %d and %d (%s)", who, Math.abs(c[2] - b[2]) * 100 / h, f, f + 1, stamp(f))); }
                if (a != null && sameShot(film, f - 1, f)) {
                    float ax = Math.abs(c[0] - 2 * b[0] + a[0]) / h, ay = Math.abs(c[1] - 2 * b[1] + a[1]) / h;
                    if (Math.max(ax, ay) > JOLT && step <= JUMP * h && f - lastJolt > fps / 2) {
                        r.jolts++; lastJolt = f;
                        r.findings.add(String.format(Locale.US, "%s jolts (%.1f%% of the height per frame, %s) at frame %d (%s)", who, Math.max(ax, ay) * 100, ax >= ay ? "across" : "up and down", f, stamp(f)));
                    }
                }
            }
        }
        for (Map.Entry<String, TreeMap<Integer, String>> e : pictures.entrySet()) {
            String who = names.get(e.getKey());
            TreeMap<Integer, String> m = e.getValue();
            for (Map.Entry<Integer, String> fe : m.entrySet()) {
                int f = fe.getKey();
                String c = m.get(f + 1);
                if (c == null || !sameShot(film, f, f + 1) || c.equals(fe.getValue()) || c.startsWith("~") || fe.getValue().startsWith("~")) continue;
                r.picturePops++;
                r.findings.add(String.format(Locale.US, "%s's picture changes from %s to %s between frames %d and %d (%s) without a cross-fade", who, fe.getValue(), c, f, f + 1, stamp(f)));
            }
        }
        for (Map.Entry<String, TreeMap<Integer, Integer>> e : faces.entrySet()) {
            int extra = 0, first = -1;
            for (Map.Entry<Integer, Integer> fe : e.getValue().entrySet()) if (fe.getValue() > 1) { extra++; if (first < 0) first = fe.getKey(); }
            if (extra > 0) { r.extraFaces += extra; r.findings.add(String.format(Locale.US, "%s has more than one face drawn in %d frames, first at frame %d (%s)", names.get(e.getKey()), extra, first, stamp(first))); }
        }
        r.frames = all.size();
        return r;
    }

    private boolean sameShot(Film film, int f0, int f1) {
        float t0 = f0 / (float) fps, t1 = f1 / (float) fps;
        Film.Shot s0 = shotAt(film, t0), s1 = shotAt(film, t1);
        return s0 != null && s0 == s1 && film.segAt(t0) == film.segAt(t1);
    }

    private static Film.Shot shotAt(Film film, float t) {
        for (Film.Shot s : film.shots) if (t >= s.t && t < s.t + s.dur) return s;
        return null;
    }

    private String stamp(int f) { float t = f / (float) fps; return String.format(Locale.US, "%d:%04.1f", (int) (t / 60), t % 60); }
}

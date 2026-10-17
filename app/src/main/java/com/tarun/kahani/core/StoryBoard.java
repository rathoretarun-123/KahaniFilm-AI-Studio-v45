package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * v47.8 (the user: "add one more section in the studio section to take upto 100 images of 3x2 grid as per the story,
 * this will have more than one character at once with objects as well, without background … train your director etc
 * to check these pictures and put rightly in place in animation or take reference of them; starting title and end image
 * should be only one"): the story board.
 * <p>
 * Up to {@link #MAX} pictures in the story's order. The first is the title picture and the last the end picture — one
 * picture each, never split. Every picture between them is a sheet of six moments of the story, three across and two
 * down; each moment is one or more characters with their things on a clear background.
 * <p>
 * Each moment is read for who is in it, left to right: every figure standing apart in it is matched to the character
 * whose own picture has the closest colours (anything else is a thing). In story order, a moment is placed at the first
 * shot of the film, from where the previous one was placed on, that has all of those characters on the stage. The shot
 * list names the moment each shot follows, and the director's check reports a moment whose characters stand in another
 * left-to-right order in the film, and a moment that could not be placed. The moments are references the film is
 * checked and staged against; they are not pasted into the film over the animated characters (their lips and eyes
 * would stop).
 */
public final class StoryBoard {
    public static final int MAX = 100, COLS = 3, ROWS = 2;
    /** A figure smaller than this share of a moment's area is a speck, not a character or a thing. */
    static final float MIN_FIGURE = 0.012f;
    /** The least colour likeness (0..1, histogram intersection) for a figure to be read as a character. */
    static final float MIN_LIKE = 0.42f;

    /** One moment of a sheet. */
    public static final class Moment {
        /** Its place in the story (0 = the first moment of the first sheet), its sheet (1 = the first after the title) and its panel (1..6). */
        public int order, sheet, panel;
        public String file;
        /** Character ids, left to right, and each one's centre across the moment (0..1). */
        public final List<String> who = new ArrayList<String>();
        public final List<Float> whoX = new ArrayList<Float>();
        /** Figures that are none of the characters (the things). */
        public int things;
        /** Nothing drawn in this panel. */
        public boolean empty;
        /** The film shot it was placed at (index into Film.shots), -1 none. */
        public int shot = -1;

        public String label() { return "sheet " + sheet + ", moment " + panel; }
    }

    public final List<Moment> moments = new ArrayList<Moment>();
    /** The title picture and the end picture (null: not given). */
    public String titleFile, endFile;
    public final List<String> notes = new ArrayList<String>();
    /** The director's findings: a moment not placed, or one staged in another order (read by the production check). */
    public final List<String> findings = new ArrayList<String>();

    /** How the pictures divide: the first and the last one picture each when there are two or more, the rest are sheets. */
    public static int[] roles(int n) {
        int[] r = new int[n];               // 0 sheet, 1 title, 2 end
        if (n >= 2) { r[0] = 1; r[n - 1] = 2; }
        return r;
    }

    /** A colour signature of the opaque pixels of {w×h} ARGB pixels: a 4×4×4 RGB histogram summing to 1 (null: too few pixels). */
    public static float[] signature(int[] px, int off, int stride, int x0, int y0, int x1, int y1) {
        float[] hgram = new float[64];
        int n = 0;
        int step = Math.max(1, Math.min(x1 - x0, y1 - y0) / 96);
        for (int y = y0; y < y1; y += step) for (int x = x0; x < x1; x += step) {
            int c = px[off + y * stride + x];
            if ((c >>> 24) < 160) continue;
            int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
            hgram[(r >> 6) * 16 + (g >> 6) * 4 + (b >> 6)]++;
            n++;
        }
        if (n < 30) return null;
        for (int i = 0; i < 64; i++) hgram[i] /= n;
        return hgram;
    }

    /** How alike two signatures are: 1 the same colours in the same shares, 0 nothing in common. */
    public static float likeness(float[] a, float[] b) {
        if (a == null || b == null) return 0;
        float s = 0;
        for (int i = 0; i < 64; i++) s += Math.min(a[i], b[i]);
        return s;
    }

    /**
     * Reads the board: the pictures (names the loader knows), in the story's order. Each character's colours come from
     * Art.Sprite.colours (read when its picture was loaded).
     */
    public static StoryBoard read(Art.Loader L, List<String> files, Art art, Story story) {
        StoryBoard b = new StoryBoard();
        if (files == null || files.isEmpty()) return b;
        List<String> pics = files.size() > MAX ? files.subList(0, MAX) : files;
        if (files.size() > MAX) b.notes.add("story board: the first " + MAX + " of " + files.size() + " pictures are read");
        int[] roles = roles(pics.size());
        int sheet = 0, order = 0;
        for (int i = 0; i < pics.size(); i++) {
            String f = pics.get(i);
            if (roles[i] == 1) { b.titleFile = f; continue; }
            if (roles[i] == 2) { b.endFile = f; continue; }
            sheet++;
            int[] d = null;
            try { d = L.decode(f, 1800); } catch (RuntimeException e) { d = null; }
            if (d == null || d[0] < COLS * 8 || d[1] < ROWS * 8 || d.length < 2 + d[0] * d[1]) {
                b.notes.add("story board sheet " + sheet + " (" + f + ") could not be read");
                order += COLS * ROWS;
                continue;
            }
            for (int r = 0; r < ROWS; r++) for (int c = 0; c < COLS; c++) {
                Moment m = new Moment();
                m.order = order++; m.sheet = sheet; m.panel = r * COLS + c + 1; m.file = f;
                readMoment(m, d, c, r, art, story);
                b.moments.add(m);
            }
        }
        int seen = 0, known = 0;
        for (Moment m : b.moments) if (!m.empty) { seen++; if (!m.who.isEmpty()) known++; }
        b.notes.add(String.format(Locale.US, "story board: %d sheets, %d moments drawn, %d with a character known by its colours%s%s",
                sheet, seen, known, b.titleFile != null ? ", a title picture" : "", b.endFile != null ? ", an end picture" : ""));
        return b;
    }

    /** One panel of a sheet {w, h, px…}: its figures, cut out when the sheet came with a background, read left to right. */
    static void readMoment(Moment m, int[] d, int col, int row, Art art, Story story) {
        int w = d[0], h = d[1];
        int x0 = col * w / COLS, x1 = (col + 1) * w / COLS, y0 = row * h / ROWS, y1 = (row + 1) * h / ROWS;
        int inset = Math.max(1, Math.min(x1 - x0, y1 - y0) / 60);          // the gutter lines between panels stay out
        x0 += inset; x1 -= inset; y0 += inset; y1 -= inset;
        int pw = x1 - x0, ph = y1 - y0;
        int[] px = new int[pw * ph];
        for (int y = 0; y < ph; y++) System.arraycopy(d, 2 + (y0 + y) * w + x0, px, y * pw, pw);
        if (!Cutout.hasAlpha(px)) {
            try {
                Cutout.Result cr = Cutout.process(px, pw, ph);
                // (the cut-out is cropped to the figures: a sheet with its background is read as one group, left to right)
                if (cr != null && cr.px != null && cr.w > 4 && cr.h > 4) { px = cr.px; pw = cr.w; ph = cr.h; }
            } catch (RuntimeException e) { /* read as it is */ }
        }
        // the figures: what stands apart on the clear ground, on a coarse grid (8-connected)
        int gs = Math.max(1, Math.max(pw, ph) / 120), gw = (pw + gs - 1) / gs, gh = (ph + gs - 1) / gs;
        boolean[] on = new boolean[gw * gh];
        for (int gy = 0; gy < gh; gy++) for (int gx = 0; gx < gw; gx++) {
            int x = Math.min(pw - 1, gx * gs + gs / 2), y = Math.min(ph - 1, gy * gs + gs / 2);
            on[gy * gw + gx] = (px[y * pw + x] >>> 24) >= 128;
        }
        int[] label = new int[gw * gh];
        java.util.Arrays.fill(label, -1);
        List<int[]> boxes = new ArrayList<int[]>();      // {minx, miny, maxx, maxy, cells}
        int[] stack = new int[gw * gh];
        for (int i = 0; i < on.length; i++) {
            if (!on[i] || label[i] >= 0) continue;
            int id = boxes.size(), sp = 0;
            int[] bx = {gw, gh, -1, -1, 0};
            stack[sp++] = i; label[i] = id;
            while (sp > 0) {
                int q = stack[--sp], qx = q % gw, qy = q / gw;
                bx[0] = Math.min(bx[0], qx); bx[1] = Math.min(bx[1], qy); bx[2] = Math.max(bx[2], qx); bx[3] = Math.max(bx[3], qy); bx[4]++;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int nx = qx + dx, ny = qy + dy;
                    if (nx < 0 || ny < 0 || nx >= gw || ny >= gh) continue;
                    int nq = ny * gw + nx;
                    if (on[nq] && label[nq] < 0) { label[nq] = id; stack[sp++] = nq; }
                }
            }
            boxes.add(bx);
        }
        List<int[]> figs = new ArrayList<int[]>();
        for (int[] bx : boxes) if (bx[4] >= MIN_FIGURE * gw * gh) figs.add(bx);
        if (figs.isEmpty()) { m.empty = true; return; }
        java.util.Collections.sort(figs, new java.util.Comparator<int[]>() {
            public int compare(int[] a, int[] b) { return (a[0] + a[2]) - (b[0] + b[2]); }
        });
        // each figure to the character it looks most like (each character at most once in a moment)
        List<String> ids = new ArrayList<String>();
        List<float[]> sigs = new ArrayList<float[]>();
        if (art != null && story != null) for (Story.CharacterDef c : story.characters) {
            Art.Sprite s = art.sprites.get(c.id);
            if (s != null && s.colours != null) { ids.add(c.id); sigs.add(s.colours); }
        }
        // (v47.8, the user: "why the director is not able to identify boy girl man woman": with two or more figures, their
        // heights against each other are read too — a child is drawn shorter than a grown-up, so a short figure is not
        // taken for a man or a woman, nor a tall one for a boy or a girl, whatever their clothes' colours)
        List<Float> tall = new ArrayList<Float>();
        if (story != null) for (Story.CharacterDef c : story.characters) {
            Art.Sprite s = art != null ? art.sprites.get(c.id) : null;
            if (s != null && s.colours != null) tall.add(c.look != null ? c.look.height : 1f);
        }
        float maxFig = 0, maxTall = 0;
        for (int[] bx : figs) maxFig = Math.max(maxFig, bx[3] - bx[1] + 1);
        for (float t : tall) maxTall = Math.max(maxTall, t);
        float[][] like = new float[figs.size()][ids.size()];
        for (int i = 0; i < figs.size(); i++) {
            int[] bx = figs.get(i);
            float[] sg = signature(px, 0, pw, Math.min(pw - 1, bx[0] * gs), Math.min(ph - 1, bx[1] * gs), Math.min(pw, (bx[2] + 1) * gs), Math.min(ph, (bx[3] + 1) * gs));
            float rel = maxFig > 0 ? (bx[3] - bx[1] + 1) / maxFig : 1;
            for (int j = 0; j < ids.size(); j++) {
                like[i][j] = likeness(sg, sigs.get(j));
                if (figs.size() >= 2 && maxTall > 0) like[i][j] -= 0.6f * Math.abs(rel - tall.get(j) / maxTall);
            }
        }
        String[] got = new String[figs.size()];
        boolean[] used = new boolean[ids.size()];
        for (int k = 0; k < Math.min(figs.size(), ids.size()); k++) {
            int bi = -1, bj = -1; float best = MIN_LIKE;
            for (int i = 0; i < figs.size(); i++) if (got[i] == null) for (int j = 0; j < ids.size(); j++) if (!used[j] && like[i][j] > best) { best = like[i][j]; bi = i; bj = j; }
            if (bi < 0) break;
            got[bi] = ids.get(bj); used[bj] = true;
        }
        for (int i = 0; i < figs.size(); i++) {
            int[] bx = figs.get(i);
            if (got[i] != null) { m.who.add(got[i]); m.whoX.add((bx[0] + bx[2] + 1) * 0.5f / gw); }
            else m.things++;
        }
    }

    /**
     * Places the moments at the film's shots in story order (the director's reading of the board) and checks each placed
     * moment's left-to-right order against the stage. Appends the board to the shot list.
     */
    public void place(Film film, Story story) {
        if (film == null || moments.isEmpty()) return;
        java.util.Map<String, String> names = new java.util.HashMap<String, String>();
        if (story != null) for (Story.CharacterDef c : story.characters) names.put(c.id, c.displayName);
        int from = 0, placed = 0;
        StringBuilder list = new StringBuilder("\n────────── STORY BOARD (the user's moments, in story order)\n");
        for (Moment m : moments) {
            if (m.empty) continue;
            if (m.who.isEmpty()) {
                list.append(m.label()).append(": no character known by its colours (").append(m.things).append(" figures) — a reference only\n");
                continue;
            }
            // (the next moment is a later moment of the story: the first fitting shot after the last one placed, else that same shot)
            int at = -1;
            for (int i = placed > 0 ? from + 1 : from; i < film.shots.size() && at < 0; i++) if (onStage(film, film.shots.get(i), m.who)) at = i;
            if (at < 0 && placed > 0 && onStage(film, film.shots.get(from), m.who)) at = from;
            if (at < 0) {
                findings.add(m.label() + " (" + names(m.who, names) + " together) is not placed: they are never on the stage together after shot " + (from + 1));
                list.append(m.label()).append(": ").append(names(m.who, names)).append(" — not placed\n");
                continue;
            }
            m.shot = at; placed++;
            from = at;          // (the next moment follows after it, or in the same shot when none after fits; never before)
            Film.Shot sh = film.shots.get(at);
            list.append(m.label()).append(": ").append(names(m.who, names)).append(" → shot ").append(at + 1)
                    .append(String.format(Locale.US, " at %d:%04.1f", (int) (sh.t / 60), sh.t % 60)).append('\n');
            // the order across the stage at the middle of the shot against the board's
            if (m.who.size() >= 2) {
                Film.Seg s = film.segAt(sh.t + Math.min(sh.dur * 0.5f, 1f));
                float tm = sh.t + Math.min(sh.dur * 0.5f, 1f);
                List<Float> xs = new ArrayList<Float>();
                for (String id : m.who) { Film.Actor a = actorOf(s, id); xs.add(a == null ? Float.NaN : Director.xAt(a, tm)); }
                for (int i = 0; i + 1 < m.who.size(); i++) {
                    float xa = xs.get(i), xb = xs.get(i + 1);
                    if (!Float.isNaN(xa) && !Float.isNaN(xb) && xa > xb + 1)
                        findings.add(m.label() + ": the board has " + names.get(m.who.get(i)) + " to the left of " + names.get(m.who.get(i + 1))
                                + ", the film has them the other way round in shot " + (at + 1));
                }
            }
        }
        notes.add("story board: " + placed + " moments placed at their shots");
        film.shotList = (film.shotList == null ? "" : film.shotList) + list;
        for (String n : notes) film.notes.add(n);
        for (String f : findings) { film.notes.add("  ↳ story board check: " + f); film.boardChecks.add(f); }
    }

    private static String names(List<String> ids, java.util.Map<String, String> names) {
        StringBuilder b = new StringBuilder();
        for (String id : ids) { if (b.length() > 0) b.append(", "); b.append(names.containsKey(id) ? names.get(id) : id); }
        return b.toString();
    }

    private static Film.Actor actorOf(Film.Seg s, String id) {
        if (s == null) return null;
        for (Film.Actor a : s.actors) if (a.c.id.equals(id)) return a;
        return null;
    }

    /** Whether every one of these characters is on the stage at the middle of the shot. */
    static boolean onStage(Film film, Film.Shot sh, List<String> who) {
        float tm = sh.t + Math.min(sh.dur * 0.5f, 1f);
        Film.Seg s = film.segAt(tm);
        if (s == null || s.type != Film.S_SCENE) return false;
        for (String id : who) {
            Film.Actor a = actorOf(s, id);
            if (a == null || !a.stateAt(tm).visible) return false;
        }
        return true;
    }
}

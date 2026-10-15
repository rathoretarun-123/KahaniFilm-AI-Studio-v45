package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * v46: the reference-image rules of the studio, and what each character's reference set still lacks.
 * A character may have up to 18 reference images (a front, two sides, two three-quarters, a back and up to ten more,
 * singly or on contact sheets of up to 3×2 panels); a prop or a place up to 10. A partial set is accepted; the gaps
 * are named, never silently filled.
 */
public final class ReferenceCoverage {
    private ReferenceCoverage() {}

    public static final int MAX_CHARACTER_REFERENCES = 18;
    public static final int MAX_PROP_REFERENCES = 10;
    public static final int MAX_LOCATION_REFERENCES = 10;
    /** The largest contact-sheet grid a reference may use. */
    public static final int SHEET_COLUMNS = 3, SHEET_ROWS = 2;
    /** The views a full character set holds, and how many of each. */
    public static final String[] VIEW_NAMES = {"front", "side", "three-quarter", "back"};
    public static final int[] VIEW_WANT = {1, 2, 2, 1};

    /** The number of reference pictures one upload batch may hold for this kind of thing ("char", "obj", "scene"/place). */
    public static int limitFor(String kind) {
        if (kind == null) return MAX_CHARACTER_REFERENCES;
        String k = kind.toLowerCase(java.util.Locale.ROOT);
        if (k.startsWith("obj") || k.equals("prop") || k.equals("object") || k.equals("thing")) return MAX_PROP_REFERENCES;
        if (k.startsWith("scene") || k.equals("place") || k.equals("location")) return MAX_LOCATION_REFERENCES;
        return MAX_CHARACTER_REFERENCES;
    }

    /** True when a split of n panels fits a sheet of at most SHEET_COLUMNS × SHEET_ROWS. */
    public static boolean fitsSheet(int panels) { return panels >= 2 && panels <= SHEET_COLUMNS * SHEET_ROWS; }

    /**
     * How sure a sheet split is: panels of near-equal area in a plausible grid count are "sure"; anything else is
     * "check" (the user is shown the crops before they are trusted). Areas are w*h per piece.
     */
    public static String splitConfidence(long[] areas) {
        if (areas == null || areas.length < 2) return "check";
        long[] s = areas.clone();
        java.util.Arrays.sort(s);
        long med = s[s.length / 2];
        if (med <= 0) return "check";
        int off = 0;
        for (long a : areas) if (a < med * 0.55 || a > med * 1.8) off++;
        boolean gridCount = areas.length == 2 || areas.length == 3 || areas.length == 4 || areas.length == 5 || areas.length == 6 || areas.length == 8 || areas.length == 9 || areas.length == 10 || areas.length == 12;
        return off == 0 && gridCount ? "sure" : "check";
    }

    public static final class Coverage {
        public String key = "", label = "";
        public boolean front;
        public final Set<String> views = new LinkedHashSet<String>();
        public int poses, pictures;
        public final List<String> missing = new ArrayList<String>();
        public String toString() {
            return label + ": " + (front ? "front" : "NO FRONT") + ", views " + views + ", " + poses + " poses, " + pictures + " pictures"
                    + (missing.isEmpty() ? " — complete" : " — missing: " + missing);
        }
    }

    /** What the manifest (cast.txt) holds for every character of the story, and which useful views it lacks. */
    public static List<Coverage> characters(Story st, String cast) {
        List<Coverage> out = new ArrayList<Coverage>();
        if (st == null) return out;
        String manifest = cast == null ? "" : cast;
        for (Story.CharacterDef c : st.cast()) {
            Coverage cv = new Coverage();
            cv.key = c.displayName; cv.label = c.shown();
            int[] have = new int[4];
            for (String l : manifest.split("\n")) {
                String[] f = l.split("\\|");
                if (f.length < 3 || ScriptParser.resolve(st, f[1]) != c) continue;
                if (f[0].equals("char")) { cv.front = true; have[0]++; cv.pictures++; }
                else if (f[0].equals("view") && f.length >= 4) {
                    int a; try { a = Math.abs(Integer.parseInt(f[2].trim())); } catch (NumberFormatException e) { continue; }
                    String name = a >= 150 ? "back" : a >= 70 ? "side" : a >= 20 ? "three-quarter" : "front";
                    cv.views.add(name); cv.pictures++;
                    for (int i = 0; i < VIEW_NAMES.length; i++) if (VIEW_NAMES[i].equals(name)) have[i]++;
                } else if (f[0].equals("pose")) { cv.poses++; cv.pictures++; }
            }
            if (!cv.front) cv.missing.add("front (full body)");
            for (int i = 1; i < VIEW_NAMES.length; i++) {
                int want = VIEW_WANT[i], got = Math.min(have[i], want);
                // a pose picture at that angle also shows the view: count the poses tagged with the angle
                for (String l : manifest.split("\n")) {
                    String[] f = l.split("\\|");
                    if (f.length >= 5 && f[0].equals("pose") && ScriptParser.resolve(st, f[1]) == c) {
                        int a; try { a = Math.abs(Integer.parseInt(f[3].trim())); } catch (NumberFormatException e) { continue; }
                        String name = a >= 150 ? "back" : a >= 70 ? "side" : a >= 20 ? "three-quarter" : "front";
                        if (VIEW_NAMES[i].equals(name) && got < want) { got++; cv.views.add(name); }
                    }
                }
                if (got < want) cv.missing.add(VIEW_NAMES[i] + (want - got > 1 ? " ×" + (want - got) : ""));
            }
            if (cv.poses + (cv.front ? 1 : 0) < 3) cv.missing.add("expressions / actions (a contact sheet of up to 3×2)");
            out.add(cv);
        }
        return out;
    }

    public static String report(List<Coverage> cs) {
        StringBuilder b = new StringBuilder("REFERENCE COVERAGE (up to " + MAX_CHARACTER_REFERENCES + " pictures per character, " + MAX_PROP_REFERENCES + " per prop or place; contact sheets of up to " + SHEET_COLUMNS + "×" + SHEET_ROWS + ")\n");
        for (Coverage c : cs) b.append("  • ").append(c).append('\n');
        return b.toString();
    }
}

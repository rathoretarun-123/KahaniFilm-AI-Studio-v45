package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * v46: the Reader's inventory of what the film depends on — every character, place and prop the screenplay needs —
 * and the state of each against the story's own manifest (cast.txt) and the private library. The Reader never
 * invents a missing essential; it says exactly what is absent, what exists but is not confidently matched, what was
 * generated from an authorised source, and what waits on the user.
 */
public final class AssetDependency {
    private AssetDependency() {}

    public enum State {
        /** A picture is in the manifest (the user's own, or one they accepted). */
        AVAILABLE_MATCHED,
        /** The library holds a candidate, but nobody confirmed it is this character/place/prop. */
        AVAILABLE_UNMATCHED,
        /** Nothing for it, and the film can be told without it (an insert, a background thing). */
        MISSING_NONESSENTIAL,
        /** Nothing for it, and the story cannot be shown without it. */
        MISSING_ESSENTIAL,
        /** Made by the studio (3D doll, drawn, AI) from an authorised source and accepted. */
        GENERATED,
        /** Essential, missing, and already asked of the user: the workflow pauses here. */
        BLOCKED
    }

    public static final class Dep {
        /** character | place | prop */
        public String kind = "", key = "", label = "";
        public State state = State.MISSING_NONESSENTIAL;
        public boolean essential;
        public String reason = "";
        public float confidence;
        public final List<Integer> scenes = new ArrayList<Integer>();
        public String toString() { return kind + ":" + key + " " + state + (essential ? " (essential)" : "") + (reason.length() > 0 ? " — " + reason : ""); }
    }

    /** A library candidate must score at least this to count as "available but not confidently matched". */
    public static final float CANDIDATE_MIN = 0.4f;
    /** At or above this the match is confident; a human still confirms identity before it is MATCHED. */
    public static final float CANDIDATE_SURE = 0.85f;

    /**
     * @param cast       the story manifest text (cast.txt): char|key|file, scene|n|file, shot||key|file|object, …
     * @param candidates library candidates by normalised name → confidence 0..1 (may be null)
     * @param generated  manifest keys whose picture the studio made (3D/drawn/AI) and the user accepted (may be null)
     * @param asked      manifest targets already asked of the user and still pending (may be null)
     */
    public static List<Dep> classify(Story st, String cast, Map<String, Float> candidates, Set<String> generated, Set<String> asked) {
        List<Dep> out = new ArrayList<Dep>();
        if (st == null) return out;
        String manifest = cast == null ? "" : cast;
        Set<Story.CharacterDef> haveChar = new HashSet<Story.CharacterDef>();
        Set<String> haveScene = new HashSet<String>(), haveShot = new HashSet<String>();
        Map<Story.CharacterDef, String> charKey = new LinkedHashMap<Story.CharacterDef, String>();
        for (String l : manifest.split("\n")) {
            String[] f = l.split("\\|");
            if (f.length < 3) continue;
            if (f[0].equals("char")) { Story.CharacterDef c = ScriptParser.resolve(st, f[1]); if (c != null) { haveChar.add(c); charKey.put(c, f[1]); } }
            else if (f[0].equals("scene")) haveScene.add(f[1].replaceAll("[abcr]$", "").replaceAll("v\\d+$", ""));
            else if (f[0].equals("shot") && f.length >= 4) for (String k : f[2].split(",")) haveShot.add(k.trim());
        }
        // characters: essential when they appear on stage (voice-only ones are heard, not seen)
        Map<Story.CharacterDef, List<Integer>> appears = new LinkedHashMap<Story.CharacterDef, List<Integer>>();
        for (Story.Scene sc : st.scenes) for (Story.Beat b : sc.beats) {
            if (b.speaker != null && !b.narrator) add(appears, b.speaker, sc.number);
            for (Story.CharacterDef ch : b.chorus) add(appears, ch, sc.number);
            if (b.type == Story.Beat.DIRECTION) for (Story.CharacterDef c : st.characters) if (c.fromScript && named(b.text, c)) add(appears, c, sc.number);
        }
        for (Story.CharacterDef c : st.cast()) {
            Dep d = new Dep();
            d.kind = "character"; d.key = c.displayName; d.label = c.shown();
            List<Integer> in = appears.get(c);
            if (in != null) d.scenes.addAll(in);
            d.essential = in != null && !in.isEmpty() && !c.voiceOnly;
            if (haveChar.contains(c)) {
                String k = charKey.get(c);
                d.state = generated != null && (generated.contains(k) || generated.contains("char:" + k)) ? State.GENERATED : State.AVAILABLE_MATCHED;
                d.confidence = 1f;
                d.reason = d.state == State.GENERATED ? "made by the studio and accepted" : "picture in the manifest";
            } else settle(d, "char:" + c.displayName, c.displayName, candidates, asked);
            out.add(d);
        }
        // places: essential when a scene is set there
        for (String[] p : Bible.places(st)) {
            Dep d = new Dep();
            d.kind = "place"; d.key = p[0]; d.label = st.shown(p[0]);
            boolean covered = true, any = false;
            for (Story.Scene sc : st.scenes) if (placeOf(sc, p[0])) { any = true; d.scenes.add(sc.number); if (!haveScene.contains(String.valueOf(sc.number))) covered = false; }
            d.essential = any;
            if (any && covered) {
                boolean gen = false;
                if (generated != null) for (int n : d.scenes) if (generated.contains("place:" + n) || generated.contains("scene:" + n)) gen = true;
                d.state = gen ? State.GENERATED : State.AVAILABLE_MATCHED; d.confidence = 1f; d.reason = gen ? "made by the studio and accepted" : "every scene there has its picture";
            } else if (!any) { d.state = State.MISSING_NONESSENTIAL; d.reason = "listed, but no scene is set there"; }
            else settle(d, "place:" + p[0], p[0], candidates, asked);
            out.add(d);
        }
        // props: the things the script lists are essential when a direction handles them; otherwise an insert at most
        for (Story.PlaceDef th : st.things) {
            String nm = th.name.trim();
            if (nm.length() < 2) continue;
            Dep d = new Dep();
            d.kind = "prop"; d.key = nm; d.label = nm;
            for (Story.Scene sc : st.scenes) for (Story.Beat b : sc.beats) if (b.type == Story.Beat.DIRECTION && Director.term(b.text, nm) && !d.scenes.contains(sc.number)) d.scenes.add(sc.number);
            d.essential = !d.scenes.isEmpty();
            if (haveShot.contains(nm)) {
                d.state = generated != null && generated.contains("shot:0:" + nm) ? State.GENERATED : State.AVAILABLE_MATCHED; d.confidence = 1f; d.reason = "insert picture in the manifest";
            } else settle(d, "shot:0:" + nm, nm, candidates, asked);
            out.add(d);
        }
        return out;
    }

    private static void settle(Dep d, String target, String name, Map<String, Float> candidates, Set<String> asked) {
        float best = 0f;
        if (candidates != null) {
            String n = Txt.norm(name);
            for (Map.Entry<String, Float> e : candidates.entrySet()) {
                String k = Txt.norm(e.getKey());
                if (k.equals(n) || k.contains(n) || n.contains(k) || Bible.similar(e.getKey(), name)) best = Math.max(best, e.getValue());
            }
        }
        d.confidence = best;
        if (best >= CANDIDATE_MIN) {
            d.state = State.AVAILABLE_UNMATCHED;
            d.reason = String.format(Locale.US, "a library picture scores %.2f — %s", best, best >= CANDIDATE_SURE ? "confirm it is the same one" : "below the sure mark, ask the user");
        } else if (asked != null && asked.contains(target) && d.essential) { d.state = State.BLOCKED; d.reason = "asked of the user, waiting"; }
        else if (d.essential) { d.state = State.MISSING_ESSENTIAL; d.reason = "no picture and nothing like it in the library"; }
        else { d.state = State.MISSING_NONESSENTIAL; d.reason = "no picture; the film can show the story without it"; }
    }

    private static void add(Map<Story.CharacterDef, List<Integer>> m, Story.CharacterDef c, int scene) {
        List<Integer> l = m.get(c);
        if (l == null) { l = new ArrayList<Integer>(); m.put(c, l); }
        if (!l.contains(scene)) l.add(scene);
    }

    private static boolean named(String text, Story.CharacterDef c) {
        if (Director.term(text, c.displayName)) return true;
        for (String a : c.aliases) if (a.length() > 1 && Director.term(text, a)) return true;
        return false;
    }

    /** The scene is set in this place (the same reading the Studio uses when a picture is placed). */
    public static boolean placeOf(Story.Scene sc, String place) {
        String first = sc.setting.length() > 0 ? sc.setting : sc.title;
        return Bible.similar(place, first) || Txt.norm(first).contains(Txt.norm(place)) || Bible.similar(Bible.firstClauseOf(first), place);
    }

    /** Only the dependencies in this state. */
    public static List<Dep> in(List<Dep> deps, State s) { List<Dep> o = new ArrayList<Dep>(); for (Dep d : deps) if (d.state == s) o.add(d); return o; }

    /** The user-facing list of what must be uploaded or chosen before the film can be made. */
    public static String request(List<Dep> deps) {
        StringBuilder b = new StringBuilder();
        for (Dep d : deps) if (d.state == State.MISSING_ESSENTIAL || d.state == State.BLOCKED)
            b.append(b.length() > 0 ? "\n" : "").append("• ").append(d.label).append(" (").append(d.kind).append(d.scenes.isEmpty() ? "" : ", scene " + d.scenes).append("): please upload a picture");
        for (Dep d : deps) if (d.state == State.AVAILABLE_UNMATCHED)
            b.append(b.length() > 0 ? "\n" : "").append("• ").append(d.label).append(" (").append(d.kind).append("): a library picture may be it — please confirm");
        return b.toString();
    }
}

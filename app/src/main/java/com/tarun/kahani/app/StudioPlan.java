package com.tarun.kahani.app;

import com.tarun.kahani.core.Bible;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;
import com.tarun.kahani.core.Txt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Studio's request list (the user: "debug the studio page properly that is asking for only what is mention in
 * script"): what the Studio, the make-film popup and the film's own wait ask pictures for, and how many.
 * <ol>
 * <li>each character the script names — never the narrator (कथावाचक, सूत्रधार, नैरेटर, Narrator, voice-over), a voice
 *     only, or a group word ("सब", "सभी", "दोनों", "everyone"): up to {@link #MAX_CHARACTER_PICTURES} pictures;</li>
 * <li>each object the script lists: up to {@link #MAX_OBJECT_PICTURES};</li>
 * <li>each place and background of the script: up to {@link #MAX_PLACE_PICTURES};</li>
 * <li>additional reference pictures (the story board): up to {@link #MAX_ADDITIONAL_REFERENCES};</li>
 * <li>the starting page and 6. the ending page: one picture each;</li>
 * <li>reference images and videos for the director's checks: up to {@link #MAX_REFERENCE_MEDIA} in all.</li>
 * </ol>
 * A sheet picture is split into at most {@link #SHEET_COLUMNS} × {@link #SHEET_ROWS} panels, never more: a picture
 * that would come apart into more pieces than that is kept whole (a character with a thing stays intact).
 */
final class StudioPlan {
    private StudioPlan() {}

    static final int MAX_CHARACTER_PICTURES = 18, MAX_OBJECT_PICTURES = 18, MAX_PLACE_PICTURES = 18;
    static final int MAX_ADDITIONAL_REFERENCES = 100, MAX_REFERENCE_MEDIA = 18;
    static final int SHEET_COLUMNS = 3, SHEET_ROWS = 2, MAX_SHEET_PANELS = SHEET_COLUMNS * SHEET_ROWS;

    /** The kinds of an upload target ("angles:<kind>:<key>:<shown>"). */
    static final String CHARACTER = "char", OBJECT = "obj", PLACE = "scene";

    /** Pictures one subject takes: a character, an object (or a change of clothes) or a place. */
    static int limitFor(String kind) {
        if (kind == null) return MAX_CHARACTER_PICTURES;
        String k = kind.toLowerCase(java.util.Locale.ROOT);
        if (k.startsWith("obj") || k.equals("prop") || k.equals("object") || k.equals("thing") || k.equals("costume")) return MAX_OBJECT_PICTURES;
        if (k.startsWith("scene") || k.equals("place") || k.equals("location")) return MAX_PLACE_PICTURES;
        return MAX_CHARACTER_PICTURES;
    }

    /** The names a narrator goes by (core's ScriptParser.isNarrator, and the spellings it may not know). */
    private static final String[] NARRATOR_WORDS = {"कथावाचक", "सूत्रधार", "नैरेटर", "नरेटर", "नेरेटर", "कथाकार", "वाचक", "narrator", "narration",
            "voice over", "voice-over", "voiceover", "वॉयस ओवर", "वॉइस ओवर", "वॉयसओवर", "वाइस ओवर"};

    /** True when the name is the narrator's — heard only, never pictured, never asked for a picture. */
    static boolean narratorLike(String name) {
        if (name == null || name.trim().length() == 0) return false;
        return ScriptParser.isNarrator(name) || Txt.has(name, NARRATOR_WORDS);
    }

    /** True when the character is the narrator under any of its names or its role. */
    static boolean narratorLike(Story.CharacterDef c) {
        if (c == null) return false;
        if (narratorLike(c.displayName) || narratorLike(c.fullName) || narratorLike(c.role)) return true;
        for (String a : c.aliases) if (narratorLike(a)) return true;
        return false;
    }

    /** Words for several speakers at once ("सब:", "दोनों:", "everyone:") — never a character of their own. */
    private static final String[] GROUP_WORDS = {"सब", "सभी", "सब लोग", "सभी लोग", "सब मिलकर", "सब एक साथ", "सब बच्चे", "सभी बच्चे", "सब के सब", "दोनों", "तीनों", "चारों",
            "सारे", "सारे लोग", "भीड़", "everyone", "everybody", "all", "all together", "both", "together", "crowd", "the crowd", "chorus", "all of them"};

    static boolean groupWord(String name) {
        if (name == null) return false;
        String n = Txt.norm(name).replaceAll("[\\(\\)\\[\\]:：\"'।.!?,]+", " ").replaceAll("\\s+", " ").trim();
        if (n.length() == 0) return false;
        for (String g : GROUP_WORDS) if (n.equals(Txt.norm(g))) return true;
        return false;
    }

    /** The character is pictured and asked for: seen in the film, not the narrator, not a voice only, not a group. */
    static boolean askable(Story.CharacterDef c) {
        return c != null && !c.voiceOnly && !narratorLike(c) && !groupWord(c.displayName);
    }

    /** Each character the script names that is asked for a picture, in the script's order. */
    static List<Story.CharacterDef> characters(Story st) {
        List<Story.CharacterDef> out = new ArrayList<Story.CharacterDef>();
        if (st == null) return out;
        for (Story.CharacterDef c : st.characters) if (askable(c)) out.add(c);
        return out;
    }

    /**
     * The story's narrator and group speakers are voices only: marked so on the story (they then never enter
     * Story.cast(), the film's stage, the missing-pictures prompts, AI picture making or the library's suggestions).
     */
    static Story voicesOnly(Story st) {
        if (st == null) return null;
        for (Story.CharacterDef c : st.characters) if (narratorLike(c) || groupWord(c.displayName)) c.voiceOnly = true;
        return st;
    }

    /** Each object the script lists: {name, shown, description}, once each (a person's or a place's name is no object). */
    static List<String[]> objects(Story st) {
        List<String[]> out = new ArrayList<String[]>();
        if (st == null) return out;
        Set<String> names = new HashSet<String>();
        for (Story.CharacterDef c : st.characters) { names.add(Txt.norm(c.displayName)); for (String a : c.aliases) names.add(Txt.norm(a)); }
        for (String[] pl : Bible.places(st)) names.add(Txt.norm(pl[0]));
        Set<String> seen = new HashSet<String>();
        for (Story.PlaceDef th : st.things) {
            String nm = th.name == null ? "" : th.name.trim();
            if (nm.length() < 2 || names.contains(Txt.norm(nm)) || Bible.intangible(nm) || !seen.add(Txt.norm(nm))) continue;
            out.add(new String[]{nm, st.shown(nm), th.description == null ? "" : th.description});
        }
        // Also ask for tangible things that the screenplay introduces through an action rather than a separate
        // "Objects" list. Only direction beats count; dialogue, narrator text, scene headings and descriptions
        // elsewhere must not create asset requests. Reuse the renderer's vocabulary so rigid and soft props share
        // the same names, and suppress a word when it is already part of a longer matching thing or a place name.
        Set<String> placeNames = new HashSet<String>();
        for (String[] pl : Bible.places(st)) placeNames.add(Txt.norm(pl[0]));
        List<String> vocabulary = new ArrayList<String>();
        for (String word : com.tarun.kahani.core.Rigidity.RIGID_WORDS) vocabulary.add(word);
        for (String word : com.tarun.kahani.core.Rigidity.SOFT_WORDS) vocabulary.add(word);
        // Material adjectives describe an object's appearance; they are not props by themselves.
        String[] materialOnly = {"wooden", "wood", "iron", "brass", "copper", "steel", "metal",
                "लकड़ी", "लोहा", "लोहे", "पीतल", "तांबा", "ताँबा", "स्टील", "धातु"};
        List<String> actionObjects = new ArrayList<String>();
        Set<String> actionSeen = new HashSet<String>();
        for (Story.Scene scene : st.scenes) for (Story.Beat beat : scene.beats) {
            if (beat.type != Story.Beat.DIRECTION || !hasPhysicalAction(beat.text)) continue;
            for (String word : vocabulary) {
                String key = Txt.norm(word);
                if (key.length() < 2 || !Txt.hasWholeWord(beat.text, word) || Bible.intangible(word)) continue;
                boolean material = false;
                for (String m : materialOnly) if (key.equals(Txt.norm(m))) { material = true; break; }
                if (material || names.contains(key) || actionSeen.contains(key)) continue;
                boolean isPlace = false;
                for (String place : placeNames) if (place.contains(key) || key.equals(place)) { isPlace = true; break; }
                if (isPlace) continue;
                actionObjects.add(word);
                actionSeen.add(key);
            }
        }
        // Prefer the longest matched word ("बाल्टी", not "बाल"; "walking stick", not a second "stick" request).
        for (String word : actionObjects) {
            String key = Txt.norm(word);
            boolean partOfLonger = false;
            for (String other : actionObjects) {
                String otherKey = Txt.norm(other);
                if (otherKey.length() > key.length() && otherKey.contains(key)) { partOfLonger = true; break; }
            }
            if (partOfLonger || !seen.add(key)) continue;
            out.add(new String[]{word, st.shown(word), ""});
        }
        return out;
    }

    /** A prop mentioned in a physical action, in Hindi or English, not a mere topic or setting. */
    private static boolean hasPhysicalAction(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        return Txt.hasStart(text,
                "उठ", "पकड़", "थाम", "रख", "लेकर", "लेता", "लेती", "लिया", "लिये", "लाए", "लाता", "लाती",
                "देता", "देती", "दिया", "दिए", "खोल", "बंद", "लिख", "पढ़", "पहन", "फेंक", "उछाल", "खींच",
                "धकेल", "घसीट", "पीता", "पीती", "खाता", "खाती", "डाल", "भरता", "भरती", "चलाता", "चलाती",
                "बजाता", "बजाती", "झाड़ता", "झाड़ती", "साफ़", "साफ", "टूट", "गिराता", "गिराती", "उड़ाता", "उड़ाती",
                "बैठता", "बैठती", "लेटता", "लेटती", "सहारा", "ओढ़", "लपेट", "चढ़ता", "चढ़ती", "उतार",
                "picks up", "pick up", "picking up", "holds", "hold", "holding", "takes", "take", "taking",
                "puts", "put", "placing", "places", "opens", "open", "closes", "close", "writes", "write",
                "reads", "read", "wears", "wear", "throws", "throw", "carries", "carry", "grabs", "grab",
                "lifts", "lift", "pulls", "pull", "pushes", "push", "drinks", "drink", "eats", "eat",
                "pours", "pour", "uses", "use", "swings", "swing", "plays", "play", "sits on", "leans on");
    }

    /** One place of the script and the scenes set there (none: a place seen around the scenes, given on the first). */
    static final class Place {
        final String name, shown, description;
        final List<Story.Scene> scenes = new ArrayList<Story.Scene>();
        /** The scene whose pictures this place's uploads are saved as. */
        final Story.Scene host;
        Place(String name, String shown, String description, Story.Scene host) { this.name = name; this.shown = shown; this.description = description; this.host = host; }
        /** True when no scene is set here by name (it is part of where the story happens). */
        boolean partOf() { return scenes.isEmpty(); }
    }

    /**
     * Each place and background of the script, once: the places it lists (variants of one place — the neem, the
     * corner under it, its shade — are one), then the place of any scene the list does not hold. An hour ("समय: शाम")
     * is no place.
     */
    static List<Place> places(Story st) {
        List<Place> out = new ArrayList<Place>();
        if (st == null || st.scenes.isEmpty()) return out;
        Set<Integer> covered = new HashSet<Integer>();
        Set<String> idents = new HashSet<String>();
        List<String[]> listed = Bible.listedPlaces(st);
        for (String[] p : listed) add(out, st, p[0], p[1], covered, idents);
        if (!st.places.isEmpty()) {
            // a script with a list of places: the scenes set somewhere else still have their place asked for
            for (String[] p : Bible.places(st)) {
                boolean fromList = false;
                for (Story.PlaceDef d : st.places) if (d.name.equals(p[0])) fromList = true;
                if (fromList) continue;
                boolean uncovered = false;
                for (Story.Scene sc : st.scenes) if (!covered.contains(sc.number) && AutoLibrary.placeOf(sc, p[0])) uncovered = true;
                if (uncovered) add(out, st, p[0], p[1], covered, idents);
            }
        }
        return out;
    }

    private static void add(List<Place> out, Story st, String name, String desc, Set<Integer> covered, Set<String> idents) {
        String ident = Bible.placeIdentity(name);
        if (ident.length() > 0 && !idents.add(ident)) return;
        List<Story.Scene> here = new ArrayList<Story.Scene>();
        for (Story.Scene sc : st.scenes) if (!covered.contains(sc.number) && AutoLibrary.placeOf(sc, name)) here.add(sc);
        Place pl = new Place(name, st.shown(name), desc == null ? "" : desc, here.isEmpty() ? st.scenes.get(0) : here.get(0));
        for (Story.Scene sc : here) { pl.scenes.add(sc); covered.add(sc.number); }
        out.add(pl);
    }

    /** One picture request of the Studio: {kind, key, shown}; the upload target is "angles:kind:key:shown". */
    static final class Request {
        final String kind, key, shown;
        final int limit;
        Request(String kind, String key, String shown) { this.kind = kind; this.key = key; this.shown = shown; this.limit = limitFor(kind); }
        String target() { return "angles:" + kind + ":" + key + ":" + shown; }
        public String toString() { return kind + ":" + key; }
    }

    /**
     * The whole request list: each character (by its display name), each object, each place (by its host scene) —
     * exactly what the script names, nothing else.
     */
    static List<Request> requests(Story st) {
        List<Request> out = new ArrayList<Request>();
        for (Story.CharacterDef c : characters(st)) out.add(new Request(CHARACTER, c.displayName, c.shown()));
        for (String[] o : objects(st)) out.add(new Request(OBJECT, o[0], o[1]));
        for (Place p : places(st)) out.add(new Request(PLACE, String.valueOf(p.host.number), p.shown));
        return out;
    }

    /**
     * The pieces a sheet splits into, within 3×2: two to six are its panels; more than six means the picture is not a
     * sheet of at most 3×2 views (a crowd, a character with its things) — nothing is split, the picture is kept whole.
     */
    static <T> List<T> withinSheet(List<T> parts) {
        if (parts == null || parts.size() < 2 || parts.size() > MAX_SHEET_PANELS) return new ArrayList<T>();
        return parts;
    }

    /** True when an upload of this many pieces is split (a sheet of at most 3×2). */
    static boolean splits(int pieces) { return pieces >= 2 && pieces <= MAX_SHEET_PANELS; }
}

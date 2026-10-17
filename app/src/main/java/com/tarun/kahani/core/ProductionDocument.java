package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * v47.6: the one production document every stage reads and writes, offline and deterministic. It is built from what
 * the reader (ScriptParser → Story) and the director (Director → Film, the plan the renderer draws) already produce —
 * it does not read the story a second way or plan shots of its own — and it records, with stable ids:
 * <ul>
 * <li>the characters, props and places and every fact about them, each EXPLICIT (the user wrote it), INFERRED (read from
 *     context, with the reason) or UNKNOWN, with the words it came from;</li>
 * <li>the dialogue exactly as written, in order, with its speaker and its delivery note (never spoken);</li>
 * <li>each description's actions as single ordered steps, and the negations ("बिना पलक झपकाए": must not blink);</li>
 * <li>the environment each scene asks for, as causes and effects (the wind → the neem's leaves, the dust, the kurta),
 *     each with the way the film can carry it out (a procedural animation, a particle system, a sound) — or UNSUPPORTED,
 *     said so — and whether the director actually scheduled it;</li>
 * <li>where each listed thing is and who holds it, part by part (the letter: in her bag → परी → रघुवीर जी);</li>
 * <li>the shots the renderer will draw (the director's own shot list, not a second one);</li>
 * <li>an issue register (BLOCKER, MAJOR, MINOR, WARNING, PASS) and the reviews of the reader, the editor, the director
 *     and the technical director, each bound to the revision it read: any change raises the revision, and a review of an
 *     older revision approves nothing; the director signs off creatively but never over an open BLOCKER.</li>
 * </ul>
 * What it does not do: it is no physics engine. Where the film carries a physical requirement by a procedural
 * approximation, the method says so.
 */
public final class ProductionDocument {
    public static final int SCHEMA_VERSION = 1;

    public static final String EXPLICIT = "EXPLICIT", INFERRED = "INFERRED", UNKNOWN = "UNKNOWN";
    public static final String BLOCKER = "BLOCKER", MAJOR = "MAJOR", MINOR = "MINOR", WARNING = "WARNING", PASS = "PASS";
    public static final String MATCHED = "MATCHED", CANDIDATE = "CANDIDATE", NEEDS_REVIEW = "NEEDS_REVIEW", MISSING = "MISSING",
            REJECTED = "REJECTED", GENERATED_PENDING_APPROVAL = "GENERATED_PENDING_APPROVAL", APPROVED = "APPROVED",
            /** a place the list names that no scene happens in (what the main place shows around it): no picture of its own is drawn */
            NOT_REQUIRED = "NOT_REQUIRED";
    /** How the film carries out an effect (capability categories — only the ones named here are used by this app). */
    public static final String KEYFRAMED = "KEYFRAMED", PROCEDURAL = "PROCEDURAL_ANIMATION", PARTICLES = "PARTICLE_SYSTEM",
            SOUND = "SOUND_EFFECT", BACKDROP = "IN_BACKDROP_PICTURE", UNSUPPORTED = "UNSUPPORTED",
            /** a smell, a taste: said by the script for the mood, never seen or heard — the shot can only suggest it */
            NOT_FILMABLE = "NOT_FILMABLE";
    public static final String READER = "READER", EDITOR = "EDITOR", DIRECTOR = "DIRECTOR", TECHNICAL_DIRECTOR = "TECHNICAL_DIRECTOR", QC = "QC";

    // ------------------------------------------------------------------ records

    public static final class Fact {
        public String id, entityId, field, value, evidence, source, span, explanation;
        public double confidence;
    }

    public static final class Entity {
        public String id, kind, name, description = "";
        public final List<Fact> facts = new ArrayList<Fact>();
        public final List<String> sceneIds = new ArrayList<String>();
        public String assetState = MISSING;
        /** A character's grammatical gender for reading the verbs (+1 feminine, -1 masculine, 0 unknown). */
        public int gender;
        /** v47.7: a character that is an animal (a dog, a cow, a horse, a bird): what it does is its own action. */
        public boolean animal;
        public String fact(String field) { for (Fact f : facts) if (f.field.equals(field)) return f.value; return null; }
        public Fact factOf(String field) { for (Fact f : facts) if (f.field.equals(field)) return f; return null; }
    }

    public static final class Scene {
        public String id, placeId, heading;
        public int number;
        public final List<String> lineIds = new ArrayList<String>(), actionIds = new ArrayList<String>(), effectIds = new ArrayList<String>();
        public float t0 = -1, t1 = -1;
    }

    public static final class Line {
        public String id, sceneId, speakerId, speakerName, text, delivery;
        public int order;
        public float start = -1, dur = -1;
    }

    public static final class Action {
        public String id, sceneId, actorId, verb, targetId, span, completion, status = "PLANNED";
        /** Why a step is not carried out (with status UNSUPPORTED). */
        public String reason = "";
        public boolean negated;
        /** Said as a wish, a condition or a likeness ("अगर वह दौड़ती तो…", "मानो उड़ रही हो"): read, never staged. */
        public boolean hypothetical;
        public int order;
        public final List<String> after = new ArrayList<String>();
    }

    public static final class Effect {
        public String id, sceneId, kind, cause, affectedId, behaviour, evidence, span, method, explanation = "";
        public boolean scheduled;
    }

    public static final class StateChange {
        public String id, entityId, sceneId, state, holderId, evidence, span;
        public boolean onScreen;
        public float time = -1;
    }

    public static final class Shot {
        public String id, sceneId, subject, purpose;
        public float t, dur;
        public int size;
        public boolean scripted;
        public final List<String> lineIds = new ArrayList<String>();
    }

    public static final class Issue {
        public String id, severity, stage, sceneId, requirement, evidence, proposal;
        /** What kind of finding it is (the editor's: NARRATED_ORIGIN, TIME_GAP, INSCRIPTION_SCOPE, OFFSCREEN_PERSON, FIGURATIVE_NAME). */
        public String category = "";
        public final List<String> entityIds = new ArrayList<String>();
        public boolean resolved;
    }

    public static final class Review {
        public String role, status, rationale;
        public int revision;
        public final List<String> issueIds = new ArrayList<String>();
    }

    public static final class RenderRecord {
        public String output, status = "NOT_RUN", renderer, detail = "";
        public int revision;
        public float expectedSeconds;
        public long bytes;
    }

    // ------------------------------------------------------------------ the document

    public String productionId = "", title = "";
    public int revision = 1;
    public final List<Entity> characters = new ArrayList<Entity>(), props = new ArrayList<Entity>(), locations = new ArrayList<Entity>();
    public final List<Scene> scenes = new ArrayList<Scene>();
    public final List<Line> lines = new ArrayList<Line>();
    public final List<Action> actions = new ArrayList<Action>();
    public final List<Effect> effects = new ArrayList<Effect>();
    public final List<StateChange> states = new ArrayList<StateChange>();
    public final List<Shot> shots = new ArrayList<Shot>();
    public final List<Issue> issues = new ArrayList<Issue>();
    public final Map<String, Review> reviews = new LinkedHashMap<String, Review>();
    public final List<String> history = new ArrayList<String>();
    public RenderRecord render;
    /** The ids given to the story's people, things and places (kept with the project: a prop added later never renumbers the others). */
    public Ids ids = new Ids();
    /** v47.8: the lines of voices the reader knows are only heard (a device, an announcer, the narrator): no speaker to picture. */
    final java.util.Set<String> heardOnly = new java.util.HashSet<String>();
    private int factN, issueN, stateN;

    /**
     * The stable ids: "CHAR_001", "PROP_007", "LOC_002" — given once, by the order things first appear, and read back
     * from the project the next time, so inserting a thing in the list or renaming the film keeps every other id.
     */
    public static final class Ids {
        final Map<String, String> byKey = new LinkedHashMap<String, String>();
        final Map<String, Integer> top = new HashMap<String, Integer>();
        public static Ids parse(String text) {
            Ids s = new Ids();
            if (text == null) return s;
            for (String l : text.split("\n")) {
                int tab = l.indexOf('\t');
                if (tab <= 0) continue;
                String key = l.substring(0, tab), id = l.substring(tab + 1).trim();
                s.byKey.put(key, id);
                s.bump(id);
            }
            return s;
        }
        void bump(String id) {
            int u = id.lastIndexOf('_');
            if (u < 0) return;
            try { int n = Integer.parseInt(id.substring(u + 1)); String p = id.substring(0, u); Integer was = top.get(p); if (was == null || n > was) top.put(p, n); } catch (NumberFormatException ignored) { }
        }
        public String id(String prefix, String name) {
            String key = prefix + ":" + Txt.norm(name).trim().replaceAll("\\s+", " ");
            String got = byKey.get(key);
            if (got != null) return got;
            Integer n = top.get(prefix);
            int next = n == null ? 1 : n + 1;
            top.put(prefix, next);
            String id = String.format(java.util.Locale.ROOT, "%s_%03d", prefix, next);
            byKey.put(key, id);
            return id;
        }
        public String write() {
            StringBuilder b = new StringBuilder();
            for (Map.Entry<String, String> e : byKey.entrySet()) b.append(e.getKey()).append('\t').append(e.getValue()).append('\n');
            return b.toString();
        }
    }

    // ------------------------------------------------------------------ the reader's part

    /** The document of a story as the reader read it (ids from the project's own, when given). */
    public static ProductionDocument read(Story st, Ids ids) {
        ProductionDocument d = new ProductionDocument();
        if (ids != null) d.ids = ids;
        d.title = st.title == null ? "" : st.title;
        d.productionId = "PROD_" + Integer.toHexString((d.title + "|" + st.scenes.size()).hashCode() & 0x7fffffff);
        for (Story.CharacterDef c : st.cast()) {
            Entity e = d.entity(d.characters, "CHAR", c.displayName, c.description);
            e.gender = c.look != null ? (c.look.female ? 1 : -1) : c.genderHint;
            e.animal = c.look != null && (c.look.kind == Look.ANIMAL || c.look.kind == Look.BIRD);
            d.characterFacts(e, c);
        }
        for (Story.PlaceDef th : st.things) d.entity(d.props, "PROP", th.name, th.description);
        for (Story.PlaceDef p : st.places) d.entity(d.locations, "LOC", p.name, p.description);
        for (Entity e : d.props) d.propFacts(e, st);
        for (Entity e : d.locations) d.placeFacts(e);
        int lineN = 0;
        for (Story.Scene sc : st.scenes) {
            Scene s = new Scene();
            s.number = sc.number;
            s.id = String.format(java.util.Locale.ROOT, "SCENE_%02d", sc.number);
            s.heading = sc.heading;
            Entity place = d.placeOf(sc);
            s.placeId = place != null ? place.id : null;
            if (place != null && !place.sceneIds.contains(s.id)) place.sceneIds.add(s.id);
            d.scenes.add(s);
            int envN = 0;
            // the time a scene heading gives ("दृश्य 1 - भीगा हुआ लेखक [शाम 7:15]")
            Matcher ht = m("(सुबह|दोपहर|शाम|रात|morning|afternoon|evening|night)?\\s*([0-9]{1,2}):([0-9]{2})", Txt.digitsToAscii(sc.heading + " " + sc.title));
            if (ht.find()) envN = d.eff(s, envN, "TIME", null, "ENV", ht.group(2) + ":" + ht.group(3) + (ht.group(1) != null ? " (" + dayPart(ht.group(1)) + ")" : ""), EXPLICIT, (sc.heading + " " + sc.title).trim(), BACKDROP);
            String lastActor = null;
            for (Story.Beat b : sc.beats) {
                if (b.type == Story.Beat.DIALOGUE) {
                    Line l = new Line();
                    l.order = ++lineN;
                    l.id = String.format(java.util.Locale.ROOT, "LINE_%03d", lineN);
                    l.sceneId = s.id;
                    l.text = b.text;
                    l.delivery = b.manner == null ? "" : b.manner;
                    l.speakerName = b.speaker != null ? b.speaker.displayName : b.speakerRaw;
                    Entity sp = b.speaker != null ? d.find(d.characters, b.speaker.displayName) : null;
                    l.speakerId = sp != null ? sp.id : null;
                    // v47.8: a voice the reader knows is only heard (a device, an announcer, the narrator) is no missing speaker
                    if (sp == null && (b.narrator || (b.speaker != null && b.speaker.voiceOnly))) d.heardOnly.add(l.id);
                    if (sp != null && !sp.sceneIds.contains(s.id)) sp.sceneIds.add(s.id);
                    d.lines.add(l);
                    s.lineIds.add(l.id);
                    if (sp != null) lastActor = sp.id;
                    // the note with the line is the speaker's own action too ("(दौड़ती हुई आती है, एक हाथ में टूटा जूता …)"),
                    // never spoken; its steps are the speaker's unless it names another
                    if (sp != null && l.delivery.length() > 0)
                        for (String clause : clauses(l.delivery)) { Action a = d.action(st, s, clause, sp.id, true); if (a != null) lastActor = a.actorId; }
                    continue;
                }
                String text = b.text == null ? "" : b.text;
                envN = d.environment(s, text, envN);
                for (String sent : Director.sentences(text)) {
                    for (String clause : clauses(sent)) {
                        Action a = d.action(st, s, clause, lastActor, false);
                        if (a != null) lastActor = a.actorId;
                        else {
                            // a clause with no step still says whom the next is about ("परी का चेहरा … बदल जाता है")
                            List<Story.CharacterDef> ms = ScriptParser.mentions(st, clause);
                            Entity e = ms.isEmpty() ? null : find(d.characters, ms.get(0).displayName);
                            if (e != null) lastActor = e.id;
                        }
                    }
                }
            }
            for (Entity c : d.characters) if (!c.sceneIds.contains(s.id)) for (Action a : d.actions) if (a.sceneId.equals(s.id) && c.id.equals(a.actorId)) { c.sceneIds.add(s.id); break; }
            for (Entity p : d.props) {
                String key = head(p.name);
                for (Story.Beat b : sc.beats) if (key.length() > 1 && Txt.norm((b.text == null ? "" : b.text) + " " + (b.manner == null ? "" : b.manner)).contains(key)) { if (!p.sceneIds.contains(s.id)) p.sceneIds.add(s.id); break; }
            }
        }
        d.narratedHistory(st);
        d.history.add("r1: read from the story (" + d.characters.size() + " characters, " + d.props.size() + " props, " + d.locations.size() + " places, "
                + d.scenes.size() + " scenes, " + d.lines.size() + " lines)");
        return d;
    }

    Entity entity(List<Entity> into, String prefix, String name, String desc) {
        Entity e = new Entity();
        e.kind = prefix;
        e.name = name == null ? "" : name.trim();
        e.description = desc == null ? "" : desc.trim();
        e.id = ids.id(prefix, e.name);
        into.add(e);
        if (e.description.length() > 0) fact(e, "description", e.description, EXPLICIT, e.description, 1, "as the story's list describes it");
        return e;
    }

    Fact fact(Entity e, String field, String value, String evidence, String span, double conf, String why) {
        Fact f = new Fact();
        f.id = String.format(java.util.Locale.ROOT, "FACT_%04d", ++factN);
        f.entityId = e.id; f.field = field; f.value = value; f.evidence = evidence; f.span = span == null ? "" : span.trim();
        f.confidence = conf; f.explanation = why; f.source = "story";
        e.facts.add(f);
        return f;
    }

    static Entity find(List<Entity> list, String name) {
        if (name == null) return null;
        String n = Txt.norm(name).trim();
        for (Entity e : list) if (Txt.norm(e.name).trim().equals(n)) return e;
        return null;
    }

    public Entity byId(String id) {
        for (List<Entity> l : java.util.Arrays.asList(characters, props, locations)) for (Entity e : l) if (e.id.equals(id)) return e;
        return null;
    }

    /** The head noun of a name: its last word ("दादाजी की लकड़ी की छड़ी" is a छड़ी), stemmed. */
    static String head(String name) {
        String[] ws = Txt.norm(Txt.withoutParens(name == null ? "" : name)).trim().split("[\\s,]+");
        return ws.length == 0 ? "" : Director.stemHi(ws[ws.length - 1]);
    }

    private static Matcher m(String re, String s) { return Pattern.compile(re, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(s); }

    void characterFacts(Entity e, Story.CharacterDef c) {
        String d = Txt.digitsToAscii(c.description == null ? "" : c.description);
        Matcher a = m("([0-9]+)\\s*(साल|वर्ष|years?|yrs?)", d);
        if (c.age > 0) fact(e, "age", String.valueOf(c.age), EXPLICIT, a.find() ? a.group() : String.valueOf(c.age), 1, "the age written with the name");
        float ft = Director.heightFeet(d);
        Matcher hm = m("(हाइट|कद|लंबाई|height)[^,।]*", d);
        if (ft > 0) fact(e, "heightFeet", String.format(java.util.Locale.ROOT, "%.2f", ft), EXPLICIT, hm.find() ? hm.group() : "", 1, "the height written in feet and inches");
        Matcher w = m("([0-9]+)\\s*(किलो|kg)", d);
        if (w.find()) fact(e, "weightKg", w.group(1), EXPLICIT, w.group(), 1, "the weight written");
        Matcher p = m("कमर\\s*([0-9]+)\\s*डिग्री[^,।]*", d);
        if (p.find()) fact(e, "posture", "back bent " + p.group(1) + "°", EXPLICIT, p.group(), 1, "the stoop written; never straightened");
        else if (Txt.has(d, "झुकी हुई कमर", "कमर झुकी", "stooped", "hunched")) fact(e, "posture", "stooped", EXPLICIT, "", 0.9, "the stoop written");
        Matcher f = m("(पांव|पैर|feet)[^,।]*(नंगे|nanga|bare)[^,।]*", d);
        if (f.find()) fact(e, "feet", "bare", EXPLICIT, f.group(), 1, "bare feet written");
        if (c.look != null) fact(e, "lookKind", lookKind(c.look.kind), INFERRED, "", 0.8, "read by the look designer from the age, the name and the description");
    }

    static String lookKind(int k) {
        switch (k) { case Look.GIRL: return "girl"; case Look.WOMAN: return "woman"; case Look.MAN: return "man"; case Look.BOY: return "boy"; case Look.OLD_MAN: return "old man"; default: return "other"; }
    }

    static final String[] MATERIAL_WORDS = {"लकड़ी", "लकड़ी", "लोहे", "लोहा", "कागज", "कागज़", "रबर", "कपड़े", "कपड़ा", "प्लास्टिक", "कांच", "एल्यूमीनियम", "ईंट", "मिट्टी",
            "wood", "wooden", "iron", "paper", "rubber", "cloth", "plastic", "glass", "aluminium", "brick"};
    static final String[] CONDITION_WORDS = {"टूटा", "टूटी", "टूटे", "फटा", "फटी", "फटे", "जंग", "उखड़", "उधड़", "फीका", "फीकी", "मैला", "मैली", "घिस", "पुराना", "पुरानी", "धूल",
            "broken", "torn", "rust", "faded", "dirty", "worn", "old", "dusty"};
    static final String[] COLOUR_WORDS = {"लाल", "नीला", "नीली", "नीले", "गुलाबी", "पीला", "पीली", "पीले", "काला", "काली", "काले", "भूरा", "भूरी", "भूरे", "सफेद", "हरा", "हरी", "हरे",
            "red", "blue", "pink", "yellow", "black", "brown", "white", "green"};

    void propFacts(Entity e, Story st) {
        String all = Txt.digitsToAscii(e.name + " । " + e.description);
        Matcher dm = m("([0-9]+(?:\\.[0-9]+)?)\\s*(फुट|इंच|ft|feet|inch|inches)\\s*(लंबी|लंबा|लंबे|ऊंचा|ऊँचा|ऊंची|चौड़ा|चौड़ी|long|tall|high|wide)", all);
        while (dm.find()) fact(e, "dimension", dm.group(1) + " " + dm.group(2) + " " + dm.group(3), EXPLICIT, dm.group(), 1, "a size written");
        Matcher sz = m("([0-9]+)\\s*[x×]\\s*([0-9]+)\\s*(इंच|inch|inches|cm)", all);
        if (sz.find()) fact(e, "size", sz.group(1) + "x" + sz.group(2) + " " + sz.group(3), EXPLICIT, sz.group(), 1, "a size written");
        List<String> mats = Rigidity.found(Txt.norm(all), normAll(MATERIAL_WORDS));
        if (!mats.isEmpty()) fact(e, "material", join(mats), EXPLICIT, "", 1, "the material words written");
        fact(e, "materialClass", Rigidity.materialName(Rigidity.material(e.name)), INFERRED, e.name, 0.7, "read from the head noun of the name by the shared materials list (rules, not a simulation)");
        List<String> cond = Rigidity.found(Txt.norm(all), normAll(CONDITION_WORDS));
        if (!cond.isEmpty()) fact(e, "condition", join(cond), EXPLICIT, "", 1, "the wear and damage written");
        List<String> col = Rigidity.found(Txt.norm(all), normAll(COLOUR_WORDS));
        if (!col.isEmpty()) fact(e, "colour", join(col), EXPLICIT, "", 1, "the colours written");
        // the owner: "परी का गुलाबी स्कूल बैग" — a character named before का/की/के; a relation word ("दादाजी की छड़ी") is the
        // one the dialogue calls so (INFERRED, said why), else unknown
        // "काला छाता (मीरा का)": whose it is, in brackets after the name
        Matcher pm = m("\\(\\s*([^()]+?)\\s+(का|की|के)\\s*\\)", e.name);
        if (pm.find()) {
            Story.CharacterDef c = ScriptParser.resolve(st, pm.group(1).trim());
            Entity ce = c != null ? find(characters, c.displayName) : null;
            if (ce != null) fact(e, "owner", ce.id, EXPLICIT, pm.group(), 1, "the name says whose");
        }
        Matcher om = m("^(.+?)\\s+(का|की|के)\\s", Txt.withoutParens(e.name) + " ");
        if (om.find()) {
            String who = om.group(1).trim();
            Story.CharacterDef c = ScriptParser.resolve(st, who);
            Entity ce = c != null ? find(characters, c.displayName) : null;
            if (ce != null && c.fromScript) fact(e, "owner", ce.id, EXPLICIT, om.group(), 1, "the name says whose");
            else {
                Entity called = calledBy(st, who);
                if (called != null) fact(e, "owner", called.id, INFERRED, om.group(), 0.85, "\"" + who + "\" is what the dialogue calls " + called.name);
                else if (Txt.has(who, "दादा", "नाना", "बाबा", "माँ", "मां", "पापा", "दीदी", "भैया")) fact(e, "owner", "", UNKNOWN, om.group(), 0, "a relation word no line ties to a character");
            }
        }
        // a broken part: "आगे वाला दायां पैर बीच से टूटा हुआ है"
        Matcher bp = m("(आगे|पीछे)\\s*(?:वाला|वाली|का|की)?\\s*(दायां|बायां|दाहिना|बाया|दायाँ|बायाँ)\\s*(पैर|पाया|टांग)[^,।]*?टूट", all);
        if (bp.find()) fact(e, "brokenPart", (bp.group(1).startsWith("आगे") ? "front-" : "back-") + (bp.group(2).startsWith("दा") ? "right" : "left") + " leg", EXPLICIT, bp.group(), 1, "the broken part written");
        // what it is under and holds up: "बेंच के नीचे लगी ईंट … टूटे हुए पैर के नीचे सहारा देने के लिए"
        Matcher un = m("^(.+?)\\s+के\\s+नीचे", e.name);
        if (un.find() && Txt.has(all, "सहारा", "support", "टिका")) {
            Entity held = null;
            String hk = head(un.group(1));
            for (Entity o : props) if (o != e && head(o.name).equals(hk)) held = o;
            if (held != null) fact(e, "supports", held.id + (Txt.has(all, "टूटे हुए पैर", "broken leg") ? " (its broken leg)" : ""), EXPLICIT, un.group() + " … सहारा", 1, "what it holds up, written");
        }
        // footwear: "बायें पैर में पहनी है दायां पैर पूरी तरह नंगा है … घाव"
        Matcher wf = m("(बायें|बाएं|बाएँ|दायें|दाएं|दाएँ|left|right)\\s*(पैर|foot)\\s*(में|on)?\\s*(पहनी|पहना|पहने|worn)", all);
        if (wf.find()) fact(e, "wornOn", (wf.group(1).startsWith("ब") || wf.group(1).equalsIgnoreCase("left") ? "left" : "right") + " foot", EXPLICIT, wf.group(), 1, "the foot it is on, written");
        Matcher bf = m("(दायां|बायां|दायाँ|बायाँ|right|left)\\s*(पैर|foot)[^,।]*?(नंगा|bare)", all);
        if (bf.find()) fact(e, "otherFoot", (bf.group(1).startsWith("बा") || bf.group(1).equalsIgnoreCase("left") ? "left" : "right") + " foot bare", EXPLICIT, bf.group(), 1, "the bare foot written");
        if (Txt.has(all, "घाव", "wound", "cut on")) fact(e, "wound", "a small wound on the bare foot's sole", EXPLICIT, "घाव", 1, "the wound written");
        if (Txt.has(all, "लिखा है", "written", "लिखावट")) {
            // after "लिखा है" ("… अक्षरों में लिखा है बापू के लिए"), or before it when a clause follows ("… अक्षरों में पत्र पेटी
            // लिखा है जो लगभग मिट चुका है")
            Matcher wr = m("लिखा है\\s*([^,।]+)$", all.trim());
            Matcher wb = m("(?:अक्षरों में|शब्दों में|स्याही से)\\s+([^,।]{2,40}?)\\s+लिखा है", all);
            boolean clauseAfter = wr.find() && Txt.has(" " + wr.group(1).trim() + " ", " जो ", " जिस", " और ", " पर ");
            wr.reset();
            if (wr.find() && !clauseAfter) fact(e, "writing", wr.group(1).trim(), EXPLICIT, wr.group(), 1, "the words written on it");
            else if (wb.find()) fact(e, "writing", wb.group(1).trim(), EXPLICIT, wb.group(), 1, "the words written on it");
        }
    }

    /** The one the dialogue calls by this word ("दादाजी, क्या ये वाली चिट्ठी है?" — परी speaks to the only other one there). */
    Entity calledBy(Story st, String word) {
        String w = Txt.norm(word).trim();
        if (w.isEmpty()) return null;
        Entity found = null;
        for (Story.Scene sc : st.scenes) for (Story.Beat b : sc.beats) {
            if (b.type != Story.Beat.DIALOGUE || b.speaker == null || !Txt.norm(b.text).contains(w)) continue;
            List<Story.CharacterDef> others = new ArrayList<Story.CharacterDef>();
            for (Story.Beat o : sc.beats) if (o.type == Story.Beat.DIALOGUE && o.speaker != null && o.speaker != b.speaker && !others.contains(o.speaker)) others.add(o.speaker);
            if (others.size() == 1) { Entity e = find(characters, others.get(0).displayName); if (found == null) found = e; else if (found != e) return null; }
        }
        return found;
    }

    void placeFacts(Entity e) {
        String d = e.description;
        Matcher f = m("^(.+?)\\s+के\\s+(?:ठीक\\s+)?सामने\\s*([0-9]+)?\\s*(कदम|मीटर|steps|m)?", Txt.digitsToAscii(d));
        if (f.find()) {
            Entity t = placeNamed(f.group(1), e);
            if (t != null) fact(e, "inFrontOf", t.id + (f.group(2) != null ? " at " + f.group(2) + " " + f.group(3) : ""), EXPLICIT, f.group(), 1, "where it is, written");
        }
        Matcher b = m("^(.+?)\\s+के\\s+पीछे", d);
        if (b.find()) { Entity t = placeNamed(b.group(1), e); if (t != null) fact(e, "behind", t.id, EXPLICIT, b.group(), 1, "where it is, written"); }
        if (Txt.has(d, "मुख्य लोकेशन", "मुख्य स्थान", "मुख्य जगह", "main location")) fact(e, "main", "true", EXPLICIT, "मुख्य लोकेशन", 1, "the list calls it the main location");
        Matcher h = m("यहीं\\s+([^,।]+?)\\s+(रखी|रखा|रखे|है)", d);
        if (h.find()) {
            String hk = head(h.group(1));
            for (Entity p : props) if (head(p.name).equals(hk)) fact(e, "contains", p.id, EXPLICIT, h.group(), 1, "what is here, written");
        }
    }

    Entity placeNamed(String words, Entity not) {
        String w = Txt.norm(words);
        for (Entity p : locations) {
            if (p == not) continue;
            for (String part : ScriptParser.placeWords(p.name, false)) if (part.length() > 2 && w.contains(part)) return p;
        }
        return null;
    }

    /**
     * The listed place a scene is set in: the longest listed name its setting (else its title) begins with, or null. The
     * same reading the document gives every scene; the Studio shows one card per listed place by it.
     */
    public static Story.PlaceDef placeOfScene(Story st, Story.Scene sc) {
        String set = Txt.norm(sc.setting.length() > 0 ? sc.setting : sc.title);
        Story.PlaceDef best = null; int bl = 0;
        for (Story.PlaceDef p : st.places) { String n = Txt.norm(p.name).trim(); if (n.length() > bl && set.startsWith(n)) { best = p; bl = n.length(); } }
        return best;
    }

    Entity placeOf(Story.Scene sc) {
        String set = Txt.norm(sc.setting.length() > 0 ? sc.setting : sc.title);
        Entity best = null; int bl = 0;
        for (Entity p : locations) { String n = Txt.norm(p.name).trim(); if (n.length() > bl && set.startsWith(n)) { best = p; bl = n.length(); } }
        return best;
    }

    /** A description's sentence in its clauses: at commas and at "और", each clause one step. */
    static List<String> clauses(String sent) {
        List<String> out = new ArrayList<String>();
        for (String c : sent.split(",|\\s+और\\s+|\\s+फिर\\s+|\\s+then\\s+|\\s+and then\\s+")) {
            // (and after each finished verb: "धीरे से खड़ी होती है रघुवीर जी की तरफ दो कदम बढ़ती है" is two steps)
            for (String one : c.split("(?<=\\s(?:है|हैं))\\s+(?=\\S)")) {
                one = one.trim();
                if (one.length() > 1) out.add(one);
            }
        }
        return out;
    }

    /** Verbs a step is read by, most particular first: {verb, words…}. */
    static final String[][] VERBS = {
            {"HEAD_ON_SHOULDER", "कंधे पर सिर", "head on his shoulder", "head on her shoulder"}, {"HAND_ON_HAND", "हाथ पर रख", "हाथ के ऊपर रख", "hand on his hand", "hand on her hand"},
            {"KNEEL", "घुटनों के बल", "kneel"}, {"SIT_BESIDE", "बगल में बैठ", "पास बैठ", "sits beside"}, {"SIT", "बैठ जात", "बैठ गए", "बैठ गई", "बैठे हैं", "बैठा है", "बैठी है", "बैठते", "बैठता", "बैठती", "sits", "sat"},
            {"STAND_UP", "खड़ी होती", "खड़ा होता", "खड़े होते", "उठ खड़", "stands up", "gets up"}, {"RUN", "दौड़", "भाग", "runs", "running"},
            {"STEP_TOWARD", "कदम बढ़", "की तरफ बढ़", "steps toward", "walks toward"}, {"GO_TO", "के पास जाकर", "के पास जा", "goes to", "walks to"},
            {"PLACE", "जमीन पर रख", "नीचे रख", "पर रख देती", "पर रख देता", "puts down", "sets down", "places"}, {"REACH_INTO", "में छोटा हाथ डाल", "में हाथ डाल", "reaches into"},
            {"TAKE_OUT", "निकाल", "takes out", "pulls out"}, {"TAKE_FROM", "के हाथ से", "takes it from", "takes the"}, {"PICK_UP", "उठाती", "उठाता", "उठाते", "उठा लेत", "picks up", "lifts"}, {"CLUTCH", "सीने से लगा", "clutches"},
            {"HOLD", "पकड़", "थाम", "holds", "grips"}, {"REACH", "हाथ आगे बढ़", "हाथ बढ़", "reaches"}, {"GIVE", "बढ़ाती है", "बढ़ाता है", "बढ़ाते", "आगे बढ़ा", "दे देत", "देती है", "देता है", "hands", "gives", "holds out"},
            {"OPEN", "खोलते", "खोलती", "खोलता", "खोल कर", "खोलने", "opens"}, {"DRINK", "चुस्की", "पीते हैं", "पीती है", "पीता है", "पी रहे", "sips", "drinks"},
            {"POUR", "छान", "उड़ेल", "pours", "strains"}, {"ADJUST", "ठीक कर", "adjusts"}, {"WIPE", "पोंछ", "wipes"},
            {"LOOK", "की तरफ देख", "को देख", "देखती", "देखते", "looks at"}, {"STARTLE", "चौंक", "startled"}, {"LOWER_HEAD", "सिर झुका", "सिर नीचे", "झुका", "lowers her head", "lowers his head"}, {"SMILE", "मुस्कान", "मुस्कुरा", "smile"},
            {"CRY", "आंसू", "आँसू", "रो", "sob", "tears"}, {"FREEZE", "रुक जाते", "स्थिर हो", "freeze"}, {"BREATHE", "सांस", "breath"},
            {"ARRIVE", "आती है", "आता है", "आते हैं", "घुसते", "घुसता", "घुसती", "arrives", "comes in", "walks in"}};

    static String dayPart(String w) {
        switch (w) { case "सुबह": return "morning"; case "दोपहर": return "afternoon"; case "शाम": return "evening"; case "रात": return "night"; default: return w; }
    }

    Action action(Story st, Scene s, String clause, String lastActor, boolean speakersNote) {
        String c = Txt.norm(clause);
        String verb = null;
        for (String[] v : VERBS) {
            for (int i = 1; i < v.length; i++) if (c.contains(Txt.norm(v[i]))) { verb = v[0]; break; }
            if (verb != null) break;
        }
        // "सिर झटके से उठता है", "चेहरा आसमान की तरफ उठाते हैं": the head comes up (no thing is picked up)
        if (Txt.has(c, "सिर", "चेहरा", "नज़र", "नजर", "head", "face") && Txt.has(c, "उठ", "raises", "lifts", "looks up")) verb = "RAISE_HEAD";
        // v47.7: an animal of the cast chewing the cud, grazing, barking, its tail or its ears (Director.animalDoings)
        if (animalDoing(c)) {
            List<Story.CharacterDef> named = ScriptParser.mentions(st, clause);
            Entity who = !named.isEmpty() ? find(characters, named.get(0).displayName) : lastActor != null ? byId(lastActor) : null;
            if (who != null && who.animal) verb = "ANIMAL_DOES";
        }
        if (verb == null) return null;
        // "भट्टी के पास बैठा": beside a thing is a sit; beside someone is a sit beside them
        if (verb.equals("SIT_BESIDE") && ScriptParser.mentions(st, clause).size() < 2 && !Txt.has(c, "उसके पास", "उनके पास", "बगल में", "beside him", "beside her")) verb = "SIT";
        // "दूर से दौड़ने की आवाज आती है" is a sound the environment hears, not a step anyone takes
        if (Txt.has(c, "की आवाज", "की आहट", "sound of", "the noise of")) return null;
        // "बड़ी मुस्कान एक सेकंड में गायब हो जाती है": the smile goes
        if (verb.equals("SMILE") && Txt.has(c, "गायब", "उतर जा", "fades", "disappears", "vanishes")) verb = "SMILE_FADES";
        List<Story.CharacterDef> ms = ScriptParser.mentions(st, clause);
        String actor = null;
        if (!ms.isEmpty()) { Entity e = find(characters, ms.get(0).displayName); if (e != null) actor = e.id; }
        if (actor == null && Txt.has(" " + clause, " वह ", " वो ", " उसके ", " उसका ", " she ", " he ")) actor = lastActor;
        if (actor == null && speakersNote) actor = lastActor;
        if (actor == null && (verb.equals("LOWER_HEAD") || verb.equals("RAISE_HEAD") || verb.equals("SMILE") || verb.equals("SMILE_FADES") || verb.equals("CRY") || verb.equals("BREATHE"))) actor = lastActor;
        // the verb's gender names the doer: "धीरे से खड़ी होती है रघुवीर जी की तरफ…" is परी's step, not रघुवीर जी's; "परी के हाथ
        // से चिट्ठी लेते हैं" is रघुवीर जी's; "एक छोटी बच्ची … बैठी है" is परी
        int g = verbGender(c);
        // ("एक छोटी बच्ची …" names her by what she is; "रोती हुई आँखों में … मुस्कान आती है" agrees with the smile, not with
        // whose it is — the one the sentence was about keeps it)
        int named = Txt.has(c, "बच्ची", "लड़की", "लडकी", "औरत", "महिला", "girl", "woman") ? 1 : Txt.has(c, "आदमी", "लड़का", "लडका", "बूढ़ा", "बूढा", "बुजुर्ग", " man", "boy") ? -1 : 0;
        if (named != 0) g = named;
        else if ((ms.isEmpty() || speakersNote) && Txt.has(c, "आँख", "आंख", "मुस्कान", "चेहर", "हाथ", "उंगल", "उँगल", "आंसू", "आँसू", "होंठ", "पलक", "कंधे", "पैर", "बाल", "smile", "eyes", "tears", "fingers")) g = 0;
        Entity cur = actor != null ? byId(actor) : null;
        if (g != 0 && (cur == null || cur.gender == -g)) {
            String agree = null;
            for (Story.CharacterDef m : ms) { Entity e = find(characters, m.displayName); if (e != null && e.gender == g) { agree = e.id; break; } }
            if (agree == null && lastActor != null && byId(lastActor) != null && byId(lastActor).gender == g) agree = lastActor;
            if (agree == null) for (int i = actions.size() - 1; i >= 0 && agree == null; i--) { Action p = actions.get(i); Entity e = byId(p.actorId); if (p.sceneId.equals(s.id) && e != null && e.gender == g) agree = e.id; }
            if (agree == null) { Entity only = null; int n = 0; for (Entity e : characters) if (e.gender == g) { only = e; n++; } if (n == 1) agree = only.id; }
            if (agree != null) actor = agree;
        }
        if (actor == null) return null;
        int n = s.actionIds.size() + 1;
        Action a = new Action();
        a.order = n;
        a.id = String.format(java.util.Locale.ROOT, "ACT_S%d_%03d", s.number, n);
        a.sceneId = s.id; a.actorId = actor; a.verb = verb; a.span = clause.trim();
        a.negated = Txt.has(c, " नहीं ", "not ", "never ") && !Txt.has(c, "बिना");
        a.hypothetical = Txt.has(" " + c + " ", " अगर ", " यदि ", " मानो ", " जैसे कि ", " if ", " would ", " as if ");
        // the target: the listed thing (by its head noun) named first in the clause, or another character named in it —
        // only for steps done to something (kneeling or running has none); the ground's own cover is never a target
        boolean toThing = !Txt.has(verb, "KNEEL", "RUN", "STARTLE", "BREATHE", "SMILE", "CRY", "FREEZE", "LOWER_HEAD", "RAISE_HEAD", "STAND_UP", "HEAD_ON_SHOULDER", "HAND_ON_HAND", "ARRIVE");
        int best = Integer.MAX_VALUE;
        if (toThing) for (Entity p : props) {
            String h = head(p.name);
            if (h.length() < 2 || Rigidity.groundCover(p.name)) continue;
            int at = Director.startOf(c, h);
            if (at >= 0 && at < best) { best = at; a.targetId = p.id; }
        }
        if (a.targetId == null && (toThing || verb.equals("HEAD_ON_SHOULDER") || verb.equals("HAND_ON_HAND") || verb.equals("RUN")))
            for (Story.CharacterDef m : ms) { Entity e = find(characters, m.displayName); if (e != null && !e.id.equals(actor)) { a.targetId = e.id; break; } }
        // the steps of one actor follow one another
        for (int i = actions.size() - 1; i >= 0; i--) { Action p = actions.get(i); if (p.sceneId.equals(s.id) && p.actorId.equals(actor)) { a.after.add(p.id); break; } }
        a.completion = completionOf(a);
        actions.add(a);
        s.actionIds.add(a.id);
        // a negation inside the step: "बिना पलक झपकाए" (must not blink), "बिना आवाज किए" (without a sound)
        Matcher neg = m("बिना\\s+([^,।]+?)(?:\\s+(?:किए|किये|झपकाए|के))", clause);
        while (neg.find()) {
            Action no = new Action();
            no.order = ++n; no.id = String.format(java.util.Locale.ROOT, "ACT_S%d_%03d", s.number, n);
            no.sceneId = s.id; no.actorId = actor; no.verb = "MUST_NOT"; no.negated = true; no.span = neg.group().trim();
            no.completion = "never seen: " + neg.group(1).trim();
            actions.add(no); s.actionIds.add(no.id);
        }
        return a;
    }

    static boolean startsWord(String n, String stem) { return Director.startOf(n, stem) >= 0; }

    /** v47.8: the word is used to call someone ("बेटा," "बेटी!" "son,"): followed at once by a comma, "!" or "?", or ending the line. */
    static boolean addressed(String n, String word) {
        for (int at = n.indexOf(word); at >= 0; at = n.indexOf(word, at + 1)) {
            boolean left = at == 0 || !Character.isLetterOrDigit(n.charAt(at - 1)) && !Txt.isDevanagari(n.charAt(at - 1));
            int e = at + word.length();
            if (!left) continue;
            String rest = n.substring(e).trim(), before = n.substring(0, at).trim();
            if (rest.isEmpty() || rest.charAt(0) == ',' || rest.charAt(0) == '!' || rest.charAt(0) == '?' || rest.charAt(0) == '،') return true;
            // "सब ठीक है, बेटा।": called at the end of the sentence, after a comma
            if (before.endsWith(",") && (rest.charAt(0) == '।' || rest.charAt(0) == '.' || rest.charAt(0) == '"' || rest.charAt(0) == '”')) return true;
        }
        return false;
    }
    private static final Pattern FEM_VERB = Pattern.compile("(?:ती|ई|यी)\\s+(?:है|हैं|थी)(?:\\s|$)|(?:बैठी|खड़ी|लेटी|रुकी|गई|आई)(?:\\s|$)|(?:जाती|देती|लेती|रहती|रखती|बढ़ती|आती|बैठती)(?:\\s|$)");
    private static final Pattern MASC_VERB = Pattern.compile("(?:ते|ता)\\s+(?:है|हैं|थे)(?:\\s|$)|(?:बैठे|खड़े|लेटे|रुके|गए|आए)(?:\\s|$)|(?:जाते|देते|लेते|रहते|रखते|बढ़ाते|आते|बैठते|जाता|देता|लेता)(?:\\s|$)");

    /** The gender a Hindi clause's verb agrees with: +1 feminine ("बैठ जाती है"), -1 masculine ("लेते हैं"), 0 when it does not say. */
    static int verbGender(String clause) {
        String c = Txt.norm(clause) + " ";
        boolean f = FEM_VERB.matcher(c).find(), m = MASC_VERB.matcher(c).find();
        return f == m ? 0 : f ? 1 : -1;
    }

    static String completionOf(Action a) {
        switch (a.verb) {
            case "SIT": case "SIT_BESIDE": return "the body is seated" + (a.verb.equals("SIT_BESIDE") ? " beside the other, on the same seat" : "");
            case "KNEEL": return "the body kneels";
            case "STAND_UP": return "the body stands";
            case "PLACE": return "the thing lies in its place";
            case "TAKE_OUT": return "the thing is in the actor's hand";
            case "TAKE_FROM": return "the thing is in the actor's hand and no longer in the other's";
            case "HOLD": case "CLUTCH": return "the thing is in the actor's hands";
            case "PICK_UP": return "the thing is in the actor's hand, off the ground";
            case "GO_TO": case "STEP_TOWARD": case "RUN": return "the actor has arrived";
            case "HEAD_ON_SHOULDER": return "the head rests on the other's shoulder";
            case "HAND_ON_HAND": return "the hand rests on the other's hand";
            case "SMILE_FADES": return "the smile is gone";
            case "GIVE": return "the thing is held out to the other";
            case "OPEN": return "the thing is open";
            case "DRINK": return "the actor drinks";
            case "POUR": return "the tea (the drink) is poured";
            case "ARRIVE": return "the actor is in the place";
            default: return "performed";
        }
    }

    /** The environment a description asks for, clause by clause: what, caused by what, and how the film carries it. */
    int environment(Scene s, String text, int n) {
        boolean windHere = false;
        for (String sentence : text.split("[।.!?]")) {
            // a sentence's wind moves what its own parts name: "गर्म हवा से धूल उड़ रही है और नीम के सूखे पत्ते जमीन पर सरक रहे हैं"
            boolean windSentence = Txt.has(Txt.norm(sentence), "हवा", "wind", "breeze");
            for (String raw : clauseParts(sentence)) {
                String p = raw.trim(), np = Txt.norm(p);
                if (p.isEmpty()) continue;
                Matcher tm = m("([0-9]{1,2})(?::([0-9]{2}))?\\s*बजे", Txt.digitsToAscii(p));
                if (tm.find()) n = eff(s, n, "TIME", null, "ENV", tm.group(1) + ":" + (tm.group(2) != null ? tm.group(2) : "00") + (Txt.has(p, "दोपहर") ? " (afternoon)" : Txt.has(p, "सुबह") ? " (morning)" : Txt.has(p, "शाम") ? " (evening)" : Txt.has(p, "रात") ? " (night)" : ""), EXPLICIT, p, BACKDROP);
                if (Txt.has(np, "आसमान", "sky") && Txt.has(np, "साफ", "नीला", "clear", "blue")) n = eff(s, n, "SKY", null, "ENV", "clear blue sky", EXPLICIT, p, BACKDROP);
                if (Txt.has(np, "बादल नहीं", "no cloud")) n = eff(s, n, "SKY", null, "ENV", "no clouds (must not appear)", EXPLICIT, p, BACKDROP);
                if (Txt.has(np, "धूप", "sunlight", "sun ") && Txt.has(np, "तेज", "कड़क", "कडक", "strong", "harsh")) n = eff(s, n, "SUN", null, "ENV", "strong sunlight", EXPLICIT, p, BACKDROP);
                boolean dapple = Txt.has(np, "धूप") && Txt.has(np, "छन", "धब्ब", "गोल");
                if (dapple) n = eff(s, n, "DAPPLED_LIGHT", Txt.has(np, "हिल") ? "WIND" : "SUN", propLike("नीम", "पेड"), Txt.has(np, "हिल") ? "sun patches through the leaves, moving" : "sun patches through the leaves", EXPLICIT, p, Txt.has(np, "हिल") ? PROCEDURAL : BACKDROP);
                if (Txt.has(np, "छाया", "shade", "shadow")) n = eff(s, n, "SHADE", "SUN", propLike("नीम", "पेड"), "shade", EXPLICIT, p, BACKDROP);
                if (Txt.has(np, "हवा") && Txt.has(np, "चल रही", "चल रहा", "चलती", "blowing", "blows")) {
                    windHere = true;
                    n = eff(s, n, "WIND", null, "ENV", (Txt.has(np, "हल्की", "हल्का", "light") ? "light " : Txt.has(np, "तेज", "strong") ? "strong " : "") + (Txt.has(np, "गर्म", "hot", "warm") ? "hot " : Txt.has(np, "ठंडी", "cool") ? "cool " : "") + "wind", EXPLICIT, p, PROCEDURAL);
                }
                // what the wind moves: "… जिससे नीम के पत्ते धीरे-धीरे हिल रहे हैं", "हवा से धूल उड़ रही है", "कुर्ता हवा में हल्का हिल रहा है"
                boolean moves = Txt.has(np, "हिल", "उड़", "उड", "सरक", "लहर", "sway", "flutter", "drift", "blow");
                boolean explicitCause = moves && ((Txt.has(np, "जिससे") && windHere) || Txt.has(np, "हवा से", "हवा में", "in the wind", "with the wind"));
                boolean sameSentence = moves && !explicitCause && (windSentence || windHere);
                if (explicitCause || sameSentence) {
                    String ev = explicitCause ? EXPLICIT : INFERRED;
                    int before = effects.size();
                    if (Txt.has(np, "पत्त", "leaves", "leaf") && !dapple) {
                        boolean ground = Txt.has(np, "जमीन", "ground", "सरक");
                        if (Txt.has(np, "सरक", "slide", "skitter")) n = eff(s, n, "LEAVES_SLIDE", "WIND", propLike("पत्त"), "dry leaves slide along the ground", ev, p, UNSUPPORTED);
                        else if (Txt.has(np, "उड़", "उड", "fly", "lift")) n = eff(s, n, "LEAVES_LIFT", "WIND", propLike("पत्त"), (ground ? "leaves on the ground lift" : "leaves fly") + " in the wind", ev, p, PARTICLES);
                        else n = eff(s, n, "LEAF_SWAY", "WIND", propLike("नीम", "पेड"), "leaves sway on the tree", ev, p, PROCEDURAL);
                    }
                    if (Txt.has(np, "धूल", "dust")) n = eff(s, n, "DUST", "WIND", "ENV", "dust lifts in the wind", ev, p, PARTICLES);
                    if (Txt.has(np, "कुर्त", "कपड", "दुपट्ट", "साड़ी", "आस्तीन", "clothes", "kurta")) n = eff(s, n, "CLOTH", "WIND", characterIn(p), "clothes stir in the wind", ev, p, PROCEDURAL);
                    if (!explicitCause) for (int i = before; i < effects.size(); i++) effects.get(i).explanation = "the same sentence has the wind; these words do not say it moves this";
                } else if (Txt.has(np, "धूल के कण", "dust motes", "dust particles")) n = eff(s, n, "DUST", windHere ? "WIND" : null, "ENV", "dust motes in the air", EXPLICIT, p, PARTICLES);
                // everything else the description asks of the world, by the shared table (any story, any language)
                boolean neg = Txt.has(" " + np + " ", " नहीं ", " ना ", " बिना ", " no ", " not ", " without ");
                for (EnvRule r : ENV_RULES) {
                    if (!Txt.has(np, r.words) || (r.not.length > 0 && Txt.has(np, r.not)) || neg) continue;
                    // (v47.7: a dog of the cast barking is its own action, staged with its voice — not a dog far off)
                    if (r.kind.equals("SOUND_DOG") && (animalNamedIn(np) || (animalNamedIn(sentence) && !Txt.has(np, "दूर", "कहीं", "far off", "in the distance")))) continue;
                    String method = r.method;
                    // (the characters' own shadows long on the wet ground: the renderer gives them only a contact shadow)
                    if (r.kind.equals("SHADOW") && (Txt.has(np, "तीनों", "दोनों", "उनकी", "उसकी", "their", "his", "her") || characterNamedIn(p) != null)) method = UNSUPPORTED;
                    n = eff(s, n, r.kind, r.cause, "ENV", r.behaviour(np), EXPLICIT, p, method);
                }
            }
            // the bench that rocks as she sits on it (the whole sentence: the reason follows in its own part)
            String ns = Txt.norm(sentence);
            if (Txt.has(ns, "बेंच", "bench") && Txt.has(ns, "हिलती", "हिलता", "डगमग", "wobble"))
                n = eff(s, n, "BENCH_WOBBLE", Txt.has(ns, "क्योंकि", "because") ? "SITTING_ON_BROKEN_LEG" : "SITTING", propLike("बेंच"), "the bench rocks a little as she sits (its broken leg stands on a brick)", EXPLICIT, sentence, PROCEDURAL);
        }
        return n;
    }

    /**
     * A sentence's clauses: at commas, and at "और" / "and" only where it joins two clauses ("धूल उड़ रही है और … पत्ते सरक
     * रहे हैं"), never inside a phrase ("धब्बे बेंच और दोनों के ऊपर हिल रहे हैं" is one clause).
     */
    static List<String> clauseParts(String sentence) {
        List<String> out = new ArrayList<String>();
        for (String part : sentence.split(",")) {
            Matcher j = Pattern.compile("(?:है|हैं|थी|थे|था|गई|गए|गया)\\s+(?:और|and)\\s+").matcher(part);
            int from = 0;
            while (j.find()) { int cut = j.start() + j.group().indexOf(' '); out.add(part.substring(from, cut)); from = j.end(); }
            out.add(part.substring(from));
        }
        return out;
    }

    /**
     * One kind of thing a description can ask of the world, the words that ask for it (Hindi and English), what it
     * comes from, how the film carries it out, and what in the director's plan shows that it is carried out (a kind
     * of weather, a sound, an effect, an ambience) — shared by every story, nothing of one story in it.
     */
    static final class EnvRule {
        final String kind, cause, method, behaviour, explanation, amb;
        final String[] words, not;
        final int[] weather, sfx, fx;
        EnvRule(String kind, String cause, String method, String behaviour, String explanation, int[] weather, int[] sfx, int[] fx, String amb, String[] words, String... not) {
            this.kind = kind; this.cause = cause; this.method = method; this.behaviour = behaviour; this.explanation = explanation;
            this.weather = weather; this.sfx = sfx; this.fx = fx; this.amb = amb; this.words = words; this.not = not;
        }
        String behaviour(String np) {
            if (kind.equals("RAIN")) return Txt.has(np, "मूसलाधार", "तेज", "जोरदार", "heavy", "downpour", "pouring") ? "heavy rain" : Txt.has(np, "हल्की", "धीमी", "बूंदाबांदी", "बूँदाबाँदी", "light", "drizzl") ? "light rain" : "rain";
            if (kind.equals("SOUND_BIRDS") && Txt.has(np, "तेज", "louder")) return "birds, louder";
            return behaviour;
        }
    }
    static int[] ints(int... v) { return v; }
    static String[] ws(String... v) { return v; }
    static final int[] NONE = new int[0];
    static final EnvRule[] ENV_RULES = {
            new EnvRule("RAIN", null, PARTICLES, "rain", "", ints(Film.W_RAIN, Film.W_STORM), NONE, NONE, null,
                    ws("बारिश", "बरसात", "बूंदाबांदी", "बूँदाबाँदी", "rain", "drizzl", "downpour"), "बारिश का पानी", "बारिश के बाद", "बारिश रुक", "बारिश थम", "बारिश लगभग रुक", "rainbow", "rainwater", "rain stop", "बारिश के छल्ले",
                    "की आवाज", "पट-पट", "टप-टप", "आखिरी बूंद", "sound of"),
            new EnvRule("RAIN_STOPS", "RAIN", PROCEDURAL, "the rain stops", "", NONE, NONE, NONE, null, ws("बारिश रुक", "बारिश लगभग रुक", "बारिश थम", "rain stops", "rain has stopped", "the rain eases")),
            new EnvRule("LIGHTNING", null, PROCEDURAL, "lightning flashes", "a flash over the frame and a bolt in the sky (procedural)", NONE, ints(Film.SFX_THUNDER), ints(Film.FX_LIGHTNING), null,
                    ws("बिजली कड़क", "बिजली चमक", "बिजली गिर", "बिजली से", "lightning"), "बिजली का", "बिजली गई", "बिजली नहीं"),
            new EnvRule("CLOUDS", null, PROCEDURAL, "clouds", "", ints(Film.W_CLOUDS, Film.W_STORM), NONE, NONE, null, ws("बादल", "clouds"), "बादल नहीं", "एक भी बादल", "no cloud", "बादलों के बीच से"),
            new EnvRule("FOG", null, PROCEDURAL, "fog", "", ints(Film.W_FOG), NONE, NONE, null, ws("कोहरा", "कोहरे", "धुंध", "fog", "mist")),
            new EnvRule("SNOW", null, PARTICLES, "snow falling", "", ints(Film.W_SNOW), NONE, NONE, null, ws("बर्फ गिर", "बर्फ़ गिर", "बर्फबारी", "बर्फ़बारी", "snowfall", "snowing", "snow falls")),
            new EnvRule("STEAM", null, PARTICLES, "steam rises", "", NONE, NONE, ints(Film.FX_STEAM), null, ws("भाप", "steam")),
            new EnvRule("SMOKE", null, PARTICLES, "smoke rises", "", NONE, NONE, ints(Film.FX_SMOKE, Film.FX_STEAM), null, ws("धुआं", "धुंआ", "धुआँ", "धुएं", "धुएँ", "smoke")),
            new EnvRule("FIRE", null, PARTICLES, "a fire burns", "", ints(Film.W_FIRE), NONE, NONE, null, ws("आग जल", "आग लग", "जलती आग", "लपटें", "अलाव", "flames", "bonfire", "fire burns")),
            new EnvRule("EMBERS", null, BACKDROP, "coals glowing in the stove", "as the picture of the place or the stove shows it; no glow is animated", NONE, NONE, NONE, null,
                    ws("कोयले दहक", "दहकते कोयले", "अंगारे", "आंच तेज", "embers", "glowing coals")),
            new EnvRule("LIGHT_SOURCE", null, BACKDROP, "a lamp or a bulb lights the place", "the light as the pictures show it; no light source is simulated", NONE, NONE, NONE, null,
                    ws("बल्ब", "स्ट्रीट लाइट", "लालटेन", "लैंप", "ट्यूबलाइट", "bulb", "street light", "streetlight", "lamp", "lantern"), "भिनभिना", "अक्स", "परछाई", "रिफ्लेक्शन"),
            new EnvRule("LIGHT_FLICKER", null, UNSUPPORTED, "the light flickers or sways", "the light of the picture does not flicker or sway (no light simulation)", NONE, NONE, NONE, null,
                    ws("टिमटिमा", "रोशनी हिल", "रोशनी कांप", "flicker", "light sways")),
            new EnvRule("REFLECTION", null, UNSUPPORTED, "a reflection in the water", "no reflections are drawn on water", NONE, NONE, NONE, null,
                    ws("अक्स", "रिफ्लेक्शन", "reflection", "पानी में बल्ब", "पानी में परछाई", "पानी में रिफ्ले")),
            new EnvRule("SHADOW", "LIGHT", BACKDROP, "shadows", "as the place's picture shows them", NONE, NONE, NONE, null, ws("परछाई", "परछाइयां", "परछाइयाँ", "shadows"), "पानी में बल्ब की परछाई"),
            new EnvRule("DRIP", null, UNSUPPORTED, "water drips from the roof", "water dripping from an edge is not drawn (the rain's own drops are)", NONE, NONE, NONE, null,
                    ws("टपक", "पानी की धार", "धार गिर", "धार सीधे", "कोनों से टिप", "dripping", "drips")),
            new EnvRule("PUDDLE_RINGS", "RAIN", PARTICLES, "raindrop rings on the puddles", "the rain's splashes on the ground (procedural); no rings spread on the water", ints(Film.W_RAIN, Film.W_STORM), NONE, NONE, null,
                    ws("छल्ले", "ripples")),
            new EnvRule("WATER_FLOW", null, PROCEDURAL, "water flows", "the water of the place's picture moves (procedural)", ints(Film.W_SEA), NONE, NONE, null,
                    ws("पानी उफन", "पानी बह", "बह रहे", "नाली का पानी", "water flows", "flooding")),
            new EnvRule("WATER_RECEDES", null, UNSUPPORTED, "the water goes down", "the water of a picture does not rise or fall", NONE, NONE, NONE, null, ws("पानी उतर", "water recedes", "water goes down")),
            new EnvRule("FLUTTER", "WIND", UNSUPPORTED, "a thing flutters in the wind", "a prop's picture does not move in the wind (only the characters' clothes and the place's plants do)", NONE, NONE, NONE, null,
                    ws("फड़फड़", "फडफड", "flutter", "flapping")),
            new EnvRule("SMELL", null, NOT_FILMABLE, "a smell", "a smell is neither seen nor heard; the shot can only suggest it (steam, faces)", NONE, NONE, NONE, null,
                    ws("गंध", "खुशबू", "सुगंध", "महक", "smell", "aroma", "scent")),
            new EnvRule("SOUND_RAIN", "RAIN", SOUND, "the patter of the rain", "the rain recording of the sound library, when there is one", NONE, ints(Film.SFX_UMBRELLA_RAIN), NONE, "rain",
                    ws("टप-टप", "पट-पट", "टिप-टिप", "बारिश की आवाज", "बारिश की पट", "rain patter", "sound of rain")),
            new EnvRule("SOUND_BIRDS", null, SOUND, "birds", "", ints(Film.W_BIRDS), ints(Film.SFX_BIRDS), NONE, null, ws("चिड़ि", "चिडि", "पक्षी", "चहचहा", "birds")),
            new EnvRule("SOUND_BELL", null, SOUND, "a bell now and then", "", NONE, ints(Film.SFX_CYCLE_BELL, Film.SFX_BELL), NONE, null, ws("घंटी", "bell")),
            new EnvRule("SOUND_RUSTLE", "WIND", SOUND, "leaves rustle", "", NONE, ints(Film.SFX_RUSTLE, Film.SFX_WIND), NONE, null, ws("सर-सर", "सरसर", "rustle")),
            new EnvRule("SOUND_STEPS", null, SOUND, "footsteps", "", NONE, ints(Film.SFX_STEPS, Film.SFX_STEPS_RUN, Film.SFX_STEPS_HARD, Film.SFX_STEPS_LIMP, Film.SFX_STICK), NONE, null,
                    ws("दौड़ने की आवाज", "चलने की आवाज", "कदमों की आवाज", "छप-छप", "footsteps")),
            new EnvRule("SOUND_HORN", null, SOUND, "horns far off", "", NONE, ints(Film.SFX_HORN, Film.SFX_TRAFFIC), NONE, null, ws("हॉर्न", "horn", "honk")),
            new EnvRule("SOUND_HUM", null, SOUND, "an electric hum", "", NONE, ints(Film.SFX_HUM, Film.SFX_BUZZ), NONE, null, ws("भिनभिना", "hum", "buzz"), "बंद", "stops"),
            new EnvRule("SOUND_THUNDER", "LIGHTNING", SOUND, "thunder", "", NONE, ints(Film.SFX_THUNDER), NONE, null, ws("गरज", "गड़गड़ाहट", "thunder"), "शेर", "बाघ"),
            new EnvRule("SOUND_SIP", null, SOUND, "sips", "", NONE, ints(Film.SFX_SIP), NONE, null, ws("चुस्की", "sip")),
            new EnvRule("SOUND_DOG", null, UNSUPPORTED, "a dog barks far off", "the sound library has no dog's bark for a dog that is not in the story", NONE, NONE, NONE, null, ws("भौंक", "barking", "a dog barks")),
            new EnvRule("SOUND_AZAAN", null, UNSUPPORTED, "the call to prayer from far off", "there is no recording of the azaan in the studio's sounds", NONE, NONE, NONE, null, ws("अजान", "अज़ान", "azaan", "azan", "adhan")),
    };
    static EnvRule rule(String kind) { for (EnvRule r : ENV_RULES) if (r.kind.equals(kind)) return r; return null; }

    /** The character a description names, or null. */
    String characterNamedIn(String text) { for (Entity c : characters) if (Txt.has(text, c.name)) return c.id; return null; }

    /** v47.7: whether the text names an animal of the cast. */
    boolean animalNamedIn(String text) { for (Entity c : characters) if (c.animal && Txt.has(text, c.name)) return true; return false; }

    /** v47.7: whether a clause says an animal's own doing (the director's words for it: Director.ANIMAL_DOINGS). */
    static boolean animalDoing(String c) {
        for (Object[] r : Director.ANIMAL_DOINGS) for (String w : (String[]) r[1]) if (c.contains(Txt.norm(w))) return true;     // (both read the same way)
        return false;
    }

    int eff(Scene s, int n, String kind, String cause, String affected, String behaviour, String evidence, String span, String method) {
        // (said twice in a scene — "छाया" in the opening and again by the bench — it is one requirement)
        for (Effect o : effects) if (o.sceneId.equals(s.id) && o.kind.equals(kind) && o.behaviour.equals(behaviour)) return n;
        Effect e = new Effect();
        e.id = String.format(java.util.Locale.ROOT, "ENV_S%d_%02d", s.number, ++n);
        e.sceneId = s.id; e.kind = kind; e.cause = cause; e.affectedId = affected == null ? "ENV" : affected; e.behaviour = behaviour;
        e.evidence = evidence; e.span = span.trim(); e.method = method;
        effects.add(e); s.effectIds.add(e.id);
        return n;
    }

    String propLike(String... words) {
        for (Entity p : props) if (Txt.has(p.name, words)) return p.id;
        return "ENV";
    }

    String characterIn(String text) {
        for (Entity c : characters) if (Txt.has(text, c.name)) return c.id;
        return characters.isEmpty() ? "ENV" : characters.get(0).id;
    }

    /** What a line says happened before ("मुझे ये … जमीन पर सूखे पत्तों में गिरी हुई मिली थी"): the thing's history, not on screen. */
    void narratedHistory(Story st) {
        for (Line l : lines) {
            String t = Txt.norm(l.text);
            if (!Txt.has(t, "मिली थी", "मिला था", "मिले थे", "found it", "i found")) continue;
            for (Entity p : props) {
                String h = head(p.name);
                // the thing the line is about: named, or "ये" in a line that names it
                if (!(h.length() > 1 && startsWord(t, h)) && !(Txt.has(" " + t + " ", " ये ", " यह ") && Txt.has(t, h))) continue;
                // "आपके डाक-बक्से के पीछे" — named as where, not as what was found
                int at = Director.startOf(t, h);
                if (at >= 0 && Pattern.compile("^\\S*\\s+(?:के|की)\\s+(?:पीछे|पास|नीचे|ऊपर|अंदर|सामने|बगल)").matcher(t.substring(at)).find()) continue;
                StateChange c = new StateChange();
                c.id = String.format(java.util.Locale.ROOT, "STATE_%03d", ++stateN);
                c.entityId = p.id; c.sceneId = l.sceneId; c.onScreen = false; c.evidence = EXPLICIT; c.span = l.text;
                c.state = Txt.has(t, "जमीन", "ground") ? "ON_GROUND" + (Txt.has(t, "पीछे", "behind") ? " behind " + (Txt.has(t, "डाक") ? "the postbox" : "something") : "") : "FOUND";
                c.holderId = l.speakerId;
                states.add(c);
            }
        }
    }

    // ------------------------------------------------------------------ the director's plan, as the renderer will draw it

    /**
     * What the director planned for this story (the Film the renderer draws): the scenes' times, each line's start, the
     * shots, where each listed thing is and who holds it, the environment actually scheduled, and each step's evidence.
     */
    public void attach(Film film) {
        if (film == null) return;
        List<Film.Seg> segs = new ArrayList<Film.Seg>();
        for (Film.Seg sg : film.segs) if (sg.type == Film.S_SCENE) segs.add(sg);
        for (int i = 0; i < scenes.size() && i < segs.size(); i++) { scenes.get(i).t0 = segs.get(i).t0; scenes.get(i).t1 = segs.get(i).t1; }
        for (int i = 0; i < lines.size() && i < film.lines.size(); i++) { lines.get(i).start = film.lines.get(i).start; lines.get(i).dur = film.lines.get(i).dur; }
        shots.clear();
        int[] perScene = new int[scenes.size() + 1];
        for (Film.Shot fs : film.shots) {
            int si = sceneAt(fs.t + 0.01f);
            if (si < 0) continue;
            Shot sh = new Shot();
            sh.sceneId = scenes.get(si).id;
            sh.id = String.format(java.util.Locale.ROOT, "SHOT_S%d_%02d", scenes.get(si).number, ++perScene[si]);
            sh.t = fs.t; sh.dur = fs.dur; sh.size = fs.size; sh.subject = fs.subject; sh.purpose = fs.purpose;
            Film.Seg sg = segs.get(si);
            for (Film.Cam c : sg.cams) if (Math.abs(c.t - fs.t) < 0.05f && c.scripted) sh.scripted = true;
            for (Line l : lines) if (l.start >= 0 && l.start < fs.t + fs.dur && l.start + Math.max(0.1f, l.dur) > fs.t) sh.lineIds.add(l.id);
            shots.add(sh);
        }
        // who holds each listed thing, and where it lies (the renderer draws exactly these)
        List<StateChange> on = new ArrayList<StateChange>();
        for (int si = 0; si < segs.size() && si < scenes.size(); si++) {
            for (Film.Fx f : segs.get(si).fx) {
                if (f.type != Film.FX_HELD || f.what == null) continue;
                Entity p = find(props, f.what);
                if (p == null) continue;
                StateChange c = new StateChange();
                c.entityId = p.id; c.sceneId = scenes.get(si).id; c.onScreen = true; c.time = f.t0; c.evidence = "PLANNED";
                if (f.kind == Film.THING_PLACE) { c.state = "IN_PLACE"; c.holderId = scenes.get(si).placeId; }
                else { Entity h = f.a != null ? find(characters, f.a.c.displayName) : null; c.state = f.kind == Film.THING_TAKE ? "TAKEN_OUT_BY" : f.kind == Film.THING_GIVE ? "HELD_OUT_BY" : "HELD_BY"; c.holderId = h != null ? h.id : null; }
                c.span = String.format(java.util.Locale.US, "%.1f-%s s", f.t0, f.t1 > 1e5f ? "end" : String.format(java.util.Locale.US, "%.1f", f.t1));
                on.add(c);
            }
        }
        java.util.Collections.sort(on, new java.util.Comparator<StateChange>() { public int compare(StateChange a, StateChange b) { return Float.compare(a.time, b.time); } });
        for (StateChange c : on) { c.id = String.format(java.util.Locale.ROOT, "STATE_%03d", ++stateN); states.add(c); }
        // the environment the director scheduled
        for (Effect e : effects) {
            Scene s = sceneById(e.sceneId);
            if (s == null || s.t0 < 0) continue;
            int w = weatherOf(e.kind), sfx = sfxOf(e.kind);
            if (w >= 0) for (Film.Weather fw : film.weather) if (fw.type == w && fw.t0 < s.t1 && fw.t1 > s.t0 && fw.strength > 0) e.scheduled = true;
            if (sfx >= 0) for (Film.Sfx fs : film.sfx) if (fs.type == sfx && fs.t >= s.t0 - 0.5f && fs.t < s.t1) e.scheduled = true;
            EnvRule rr = rule(e.kind);
            if (rr != null && !e.method.equals(UNSUPPORTED) && !e.method.equals(NOT_FILMABLE)) {
                for (int wt : rr.weather) for (Film.Weather fw : film.weather) if (fw.type == wt && fw.t0 < s.t1 && fw.t1 > s.t0 && fw.strength > 0) e.scheduled = true;
                for (int st : rr.sfx) for (Film.Sfx fs : film.sfx) if (fs.type == st && fs.t >= s.t0 - 0.5f && fs.t < s.t1) e.scheduled = true;
                Film.Seg sgx = segOf(segs, s);
                if (sgx != null) for (int ft : rr.fx) for (Film.Fx f : sgx.fx) if (f.type == ft) e.scheduled = true;
                if (rr.amb != null) for (Film.Amb a : film.ambience) if (a.t0 < s.t1 && a.t1 > s.t0 && a.words != null && a.words.contains(rr.amb)) e.scheduled = true;
                // the rain stopping: a rain that ends inside the scene
                if (e.kind.equals("RAIN_STOPS")) for (Film.Weather fw : film.weather) if ((fw.type == Film.W_RAIN || fw.type == Film.W_STORM) && fw.t1 > s.t0 && fw.t1 < s.t1 + 0.5f) e.scheduled = true;
                if (e.scheduled && rr.explanation.length() > 0) e.explanation = rr.explanation;
            }
            if (rr != null && (e.method.equals(UNSUPPORTED) || e.method.equals(NOT_FILMABLE))) e.explanation = rr.explanation;
            if (e.method.equals(BACKDROP)) { e.scheduled = true; if (rr != null && rr.explanation.length() > 0) e.explanation = rr.explanation; }          // the place's picture carries it
            if (e.kind.equals("DAPPLED_LIGHT") && e.method.equals(PROCEDURAL) && e.scheduled) e.explanation = "soft light patches swaying over the frame (screen-space, procedural; not cast by the tree)";
            if (e.kind.equals("BENCH_WOBBLE")) for (Film.Seg sg : segs) for (Film.Fx f : sg.fx) if (f.type == Film.FX_WOBBLE && f.t0 >= s.t0 && f.t0 < s.t1) e.scheduled = true;
            if (e.kind.equals("CLOTH")) e.scheduled = anyWind(film, s);
            // the place picture's foliage bends with the wind outdoors (Nature.backdropMesh): the tree's sway is the wind's
            if (e.kind.equals("LEAF_SWAY")) {
                Film.Seg sg = segOf(segs, s);
                e.scheduled = anyWind(film, s) && sg != null && Sets.outdoorSet(sg.set);
                if (e.scheduled) e.explanation = "the place picture's foliage is bent by the wind (a mesh warp of the picture, procedural)";
            }
            if (e.kind.equals("DUST") && !e.scheduled) for (Film.Weather fw : film.weather) if (fw.type == Film.W_MOTES && fw.t0 < s.t1 && fw.t1 > s.t0 && fw.strength > 0) e.scheduled = true;
        }
        // each step's evidence in the plan (again in validate(), once the pictures are known)
        this.film = film; this.sceneSegs = segs;
        for (Action a : actions) a.status = evidenceOf(film, segs, a);
        history.add("r" + revision + ": the director's plan attached (" + shots.size() + " shots)");
    }

    static Film.Seg segOf(List<Film.Seg> segs, Scene s) { for (Film.Seg sg : segs) if (Math.abs(sg.t0 - s.t0) < 0.01f) return sg; return null; }

    private Film film;
    private List<Film.Seg> sceneSegs;

    static boolean anyWind(Film film, Scene s) { for (Film.Weather w : film.weather) if (w.type == Film.W_WIND && w.t0 < s.t1 && w.t1 > s.t0) return true; return false; }

    static int weatherOf(String kind) {
        if (kind.equals("WIND")) return Film.W_WIND;
        if (kind.equals("DUST")) return Film.W_DUST;
        if (kind.equals("LEAVES_LIFT")) return Film.W_LEAVES;
        if (kind.equals("DAPPLED_LIGHT")) return Film.W_DAPPLE;
        return -1;
    }

    static int sfxOf(String kind) {
        if (kind.equals("WIND")) return Film.SFX_WIND;
        return -1;
    }

    int sceneAt(float t) { for (int i = 0; i < scenes.size(); i++) if (scenes.get(i).t0 >= 0 && t >= scenes.get(i).t0 && t < scenes.get(i).t1) return i; return -1; }

    Scene sceneById(String id) { for (Scene s : scenes) if (s.id.equals(id)) return s; return null; }

    /** PLANNED-AND-SEEN in the plan, NOT_IN_PLAN, or NOT_CHECKABLE (no check exists for this kind of step — said, not guessed). */
    String evidenceOf(Film film, List<Film.Seg> segs, Action a) {
        Scene s = sceneById(a.sceneId);
        Entity actor = byId(a.actorId);
        if (a.hypothetical) return "HYPOTHETICAL";
        if (s == null || s.t0 < 0 || actor == null || a.negated) return "NOT_CHECKABLE";
        Film.Actor fa = null; Film.Seg seg = null;
        for (Film.Seg sg : segs) if (Math.abs(sg.t0 - s.t0) < 0.01f) { seg = sg; for (Film.Actor x : sg.actors) if (Txt.norm(x.c.displayName).trim().equals(Txt.norm(actor.name).trim())) fa = x; }
        if (fa == null) return "NOT_IN_PLAN";
        switch (a.verb) {
            case "SIT": case "SIT_BESIDE": case "KNEEL":
                for (Film.Key k : fa.keys) if (k.body == Pose.SIT || k.body == Pose.KNEEL || k.body == Pose.CROUCH) return "IN_PLAN";
                return "NOT_IN_PLAN";
            case "STAND_UP":
                for (Film.Key k : fa.keys) if (k.body == Pose.STAND && k.t > s.t0 + 0.5f) return "IN_PLAN";
                return "NOT_IN_PLAN";
            case "RUN": case "GO_TO": case "STEP_TOWARD": case "ARRIVE": {
                for (Film.Key k : fa.keys) if (k.moveDur > 0) return "IN_PLAN";
                // (coming in: off the stage at the scene's start, on it later)
                if (a.verb.equals("ARRIVE") && !fa.stateAt(s.t0 + 0.05f).visible) for (Film.Key k : fa.keys) if (k.visible && k.t > s.t0 + 0.3f) return "IN_PLAN";
                return "NOT_IN_PLAN";
            }
            case "TAKE_OUT": case "TAKE_FROM": case "HOLD": case "CLUTCH": case "PICK_UP": case "GIVE": {
                if (a.targetId == null) return "NOT_CHECKABLE";
                Entity p = byId(a.targetId);
                if (p == null || !p.kind.equals("PROP")) return "NOT_CHECKABLE";
                // (no picture of the thing: the director stages a drawn everyday one where it has one, which this does not check)
                if (!MATCHED.equals(p.assetState)) { a.reason = "no picture of " + p.name + " to follow through the plan"; return "NOT_CHECKABLE"; }
                for (Film.Fx f : seg.fx) if (f.type == Film.FX_HELD && f.a == fa && f.what != null && find(props, f.what) == p) return "IN_PLAN";
                return Rigidity.groundCover(p.name) ? "NOT_CHECKABLE" : "NOT_IN_PLAN";
            }
            case "ANIMAL_DOES": {
                // (chewing, grazing, its voice, its tail and ears: the director's own act for the animal, in this scene)
                for (Film.Act x : fa.acts) if (x.type == Film.G_ANIMAL && x.t0 >= seg.t0 - 0.05f && x.t0 <= seg.t1) return "IN_PLAN";
                return "NOT_IN_PLAN";
            }
            case "HEAD_ON_SHOULDER": case "HAND_ON_HAND":
                a.reason = "the renderer has no gesture for one body resting on another; the two sit side by side instead";
                return "UNSUPPORTED";
            case "PLACE": {
                if (a.targetId == null) return "NOT_CHECKABLE";
                Entity p = byId(a.targetId);
                if (p != null && !MATCHED.equals(p.assetState)) { a.reason = "no picture of " + p.name + " to follow through the plan"; return "NOT_CHECKABLE"; }
                if (p != null) for (String n : film.notes) if (n.contains(p.name + ": stays on")) { a.reason = n.trim().replace("↳ ", ""); return "UNSUPPORTED"; }
                for (Film.Fx f : seg.fx) if (f.type == Film.FX_HELD && f.kind == Film.THING_PLACE && f.what != null && find(props, f.what) == p) return "IN_PLAN";
                return "NOT_IN_PLAN";
            }
            default: return "NOT_CHECKABLE";
        }
    }

    // ------------------------------------------------------------------ the pictures

    /** Which of the story's people, things and places have a picture the film will use (the user's, from the manifest). */
    public void assets(Art art) {
        assetsChecked = true;
        for (Entity c : characters) c.assetState = art != null && art.sprites.containsKey(charId(c)) ? MATCHED : MISSING;
        for (Entity p : props) {
            p.assetState = MISSING;
            if (art != null) for (Art.Shot sh : art.shots) if (sh.object && sh.keys != null && sh.keys.length > 0 && Txt.norm(sh.keys[0]).trim().equals(Txt.norm(p.name).trim())) p.assetState = MATCHED;
        }
        for (Entity l : locations) {
            if (l.sceneIds.isEmpty()) { l.assetState = NOT_REQUIRED; continue; }
            boolean all = art != null;
            for (String sid : l.sceneIds) { Scene s = sceneById(sid); if (s == null || art == null || !hasScenePicture(art, s.number)) all = false; }
            l.assetState = all ? MATCHED : MISSING;
        }
    }
    static boolean hasScenePicture(Art art, int number) {
        String n = String.valueOf(number);
        for (String k : art.scenes.keySet()) if (k.equals(n) || (k.startsWith(n) && k.length() > n.length() && !Character.isDigit(k.charAt(n.length())))) return true;
        return false;
    }
    /** Whether the pictures were checked (the Studio's live check leaves them to its own picture cards). */
    private boolean assetsChecked;
    private final Map<String, String> charIds = new HashMap<String, String>();
    /** The story's character id (c0, c1 …) of a document character, set by bind(). */
    String charId(Entity c) { String id = charIds.get(c.id); return id == null ? "" : id; }
    /** Ties the document's characters to the story's own (for the pictures and the plan). */
    public void bind(Story st) { for (Story.CharacterDef c : st.characters) { Entity e = find(characters, c.displayName); if (e != null) charIds.put(e.id, c.id); } }

    // ------------------------------------------------------------------ the technical director, the reviews, the sign-off

    Issue issue(String severity, String stage, String sceneId, String requirement, String evidence, String proposal, String... entityIds) {
        Issue i = new Issue();
        i.id = String.format(java.util.Locale.ROOT, "ISSUE_%03d", ++issueN);
        i.severity = severity; i.stage = stage; i.sceneId = sceneId; i.requirement = requirement; i.evidence = evidence; i.proposal = proposal;
        for (String e : entityIds) if (e != null) i.entityIds.add(e);
        issues.add(i);
        return i;
    }

    /**
     * The checks: every line has its speaker; every step its actor; every explicit environmental requirement is either
     * scheduled or said to be unsupported; every step the plan should show is in it; every person and listed thing has
     * a picture (or is said to be missing); the thing chains never have two holders at once; the ending is the script's.
     * The issues found are added to the register (earlier ones of the technical director are replaced).
     */
    public void validate() {
        if (film != null) for (Action a : actions) { a.reason = ""; a.status = evidenceOf(film, sceneSegs, a); }
        // (the checks below are the reader's and the technical director's: theirs are found anew; a later stage's — the QC of
        // the rendered film — stay until resolved)
        for (int i = issues.size() - 1; i >= 0; i--) if (issues.get(i).stage.equals(READER) || issues.get(i).stage.equals(TECHNICAL_DIRECTOR) || issues.get(i).stage.equals(EDITOR)) issues.remove(i);
        editorReview();
        if (film != null) physicsCheck();
        // v47.8: the user's story board, against the plan — a moment that could not be placed, or one staged the other way round
        if (film != null) for (String f : film.boardChecks)
            issue(WARNING, TECHNICAL_DIRECTOR, null, "the film follows the user's story board", f,
                    "stage that moment as the board shows it, or keep the film's staging and note why").category = "STORY_BOARD";
        for (Line l : lines) if (l.speakerId == null && heardOnly.contains(l.id)) issue(WARNING, READER, l.sceneId, "a voice only heard: \"" + l.speakerName + "\"",
                "\"" + l.text + "\"", "no picture is asked for; list it under पात्र to give it a picture");
        for (Line l : lines) if (l.speakerId == null && !heardOnly.contains(l.id)) issue(BLOCKER, READER, l.sceneId, "every line has a listed speaker", "no character for \"" + l.speakerName + "\"", "list the speaker under पात्र, or fix the name");
        for (Effect e : effects) {
            if (e.method.equals(UNSUPPORTED)) issue(e.evidence.equals(EXPLICIT) ? MAJOR : WARNING, TECHNICAL_DIRECTOR, e.sceneId, e.kind + ": " + e.behaviour, "\"" + e.span + "\"", "not carried out by this renderer; "
                    + (e.kind.equals("DAPPLED_LIGHT") ? "the place's picture keeps its sun patches still" : e.kind.equals("LEAVES_SLIDE") ? "leaves lying on the ground stay still" : e.explanation.length() > 0 ? e.explanation : "no approximation yet"), e.affectedId);
            else if (e.method.equals(NOT_FILMABLE)) continue;
            else if (e.evidence.equals(EXPLICIT) && !e.scheduled && !e.method.equals(BACKDROP)) issue(e.kind.startsWith("SOUND") ? MINOR : MAJOR, TECHNICAL_DIRECTOR, e.sceneId, e.kind + ": " + e.behaviour, "\"" + e.span + "\" — not in the director's plan", "schedule it in the scene", e.affectedId);
        }
        for (Action a : actions) if ("NOT_IN_PLAN".equals(a.status)) issue(MAJOR, TECHNICAL_DIRECTOR, a.sceneId, a.verb + " (" + a.completion + ")", "\"" + a.span + "\"", "stage this step", a.actorId, a.targetId);
        for (Action a : actions) if ("UNSUPPORTED".equals(a.status)) issue(MAJOR, TECHNICAL_DIRECTOR, a.sceneId, a.verb + " (" + a.completion + ")", "\"" + a.span + "\"", "not carried out: " + a.reason, a.actorId, a.targetId);
        for (Effect e : effects) if (e.evidence.equals(INFERRED) && !e.scheduled && !e.method.equals(BACKDROP) && !e.method.equals(UNSUPPORTED) && !e.method.equals(NOT_FILMABLE))
            issue(WARNING, TECHNICAL_DIRECTOR, e.sceneId, e.kind + ": " + e.behaviour, "\"" + e.span + "\" (inferred: " + e.explanation + ")", "schedule it if the story means it", e.affectedId);
        if (assetsChecked) for (Entity l : locations) if (l.assetState.equals(MISSING)) issue(MAJOR, TECHNICAL_DIRECTOR, null, "a picture of " + l.name, "a scene happens here and has no place picture", "upload one", l.id);
        for (Shot sh : shots) if (sh.dur < 0.5f) issue(MINOR, TECHNICAL_DIRECTOR, sh.sceneId, "every shot holds long enough to read", sh.id + " lasts " + String.format(java.util.Locale.ROOT, "%.2f", sh.dur) + " s", "merge it with its neighbour");
        if (assetsChecked) for (Entity c : characters) if (c.assetState.equals(MISSING)) issue(MAJOR, TECHNICAL_DIRECTOR, null, "a picture of " + c.name, "no picture in the manifest", "upload one (front first)", c.id);
        if (assetsChecked) for (Entity p : props) if (p.assetState.equals(MISSING) && !p.sceneIds.isEmpty()) issue(WARNING, TECHNICAL_DIRECTOR, null, "a picture of " + p.name, "no picture in the manifest", "upload one, or the director draws an everyday one", p.id);
        // a thing never has two holders at once
        for (Entity p : props) {
            List<StateChange> held = new ArrayList<StateChange>();
            for (StateChange c : states) if (c.entityId.equals(p.id) && c.onScreen && c.state.endsWith("_BY")) held.add(c);
            for (int i = 0; i + 1 < held.size(); i++) {
                StateChange a = held.get(i), b = held.get(i + 1);
                float aEnd = endOf(a.span);
                if (a.holderId != null && !a.holderId.equals(b.holderId) && aEnd > b.time + 0.05f && a.sceneId.equals(b.sceneId))
                    issue(BLOCKER, TECHNICAL_DIRECTOR, a.sceneId, p.name + " is in one place at a time", a.holderId + " until " + aEnd + " s, " + b.holderId + " from " + b.time + " s", "end the first hold at the hand-over", p.id);
            }
        }
        reviews.remove(TECHNICAL_DIRECTOR);
        Review r = new Review();
        r.role = TECHNICAL_DIRECTOR; r.revision = revision;
        int blockers = 0;
        for (Issue i : issues) if (!i.resolved) { r.issueIds.add(i.id); if (i.severity.equals(BLOCKER)) blockers++; }
        r.status = blockers > 0 ? "BLOCKED" : "VALIDATED";
        r.rationale = blockers + " blocker(s), " + (r.issueIds.size() - blockers) + " other open issue(s)";
        reviews.put(TECHNICAL_DIRECTOR, r);
    }

    /** The film's frame rate: the technical director's checks of the plan step through every frame of it. */
    public static final int FPS = 24;
    /** How many frames of the plan the last physics check looked at. */
    public int framesChecked;

    /**
     * The technical director's physics check of the plan, frame by frame at 24 frames a second (the plan the renderer
     * draws, not the pixels): nobody passes through someone standing or sitting still, or through a thing standing in
     * the place, on the same ground line; nobody sits on nothing (seated, with no seat and not on the floor); nothing is
     * held by two at once (above). Each finding names the scene and the first frame. A plan check: it does not see the
     * rendered pictures (their feet on the ground, a hand round a thing — those need the frames themselves).
     */
    void physicsCheck() {
        framesChecked = 0;
        for (Scene s : scenes) {
            Film.Seg sg = segOf(sceneSegs, s);
            if (sg == null) continue;
            java.util.Set<String> said = new java.util.HashSet<String>();
            for (int f = (int) Math.ceil(s.t0 * FPS); f < s.t1 * FPS; f++) {
                float t = f / (float) FPS;
                framesChecked++;
                for (Film.Actor a : sg.actors) {
                    Film.Key ka = a.stateAt(t);
                    if (!ka.visible || ka.anchor != Film.A_GROUND || a.look == null || !(a.look.isHumanoid() || a.look.isObject())) continue;
                    float ha = Renderer.actorHeight(a.look, a.c, null, sg), xa = Director.xAt(a, t), la = Director.liftAt(a, t);
                    float speed = Math.abs(Director.xAt(a, t + 1f / FPS) - xa) * FPS;
                    // sitting on nothing (v47.7: a thing that is a character settles on the ground where it stands)
                    if (ka.body == Pose.SIT && ka.seat < 0 && !a.look.isObject() && !said.contains("seat" + a.c.displayName)) {
                        said.add("seat" + a.c.displayName);
                        physicsIssue(s, f, "nobody sits on nothing", a.c.displayName + " is seated with no seat drawn under them", "seat them on the place's seat, on the floor, or stand them", a.c.displayName);
                    }
                    // v47.8: a teleport — one frame here, the next far away, inside the scene (two rules' keys at one moment)
                    if (f > Math.ceil(s.t0 * FPS) && !said.contains("jump" + a.c.displayName)) {
                        float tp = t - 1f / FPS;
                        Film.Key kp = a.stateAt(tp);
                        float xp = Director.xAt(a, tp), jump = Math.abs(xa - xp);
                        boolean riding = false;
                        for (Film.Act x : a.acts) if (x.type == Film.G_RIDE && x.t0 < t + 0.5f && x.t1 > tp - 0.5f) riding = true;
                        if (kp.visible && kp.anchor == Film.A_GROUND && jump > 0.12f * ha && !riding && a.trackAt(t) == null && a.trackAt(tp) == null
                                && !Director.offStage(xp, ha) && !Director.offStage(xa, ha) && sg.t0 < tp) {
                            said.add("jump" + a.c.displayName);
                            physicsIssue(s, f, "nobody jumps from place to place", a.c.displayName + String.format(java.util.Locale.ROOT, " jumps %.0f px in one frame (teleport)", jump), "a walk from where they are, as long as a body takes for it", a.c.displayName);
                        }
                    }
                    if (speed < 20) continue;                                                       // standing still
                    for (Film.Actor o : sg.actors) {
                        if (o == a) continue;
                        Film.Key ko = o.stateAt(t);
                        if (!ko.visible || ko.anchor != Film.A_GROUND) continue;
                        if (Math.abs(Director.liftAt(o, t) - la) > 6 || ko.depth != ka.depth) continue;   // on another ground line
                        if (Math.abs(Director.xAt(o, t + 1f / FPS) - Director.xAt(o, t)) * FPS > 20) continue;
                        float half = Renderer.actorHeight(o.look, o.c, null, sg) * (ko.body == Pose.SIT ? 0.3f : 0.12f);
                        if (Math.abs(Director.xAt(o, t) - xa) < half && !said.contains("through" + a.c.displayName + o.c.displayName)) {
                            said.add("through" + a.c.displayName + o.c.displayName);
                            physicsIssue(s, f, "nobody passes through anyone", a.c.displayName + " moves through " + o.c.displayName + (ko.body == Pose.SIT ? " and the seat" : ""), "the walk goes in front of them (the director's path) or stops beside them", a.c.displayName);
                        }
                    }
                    for (Film.Fx fx : sg.fx) {
                        if (!(fx.type == Film.FX_HELD && fx.kind == Film.THING_PLACE) || t < fx.t0 || t > fx.t1 || Math.abs(la - fx.y) > 6) continue;   // (a thing set further back stands behind the walk)
                        float half = Math.max(ha * 0.06f, fx.size * ha * 0.25f);
                        if (Math.abs(fx.x - xa) < half && !said.contains("thing" + a.c.displayName + fx.what)) {
                            said.add("thing" + a.c.displayName + fx.what);
                            physicsIssue(s, f, "nobody passes through a thing", a.c.displayName + " moves through " + fx.what, "the walk goes in front of it or stops beside it", a.c.displayName);
                        }
                    }
                }
            }
        }
    }

    void physicsIssue(Scene s, int frame, String rule, String what, String proposal, String who) {
        Entity e = find(characters, who);
        issue(MAJOR, TECHNICAL_DIRECTOR, s.id, rule, what + String.format(java.util.Locale.ROOT, " — first at frame %d (%.2f s, %d fps)", frame, frame / (float) FPS, FPS), proposal, e != null ? e.id : null).category = "PHYSICS";
    }

    static final Pattern SPAN_OF_TIME = Pattern.compile("([0-9०-९]+|एक|दो|तीन|चार|पांच|पाँच|छह|सात|आठ|नौ|दस|बारह|पंद्रह|पन्द्रह|बीस|पच्चीस|तीस|चालीस|पचास|कई|बहुत|one|two|three|five|ten|fifteen|twenty|thirty|fifty|many)\\s+(साल|सालों|बरस|वर्ष|वर्षों|महीने|years|months)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    static final String[] RELATIONS = {"बेटा", "बेटे", "बेटी", "मुन्ना", "मुन्नी", "पति", "पत्नी", "पोता", "पोती", "son", "daughter", "husband", "wife", "grandson", "granddaughter"};

    /**
     * The editor's reading, offline and the same for every story: what the script leaves open, said and never settled for
     * it — a thing whose arrival is only told, a long time a thing lay unfound, words on a thing that may be only its
     * address, a person who is only named in the lines, a character's name said as a word. Notes (WARNING), never blocks.
     */
    void editorReview() {
        String longest = null;
        for (Line l : lines) { Matcher t = SPAN_OF_TIME.matcher(l.text); if (t.find()) { longest = t.group().trim(); break; } }
        for (StateChange c : states) {
            if (c.onScreen) continue;
            Entity p = byId(c.entityId);
            if (p == null) continue;
            editorNote("NARRATED_ORIGIN", c.sceneId, "how " + p.name + " came to be " + c.state.toLowerCase(java.util.Locale.ROOT).replace('_', ' ') + " is only told, not shown",
                    "\"" + c.span + "\"", "keep it told (the line carries it), or show it in a short shot; the story itself does not say how it got there", p.id);
            if (longest != null) editorNote("TIME_GAP", c.sceneId, "why " + p.name + " lay unfound through \"" + longest + "\"",
                    "the lines give the time (" + longest + ") and where it was found; nothing says why no one found it", "leave it to the audience, or let a line say it", p.id);
        }
        for (Entity p : props) {
            String w = p.fact("writing");
            // (a letter, a note, a card: are these the words on its outside, or all of it?)
            boolean paper = Txt.has(Txt.norm(p.name), "चिट्ठी", "पत्र", "खत", "कागज", "लिफाफ", "कार्ड", "डायरी", "letter", "note", "card", "envelope", "diary");
            if (w != null && w.length() > 0 && paper) editorNote("INSCRIPTION_SCOPE", null, "is \"" + w + "\" what is written on the outside of " + p.name + " or all that can be read of it",
                    "the list gives only these words", "say which; the film shows only these words", p.id);
        }
        java.util.Set<String> said = new java.util.HashSet<String>();
        for (Line l : lines) {
            String t = Txt.norm(l.text);
            for (String r : RELATIONS) {
                if (said.contains(r) || !startsWord(t, Txt.norm(r))) continue;
                // (v47.8: an English word whole — "Warm songs?" named an unseen son)
                if (r.charAt(0) < 128 && !Pattern.compile("\\b" + Pattern.quote(r) + "\\b", Pattern.CASE_INSENSITIVE).matcher(t).find()) continue;
                boolean cast = false;
                for (Entity c : characters) if (Txt.has(c.name + " " + c.description, r)) cast = true;
                if (cast) continue;
                // v47.8: said to someone ("डरो मत बेटा, यह तो बस हवा थी", "हाँ बेटा, नाश्ते के बाद") — a form of address to the
                // one there in the scene (who may not speak in it), not a person who is only talked about (fourteen
                // stories had the child on screen noted as an unseen son)
                if (addressed(t, Txt.norm(r))) continue;
                said.add(r);
                editorNote("OFFSCREEN_PERSON", l.sceneId, "\"" + r + "\" is named only in the lines: all the story establishes about them is what is said",
                        "\"" + l.text + "\" (" + l.speakerName + ")", "no picture is asked for; if they matter to a shot (a photo, a letter in their hand), list them", l.speakerId);
            }
            for (Entity c : characters) {
                if (c.id.equals(l.speakerId)) continue;
                Matcher f = Pattern.compile("(?:सच में|सचमुच|सच्ची|really|truly|a real)\\s+(?:एक\\s+)?" + Pattern.quote(Txt.norm(c.name)) + "(?:\\s|$|[।!?,])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(t);
                if (f.find()) editorNote("FIGURATIVE_NAME", l.sceneId, "\"" + f.group().trim() + "\" says " + c.name + "'s name as a word — wordplay, kept as written",
                        "\"" + l.text + "\"", "keep the line (it is the author's); the voice and the face can play it either way", c.id);
            }
        }
        Review r = new Review();
        r.role = EDITOR; r.revision = revision;
        for (Issue i : issues) if (EDITOR.equals(i.stage) && !i.resolved) r.issueIds.add(i.id);
        r.status = "REVIEWED";
        r.rationale = r.issueIds.size() + " note(s) on what the story leaves open; the dialogue is kept as written";
        reviews.put(EDITOR, r);
    }

    void editorNote(String category, String sceneId, String requirement, String evidence, String proposal, String entityId) {
        for (Issue i : issues) if (EDITOR.equals(i.stage) && category.equals(i.category) && requirement.equals(i.requirement)) return;
        issue(WARNING, EDITOR, sceneId, requirement, evidence, proposal, entityId).category = category;
    }

    /** An issue found by a later stage (the QC of the rendered film, a reviewer): kept across re-validation until resolved. */
    public Issue addIssue(String severity, String stage, String sceneId, String requirement, String evidence, String proposal) {
        return issue(severity, stage, sceneId, requirement, evidence, proposal);
    }

    /** An issue resolved, said how: the document changed, so a new revision — every earlier approval approves nothing any more. */
    public void resolve(String issueId, String how) {
        for (Issue i : issues) if (i.id.equals(issueId) && !i.resolved) { i.resolved = true; i.proposal = i.proposal + " (resolved: " + how + ")"; revise("resolved " + issueId + ": " + how); }
    }

    static float endOf(String span) {
        try { String e = span.substring(span.indexOf('-') + 1).replace(" s", "").trim(); return e.equals("end") ? 1e6f : Float.parseFloat(e); } catch (RuntimeException ex) { return 1e6f; }
    }

    /** A review of this revision by a role (READER, EDITOR, DIRECTOR); the director's is refused over an open BLOCKER. */
    public Review review(String role, String status, String rationale) {
        Review r = new Review();
        r.role = role; r.revision = revision; r.status = status; r.rationale = rationale;
        if (role.equals(DIRECTOR) && "APPROVED".equals(status)) {
            for (Issue i : issues) if (!i.resolved && i.severity.equals(BLOCKER)) { r.status = "REFUSED"; r.rationale = "open blocker " + i.id + ": " + i.requirement; r.issueIds.add(i.id); }
            Review td = reviews.get(TECHNICAL_DIRECTOR);
            if (td == null || td.revision != revision) { r.status = "REFUSED"; r.rationale = "no technical validation of revision " + revision; }
        }
        reviews.put(role, r);
        return r;
    }

    /** A change to the document: a new revision — every review of the old one approves nothing any more. */
    public void revise(String what) {
        revision++;
        history.add("r" + revision + ": " + what);
    }

    /** Ready to render: the technical director validated this revision, the director approved this revision, no BLOCKER open. */
    public boolean approvedForRender() {
        Review td = reviews.get(TECHNICAL_DIRECTOR), dir = reviews.get(DIRECTOR);
        if (td == null || td.revision != revision || !"VALIDATED".equals(td.status)) return false;
        if (dir == null || dir.revision != revision || !"APPROVED".equals(dir.status)) return false;
        for (Issue i : issues) if (!i.resolved && i.severity.equals(BLOCKER)) return false;
        return true;
    }

    /** The film's output, checked: it exists, it is not empty, it is the revision approved. Never COMPLETE on a promise. */
    public RenderRecord verifyRender(java.io.File out, String renderer, float expectedSeconds) {
        RenderRecord r = new RenderRecord();
        r.revision = revision; r.renderer = renderer; r.expectedSeconds = expectedSeconds;
        r.output = out == null ? "" : out.getName();
        r.bytes = out != null && out.isFile() ? out.length() : 0;
        if (out == null || !out.isFile()) { r.status = "FAILED"; r.detail = "no output file"; }
        else if (r.bytes < 1024) { r.status = "FAILED"; r.detail = "the output is empty (" + r.bytes + " bytes)"; }
        else if (!approvedForRender()) { r.status = "UNAPPROVED"; r.detail = r.bytes + " bytes, but revision " + revision + " was not approved for rendering"; }
        else if (r.bytes < 1024) { r.status = "FAILED"; r.detail = "the output is empty (" + r.bytes + " bytes)"; }
        else { r.status = "VERIFIED"; r.detail = r.bytes + " bytes"; }
        render = r;
        return r;
    }

    /**
     * v47.7: what the frame audit of the drawn film found (FrameAudit: every character in every frame as drawn) — each
     * jolt, jump, size or picture pop and second face is a MAJOR issue of the technical director's, category "FRAMES",
     * with its frame; the count of frames read goes into the summary.
     */
    public void frameAudit(FrameAudit.Result r) {
        if (r == null) return;
        framesAudited = r.frames;
        for (String f : r.findings) issue(MAJOR, TECHNICAL_DIRECTOR, null, "every frame as drawn is smooth (no jolt, jump, pop or second face)", f,
                "ease the motion there, keep one picture through the shot, draw one face").category = "FRAMES";
    }
    public int framesAudited;

    /** The film is complete only with a verified render of the approved revision and no open QC blocker. */
    public boolean complete() {
        if (render == null || !"VERIFIED".equals(render.status) || render.revision != revision) return false;
        for (Issue i : issues) if (!i.resolved && i.severity.equals(BLOCKER)) return false;
        return true;
    }

    // ------------------------------------------------------------------ JSON

    public String toJson() {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("schemaVersion", SCHEMA_VERSION);
        root.put("productionId", productionId);
        root.put("revision", revision);
        root.put("title", title);
        root.put("characters", entities(characters));
        root.put("props", entities(props));
        root.put("locations", entities(locations));
        List<Object> sc = new ArrayList<Object>();
        for (Scene s : scenes) { Map<String, Object> o = obj("id", s.id, "number", s.number, "placeId", s.placeId, "t0", s.t0, "t1", s.t1); o.put("lineIds", s.lineIds); o.put("actionIds", s.actionIds); o.put("effectIds", s.effectIds); sc.add(o); }
        root.put("scenes", sc);
        List<Object> ls = new ArrayList<Object>();
        for (Line l : lines) ls.add(obj("id", l.id, "sceneId", l.sceneId, "order", l.order, "speakerId", l.speakerId, "speaker", l.speakerName, "text", l.text, "delivery", l.delivery, "start", l.start));
        root.put("dialogueLines", ls);
        List<Object> as = new ArrayList<Object>();
        for (Action a : actions) { Map<String, Object> o = obj("id", a.id, "sceneId", a.sceneId, "actorId", a.actorId, "verb", a.verb, "targetId", a.targetId, "negated", a.negated, "hypothetical", a.hypothetical, "span", a.span, "completion", a.completion, "status", a.status, "reason", a.reason); o.put("after", a.after); as.add(o); }
        root.put("actionBeats", as);
        List<Object> es = new ArrayList<Object>();
        for (Effect e : effects) es.add(obj("id", e.id, "sceneId", e.sceneId, "kind", e.kind, "cause", e.cause, "affectedId", e.affectedId, "behaviour", e.behaviour, "evidence", e.evidence, "span", e.span, "method", e.method, "scheduled", e.scheduled, "explanation", e.explanation));
        root.put("environmentalRequirements", es);
        List<Object> ss = new ArrayList<Object>();
        for (StateChange c : states) ss.add(obj("id", c.id, "entityId", c.entityId, "sceneId", c.sceneId, "state", c.state, "holderId", c.holderId, "onScreen", c.onScreen, "time", c.time, "evidence", c.evidence, "span", c.span));
        root.put("objectStates", ss);
        List<Object> sh = new ArrayList<Object>();
        for (Shot s : shots) { Map<String, Object> o = obj("id", s.id, "sceneId", s.sceneId, "t", s.t, "dur", s.dur, "size", s.size, "subject", s.subject, "purpose", s.purpose, "scripted", s.scripted); o.put("lineIds", s.lineIds); sh.add(o); }
        root.put("shotPlans", sh);
        List<Object> is = new ArrayList<Object>();
        for (Issue i : issues) { Map<String, Object> o = obj("id", i.id, "severity", i.severity, "stage", i.stage, "category", i.category, "sceneId", i.sceneId, "requirement", i.requirement, "evidence", i.evidence, "proposal", i.proposal, "resolved", i.resolved); o.put("entityIds", i.entityIds); is.add(o); }
        root.put("issueRegister", is);
        List<Object> rs = new ArrayList<Object>();
        for (Review r : reviews.values()) { Map<String, Object> o = obj("role", r.role, "revision", r.revision, "status", r.status, "rationale", r.rationale); o.put("issueIds", r.issueIds); rs.add(o); }
        root.put("agentReviews", rs);
        if (render != null) root.put("renderJob", obj("output", render.output, "revision", render.revision, "renderer", render.renderer, "status", render.status, "bytes", render.bytes, "expectedSeconds", render.expectedSeconds, "detail", render.detail));
        root.put("revisionHistory", history);
        return Json.write(root);
    }

    static List<Object> entities(List<Entity> list) {
        List<Object> out = new ArrayList<Object>();
        for (Entity e : list) {
            Map<String, Object> o = obj("id", e.id, "kind", e.kind, "name", e.name, "assetState", e.assetState);
            o.put("sceneIds", e.sceneIds);
            List<Object> fs = new ArrayList<Object>();
            for (Fact f : e.facts) fs.add(obj("id", f.id, "field", f.field, "value", f.value, "evidence", f.evidence, "span", f.span, "confidence", f.confidence, "explanation", f.explanation));
            o.put("facts", fs);
            out.add(o);
        }
        return out;
    }

    static Map<String, Object> obj(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(String.valueOf(kv[i]), kv[i + 1] instanceof Float ? Double.valueOf((Float) kv[i + 1]) : kv[i + 1]);
        return m;
    }

    static String[] normAll(String[] ws) { String[] o = new String[ws.length]; for (int i = 0; i < ws.length; i++) o[i] = Txt.norm(ws[i]); return o; }

    static String join(List<String> ws) { StringBuilder b = new StringBuilder(); for (String w : ws) { if (b.length() > 0) b.append(", "); b.append(w); } return b.toString(); }

    /** The summary and the open issues, most serious first, for the Studio and the film pages (at most max issues). */
    public String report(int max) {
        StringBuilder b = new StringBuilder(summary());
        List<Issue> open = new ArrayList<Issue>();
        for (String sev : new String[]{BLOCKER, MAJOR, MINOR, WARNING}) for (Issue i : issues) if (!i.resolved && i.severity.equals(sev)) open.add(i);
        for (int k = 0; k < open.size() && k < max; k++) {
            Issue i = open.get(k);
            b.append("\n").append(i.severity).append(i.sceneId != null ? " (" + i.sceneId.replace("SCENE_0", "scene ").replace("SCENE_", "scene ") + ")" : "").append(": ")
                    .append(i.requirement).append(i.proposal != null && i.proposal.length() > 0 ? " — " + i.proposal : "");
        }
        if (open.size() > max) b.append("\n… and ").append(open.size() - max).append(" more in production_document.json");
        int unsupported = 0;
        for (Effect e : effects) if (e.method.equals(UNSUPPORTED)) unsupported++;
        if (unsupported > 0) b.append("\n").append(unsupported).append(" of the script's environment requirements have no method in this renderer (listed above, never claimed)");
        return b.toString();
    }

    /** A short summary for the Studio page and the logs. */
    public String summary() {
        int open = 0, block = 0;
        for (Issue i : issues) if (!i.resolved) { open++; if (i.severity.equals(BLOCKER)) block++; }
        int planned = 0, seen = 0;
        for (Effect e : effects) if (e.evidence.equals(EXPLICIT) && !e.method.equals(NOT_FILMABLE)) { planned++; if (e.scheduled) seen++; }
        int physics = 0, drawn = 0;
        for (Issue i : issues) if (!i.resolved && "PHYSICS".equals(i.category)) physics++; else if (!i.resolved && "FRAMES".equals(i.category)) drawn++;
        return String.format(java.util.Locale.ROOT, "revision %d: %d characters, %d props, %d places, %d scenes, %d lines, %d steps, %d of %d environment requirements in the plan, %d shots, %d frames checked at %d fps (%d physics findings)%s, %d open issues (%d blockers)%s",
                revision, characters.size(), props.size(), locations.size(), scenes.size(), lines.size(), actions.size(), seen, planned, shots.size(), framesChecked, FPS, physics,
                framesAudited > 0 ? String.format(java.util.Locale.ROOT, ", %d frames audited as drawn (%d findings)", framesAudited, drawn) : "", open, block, approvedForRender() ? ", approved for rendering" : "");
    }
}

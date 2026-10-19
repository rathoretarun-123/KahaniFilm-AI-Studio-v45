package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * v47.13 (the user: "narrator voices and include them in apk library with proper metadata"): the studio's narrator
 * voices, read from {@code assets/voices/narrators.json} — each with its name (Hindi and English), language, gender,
 * age, tone, pace, what it suits and a sample line, and how it is spoken: offline by the phone's own speech engine at
 * a pitch and a pace, online by a named neural voice. A short recorded sample of each is bundled to hear it; the film's speech is made with the film.
 */
public final class NarratorVoices {
    private NarratorVoices() {}

    public static final String ASSET = "voices/narrators.json";

    /** One narrator voice and its metadata. */
    public static final class Voice {
        public String id = "", name = "", nameEn = "", language = "", gender = "", age = "", tone = "", pace = "", bestFor = "", sample = "";
        /** The asset of its recorded sample (voices/samples/…), "" when none, and what it was made with. */
        public String recording = "", recordingMadeWith = "";
        /** The phone's speech engine: pitch and speech rate (1 = the engine's own). */
        public float phonePitch = 1f, phoneRate = 1f;
        /** The online neural voice and its pitch (Hz) and rate (%) changes. */
        public String naturalVoice = "";
        public int naturalPitchHz, naturalRatePct;
        /** The studio's default narrator for its language. */
        public boolean isDefault;

        public boolean hindi() { return "hi".equals(language); }
        public EdgeVoice.Cast natural() { return new EdgeVoice.Cast(naturalVoice, naturalPitchHz, naturalRatePct); }
        /** "Hindi · female · elderly · warm, gentle, affectionate · slow — bedtime stories, …". */
        public String describe() {
            return (hindi() ? "Hindi" : "English") + " · " + gender + " · " + age + " · " + tone + " · " + pace + " — " + bestFor;
        }
    }

    /** The voices of a narrators.json text (an empty list when it cannot be read). */
    public static List<Voice> parse(String json) {
        List<Voice> out = new ArrayList<Voice>();
        if (json == null) return out;
        Object root;
        try { root = Json.parseLoose(json); } catch (RuntimeException e) { return out; }
        List<Object> vs = Json.arr(root, "voices");
        if (vs == null) return out;
        for (Object o : vs) {
            Voice v = new Voice();
            v.id = Json.str(o, "id", ""); v.name = Json.str(o, "name", ""); v.nameEn = Json.str(o, "name_en", "");
            v.language = Json.str(o, "language", ""); v.gender = Json.str(o, "gender", ""); v.age = Json.str(o, "age", "");
            v.tone = Json.str(o, "tone", ""); v.pace = Json.str(o, "pace", ""); v.bestFor = Json.str(o, "best_for", "");
            v.sample = Json.str(o, "sample", "");
            v.recording = Json.str(o, "recording", ""); v.recordingMadeWith = Json.str(o, "recording_made_with", "");
            Map<String, Object> ph = Json.obj(o, "phone"), nat = Json.obj(o, "natural");
            if (ph != null) { v.phonePitch = (float) Json.num(ph, "pitch", 1); v.phoneRate = (float) Json.num(ph, "rate", 1); }
            if (nat != null) {
                v.naturalVoice = Json.str(nat, "voice", "");
                v.naturalPitchHz = (int) Math.round(Json.num(nat, "pitch_hz", 0));
                v.naturalRatePct = (int) Math.round(Json.num(nat, "rate_pct", 0));
            }
            v.isDefault = Json.bool(o, "default", false);
            if (v.id.length() > 0) out.add(v);
        }
        return out;
    }

    public static Voice byId(List<Voice> all, String id) {
        if (all == null || id == null) return null;
        for (Voice v : all) if (v.id.equals(id)) return v;
        return null;
    }

    /** The voices of the story's language first, then the others. */
    public static List<Voice> forStory(List<Voice> all, boolean hindi) {
        List<Voice> out = new ArrayList<Voice>();
        for (Voice v : all) if (v.hindi() == hindi) out.add(v);
        for (Voice v : all) if (v.hindi() != hindi) out.add(v);
        return out;
    }
}

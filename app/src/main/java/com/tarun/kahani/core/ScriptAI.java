package com.tarun.kahani.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The "script reader" with a language model: turns any natural-language story (prose, Hindi or English) into the
 * screenplay layout the director understands, keeping every detail of characters, dresses, places, emotions and
 * sounds. The answer is checked with the normal parser; if it is not usable the original text is kept.
 */
public final class ScriptAI {
    private ScriptAI() {}

    static final String FORMAT_HI =
            "शीर्षक: <फ़िल्म का नाम>\n\n"
            + "पात्र\n<पूरा नाम> (<उम्र>) (<चेहरा, शरीर, बाल, कपड़े, आवाज़ और पहचान के विवरण>)\n\n"
            + "वस्तुएँ\n<वस्तु का नाम> (<दिखावट और कहानी में उसका उपयोग>)\n\n"
            + "स्थान और पृष्ठभूमि\n<जगह का नाम> (<दिखावट और पहचान के विवरण>)\n\n"
            + "कहानी\n\nदृश्य 1\n(<कौन कहाँ है, क्या करता है, दृश्य में क्या बदलता है, रोशनी/मौसम/ध्वनि और कैमरा>)\n"
            + "<पात्र का नाम>: \"<ठीक वही संवाद>\" (<संवाद बोलने का भाव, आवाज़, विराम और साथ की हरकत>)\n\n"
            + "दृश्य 2\n(<दृश्य का विवरण>)\n<पात्र का नाम>: \"<संवाद>\" (<बोलने का ढंग>)\n\nसमाप्त\n";

    static final String FORMAT_EN =
            "Title: <film title>\n\nCharacters\n<Full name> (<age>) (<face, body, hair, clothes, voice and identity details>)\n\n"
            + "Objects\n<Object name> (<appearance and story use>)\n\n"
            + "Places and Backgrounds\n<Place name> (<appearance and identifying details>)\n\n"
            + "Story\n\nScene 1\n(Who is where, what they do, what changes, light/weather/sound and camera)\n"
            + "<Character name>: \"<exact dialogue>\" (<delivery: emotion, voice, pause and accompanying action>)\n\n"
            + "Scene 2\n(Scene description)\n<Character name>: \"<dialogue>\" (<delivery>)\n\nEND\n";

    public static String system(boolean hindi) {
        return "You are an expert screenwriter and film director for family animated films, "
                + "working like a careful script supervisor. You read a story written in natural language and rewrite it "
                + "as a screenplay WITHOUT losing any detail: every character (with age, face, hair, dress colours, "
                + "jewellery, items they carry), every place (with time of day, weather, light and natural sounds), every "
                + "action, emotion, nuance and sound effect. Keep the author's own words for dialogue wherever possible. "
                + "Use a rich, precise visual-action vocabulary: anticipation, reach, grasp, contact, release, transfer of weight, "
                + "gaze shift, blink, breath, hesitation, recoil, settle, follow-through, overlap, foot plant, balance correction, "
                + "turn-taking, eyeline, screen direction, foreground/midground/background, establishing view, insert, reaction close-up, "
                + "over-the-shoulder, point-of-view, push-in, pull-back, pan, tilt, tracking, rack focus, motivated cut, sound bridge, room tone. "
                + "For daily life, identify the complete readable task and its tools/props (prepare, serve, eat, drink, wash, dress, "
                + "write, read, open, close, carry, put down, sit, stand, walk, wait, greet, clean, cook, travel), but never add an action "
                + "that changes story facts. Preserve each character's identity, age, face, hair, wardrobe, accessories, handedness when known, "
                + "and spatial location. Mark explicit facts separately from safe visual inference and unresolved uncertainty. "
                + "If the story is told in prose, turn reported speech into dialogue lines of the right characters and put actions into stage directions. "
                + "Use a narrator (" + (hindi ? "कथावाचक" : "Narrator")
                + ") ONLY if the story itself asks for a narrator. Description paragraphs must never become dialogue. "
                + "Use this exact order: title, characters with age and descriptions in parentheses, story objects with descriptions in parentheses, places/backgrounds with descriptions in parentheses, Story/कहानी, numbered scenes, END/समाप्त. Put scene/action descriptions in parentheses. Put only spoken words inside double quotation marks; place delivery notes in a separate pair of parentheses after the closing quote. Do not add markdown bullets, asterisks, # headings or decorative symbols. Do not invent assets or actions. Text after a dialogue quote is performance direction, never part of the spoken line. Preserve every authored line and the ending. "
                + "Keep it child-friendly and fully clothed. Write in " + (hindi ? "Hindi (Devanagari)" : "English")
                + ". Output ONLY the screenplay text in exactly this layout, nothing else:\n\n" + (hindi ? FORMAT_HI : FORMAT_EN);
    }

    /** True when the local parser could not find a proper screenplay (prose story). */
    public static boolean needsRewrite(Story s) {
        if (s.dialogueCount() < 3) return true;
        int described = 0;
        for (Story.CharacterDef c : s.characters) if (c.fromScript && c.description.length() > 20) described++;
        return described == 0;
    }

    public static final class Result {
        public String script;     // the screenplay to use
        public boolean rewritten; // true when the language model's version is used
        public String note = "";
    }

    /** Something that answers a prompt (the online language model; a stand-in in tests). */
    public interface Asker { String ask(String system, String prompt) throws IOException; }

    /** Reads the story; returns the AI screenplay when it is good, else the original. Never throws. */
    public static Result read(final Cloud cloud, String text) {
        return read(new Asker() {
            public String ask(String system, String prompt) throws IOException { return cloud.ask(system, prompt, false); }
        }, text);
    }

    /** Largest piece of a story sent in one go, so the answer is never cut off. */
    static final int CHUNK = 6500;

    public static Result read(Asker ai, String text) {
        Result r = new Result();
        r.script = text;
        Story before = ScriptParser.parse(text);
        // a story already written as a screenplay needs no rewriting: the director reads it as it is
        if (!needsRewrite(before)) {
            r.note = "Your story is already written as a screenplay, so the director reads it directly — nothing is lost and no AI "
                    + "rewrite is needed: " + before.characters.size() + " characters, " + before.scenes.size() + " scenes, "
                    + before.dialogueCount() + " lines of dialogue.";
            return r;
        }
        boolean hinglish = Hinglish.isHinglish(text);
        boolean hindi = Txt.mostlyHindi(text) || hinglish;
        try {
            String sys = system(hindi) + (hinglish ? "\n\nThe story is written in Hinglish (Hindi in English letters). Write the screenplay in "
                    + "Hindi using Devanagari script so the voices pronounce it correctly; keep English words the characters "
                    + "actually say (like 'sorry', 'thank you') in English letters." : "");
            // long stories go in parts (each part continues the scenes of the one before), so nothing is cut off
            List<String> parts = pieces(text, CHUNK);
            StringBuilder all = new StringBuilder();
            List<String> names = new ArrayList<String>();
            int scenes = 0;
            for (int i = 0; i < parts.size(); i++) {
                String prompt;
                if (i == 0) prompt = (hindi ? "कहानी:\n\n" : "Story:\n\n") + parts.get(i)
                        + (parts.size() > 1 ? "\n\n(This is part 1 of " + parts.size() + " of the story. Write the character and place sections for the "
                        + "WHOLE story you can see, then the scenes of this part only.)" : "");
                else {
                    StringBuilder nm = new StringBuilder();
                    for (String n : names) nm.append(nm.length() > 0 ? ", " : "").append(n);
                    prompt = "Continue the same screenplay with part " + (i + 1) + " of " + parts.size() + " of the story. Write ONLY scenes, "
                            + "starting with " + (hindi ? "दृश्य " : "Scene ") + (scenes + 1) + ". Use exactly these character names: " + nm
                            + ". No character or place sections.\n\n" + (hindi ? "कहानी (आगे):\n\n" : "Story (continued):\n\n") + parts.get(i);
                }
                String answer = clean(ai.ask(sys, prompt));
                if (i == 0) {
                    Story first = ScriptParser.parse(answer);
                    for (Story.CharacterDef c : first.characters) names.add(c.displayName);
                }
                all.append(answer).append("\n\n");
                Story sofar = ScriptParser.parse(all.toString());
                scenes = sofar.scenes.size();
            }
            String answer = all.toString().trim();
            Story after = ScriptParser.parse(answer);
            int need = Math.max(1, before.dialogueCount() * 6 / 10);
            if (after.dialogueCount() >= need && after.characters.size() >= 1 && after.scenes.size() >= 1) {
                r.script = answer;
                r.rewritten = true;
                r.note = "AI read the story: " + after.characters.size() + " characters, " + after.scenes.size() + " scenes, "
                        + after.dialogueCount() + " lines.";
            } else {
                r.note = "The AI's screenplay had " + after.dialogueCount() + " lines of dialogue and " + after.scenes.size()
                        + " scenes, fewer than your story (" + before.dialogueCount() + " lines), so your original story is used — "
                        + "the director reads it directly. Try again later, or add a Gemini key in Settings for a stronger AI.";
            }
        } catch (IOException e) {
            r.note = "Could not reach the AI (check internet / key) — the built-in reader will be used. " + shortErr(e);
        } catch (RuntimeException e) {
            r.note = "Could not read the AI answer — your original story will be used.";
        }
        return r;
    }

    /** The story in pieces of at most max characters, cut between paragraphs (or scenes). */
    static List<String> pieces(String text, int max) {
        List<String> out = new ArrayList<String>();
        if (text.length() <= max) { out.add(text); return out; }
        String[] paras = text.split("\n\\s*\n|(?=\n\\s*(दृश्य|Scene|SCENE|सीन)\\s*[0-9])");
        StringBuilder cur = new StringBuilder();
        for (String p : paras) {
            if (cur.length() > 0 && cur.length() + p.length() > max) { out.add(cur.toString().trim()); cur.setLength(0); }
            while (p.length() > max) {           // a single huge paragraph: cut at a sentence end
                int cut = Math.max(p.lastIndexOf('।', max), Math.max(p.lastIndexOf(". ", max), p.lastIndexOf('\n', max)));
                if (cut < max / 2) cut = max;
                out.add(p.substring(0, cut + 1).trim());
                p = p.substring(cut + 1);
            }
            cur.append(p).append("\n\n");
        }
        if (cur.toString().trim().length() > 0) out.add(cur.toString().trim());
        return out;
    }

    /** Removes the decoration language models add: code fences, **bold**, # headings, bullet marks before lines. */
    static String clean(String s) {
        String t = stripFences(s);
        StringBuilder b = new StringBuilder();
        for (String line : t.split("\n")) {
            String l = line.replace("**", "").replace("__", "");
            l = l.replaceFirst("^\\s*#{1,6}\\s*", "");
            if (l.trim().startsWith("- ") && l.contains(":")) l = l.trim().substring(2);
            b.append(l).append('\n');
        }
        return b.toString().trim();
    }

    static String shortErr(Exception e) {
        String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return m.length() > 120 ? m.substring(0, 120) : m;
    }

    static String stripFences(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            t = nl > 0 ? t.substring(nl + 1) : t;
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t.trim();
    }

    // ------------------------------------------------------------------ post-preview commands

    /** Turns a free-form editing request into Edits commands with the language model. */
    public static List<Map<String, Object>> commands(Cloud cloud, String request, List<String> names) throws IOException {
        String sys = "You convert a user's request about editing a finished cartoon video into JSON commands. "
                + "Reply ONLY with {\"commands\":[...]} . Each command is an object with \"op\" and optional \"who\", \"factor\", \"value\", \"on\". "
                + "Allowed ops: brightness (factor 1.2 = brighter, value -0.8..0.8), contrast (factor), saturation (factor), "
                + "warmth (factor 1.2 = warmer), music (factor = volume multiplier), sfx (factor), ambience (factor), voices (factor), "
                + "narrator (factor), voice_gain (who, factor), voice_pitch (who, factor 1.1 = higher), voice_rate (who, factor 1.1 = faster), voice_style (who, style one of raspy|trembling|booming|robotic|whisper|nasal|ghostly|magical|growl, on true/false), "
                + "speed (factor), subtitles (on true/false), file_size (factor <1 smaller, >1 larger), resolution (value 480/720/1080), "
                + "aspect (value \"16:9\"|\"9:16\"|\"1:1\"), reset. Character names: " + names + ". "
                + "Use factor 1.3 for 'increase', 0.7 for 'decrease', 1.6/0.4 for 'a lot'. If nothing matches reply {\"commands\":[]}.";
        Object o = Cloud.jsonIn(cloud.ask(sys, request, true));
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        List<Object> cs = o instanceof List ? castList(o) : Json.arr(o, "commands");
        if (cs != null) for (Object c : cs) if (c instanceof Map) out.add(castMap(c));
        return out;
    }

    @SuppressWarnings("unchecked")
    static List<Object> castList(Object o) { return (List<Object>) o; }

    @SuppressWarnings("unchecked")
    static Map<String, Object> castMap(Object o) { return (Map<String, Object>) o; }

    // ------------------------------------------------------------------ picture recognition

    /**
     * Which character or place does this uploaded picture show? Uses the file name first (works offline),
     * then Gemini vision when a key is set. Returns one of the candidates, or null.
     */
    public static String identify(Cloud cloud, String fileName, byte[] jpeg, List<String> candidates) {
        String byName = matchName(fileName, candidates);
        if (byName != null) return byName;
        if (cloud == null || !cloud.hasAiKey() || jpeg == null) return null;
        List<byte[]> ims = new ArrayList<byte[]>();
        ims.add(jpeg);
        String sys = "You match pictures to the cast and places of a children's film.";
        String q = "Which ONE of these does the picture show? Answer with the exact name only, or NONE.\n" + candidates;
        if (cloud.hasGemini()) {
            try { return matchName(cloud.gemini(sys, q, false, ims, null), candidates); } catch (IOException e) { /* v47.4: ChatGPT at once */ }
        }
        if (cloud.hasOpenAI()) {
            try { return matchName(cloud.openai(sys, q, false, ims), candidates); } catch (IOException e) { return null; }
        }
        return null;
    }

    /** What the AI saw in a picture. */
    public static final class Seen {
        public String match;          // candidate name, or null
        public String type = "";      // character | place | title | end | other
        public boolean realPhoto;
        public String caption = "";
    }

    /**
     * Shows the picture to the vision model together with the story's characters and places (name + description)
     * and asks which one it shows, whether it is a real photo, and a short caption for the library.
     */
    public static Seen look(Cloud cloud, byte[] jpeg, List<String[]> candidates) throws IOException {
        StringBuilder b = new StringBuilder();
        b.append("This picture was uploaded for a children's animated film. Here are the film's characters and places:\n");
        for (String[] c : candidates) {
            String d = c[1] == null ? "" : Bible.oneLine(c[1]);
            if (d.length() > 260) d = d.substring(0, 260);
            b.append("- ").append(c[0]).append(": ").append(d).append("\n");
        }
        b.append("\nWhich ONE of these does the picture show? Use the descriptions (age, clothes, colours, animal, place features). ")
                .append("Reply only with JSON: {\"match\":\"<exact name from the list or NONE>\",\"type\":\"character|place|title|end|other\",")
                .append("\"real_photo\":<true if it is a real camera photograph of a real person/place, false for drawings, cartoons, 3D renders or AI art>,")
                .append("\"caption\":\"<short English description: who/what, clothes, colours, setting>\"}");
        Object o = Cloud.jsonIn(cloud.vision(b.toString(), jpeg));
        Seen s = new Seen();
        if (o == null) return s;
        String m = Json.str(o, "match", "NONE");
        for (String[] c : candidates) if (c[0].equals(m)) s.match = c[0];
        if (s.match == null && !m.equalsIgnoreCase("NONE")) {
            List<String> names = new ArrayList<String>();
            for (String[] c : candidates) names.add(c[0]);
            s.match = matchName(m, names);
        }
        s.type = Json.str(o, "type", "");
        s.realPhoto = Json.bool(o, "real_photo", false);
        s.caption = Json.str(o, "caption", "");
        return s;
    }

    /** What the AI heard in a sound: kind (amb, sfx, music, voices) and words in English and Hindi. */
    public static final class Heard {
        public String kind = "";
        public String words = "";
    }

    /** Lets the AI listen to (at most 20 seconds of) a sound and say what it is. */
    public static Heard listen(Cloud cloud, float[] pcm, int sr) throws IOException {
        int n = Math.min(pcm.length, sr * 20);
        float[] x = Mixer.resampleTo(java.util.Arrays.copyOf(pcm, n), sr, 16000);
        String prompt = "Listen to this sound recording for a children's animated film. Reply only with JSON: "
                + "{\"kind\":\"background|effect|music|voices\",\"words\":[\"up to 6 short English words for what is heard\", \"then the same words in Hindi (Devanagari)\"]}. "
                + "background = continuous sound that can loop under a scene (rain, river, birds, wind, traffic); effect = a single event (door, thunder, horse, bell); "
                + "voices = people talking or a crowd in the background; music = music or singing.";
        Object o = Cloud.jsonIn(cloud.listen(prompt, Wav.encode16(x, 16000)));
        Heard h = new Heard();
        if (o == null) return h;
        String k = Json.str(o, "kind", "");
        h.kind = k.startsWith("effect") ? "sfx" : k.startsWith("music") ? "music" : k.startsWith("voice") ? "voices" : k.startsWith("back") ? "amb" : "";
        List<Object> ws = Json.arr(o, "words");
        StringBuilder b = new StringBuilder();
        if (ws != null) for (Object w : ws) if (w instanceof String && ((String) w).trim().length() > 0) { if (b.length() > 0) b.append(", "); b.append(((String) w).trim()); }
        h.words = b.toString();
        return h;
    }

    /**
     * Independent story reader: infers missing but necessary visual beats, entities, continuity risks and
     * questions for the Director/Technical Director. It proposes; it never silently rewrites the author's story.
     * Output is structured JSON for an approval/production report, not executable scene data.
     */
    public static String productionReview(Cloud cloud, Story st) throws IOException {
        StringBuilder script = new StringBuilder();
        script.append("TITLE: ").append(Bible.oneLine(st.title)).append("\\nCAST:\\n");
        for (Story.CharacterDef ch : st.characters) script.append("- ").append(Bible.oneLine(ch.displayName + " " + ch.description)).append("\\n");
        script.append("PLACES:\\n");
        for (Story.PlaceDef pl : st.places) script.append("- ").append(Bible.oneLine(pl.name + " " + pl.description)).append("\\n");
        script.append("PROPS/THINGS:\\n");
        for (Story.PlaceDef it : st.things) script.append("- ").append(Bible.oneLine(it.name + " " + it.description)).append("\\n");
        for (int si = 0; si < st.scenes.size(); si++) {
            Story.Scene sc = st.scenes.get(si);
            script.append("SCENE ").append(si + 1).append(": ").append(Bible.oneLine(sc.title + " " + sc.setting)).append("\\n");
            for (Story.Beat bt : sc.beats) script.append(bt.type == Story.Beat.DIALOGUE ? "DIALOGUE " : "ACTION ")
                    .append(Bible.oneLine(bt.speakerRaw + " " + bt.manner + " " + bt.text)).append("\\n");
        }
        String system = "You are an independent story reader and script supervisor for an animated film. Read the entire script before judging any beat. The author's format orders title, Characters/पात्र, Objects/वस्तुएँ, Places and Backgrounds/स्थान और पृष्ठभूमि, Story/कहानी, numbered scenes, then END/समाप्त. Names, ages and inventory descriptions in parentheses are reference metadata, not narration. Parenthesized scene text is visual direction. Only text inside double quotation marks is spoken; a parenthetical after the closing quote is delivery direction and must never be read aloud. Keep Hindi and English content in its source language. For every causal beat record who is where, what changes, what the audience knows and what visual evidence carries intent. Infer staging, geography, time, weather, reactions, transitions and causal steps only when grounded in text. Track identity, wardrobe, accessories, handedness if known, exact location, props and state across shots. Do not invent plot events, dialogue, motives, assets or a different ending. Distinguish explicit canon, safe visual inference and uncertainty. List missing assets with story evidence and provenance; request essential missing uploads before library matches and never claim a match without inventory evidence. Check Director coverage and Technical Director feasibility. Return ONLY JSON with keys: beats, inferred_details (scene, category, proposal, evidence, confidence 0..1, requires_approval), missing_assets (name, category, scene, reason, request_upload_first true), continuity_risks, missing_coverage, questions, director_handoff, technical_handoff. Preserve authored scene order, every dialogue line and the ending. Script:\\n";
        return cloud.ask(system, script.toString(), true);
    }

    /**
     * Three-role conference: reader proposes implied story needs, director turns approved intent into coverage,
     * technical director challenges feasibility/continuity, then a chair reconciles disagreements. The result is a
     * review/handoff, not an unapproved rewrite of the user's screenplay.
     */
    public static String productionConference(Cloud cloud, Story st) throws IOException {
        // Iterative editorial workflow inspired by publicly described animation practices.
        // Reader/editor/technical director debate freely; director retains final creative authority.
        String summary = conferenceScript(st);
        String reader = productionReview(cloud, st);
        String editorSystem = "You are the independent FILM EDITOR and animatic editor. The script format lists title, cast, objects and places before Story/कहानी, then numbered scenes and END/समाप्त. Parenthesized inventory and scene descriptions are not narration; only double-quoted text is dialogue; trailing parentheses after a quote are delivery direction and are not spoken. Build a proposed rough cut from the complete screenplay and reader notes. Evaluate audience clarity, emotional rhythm, shot duration, reaction holds, transitions, setup/payoff, redundant shots, dialogue timing, silence, ambience, music when the script calls for it, and sound bridges. Preserve canon, scene order, every authored line and ending. Do not invent plot or dialogue. Return ONLY JSON with editorial_cut, pacing_risks, confusing_moments, missing_reaction_shots, sound_and_silence_notes, proposed_edits, questions_for_director.";
        String editor = cloud.ask(editorSystem, "SCREENPLAY:\n" + summary + "\nINDEPENDENT READER NOTES:\n" + reader, true);
        String directorSystem = "You are the CREATIVE DIRECTOR and final creative authority. Independently evaluate the complete screenplay, reader notes and rough cut as a human filmmaker, not a keyword matcher. Parenthesized scene text is visual direction; only quoted words are spoken dialogue; trailing parentheses after dialogue are delivery direction. Preserve all authored scenes, dialogue, facts, chronology and ending. For every beat consider motivation, audience knowledge, subtext, action/reaction, pauses, entrances/exits, geography, eyelines, screen direction, character/wardrobe identity, object ownership/state, natural phenomena and their physical consequences. Plan motivated shot size/angle/movement, blocking, expressions, body mechanics, anticipation, contact, weight transfer, prop handling, camera continuity, dialogue timing, ambience and sound bridges. Label canon versus safe visual inference; never fabricate assets or claim a library match without inventory proof. Explicitly disposition each Reader/Editor/Technical Director issue and record residual risks. Return JSON with first_cut, beat_map, proposed_scenes_and_shots, required_entities, performance_and_emotion, reader_proposals_accepted, reader_proposals_rejected_with_reason, editor_notes_accepted, editor_notes_rejected_with_reason, unresolved_questions, technical_handoff.";
        String director = cloud.ask(directorSystem, "SCREENPLAY:\n" + summary + "\nREADER NOTES:\n" + reader + "\nEDITORIAL ROUGH CUT:\n" + editor, true);
        String tdSystem = "You are the INDEPENDENT TECHNICAL DIRECTOR and continuity supervisor. The screenplay uses inventories before Story/कहानी, parenthesized visual directions, quoted dialogue and trailing parenthetical delivery notes; never treat inventory or delivery metadata as spoken story content. Challenge the first cut candidly for asset identity/availability, exact cast/place references, costume continuity, screen direction, prop state/contact, movement/rig feasibility, dialogue timing/lip sync, natural-effect cause and persistence, sound and export. Separate hard blockers from artistic preferences and report approximations honestly. Return JSON with hard_blockers, risks, reference_asset_requests, continuity_locks, shot_feasibility, dialogue_lipsync_plan, sound_plan, qc_matrix (PASS/FAIL/NEEDS_HUMAN_REVIEW), required_changes, acceptable_tradeoffs.";
        String technical = cloud.ask(tdSystem, "SCREENPLAY:\n" + summary + "\nREADER:\n" + reader + "\nEDITOR:\n" + editor + "\nDIRECTOR FIRST CUT:\n" + director, true);
        String readerReplySystem = "You are the independent STORY READER in an open editorial review. In this screenplay, parenthesized text is descriptive or delivery direction and only text inside double quotes is spoken. Respond directly to the director's cut and technical critique. Defend story clarity where warranted, concede genuine problems, identify missing causal links and audience confusion, and propose minimal fixes preserving author intent, exact dialogue and ending. You do not make the final cut. Return JSON with points_supported, points_disputed_with_reason, minimal_revisions, canon_or_dialogue_at_risk, unresolved_questions.";
        String readerReply = cloud.ask(readerReplySystem, "SCREENPLAY:\n" + summary + "\nREADER NOTES:\n" + reader + "\nDIRECTOR CUT:\n" + director + "\nTECHNICAL CRITIQUE:\n" + technical, true);
        String editorReplySystem = "You are the FILM EDITOR. The screenplay's parenthetical descriptions and post-quote delivery notes are not spoken lines; preserve all double-quoted dialogue. Discuss the reader's and technical director's notes against your rough cut. Be candid about pace, clarity, reaction timing, shot economy, transitions, silence and sound. Offer alternatives and tradeoffs without changing the story or ending. The director retains final creative authority. Return JSON with revised_editorial_cut, notes_supported, notes_disputed_with_reason, timing_changes, transition_changes, sound_changes, alternatives_for_director.";
        String editorReply = cloud.ask(editorReplySystem, "SCREENPLAY:\n" + summary + "\nEDITOR CUT:\n" + editor + "\nDIRECTOR CUT:\n" + director + "\nTECHNICAL CRITIQUE:\n" + technical + "\nREADER RESPONSE:\n" + readerReply, true);
        String revisionSystem = "You are the DIRECTOR revising your cut after candid debate. Parenthesized scene text is visual direction; only quoted words are dialogue; trailing parentheses describe delivery. Address material objections and explicitly accept, reject or adapt each with reasons. You—not the editor, reader, technical director or chair—make final creative choices. Preserve author dialogue, scene order and ending. You may choose artistic tradeoffs but may not pretend missing assets or failed objective checks are passed. Return JSON with revised_cut, director_decisions (issue, role, decision accept|reject|adapt, reason), final_beat_map, final_shot_plan, approved_inferences, approval_required, asset_requests, proposed_scene_insertions, continuity_bible, dialogue_and_sound_plan, technical_blockers_remaining, questions_for_user.";
        String revision = cloud.ask(revisionSystem, "SCREENPLAY:\n" + summary + "\nREADER:\n" + reader + "\nEDITOR:\n" + editor + "\nDIRECTOR FIRST CUT:\n" + director + "\nTECHNICAL REVIEW:\n" + technical + "\nREADER RESPONSE:\n" + readerReply + "\nEDITOR RESPONSE:\n" + editorReply, true);
        String verifySystem = "You are the technical director performing a SECOND-PASS check of the revised cut. Inventory descriptions and parenthesized directions are reference/visual data, not spoken audio; only double-quoted words are dialogue, with trailing parentheses as delivery cues. Verify which prior hard blockers are resolved; don't relitigate artistic preferences. A concern is not resolved merely because the director disagrees. Return JSON with resolved_blockers, remaining_hard_blockers, verified_asset_requirements, continuity_verification, renderability, qc_matrix, acceptable_director_tradeoffs.";
        String technicalFinal = cloud.ask(verifySystem, "DIRECTOR REVISED CUT:\n" + revision + "\nFIRST TECHNICAL REVIEW:\n" + technical + "\nREADER RESPONSE:\n" + readerReply + "\nEDITOR RESPONSE:\n" + editorReply, true);
        String finalSystem = "You are the DIRECTOR issuing the FINAL PRODUCTION DECISION after the complete conference. You have final creative authority; other roles may disagree but cannot overrule your creative decisions. Explicitly resolve material disagreements and record accept/reject/adapt with reasons. Preserve canon, dialogue and causal events. Insertions must use exact existing character names and an existing screenplay place; 1-3 visual-only actions; max 8 inserts. auto_apply=true only for high-confidence >=0.85 plot-neutral connective/reaction/establishing coverage. Approved assets must be high-confidence, plot-neutral and require_approval=false; never claim library presence unless verified. Technical truth remains mandatory: set ready_to_render=false for unresolved required assets, invalid references, continuity failures or objective render/export blockers. Return ONLY JSON with final_decision_by (exactly director), director_final_decision, director_decisions, canon_facts, approved_inferences, approval_required, final_beat_map, final_shot_plan, editorial_cut, asset_requests, approved_assets, scene_insertions, continuity_bible, dialogue_and_sound_plan, technical_blockers, qc_matrix, role_disagreements, human_questions, ready_to_render (boolean). approved_assets entries use category place|prop|background, name, description, evidence, confidence 0..1, requires_approval boolean. scene_insertions entries use after_scene (1-based original scene), title, setting (existing place), cast (exact existing names), actions (1-3 visual-only directions), purpose, confidence 0..1, auto_apply boolean. This plan does not prove rendered-film quality.";
        String finalJson = cloud.ask(finalSystem, "SCREENPLAY:\n" + summary + "\nREADER ORIGINAL NOTES:\n" + reader + "\nEDITOR ORIGINAL CUT:\n" + editor + "\nDIRECTOR FIRST CUT:\n" + director + "\nTECHNICAL FIRST REVIEW:\n" + technical + "\nREADER RESPONSE:\n" + readerReply + "\nEDITOR RESPONSE:\n" + editorReply + "\nDIRECTOR REVISION:\n" + revision + "\nTECHNICAL SECOND PASS:\n" + technicalFinal, true);
        Object parsed = Cloud.jsonIn(finalJson);
        if (!(parsed instanceof Map)) return finalJson;
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) parsed;
        // Never manufacture a director sign-off. Validate the final hand-off contract
        // against the model's actual answer, including a concrete shot plan and decisions.
        Object shotPlan = report.get("final_shot_plan");
        Object decisions = report.get("director_decisions");
        Object signoff = report.get("final_decision_by");
        Object finalDecision = report.get("director_final_decision");
        Object readiness = report.get("ready_to_render");
        boolean contractValid = signoff instanceof String
                && "director".equalsIgnoreCase(((String) signoff).trim())
                && finalDecision instanceof String && !((String) finalDecision).trim().isEmpty()
                && shotPlan instanceof java.util.List && !((java.util.List<?>) shotPlan).isEmpty()
                && decisions instanceof java.util.List
                && readiness instanceof Boolean;
        report.put("conference_contract_valid", Boolean.valueOf(contractValid));
        report.put("conference_contract_version", 1);
        report.put("conference_rounds", 9);
        report.put("reader_original_review", reader);
        report.put("editor_original_cut", editor);
        report.put("director_first_cut", director);
        report.put("technical_first_review", technical);
        report.put("reader_response", readerReply);
        report.put("editor_response", editorReply);
        report.put("director_revision", revision);
        report.put("technical_second_pass", technicalFinal);
        return Json.write(report);
    }

    private static String conferenceScript(Story st) {
        StringBuilder b = new StringBuilder();
        b.append("TITLE: ").append(Bible.oneLine(st.title)).append("\\nCAST:\\n");
        for (Story.CharacterDef ch : st.characters) b.append("- ").append(Bible.oneLine(ch.displayName + " " + ch.description)).append("\\n");
        b.append("PLACES:\\n"); for (Story.PlaceDef pl : st.places) b.append("- ").append(Bible.oneLine(pl.name + " " + pl.description)).append("\\n");
        b.append("PROPS:\\n"); for (Story.PlaceDef it : st.things) b.append("- ").append(Bible.oneLine(it.name + " " + it.description)).append("\\n");
        for (int i=0;i<st.scenes.size();i++) { Story.Scene sc=st.scenes.get(i); b.append("SCENE ").append(i+1).append(": ").append(Bible.oneLine(sc.title+" "+sc.setting)).append("\\n");
            for (Story.Beat bt:sc.beats) b.append(bt.type==Story.Beat.DIALOGUE?"DIALOGUE ":"ACTION ").append(Bible.oneLine(bt.speakerRaw+" "+bt.manner+" "+bt.text)).append("\\n"); }
        return b.toString();
    }

    /** The nature and physics effects the film engine can show, in the words the director understands. */
    public static final String[] CUES = {"rain", "heavy rain", "light rain", "rain stopped", "storm", "dust storm", "strong wind", "breeze",
            "wind stopped", "thunder and lightning", "snow", "fog", "fog cleared", "campfire", "candles", "diyas", "torches", "fireflies",
            "stars", "rainbow", "falling leaves", "flower petals falling", "clouds", "dark clouds", "birds flying", "sea waves", "high waves",
            "boat", "earthquake", "fruit fell from the tree", "ball fell", "throws a ball", "threw a stone into the water",
            "jumped into the water"};

    /**
     * Lets the AI read the story for nature and physics — also when it is only implied ("the sky opened up",
     * "monsoon evening", "the old boat creaked on the waves") — and mark each scene and line with effects from
     * CUES. Returns JSON {"scenes":[{"scene":i,"place":[..],"lines":[{"line":j,"cues":[..]}]}]} (i, j from 0).
     */
    public static String natureCues(Cloud cloud, Story st) throws IOException {
        StringBuilder all = new StringBuilder("{\"scenes\":[");
        boolean first = true;
        int si = 0;
        while (si < st.scenes.size()) {
            // a few scenes per request, so long stories fit
            StringBuilder b = new StringBuilder();
            int from = si;
            while (si < st.scenes.size() && (b.length() < 4500 || si == from)) {
                Story.Scene sc = st.scenes.get(si);
                b.append("SCENE ").append(si).append(" — place: ").append(Bible.oneLine(sc.setting + " " + sc.title)).append('\n');
                for (int bi = 0; bi < sc.beats.size(); bi++) {
                    Story.Beat bt = sc.beats.get(bi);
                    String t = Bible.oneLine((bt.manner.length() > 0 ? "(" + bt.manner + ") " : "") + bt.text);
                    if (t.length() > 180) t = t.substring(0, 180);
                    b.append("  line ").append(bi).append(bt.type == Story.Beat.DIALOGUE ? " [speech]: " : " [action]: ").append(t).append('\n');
                }
                si++;
            }
            StringBuilder vocab = new StringBuilder();
            for (String c : CUES) { if (vocab.length() > 0) vocab.append(", "); vocab.append('"').append(c).append('"'); }
            String system = "You are the director of a children's animated film. You read a script (Hindi, English or Hinglish) and decide "
                    + "which weather, nature and physics effects each scene and moment needs, also when they are only implied "
                    + "(e.g. 'the sky opened up' = rain, 'monsoon evening' = clouds and light rain, 'the sea was angry' = high waves and strong wind, "
                    + "'they sat around the fire telling stories' = campfire). Use ONLY these effect names: " + vocab
                    + ". Place effects last the whole scene; line effects start at that line. Speech only shows effects that are really happening. "
                    + "Reply ONLY with JSON: {\"scenes\":[{\"scene\":<number>,\"place\":[...],\"lines\":[{\"line\":<number>,\"cues\":[...]}]}]}";
            String ans = cloud.ask(system, b.toString(), true);
            Object o = Cloud.jsonIn(ans);
            List<Object> scenes = o == null ? null : Json.arr(o, "scenes");
            if (scenes != null) for (Object sc : scenes) {
                int n = (int) Json.num(sc, "scene", -1);
                if (n < from || n >= si) continue;
                if (!first) all.append(',');
                first = false;
                all.append(Json.write(sc));
            }
        }
        return all.append("]}").toString();
    }

    /** Puts the cues of natureCues() onto the story's scenes and lines (unknown effect names are ignored). */
    public static void applyCues(Story st, String json) {
        Object o = Json.parseLoose(json);
        List<Object> scenes = o == null ? null : Json.arr(o, "scenes");
        if (scenes == null) return;
        java.util.Set<String> ok = new java.util.HashSet<String>(java.util.Arrays.asList(CUES));
        for (Object sc : scenes) {
            int n = (int) Json.num(sc, "scene", -1);
            if (n < 0 || n >= st.scenes.size()) continue;
            Story.Scene scene = st.scenes.get(n);
            scene.cues = joinCues(Json.arr(sc, "place"), ok);
            List<Object> lines = Json.arr(sc, "lines");
            if (lines != null) for (Object l : lines) {
                int bi = (int) Json.num(l, "line", -1);
                if (bi < 0 || bi >= scene.beats.size()) continue;
                scene.beats.get(bi).cue = joinCues(Json.arr(l, "cues"), ok);
            }
        }
    }

    static String joinCues(List<Object> l, java.util.Set<String> ok) {
        StringBuilder b = new StringBuilder();
        if (l != null) for (Object x : l) {
            if (!(x instanceof String)) continue;
            String c = ((String) x).trim().toLowerCase(java.util.Locale.ROOT);
            if (!ok.contains(c)) continue;
            if (b.length() > 0) b.append(" । ");
            b.append(c).append(' ');
        }
        return b.toString();
    }

    /** Finds a candidate whose name appears in s (Devanagari or Latin spelling). */
    public static String matchName(String s, List<String> candidates) {
        if (s == null) return null;
        String n = s.replace('_', ' ').replace('-', ' ').replace('.', ' ');
        String nn = Txt.norm(n).trim();
        String best = null;
        int bestScore = 0;
        for (String c : candidates) {
            if (!CommandParser.nameIn(n, c)) continue;
            // the same name beats shared words, shared words beat names that only sound alike ("राजा" / "राजू")
            int score = c.length();
            if (Txt.norm(c).trim().equals(nn)) score += 100000;
            for (String part : c.split("\\s+")) if (part.length() >= 2 && !ScriptParser.isTitleWord(part) && Txt.has(n, part)) score += 1000;
            if (score > bestScore) { best = c; bestScore = score; }
        }
        return best;
    }
}

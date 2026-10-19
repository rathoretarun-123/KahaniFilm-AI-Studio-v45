package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The production file: after reading the script the studio writes down every character, place, shot, voice and
 * sound it needs, with ready-to-paste prompts so the user can make pictures elsewhere (ChatGPT, Grok, Meta AI…)
 * and upload them. File names that contain a character's or place's name are recognised automatically.
 */
public final class Bible {
    private Bible() {}

    /** Emotion -> words for the shot list and for voice direction. */
    public static String emotionWord(int e, boolean hindi) {
        switch (e) {
            case Pose.HAPPY: return hindi ? "खुश" : "happy";
            case Pose.LAUGH: return hindi ? "हँसते हुए" : "laughing";
            case Pose.ANGRY: return hindi ? "गुस्से में" : "angry";
            case Pose.SAD: return hindi ? "दुखी" : "sad";
            case Pose.SCARED: return hindi ? "डरे हुए" : "scared";
            case Pose.SURPRISED: return hindi ? "हैरान" : "surprised";
            case Pose.EVIL: return hindi ? "कुटिल/शैतानी" : "evil";
            case Pose.DETERMINED: return hindi ? "दृढ़" : "determined";
            case Pose.DIZZY: return hindi ? "चकराए हुए" : "dizzy";
            case Pose.WHISPER: return hindi ? "फुसफुसाते हुए" : "whispering";
            case Pose.PROUD: return hindi ? "गर्व से" : "proud";
            case Pose.CURIOUS: return hindi ? "जिज्ञासा से" : "curious";
            case Pose.PAIN: return hindi ? "दर्द में" : "in pain";
            case Pose.SUSPICIOUS: return hindi ? "शक से" : "suspicious";
            case Pose.RELIEVED: return hindi ? "राहत से" : "relieved";
            default: return hindi ? "सामान्य" : "calm";
        }
    }

    static String kindWord(Look l, boolean hi) {
        if (l == null) return "";
        switch (l.kind) {
            case Look.GIRL: return hi ? "लड़की" : "girl";
            case Look.WOMAN: return hi ? "महिला" : "woman";
            case Look.BOY: return hi ? "लड़का" : "boy";
            case Look.OLD_MAN: return hi ? "बुज़ुर्ग" : "old man";
            case Look.WITCH: return hi ? "जादूगरनी" : "witch";
            case Look.MONSTER: return hi ? "राक्षस" : "monster";
            case Look.MONKEY: return hi ? "बंदर" : "monkey";
            case Look.ANIMAL: return hi ? "जानवर" : "animal";
            case Look.BIRD: return hi ? "पक्षी" : "bird";
            default: return hi ? "पुरुष" : "man";
        }
    }

    /** Suggested voice for a character (used when no sample is given). */
    public static String voiceHint(Story.CharacterDef c, boolean hi) {
        Look l = c.look;
        if (l == null) return hi ? "सामान्य आवाज़" : "normal voice";
        String age = c.age > 0 ? (hi ? c.age + " वर्ष" : c.age + " years") : "";
        switch (l.kind) {
            case Look.GIRL: return (hi ? "बच्ची की ऊँची, मीठी आवाज़ " : "young girl's high, sweet voice ") + age;
            case Look.BOY: return (hi ? "बच्चे की चंचल आवाज़ " : "young boy's lively voice ") + age;
            case Look.WOMAN: return hi ? "महिला की कोमल आवाज़" : "woman's soft voice";
            case Look.OLD_MAN: return hi ? "बुज़ुर्ग की भारी, धीमी आवाज़" : "old man's deep, slow voice";
            case Look.WITCH: return hi ? "जादूगरनी की खरखरी, रहस्यमयी आवाज़" : "witch's raspy, mysterious voice";
            case Look.MONSTER: return hi ? "राक्षस की बहुत भारी, गूँजती आवाज़" : "monster's very deep, booming voice";
            case Look.MONKEY: case Look.ANIMAL: case Look.BIRD: return hi ? "जानवर जैसी मज़ेदार आवाज़" : "funny animal voice";
            default: return hi ? "पुरुष की गंभीर आवाज़" : "man's firm voice";
        }
    }

    public static String characterPrompt(Story.CharacterDef c) {
        // v34: the still-picture manual's six blocks (subject, action / emotion, locked style, lighting, quality,
        // camera) and its negative list, around the studio's own subject and pose rules
        StringBuilder b = new StringBuilder();
        b.append("Create ONE character for a premium 3D animated Indian family film. ");
        b.append("Name: ").append(c.displayName);
        if (c.age > 0) b.append(", age ").append(c.age);
        if (c.look != null) b.append(", ").append(kindWord(c.look, false));
        if (c.look != null && c.look.heads > 1) b.append(" with ").append(c.look.heads).append(" heads side by side on one body (the central head is the main face)");
        if (c.look != null && c.look.arms > 2) b.append(" with ").append(c.look.arms).append(" arms (the front pair holds things, the other pairs fan out behind)");
        if (c.look != null && c.look.mount >= 0) b.append(", seated on a ").append(Look.speciesWord(c.look.mount, false)).append(" (the animal is part of the picture, under the character)");
        if (c.look != null) b.append(bodyWords(c.look));
        b.append(". Description: ").append(oneLine(c.description));
        String action = "standing straight and facing the camera, arms a little away from the body (A-pose), both hands visible, nothing held in front of the body, "
                + "feet a little apart, mouth closed, eyes open, a gentle friendly expression, fully and modestly dressed, isolated on a plain pure-white background, no other people";
        return StillPrompt.build(b.toString(), action, false);
    }

    /** v35: the body as the story tells it, in the picture's prompt — shown plainly and with dignity, as part of who the character is. */
    static String bodyWords(Look l) {
        StringBuilder w = new StringBuilder();
        if (l.missingArm() != 0) w.append(", with only one arm (the ").append(l.missingArm() < 0 ? "left" : "right").append(" sleeve empty and folded)");
        if (l.missingLeg() != 0) w.append((l.condition & Look.C_ARTIFICIAL_LEG) != 0 ? ", with an artificial lower " + (l.missingLeg() < 0 ? "left" : "right") + " leg"
                : ", with one leg (the " + (l.missingLeg() < 0 ? "left" : "right") + " trouser leg folded at the knee), standing on crutches");
        if ((l.condition & Look.C_FINGERS) != 0) w.append(", with some fingers missing on the left hand");
        if ((l.condition & Look.C_ONE_EYE) != 0 && l.glasses != 5) w.append(", with one eye (the left eye closed, a faint scar)");
        if ((l.condition & Look.C_HEARING_AID) != 0) w.append(", wearing small hearing aids behind the ears");
        if ((l.condition & Look.C_LIMP) != 0 && l.aid == Look.AID_NONE) w.append(", standing with the weight a little on one leg");
        if (w.length() > 0) w.append(" — shown plainly and with dignity, as part of who they are");
        return w.toString();
    }

    public static String placePrompt(String name, String description, String aspect) {
        // the aspect ratio is a parameter of the picture (its width and height), never words in the prompt (C3)
        // v34: the still-picture manual's blocks and negative list around the plate's own layer rules
        return TechnicalDirector.clean(StillPrompt.build("Cinematic 3D animated film background plate for a premium Indian children's film, establishing plate, "
                + "no people, no characters, no text. Place: " + name + ". " + oneLine(description)
                + " Layers: blurry foreground 0-1 m, empty midground stage 2-4 m with flat ground, background 10-100 m. Vivid but natural colours. " + TechnicalDirector.STABLE + ".", "", true));
    }

    public static String oneLine(String s) {
        if (s == null) return "";
        return s.replace('\n', ' ').replace("  *", " ").replaceAll("\\s+", " ").trim();
    }

    /** Camera choice for a dialogue/direction, mirroring what the renderer does. */
    static String camera(int beatInScene, boolean speakerChanged, int emotion, boolean action, boolean hi) {
        if (beatInScene == 0) return hi ? "वाइड शॉट (पूरी जगह)" : "Wide establishing shot";
        if (action) return hi ? "वाइड/मीडियम शॉट, कैमरा एक्शन के साथ" : "Wide/medium shot following the action";
        if (emotion == Pose.ANGRY || emotion == Pose.SCARED || emotion == Pose.SURPRISED || emotion == Pose.SAD || emotion == Pose.EVIL)
            return hi ? "क्लोज़-अप (चेहरे के भाव)" : "Close-up on the face";
        if (speakerChanged) return hi ? "मीडियम क्लोज़-अप, बोलने वाले पर फ़ोकस" : "Medium close-up on the speaker";
        return hi ? "मीडियम शॉट" : "Medium shot";
    }

    /** Builds the whole production file. lib may be null. have = names that already have a picture/voice. */
    public static String write(Story st, SoundLib lib, Set<String> havePicture, Set<String> haveVoice) {
        boolean hi = false;   // the production file is written in English; story content stays in its own language
        StringBuilder b = new StringBuilder();
        String title = st.title.length() > 0 ? st.title : "My story";
        b.append(hi ? "प्रोडक्शन फ़ाइल — " : "Production file — ").append(title).append("\n");
        b.append("============================================================\n\n");
        b.append(hi ? "कैसे इस्तेमाल करें:\n"
                + "1. नीचे हर पात्र और जगह का विवरण और \"Prompt\" है। Prompt को ChatGPT / Grok / Meta AI में चिपकाकर चित्र बनवाएँ।\n"
                + "2. चित्र को पात्र/जगह के नाम से सेव करें (जैसे  वृंदा.jpg ) और ऐप में \"चित्र जोड़ें\" से अपलोड करें — ऐप नाम से पहचान लेगा।\n"
                + "3. आवाज़ के लिए हर पात्र की 10–20 सेकंड की रिकॉर्डिंग दें; उसी आवाज़ में पूरे संवाद बनेंगे।\n"
                + "4. जो चीज़ आप नहीं देंगे, स्टूडियो अपनी लाइब्रेरी से खुद चुन लेगा।\n\n"
                : "How to use:\n"
                + "1. Each character and place below has a description and a \"Prompt\". Paste the prompt into ChatGPT / Grok / Meta AI to make the picture.\n"
                + "2. Save the picture with the character/place name (e.g. Vrinda.jpg) and use \"Add many pictures at once\" in the studio — the app recognises the name.\n"
                + "3. For voices give a 10–20 second recording per character; all their dialogue will be made in that voice.\n"
                + "4. Anything you do not give, the studio picks from its own library.\n\n");

        // ---- characters
        b.append(hi ? "पात्र (" : "CHARACTERS (").append(st.characters.size()).append(")\n------------------------------------------------------------\n");
        int i = 1;
        for (Story.CharacterDef c : st.characters) {
            int lines = 0;
            for (Story.Scene sc : st.scenes) for (Story.Beat bt : sc.beats) if (bt.speaker == c) lines++;
            b.append(i++).append(". ").append(st.shown(c.fullName != null && c.fullName.length() > 0 ? c.fullName : c.displayName));
            if (c.age > 0) b.append(" (").append(c.age).append(hi ? " वर्ष)" : " yrs)");
            if (c.role != null && c.role.length() > 0) b.append(" — ").append(c.role);
            if (c.voiceOnly) b.append(hi ? " (सिर्फ़ आवाज़ — कोई चित्र नहीं चाहिए)" : " (a voice only — no picture needed)");
            b.append("\n");
            if (c.description.length() > 0) b.append("   ").append(hi ? "विवरण: " : "Description: ").append(oneLine(c.description)).append("\n");
            b.append("   ").append(hi ? "संवाद: " : "Lines: ").append(lines).append("\n");
            b.append("   ").append(hi ? "आवाज़: " : "Voice: ").append(voiceHint(c, hi));
            if (haveVoice != null && contains(haveVoice, c)) b.append(hi ? "  ✔ नमूना मिला" : "  ✔ sample given");
            b.append("\n");
            // what the studio does to the voice because of the description
            List<String> made = new ArrayList<String>(VoiceMatch.want(c).words);
            made.addAll(VoiceStyle.forCharacter(c).words);
            if (!made.isEmpty()) {
                b.append("   Voice from the description: ");
                for (int w = 0; w < made.size(); w++) b.append(w > 0 ? ", " : "").append(made.get(w));
                b.append("\n");
            }
            // a voice only (the narrator, a radio) is never asked for a picture, nor given a picture prompt
            if (c.voiceOnly) { b.append("\n"); continue; }
            b.append("   ").append(hi ? "चित्र: " : "Picture: ").append(havePicture != null && contains(havePicture, c)
                    ? (hi ? "✔ मिल गया" : "✔ given") : (hi ? "✘ चाहिए (या स्टूडियो बनाएगा)" : "✘ needed (or studio makes it)")).append("\n");
            b.append("   Prompt: ").append(characterPrompt(c)).append("\n\n");
        }

        // ---- places
        List<String[]> places = places(st);
        b.append(hi ? "जगहें (" : "PLACES (").append(places.size()).append(")\n------------------------------------------------------------\n");
        i = 1;
        for (String[] p : places) {
            b.append(i++).append(". ").append(p[0]).append("\n");
            if (p[1].length() > 0) b.append("   ").append(hi ? "विवरण: " : "Description: ").append(oneLine(p[1])).append("\n");
            if (havePicture != null && havePicture.contains(Txt.norm(p[0]))) b.append(hi ? "   ✔ चित्र मिल गया\n" : "   ✔ picture given\n");
            b.append("   Prompt: ").append(placePrompt(p[0], p[1], "16:9")).append("\n\n");
        }

        // ---- shots
        b.append(hi ? "शॉट सूची (निर्देशक की योजना)\n" : "SHOT LIST (director's plan)\n");
        b.append("------------------------------------------------------------\n");
        int shot = 1;
        for (Story.Scene sc : st.scenes) {
            b.append("\n").append(hi ? "भाग " : "Part ").append(sc.number);
            if (sc.title.length() > 0) b.append(": ").append(sc.title);
            b.append("\n");
            if (sc.setting.length() > 0) b.append("   ").append(hi ? "जगह: " : "Place: ").append(oneLine(sc.setting)).append("\n");
            if (lib != null) {
                SoundLib.Entry amb = lib.best(sc.setting + " " + sc.title, "amb", null);
                if (amb != null) b.append("   ").append(hi ? "पृष्ठभूमि ध्वनि: " : "Background sound: ").append(amb.title).append("\n");
            }
            Story.CharacterDef last = null;
            int k = 0;
            for (Story.Beat bt : sc.beats) {
                if (bt.type == Story.Beat.DIALOGUE) {
                    int emo = Director.emotionOf(bt.manner, bt.text, bt.speaker);
                    boolean changed = bt.speaker != last;
                    String who = bt.narrator || bt.speaker == null ? "Narrator (voice only)" : bt.speaker.shown();
                    b.append("   ").append(hi ? "शॉट " : "Shot ").append(shot++).append(" — ")
                            .append(bt.narrator ? (hi ? "दृश्य पर कथावाचक की आवाज़" : "Voice-over on the scene") : camera(k, changed, emo, false, hi))
                            .append("\n      ").append(who).append(" [").append(emotionWord(emo, hi)).append("]");
                    if (bt.manner.length() > 0) b.append(" (").append(st.shown(bt.manner)).append(")");
                    b.append(": \"").append(st.shown(oneLine(bt.text))).append("\"\n");
                    String said = VoiceStyle.forLine(null, bt.manner, Txt.has(bt.manner, "फुसफुसा", "whisper", "धीरे से"), false).label();
                    if (said.length() > 0) b.append("      Voice: ").append(said).append("\n");
                    last = bt.speaker;
                } else {
                    String t = oneLine(bt.text);
                    if (t.length() == 0) continue;
                    b.append("   ").append(hi ? "शॉट " : "Shot ").append(shot++).append(" — ").append(camera(k, true, Pose.NEUTRAL, true, hi))
                            .append("\n      ").append(hi ? "एक्शन: " : "Action: ").append(t).append("\n");
                    String sfx = sfxFor(t, lib);
                    if (sfx != null) b.append("      ").append(hi ? "ध्वनि: " : "Sound: ").append(sfx).append("\n");
                }
                k++;
            }
        }

        // ---- title / end
        b.append("\n").append(hi ? "शुरुआत और अंत\n" : "TITLE AND END\n").append("------------------------------------------------------------\n");
        b.append(hi ? "शीर्षक पृष्ठ: \"" : "Title page: \"").append(title).append("\" — ")
                .append(hi ? "कहानी का मुख्य चित्र + संगीत। Prompt: " : "main picture of the story + music. Prompt: ")
                .append("3D animated movie poster for a children's film named '").append(title)
                .append("', main characters together, cinematic lighting, depth of field, no text.\n");
        b.append("End page: \"").append(st.hindi ? "समाप्त" : "The End").append("\" — with gentle music.\n");
        return b.toString();
    }

    static boolean contains(Set<String> have, Story.CharacterDef c) {
        if (have.contains(Txt.norm(c.displayName)) || have.contains(c.id)) return true;
        for (String a : c.aliases) if (have.contains(Txt.norm(a))) return true;
        return false;
    }

    /** Distinct places: the script's place list, then each scene's setting. */
    /**
     * v47.4 (the user): the places and backgrounds the script lists before the story begins — only those are asked for;
     * a script with no such list falls back to the places its scenes are set in.
     */
    public static List<String[]> listedPlaces(Story st) {
        if (st.places.isEmpty()) return places(st);
        List<String[]> out = new ArrayList<String[]>();
        Set<String> seen = new LinkedHashSet<String>();
        for (Story.PlaceDef p : st.places) {
            String key = placeIdentity(p.name);
            if (p.name.length() > 0 && !intangible(p.name) && !key.isEmpty() && seen.add(key)) out.add(new String[]{p.name, p.description});
        }
        return out;
    }

    /**
     * Canonical identity for a location label. Scene writers often list the same place as “नीम का पेड़”,
     * “नीम के पेड़ के नीचे का कोना” and “नीम की छाया”. Keep the first, most fully described inventory
     * entry and collapse positional descriptions into that location, while retaining distinct anchors
     * such as the neem tree and the post office.
     */
    public static String placeIdentity(String place) {
        String n = Txt.norm(place == null ? "" : place).toLowerCase(java.util.Locale.ROOT).trim();
        n = n.replaceAll("(?:के नीचे(?: का)?|के पास|के सामने|के पीछे|की छाया में|की छाया|के किनारे|वाले? कोने में|वाले? कोना|का कोना|कोने में|कोना|छाया में|के पास वाला)", " ");
        n = n.replaceAll("(?<=[\\p{IsDevanagari}])\\s+(?:का|की|के)\\s+", " ");
        n = n.replaceAll("(?:पेड़|पेड|वृक्ष)", " ").replaceAll("\\btree\\b", " ");     // (v47.6: the text is normalised, so "पेड़" is "पेड" here)
        n = n.replaceAll("\\b(?:the|a|an|under|below|near|beside|in front of|behind|corner|shade|shadow)\\b", " ");
        n = n.replaceAll("[^\\p{IsDevanagari}a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
        return n;
    }

    /**
     * v47.8 (the user: "ask as many pictures as possible for background … as described in script"): every background
     * picture the script describes for one place — its wide view, the reverse angle, the views behind the close shots,
     * a view for each other time of day its scenes are set at, and each thing of the story seen there, where it is in
     * the place. Each entry is {kind ("view" or "thing"), what to photograph, the thing's name for a thing}. Only the
     * script's own words are used; nothing is made up for a story.
     */
    public static List<String[]> backgroundViews(Story st, String place, String description, List<Story.Scene> here) {
        List<String[]> out = new ArrayList<String[]>();
        out.add(new String[]{"view", "The wide view — the whole of " + place + ", empty (no one in it)" + (description.length() > 0 ? ": " + oneLine(description) : ""), ""});
        out.add(new String[]{"view", "The reverse angle — " + place + " seen the other way round (behind the over-the-shoulder shots)", ""});
        out.add(new String[]{"view", "The close views — the walls, corners and floor of " + place + " behind the close shots", ""});
        StringBuilder text = new StringBuilder(description);
        Set<String> times = new LinkedHashSet<String>();
        for (Story.Scene sc : here) {
            text.append(' ').append(sc.setting).append(' ').append(sc.title);
            for (Story.Beat b : sc.beats) text.append(' ').append(b.text);
            String when = timeOfDay(sc.setting + " " + sc.title);
            if (when != null) times.add(when);
        }
        if (times.size() > 1) for (String when : times) out.add(new String[]{"view", place + " in the " + when + " light (the scenes set then)", ""});
        String all = Txt.norm(text.toString()).toLowerCase(java.util.Locale.ROOT);
        for (Story.PlaceDef th : st.things) if (seenIn(th.name, all) && !intangible(th.name))
            out.add(new String[]{"thing", th.name + " where it is in " + place + " — close, and a little wider", th.name.trim()});
        return out;
    }

    /** The hour a scene is set at, in English, or null. */
    static String timeOfDay(String s) {
        String n = Txt.norm(s).toLowerCase(java.util.Locale.ROOT);
        String[][] hours = {{"morning", "सुबह", "भोर", "morning", "dawn", "sunrise"}, {"afternoon", "दोपहर", "afternoon", "noon"},
                {"evening", "शाम", "संध्या", "evening", "dusk", "sunset"}, {"night", "रात", "night", "midnight"}};
        for (String[] h : hours) for (int i = 1; i < h.length; i++) if (n.contains(Txt.norm(h[i]).toLowerCase(java.util.Locale.ROOT))) return h[0];
        return null;
    }

    /** The thing's name, its words in brackets, or its last word's stem ("पंखा" is seen in "पंखे") appears in the text. */
    public static boolean seenIn(String name, String normText) {
        String nm = name == null ? "" : name.trim();
        if (nm.length() < 2) return false;
        List<String> forms = new ArrayList<String>();
        int open = nm.indexOf('(');
        String main = open > 0 ? nm.substring(0, open).trim() : nm;
        forms.add(main);
        if (open > 0 && nm.indexOf(')', open) > open) forms.add(nm.substring(open + 1, nm.indexOf(')', open)).trim());
        String[] words = main.split("\\s+");
        String last = words[words.length - 1];
        if (last.length() > 3) forms.add(last.substring(0, last.length() - 1));
        for (String f : forms) {
            String k = Txt.norm(f).toLowerCase(java.util.Locale.ROOT).trim();
            if (k.length() >= 2 && normText.contains(k)) return true;
        }
        return false;
    }

    public static List<String[]> places(Story st) {
        List<String[]> out = new ArrayList<String[]>();
        Set<String> seen = new LinkedHashSet<String>();
        for (Story.PlaceDef p : st.places) {
            String key = placeIdentity(p.name);
            if (p.name.length() == 0 || key.isEmpty() || !seen.add(key)) continue;
            out.add(new String[]{p.name, p.description});
        }
        for (Story.Scene sc : st.scenes) {
            if (sc.setting.length() == 0) continue;
            String first = firstClause(sc.setting, "।.!?");
            String name = firstClause(first, ",;—-(");
            // v43: "समय: शाम", "Time: night" is the hour, not a place to ask a picture for
            if (timeOnly(name)) continue;
            if (name.length() > 50) name = name.substring(0, 50) + "…";
            String key = placeIdentity(name);
            boolean known = key.isEmpty() || seen.contains(key);
            if (!known) for (String[] p : out) if (similar(name, p[0])) { known = true; break; }
            if (known || !seen.add(key)) continue;
            out.add(new String[]{name, first});
        }
        return out;
    }

    /** v43: a setting that only names the hour ("समय: शाम", "Time: morning", "रात", "दोपहर") — no place in it. */
    static boolean timeOnly(String name) {
        String n = Txt.norm(name).trim().replaceAll("[:：\\-–]+", " ").replaceAll("\\s+", " ").trim();
        if (n.startsWith(Txt.norm("समय")) || n.startsWith("time ") || n.equals("time") || n.startsWith(Txt.norm("वक़्त")) || n.startsWith(Txt.norm("वक्त"))) return true;
        String[] hours = {"सुबह", "दोपहर", "शाम", "रात", "आधी रात", "भोर", "morning", "noon", "afternoon", "evening", "night", "midnight", "dawn", "dusk", "day"};
        for (String h : hours) if (n.equals(Txt.norm(h))) return true;
        return false;
    }

    public static String firstClauseOf(String s) { return firstClause(firstClause(s, "।.!?"), ",;—-("); }

    static String firstClause(String s, String stops) {
        int cut = s.length();
        for (int i = 0; i < stops.length(); i++) {
            // a hyphen inside a name ("स्काई-लाइन", "Neo-Mumbai") joins; only " - " between words separates
            int k = stops.charAt(i) == '-' ? s.indexOf(" - ") : s.indexOf(stops.charAt(i));
            if (k > 0 && k < cut) cut = k;
        }
        return s.substring(0, cut).trim();
    }

    /** Most words of a also appear in b (so "रत्नगढ़ का विशाल महल और बगीचा" ~ "रत्नगढ़ का महल और बगीचा"). */
    /**
     * v47.4 (the user: "it is asking for the picture of हवा, sky etc — they should not be asked"): the weather, the sky, the
     * light, a sound, a smell, a time of day, the cold — things a script's list may describe, but no picture can be
     * handed in for; the studio draws or plays them.
     */
    static final String[] INTANGIBLE = {"हवा", "आसमान", "आकाश", "बारिश", "बरसात", "बूँद", "बूंद", "धूप", "रोशनी", "रौशनी", "उजाला", "अंधेरा", "अँधेरा", "अंधकार", "आवाज़", "आवाज", "शोर",
            "खुशबू", "ख़ुशबू", "महक", "गंध", "बादल", "बिजली", "कोहरा", "धुंध", "धुआँ", "धुआं", "भाप", "समय", "शाम", "सुबह", "रात", "दोपहर", "चाँदनी", "चांदनी", "तारे", "सूरज", "सूर्य", "चाँद", "चांद",
            "ठंड", "ठण्ड", "गर्मी", "सन्नाटा", "ख़ामोशी", "खामोशी", "संगीत", "गीत", "मौसम", "तूफ़ान", "तूफान", "आँधी", "आंधी", "ओस", "बर्फ़", "बर्फ", "पानी", "कीचड़", "कीचड", "माहौल", "यादें", "याद", "सपना", "दर्द", "डर", "धूल", "धूलकण", "धूल-मिट्टी", "dust",
            // v47.5 (the user: "it should not ask for images of intangible things like wind, water, sky and emotions"): feelings,
            // fire and shade — the studio draws and plays them
            "खुशी", "ख़ुशी", "दुख", "दुःख", "ग़म", "गम", "प्यार", "प्रेम", "ममता", "गुस्सा", "ग़ुस्सा", "क्रोध", "उदासी", "मुस्कान", "मुस्कुराहट", "हँसी", "हंसी", "आँसू", "आंसू",
            "भावना", "भावनाएं", "भावनाएँ", "एहसास", "अहसास", "उम्मीद", "आशा", "सुकून", "शांति", "तनाव", "चिंता", "अकेलापन", "इंतज़ार", "इंतजार", "ख़ामोशी", "सांस", "साँस",
            "धड़कन", "गूँज", "गूंज", "आग", "लौ", "लपट", "लपटें", "छाया", "छाँव", "छांव", "परछाई", "परछाईं", "किरण", "किरणें", "इंद्रधनुष", "लहर", "लहरें", "हवाएँ", "हवाएं",
            "love", "happiness", "sadness", "anger", "joy", "emotion", "emotions", "feeling", "feelings", "hope", "tears", "smile", "laughter", "loneliness", "peace",
            "breath", "heartbeat", "echo", "fire", "flame", "flames", "shade", "shadow", "shadows", "rays", "rainbow", "waves", "air",
            "wind", "breeze", "sky", "rain", "drizzle", "sunlight", "sunshine", "light", "darkness", "dark", "sound", "noise", "voice", "smell", "scent", "cloud", "clouds", "lightning", "thunder",
            "fog", "mist", "smoke", "steam", "time", "evening", "morning", "night", "afternoon", "moonlight", "stars", "sun", "moon", "cold", "heat", "silence", "music", "song", "weather", "storm",
            "dew", "snow", "water", "mud", "atmosphere", "mood", "memory", "memories", "dream", "pain", "fear"};
    /** True for a thing no picture is asked for (see INTANGIBLE): its name is one of those words, or starts with one ("बारिश का पानी", "हवा में"). */
    public static boolean intangible(String name) {
        if (name == null) return true;
        String n = Txt.norm(name).trim().toLowerCase(java.util.Locale.ROOT);
        if (n.isEmpty()) return true;
        // v47.5: what the name is, is its head noun — the last word, after the last "का / की / के" ("पानी की बाल्टी" is a
        // bucket, "धूप का चश्मा" glasses, "हवा में उड़ती पतंग" a kite; "बारिश का पानी", "दोपहर की धूप", "ठंडी हवा" are
        // water, sunlight, wind); in English the word before "of" ("a bucket of water", but "the sound of rain").
        // In a list, ask for pictures when any named part is tangible: "सूखे पत्ते और धूल" still includes leaves.
        // A list of only effects such as "आसमान और हवा" stays intangible.
        String[] parts = n.split("\\s+(?:और|एवं|तथा|and|&)\\s+|\\s*[,/]\\s*");
        boolean named = false;
        for (String part : parts) {
            String item = part.trim();
            if (item.isEmpty()) continue;
            named = true;
            if (!intangibleHead(item)) return false;
        }
        return named;
    }
    private static boolean intangibleHead(String part) {
        String p = part;
        for (String g : new String[]{" का ", " की ", " के "}) { int i = p.lastIndexOf(g); if (i >= 0) p = p.substring(i + g.length()); }
        int of = p.indexOf(" of ");
        if (of > 0 && p.charAt(0) < 0x0900) p = p.substring(0, of);
        p = p.replaceAll("^(?:the|a|an)\\s+", "").trim();
        String[] ws = p.split("[\\s]+");
        String head = ws[ws.length - 1];
        for (String w : INTANGIBLE) {
            String x = Txt.norm(w).toLowerCase(java.util.Locale.ROOT);
            if (p.equals(x) || head.equals(x) || p.endsWith(" " + x)) return true;
        }
        return false;
    }

    public static boolean similar(String a, String b) {
        String[] wa = Txt.norm(a).split("\\s+");
        String nb = " " + Txt.norm(b) + " ";
        int n = 0, hit = 0;
        for (String w : wa) {
            if (w.length() < 2 || w.equals("का") || w.equals("की") || w.equals("के") || w.equals("और")) continue;
            n++;
            if (nb.contains(" " + w + " ")) hit++;
        }
        return n > 0 && hit * 10 >= n * 7;
    }

    static String sfxFor(String text, SoundLib lib) {
        if (lib == null) return null;
        SoundLib.Entry e = lib.best(text, "sfx", null);
        return e == null ? null : e.title;
    }
}

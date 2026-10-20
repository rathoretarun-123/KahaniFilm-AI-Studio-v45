package com.tarun.kahani.core;

import java.util.HashMap;
import java.util.Map;

/**
 * v47.13: a Kokoro-82M voice (Apache-2.0) for each character of a film, the same on the phone (app.Kokoro) and on the
 * desktop (MakeFilm): its two Hindi women's voices hf_alpha / hf_beta (31, 32) for women and girls, its two men's
 * voices hm_omega / hm_psi (33, 34) for men and boys, British voices for English lines; a child's voice raised with a
 * smaller throat, an old one lowered and slower (TD-PSOLA, VoiceFx.psola, afterwards), a second woman or man of the
 * story in the other voice. One cast per film.
 */
public final class KokoroCast {
    /** {Hindi speaker, English speaker, pace, pitch, formant}. */
    private final Map<Story.CharacterDef, float[]> cast = new HashMap<Story.CharacterDef, float[]>();
    private int women, men;

    public float[] voice(Story.CharacterDef who) {
        float[] v = cast.get(who);
        if (v != null) return v;
        Look lk = who == null ? null : who.look;
        int age = who == null ? -1 : who.age;
        boolean old = lk != null && (lk.kind == Look.OLD_MAN || age >= 60);
        if (lk == null) v = new float[]{33, 26, 0.92f, 1f, 1f};
        // v48 (the user, on the phone: "sounds robotic / squeaky"): Kokoro's voices are natural as they are — a child is
        // moved only a little (a big TD-PSOLA shift is what sounds squeaky and metallic); a boy from a woman's voice
        else if (lk.kind == Look.GIRL) v = new float[]{32, 23, 1.02f, 1.07f, 1.03f};
        else if (lk.kind == Look.BOY) v = new float[]{32, 23, 1.02f, 1.02f, 0.98f};
        else if (lk.kind == Look.MONSTER) v = new float[]{33, 26, 0.9f, 0.88f, 0.93f};
        else if (lk.kind == Look.MONKEY || lk.kind == Look.ANIMAL || lk.kind == Look.BIRD) v = new float[]{32, 23, 1.04f, 1.06f, 1.03f};
        else if (lk.female || lk.kind == Look.WOMAN || lk.kind == Look.WITCH) {
            v = old ? new float[]{31, 21, 0.9f, 0.97f, 0.99f} : new float[]{women % 2 == 0 ? 31 : 32, women % 2 == 0 ? 21 : 22, 0.98f, 1f, 1f};
            if (!old) women++;
        } else {
            v = old ? new float[]{33, 26, 0.9f, 0.95f, 0.98f} : new float[]{men % 2 == 0 ? 34 : 33, men % 2 == 0 ? 24 : 25, 0.98f, 1f, 1f};
            if (!old) men++;
        }
        cast.put(who, v);
        return v;
    }

    /**
     * The user's own choice of a Kokoro voice for a character (null: the narrator) from the library: a Hindi voice
     * (31 to 34) for its Hindi lines, any other for its English ones.
     */
    public void choose(Story.CharacterDef who, int speaker) {
        if (speaker < 0) return;
        float[] v = voice(who).clone();
        if (speaker >= 31 && speaker <= 34) v[0] = speaker; else v[1] = speaker;
        cast.put(who, v);
    }

    /** The speaker for a line in its language. */
    public int speaker(Film.Line l) { float[] v = voice(l.who); return (int) (l.hindi ? v[0] : v[1]); }

    /** The pace a line is spoken at: the character's, times its feeling's. */
    public float pace(Film.Line l) { return voice(l.who)[2] * VoiceFx.emotionFactors(l.emotion)[0]; }

    /** The spoken line moved to the character's pitch and throat and its feeling's pitch (TD-PSOLA). */
    public float[] shape(Film.Line l, float[] pcm, int sr) {
        if (pcm == null) return null;
        float[] v = voice(l.who);
        // the whole move kept within ±12%: beyond it the natural voice turns squeaky or hollow
        float p = Math.max(0.88f, Math.min(1.12f, v[3] * VoiceFx.emotionFactors(l.emotion)[1]));
        if (Math.abs(p - 1) < 0.01f && Math.abs(v[4] - 1) < 0.01f) return pcm;
        return VoiceFx.psola(pcm, sr, p, v[4], 1f);
    }

    /** Kokoro's language code for a line ("hi", or American-English rules with a British voice). */
    public static String lang(Film.Line l) { return l.hindi ? "hi" : "en-us"; }
}

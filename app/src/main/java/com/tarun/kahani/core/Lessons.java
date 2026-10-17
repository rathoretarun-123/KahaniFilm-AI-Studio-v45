package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * v47.9 (the user: "make your director, technical director, reader and editor learn things from each story creation
 * so that they can refine themselves"): what the team keeps from every film it makes, and how the next film changes
 * because of it. It is kept on the phone (one small text file for all stories), never sent anywhere.
 *
 * What is learned, and what it changes:
 * - the shot fixes the user chose at the shot check (calmer, closer, wider, show the listener, no cut): when the user
 *   keeps asking for calmer shots or fewer cuts, the technical director cuts less often (longer shots);
 * - the frame audit (jolts, jumps, picture pops per thousand frames): when films jolted, the director paces every move
 *   more gently;
 * - the voices (lines spoken of lines written): when lines went unspoken, the editor says so before the next film;
 * - the reader's corrections the user made to a story are not guessed at here — only counts the films really produced.
 * Each lesson weighs the recent films more than the old ones (a running average), so one odd film does not decide.
 */
public final class Lessons {
    public int films;
    /** Running shares (0..1) over the films, recent ones weighing more. */
    public float calmShare, cutRemovedShare, closerShare, widerShare, listenerShare;
    /** Running jolts + jumps + picture pops per thousand audited frames. */
    public float roughPer1000;
    /** Running share of lines left without a voice. */
    public float silentShare;
    /** How much each new film weighs in the running averages. */
    static final float NEW = 0.4f;

    public static Lessons read(String text) {
        Lessons l = new Lessons();
        if (text == null) return l;
        for (String line : text.split("\n")) {
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String k = line.substring(0, eq).trim(), v = line.substring(eq + 1).trim();
            try {
                if (k.equals("films")) l.films = Integer.parseInt(v);
                else if (k.equals("calm")) l.calmShare = Float.parseFloat(v);
                else if (k.equals("nocut")) l.cutRemovedShare = Float.parseFloat(v);
                else if (k.equals("closer")) l.closerShare = Float.parseFloat(v);
                else if (k.equals("wider")) l.widerShare = Float.parseFloat(v);
                else if (k.equals("listener")) l.listenerShare = Float.parseFloat(v);
                else if (k.equals("rough")) l.roughPer1000 = Float.parseFloat(v);
                else if (k.equals("silent")) l.silentShare = Float.parseFloat(v);
            } catch (NumberFormatException ignored) { /* a damaged line is forgotten, the rest kept */ }
        }
        return l;
    }

    public String write() {
        return String.format(Locale.US, "films=%d\ncalm=%.4f\nnocut=%.4f\ncloser=%.4f\nwider=%.4f\nlistener=%.4f\nrough=%.4f\nsilent=%.4f\n",
                films, calmShare, cutRemovedShare, closerShare, widerShare, listenerShare, roughPer1000, silentShare);
    }

    static float blend(float old, float now, int films) { return films <= 1 ? now : old + NEW * (now - old); }

    /**
     * After a film: what the user fixed at the shot check (Director.FIX_* per shot), the frame audit's findings and
     * the voices. Call once a film is made.
     */
    public void learn(java.util.Collection<Integer> fixes, int shots, int rough, int framesAudited, int spoken, int lines) {
        films++;
        int calm = 0, remove = 0, closer = 0, wider = 0, listener = 0;
        if (fixes != null) for (Integer f : fixes) {
            if (f == null) continue;
            if (f == Director.FIX_CALM) calm++;
            else if (f == Director.FIX_REMOVE) remove++;
            else if (f == Director.FIX_CLOSER) closer++;
            else if (f == Director.FIX_WIDER) wider++;
            else if (f == Director.FIX_LISTENER) listener++;
        }
        float n = Math.max(1, shots);
        calmShare = blend(calmShare, calm / n, films);
        cutRemovedShare = blend(cutRemovedShare, remove / n, films);
        closerShare = blend(closerShare, closer / n, films);
        widerShare = blend(widerShare, wider / n, films);
        listenerShare = blend(listenerShare, listener / n, films);
        if (framesAudited > 0) roughPer1000 = blend(roughPer1000, rough * 1000f / framesAudited, films);
        if (lines > 0) silentShare = blend(silentShare, (lines - spoken) / (float) lines, films);
    }

    /** The technical director's shot length for the next film, as a factor (1 = as set; up to 1.5 when the user kept asking for calm or no cut). */
    public float shotLengthScale() {
        return 1f + Math.min(0.5f, 1.5f * (calmShare + cutRemovedShare));
    }

    /** The director's pace of moves for the next film (1 = as set; down to 0.7 when films jolted). */
    public float moveScale() {
        return 1f - Math.min(0.3f, 0.1f * roughPer1000);
    }

    /** What the team learned, in words, for the making screen and the production document. */
    public List<String> notes() {
        List<String> out = new ArrayList<String>();
        if (films == 0) return out;
        out.add("The team has learned from " + films + " film" + (films == 1 ? "" : "s") + " made on this phone.");
        float s = shotLengthScale();
        if (s > 1.01f) out.add(String.format(Locale.US, "Technical director: you often asked for calmer shots or fewer cuts — shots are now %.0f%% longer.", (s - 1) * 100));
        float m = moveScale();
        if (m < 0.99f) out.add(String.format(Locale.US, "Director: earlier films jolted (%.1f findings per 1000 frames) — moves are now %.0f%% gentler.", roughPer1000, (1 - m) * 100));
        if (closerShare > 0.1f) out.add("Director: you often asked for closer shots — noted.");
        if (widerShare > 0.1f) out.add("Director: you often asked for wider shots — noted.");
        if (listenerShare > 0.1f) out.add("Editor: you often asked to see the listener — noted.");
        if (silentShare > 0.05f) out.add(String.format(Locale.US, "Editor: %.0f%% of lines went without a voice last time — install the voice in Settings → Text-to-speech, or turn on the internet.", silentShare * 100));
        return out;
    }
}

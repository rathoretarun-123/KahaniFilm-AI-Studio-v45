package com.tarun.kahani.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tarun.kahani.core.ReviewLedger.Issue;
import com.tarun.kahani.core.ReviewLedger.Role;
import com.tarun.kahani.core.ReviewLedger.Severity;

/**
 * v46: the deterministic, offline conference of the four roles over one story, recorded in a {@link ReviewLedger}.
 * <ol>
 * <li>Round 1 — the Reader reports what it inferred (StoryReasoner) and the asset dependencies (AssetDependency);
 *     the Director dispositions every creative finding; the Technical Director files the per-shot problems it could
 *     not correct.</li>
 * <li>Round 2 — the Editor reviews coverage and continuity (scenes without action, lines by unknown speakers, open
 *     continuity risks) and asks the Director for revisions; the Director answers each.</li>
 * <li>Round 3 — the Technical Director's second pass: its blockers stay unless they were actually fixed.</li>
 * </ol>
 * The cloud conference ({@code ScriptAI.productionConference}) is normalised into the same ledger by
 * {@link #fromCloud(ReviewLedger, Object)} so the audit trail has one shape whichever way the review ran.
 */
public final class ReviewConference {
    private ReviewConference() {}

    /**
     * @param st         the story
     * @param cast       the story's manifest (cast.txt), may be null
     * @param candidates library candidates (name → confidence), may be null
     * @param generated  manifest keys the studio made and the user accepted, may be null
     * @param asked      pending upload requests, may be null
     * @param technical  shot id → problems the Technical Director could not correct, may be null
     */
    public static ReviewLedger run(Story st, String cast, Map<String, Float> candidates, Set<String> generated, Set<String> asked, Map<String, List<String>> technical) {
        ReviewLedger l = new ReviewLedger(st == null ? "" : st.title);
        if (st == null) return l;
        // ---- round 1: the Reader
        for (StoryReasoner.Finding f : StoryReasoner.review(st)) {
            Severity sev = "HIGH".equalsIgnoreCase(f.confidence) ? Severity.MAJOR : "MEDIUM".equalsIgnoreCase(f.confidence) ? Severity.MINOR : Severity.NOTE;
            String ref = sceneOf(f.observation);
            l.open(Role.READER, Role.DIRECTOR, ref.length() > 0 ? "scene" : "story", ref, sev, f.observation, "reader inference (" + f.category + ", confidence " + f.confidence + ")", f.inference, "the staging follows the inference or the Director records why not");
        }
        List<AssetDependency.Dep> deps = AssetDependency.classify(st, cast, candidates, generated, asked);
        for (AssetDependency.Dep d : deps) {
            String kind = d.kind.equals("character") ? "character" : d.kind.equals("place") ? "location" : "prop";
            switch (d.state) {
                case MISSING_ESSENTIAL:
                case BLOCKED:
                    l.open(Role.READER, Role.READER, kind, d.key, Severity.BLOCKER, "no reference for " + d.label + " (" + d.state + ")", d.reason + (d.scenes.isEmpty() ? "" : "; scenes " + d.scenes), "ask the user to upload or choose a picture; never invent an essential " + kind, "a picture of " + d.label + " is in the manifest");
                    break;
                case AVAILABLE_UNMATCHED:
                    l.open(Role.READER, Role.READER, kind, d.key, Severity.MAJOR, "a library picture may be " + d.label + " but is unconfirmed", d.reason, "show the candidate to the user for confirmation", "the user confirms or rejects the candidate");
                    break;
                case MISSING_NONESSENTIAL:
                    l.open(Role.READER, Role.DIRECTOR, kind, d.key, Severity.NOTE, "no picture of " + d.label + "; not essential", d.reason, "stage without it, or draw an insert if the story gains by it", "none");
                    break;
                default:
                    break;
            }
        }
        // ---- round 1: the Director dispositions every creative finding (never a blocker)
        for (Issue i : l.open(Role.DIRECTOR)) {
            if (i.severity == Severity.BLOCKER) continue;
            if (i.refKind.equals("prop") || i.refKind.equals("location") || i.refKind.equals("character")) l.decide(i.id, "accept", "staged without the picture; an insert is drawn where the action needs it");
            else l.decide(i.id, i.severity == Severity.NOTE ? "accept" : "adapt", "staging follows the Reader's inference; the author's text stays canon");
        }
        // ---- round 1: the Technical Director
        if (technical != null) for (Map.Entry<String, List<String>> e : technical.entrySet()) for (String p : e.getValue()) {
            boolean blocker = p.contains("head cut") || p.contains("feet at") || p.contains("no reference image") || p.contains("more than one action") || p.contains("forbidden");
            l.open(Role.TECHNICAL_DIRECTOR, Role.TECHNICAL_DIRECTOR, "shot", e.getKey(), blocker ? Severity.BLOCKER : Severity.MAJOR, p, "TechnicalDirector.validate after correct()", "re-stage the shot by the protocol's rules", "TechnicalDirector.validate returns nothing for the shot");
        }
        // ---- round 2: the Editor
        if (l.nextRound()) {
            if (st.scenes.isEmpty()) l.open(Role.EDITOR, Role.READER, "story", "", Severity.BLOCKER, "the screenplay has no scenes", "ScriptParser found none", "ask the author for a scene heading", "at least one scene parses");
            for (Story.Scene sc : st.scenes) {
                if (sc.beats.isEmpty()) l.open(Role.EDITOR, Role.DIRECTOR, "scene", String.valueOf(sc.number), Severity.MAJOR, "scene " + sc.number + " has no action or dialogue", "no beats parsed", "cut the scene or give it an establishing beat", "the scene has a shot the audience can read");
                int line = 0;
                for (Story.Beat b : sc.beats) {
                    if (b.type != Story.Beat.DIALOGUE) continue;
                    line++;
                    if (b.speaker == null && !b.narrator)
                        l.open(Role.EDITOR, Role.READER, "line", sc.number + "." + line, Severity.MAJOR, "a line by an unknown speaker: " + b.speakerRaw, Bible.oneLine(b.text), "resolve the speaker to a cast member or mark it as narration", "every dialogue line has a speaker in the cast");
                    else if (b.speaker != null && b.speaker.voiceOnly && !b.offScreen)
                        l.open(Role.EDITOR, Role.DIRECTOR, "line", sc.number + "." + line, Severity.MINOR, b.speaker.shown() + " speaks but is voice-only", Bible.oneLine(b.text), "play the line off-screen over a reaction shot", "the line is heard over a listener");
                }
            }
            if (st.scenes.size() == 1) l.open(Role.EDITOR, Role.DIRECTOR, "story", "", Severity.NOTE, "one scene only: the film needs a visible beginning, middle and end inside it", "scene count 1", "open on an establishing shot and close on a settled frame", "the first and last shots read as opening and ending");
            // continuity risks the Reader raised and the Director adapted are checked, not re-litigated
            for (Issue i : l.open(Role.DIRECTOR)) if (i.severity != Severity.BLOCKER) l.decide(i.id, "adapt", "editor's note taken into the cut");
        }
        // ---- round 3: the Technical Director's second pass
        if (l.nextRound()) {
            for (Issue i : l.open(Role.TECHNICAL_DIRECTOR)) l.log.add("r3 TECHNICAL_DIRECTOR re-checked " + i.id + ": still open");
            for (Issue i : l.open(Role.READER)) l.log.add("r3 READER: " + i.id + " waits on the user");
        }
        return l;
    }

    private static String sceneOf(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[Ss]cene (\\d+)").matcher(s == null ? "" : s);
        return m.find() ? m.group(1) : "";
    }

    /**
     * Normalises a cloud production-conference report (the JSON map {@code ScriptAI.productionConference} returns)
     * into ledger issues: technical blockers and failed QC rows become Technical Director BLOCKERs, asset requests
     * and human questions Reader BLOCKERs, role disagreements Director MAJORs closed by the recorded decisions.
     */
    public static ReviewLedger fromCloud(ReviewLedger l, Object report) {
        if (!(report instanceof Map)) return l;
        for (Object o : Json.arr(report, "technical_blockers")) l.open(Role.TECHNICAL_DIRECTOR, Role.TECHNICAL_DIRECTOR, "story", "", Severity.BLOCKER, text(o), "cloud conference technical_blockers", "", "the Technical Director's second pass lists it as resolved");
        Object qc = ((Map<?, ?>) report).get("qc_matrix");
        if (qc instanceof Map) for (Map.Entry<?, ?> e : ((Map<?, ?>) qc).entrySet()) {
            String v = String.valueOf(e.getValue()).toUpperCase(java.util.Locale.ROOT);
            if (v.startsWith("FAIL")) l.open(Role.TECHNICAL_DIRECTOR, Role.TECHNICAL_DIRECTOR, "story", "", Severity.BLOCKER, "QC " + e.getKey() + ": FAIL", "cloud qc_matrix", "", "QC row passes");
            else if (v.contains("HUMAN")) l.open(Role.TECHNICAL_DIRECTOR, Role.DIRECTOR, "story", "", Severity.MAJOR, "QC " + e.getKey() + ": needs human review", "cloud qc_matrix", "", "a human reviewed it");
        }
        for (Object o : Json.arr(report, "asset_requests")) l.open(Role.READER, Role.READER, "asset", Json.str(o, "name", ""), Severity.BLOCKER, "asset requested: " + text(o), "cloud conference asset_requests", "ask the user to upload it", "the asset is in the manifest");
        for (Object o : Json.arr(report, "human_questions")) l.open(Role.READER, Role.READER, "story", "", Severity.BLOCKER, "question for the filmmaker: " + text(o), "cloud conference human_questions", "", "answered by the user");
        for (Object o : Json.arr(report, "role_disagreements")) l.open(Role.EDITOR, Role.DIRECTOR, "story", "", Severity.MAJOR, "disagreement: " + text(o), "cloud conference role_disagreements", "", "the Director records a decision");
        for (Object o : Json.arr(report, "director_decisions")) {
            String issue = Json.str(o, "issue", text(o)), decision = Json.str(o, "decision", "accept"), reason = Json.str(o, "reason", "");
            boolean matched = false;
            for (Issue i : l.open(Role.DIRECTOR)) if (i.severity != Severity.BLOCKER && (issue.length() == 0 || i.finding.contains(issue) || issue.contains(i.finding.replace("disagreement: ", "")))) { l.decide(i.id, decision, reason); matched = true; break; }
            if (!matched) l.log.add("r" + l.round + " DIRECTOR decision (" + decision + ") on: " + issue + " — " + reason);
        }
        Object ready = ((Map<?, ?>) report).get("ready_to_render");
        if (ready instanceof Boolean && !((Boolean) ready) && l.readyToRender())
            l.open(Role.DIRECTOR, Role.TECHNICAL_DIRECTOR, "story", "", Severity.BLOCKER, "the conference chair set ready_to_render=false without naming a blocker", "cloud report", "re-run the conference or inspect the saved report", "ready_to_render is true with the blocker named");
        return l;
    }

    private static String text(Object o) {
        if (o instanceof String) return (String) o;
        if (o instanceof Map) {
            StringBuilder b = new StringBuilder();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) { if (b.length() > 0) b.append("; "); b.append(e.getKey()).append(": ").append(e.getValue() instanceof String ? e.getValue() : Json.write(e.getValue())); }
            return b.length() > 400 ? b.substring(0, 400) + "…" : b.toString();
        }
        return String.valueOf(o);
    }
}

package com.tarun.kahani.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Post-render visual conference for one actual shot frame.
 *
 * The production conference plans the film before rendering; this class is deliberately separate and
 * examines the rendered preview pixels. The director resolves artistic disputes, but cannot overrule
 * a technical hard blocker. Invalid or unavailable visual-review output is never represented as PASS.
 */
public final class ShotVisualConference {
    private ShotVisualConference() {}

    public static final class Review {
        public int shotNumber;
        public boolean valid;
        public boolean readyForRender;
        public String readerStatus = "NEEDS_HUMAN_REVIEW";
        public String readerNotes = "";
        public String editorStatus = "NEEDS_HUMAN_REVIEW";
        public String editorNotes = "";
        public String technicalStatus = "NEEDS_HUMAN_REVIEW";
        public String technicalNotes = "";
        public String directorDecision = "HOLD";
        public String directorResolution = "";
        public String visualEvidence = "";
        public final List<String> hardBlockers = new ArrayList<String>();
        public String error = "";

        public String summary() {
            StringBuilder b = new StringBuilder();
            b.append("Reader ").append(readerStatus).append("; Editor ").append(editorStatus)
             .append("; Technical Director ").append(technicalStatus)
             .append("; Director ").append(directorDecision).append(".");
            if (!directorResolution.isEmpty()) b.append(" Decision: ").append(directorResolution);
            if (!technicalNotes.isEmpty()) b.append(" Technical: ").append(technicalNotes);
            if (!hardBlockers.isEmpty()) b.append(" BLOCKERS: ").append(hardBlockers);
            if (!error.isEmpty()) b.append(" Review status: ").append(error);
            return b.toString();
        }
    }

    /**
     * Inspects the supplied JPEG using the configured vision service. This is a single structured
     * conference response to limit network cost; pre-production role deliberations remain separate
     * calls in ScriptAI.productionConference().
     */
    public static Review inspect(Cloud cloud, int shotNumber, String shotDescription,
                                 String continuityContext, byte[] jpeg) throws IOException {
        if (shotNumber < 1) throw new IllegalArgumentException("shotNumber must be positive");
        Review unavailable = new Review();
        unavailable.shotNumber = shotNumber;
        if (cloud == null || jpeg == null || jpeg.length == 0) {
            unavailable.error = "Rendered frame or online vision service unavailable";
            return unavailable;
        }
        String prompt = prompt(shotNumber, shotDescription, continuityContext);
        String raw = cloud.vision(prompt, jpeg);
        return parse(raw, shotNumber);
    }

    /** Pure parser/validator, kept separate so contract behavior can be regression-tested offline. */
    public static Review parse(String raw, int expectedShot) {
        Review r = new Review();
        r.shotNumber = expectedShot;
        Object parsed = Cloud.jsonIn(raw);
        if (!(parsed instanceof Map)) {
            r.error = "Invalid JSON from visual conference";
            return r;
        }
        Map<?, ?> m = (Map<?, ?>) parsed;
        Object n = m.get("shot_number");
        if (!(n instanceof Number) || ((Number) n).intValue() != expectedShot) {
            r.error = "Shot number missing or mismatched";
            return r;
        }
        r.readerStatus = status(m.get("reader_status"));
        r.readerNotes = text(m.get("reader_notes"));
        r.editorStatus = status(m.get("editor_status"));
        r.editorNotes = text(m.get("editor_notes"));
        r.technicalStatus = status(m.get("technical_director_status"));
        r.technicalNotes = text(m.get("technical_director_notes"));
        r.directorDecision = decision(m.get("director_decision"));
        r.directorResolution = text(m.get("director_resolution"));
        r.visualEvidence = text(m.get("visual_evidence"));
        Object blockers = m.get("hard_blockers");
        if (blockers instanceof List) for (Object item : (List<?>) blockers) {
            String s = text(item);
            if (!s.isEmpty()) r.hardBlockers.add(s);
        }
        boolean statusesKnown = knownRoleStatus(r.readerStatus) && knownRoleStatus(r.editorStatus)
                && knownTechnicalStatus(r.technicalStatus) && knownDecision(r.directorDecision);
        r.valid = statusesKnown && !r.visualEvidence.trim().isEmpty();
        boolean unresolvedCreativeDispute =
                (!"PASS".equals(r.readerStatus) || !"PASS".equals(r.editorStatus))
                && r.directorResolution.trim().isEmpty();
        // Director has final say on artistic disputes; technical blockers remain non-overridable.
        r.readyForRender = r.valid && "PASS".equals(r.technicalStatus)
                && "APPROVE".equals(r.directorDecision) && r.hardBlockers.isEmpty()
                && !unresolvedCreativeDispute;
        if (!r.valid && r.error.isEmpty()) r.error = "Required review fields are missing or invalid";
        return r;
    }

    public static String prompt(int shotNumber, String shotDescription, String continuityContext) {
        String desc = limit(text(shotDescription), 1200);
        String continuity = limit(text(continuityContext), 2200);
        return "You are conducting the post-render visual review of one actual KahaniFilm shot. Inspect the attached rendered JPEG, not just the text. Shot number: "
                + shotNumber + ". Shot description: " + desc + "\nContinuity/script constraints: " + continuity
                + "\nConduct an independent four-role conference. Reader checks whether the visible action communicates the intended beat and emotion. "
                + "Editor checks framing, readable action, pacing potential, cut continuity and whether the image gives the sequence useful coverage. "
                + "Technical Director checks visible anatomy, face/identity evidence, hands, object contact, feet/grounding, geometry, lighting, clipping and obvious render defects. "
                + "Director makes the final creative decision and resolves Reader/Editor artistic disagreements with an explicit reason. The Director cannot overrule a technical hard blocker. "
                + "Do not claim identity or continuity is verified unless the supplied image/context actually proves it. Do not infer motion or lip-sync from a single still; mark those NEEDS_HUMAN_REVIEW. "
                + "Return ONLY JSON: {shot_number:number,reader_status:PASS|REVISE|NEEDS_HUMAN_REVIEW,reader_notes:string,"
                + "editor_status:PASS|REVISE|NEEDS_HUMAN_REVIEW,editor_notes:string,"
                + "technical_director_status:PASS|FAIL|NEEDS_HUMAN_REVIEW,technical_director_notes:string,"
                + "hard_blockers:[string],director_decision:APPROVE|REVISE|HOLD,director_resolution:string,visual_evidence:string}. "
                + "visual_evidence must name concrete visible evidence, not generic praise. If an artistic role requests a change but the Director keeps the shot, director_resolution must explain why. "
                + "Only set technical_director_status=PASS when no visible technical blocker exists. Never fabricate a successful inspection.";
    }

    private static String status(Object o) {
        String s = text(o).trim().toUpperCase(java.util.Locale.ROOT).replace(' ', '_');
        if ("PASS".equals(s) || "REVISE".equals(s) || "FAIL".equals(s)
                || "NEEDS_HUMAN_REVIEW".equals(s)) return s;
        return "NEEDS_HUMAN_REVIEW";
    }
    private static String decision(Object o) {
        String s = text(o).trim().toUpperCase(java.util.Locale.ROOT);
        return "APPROVE".equals(s) || "REVISE".equals(s) || "HOLD".equals(s) ? s : "HOLD";
    }
    private static boolean knownRoleStatus(String s) {
        return "PASS".equals(s) || "REVISE".equals(s) || "NEEDS_HUMAN_REVIEW".equals(s);
    }
    private static boolean knownTechnicalStatus(String s) {
        return "PASS".equals(s) || "FAIL".equals(s) || "NEEDS_HUMAN_REVIEW".equals(s);
    }
    private static boolean knownDecision(String s) {
        return "APPROVE".equals(s) || "REVISE".equals(s) || "HOLD".equals(s);
    }
    private static String text(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
    private static String limit(String s, int max) { return s.length() <= max ? s : s.substring(0, max) + "…"; }
}

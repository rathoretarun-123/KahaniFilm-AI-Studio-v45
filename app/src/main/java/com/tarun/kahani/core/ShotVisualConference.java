package com.tarun.kahani.core;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Post-storyboard visual conference for every rendered shot keyframe.
 * Reader, Technical Director and Editor inspect the actual image independently. The Director
 * reviews the same image and the three reports last. Creative disputes belong to the Director;
 * an objective Technical Director failure cannot be waived. Invalid reports fail closed.
 */
public final class ShotVisualConference {
    private ShotVisualConference() {}
    public static final int CONTRACT_VERSION = 1;

    public static final class Result {
        public final String shotId;
        public final boolean approved;
        public final String reportJson;
        public final String reason;
        Result(String id, boolean ok, String json, String why) {
            shotId = id; approved = ok; reportJson = json; reason = why;
        }
    }

    /** Validate a saved report without a model call; also used by regression tests. */
    public static Result validate(String expectedShotId, String reportJson) {
        Object parsed = Json.parseLoose(reportJson);
        if (!(parsed instanceof Map)) return rejected(expectedShotId, reportJson, "Malformed visual conference JSON.");
        Map<?, ?> root = (Map<?, ?>) parsed;
        if (!expectedShotId.equals(text(root.get("shot_id"))))
            return rejected(expectedShotId, reportJson, "Shot ID mismatch; review cannot be reused for another shot.");
        if (number(root.get("contract_version")) != CONTRACT_VERSION)
            return rejected(expectedShotId, reportJson, "Unsupported visual conference contract version.");
        Object roleObj = root.get("role_reviews");
        if (!(roleObj instanceof Map)) return rejected(expectedShotId, reportJson, "All three independent role reviews are required.");
        Map<?, ?> roles = (Map<?, ?>) roleObj;
        for (String role : new String[]{"reader", "technical_director", "editor"}) {
            Object raw = roles.get(role);
            if (!(raw instanceof Map)) return rejected(expectedShotId, reportJson, "Missing visual review from " + role + ".");
            Map<?, ?> review = (Map<?, ?>) raw;
            if (!yes(review.get("visual_inspection")))
                return rejected(expectedShotId, reportJson, role + " did not confirm visual inspection.");
            if (text(review.get("evidence")).length() < 8)
                return rejected(expectedShotId, reportJson, role + " supplied no image-grounded evidence.");
            String decision = text(review.get("decision")).toLowerCase(java.util.Locale.ROOT);
            if (!("pass".equals(decision) || "revise".equals(decision) || "block".equals(decision)))
                return rejected(expectedShotId, reportJson, role + " supplied an invalid decision.");
            if ("technical_director".equals(role) && !"pass".equals(decision))
                return rejected(expectedShotId, reportJson, "Technical Director has an unresolved objective blocker.");
            if (!"technical_director".equals(role) && !"pass".equals(decision)
                    && !hasDisposition(root.get("director_dispositions"), role))
                return rejected(expectedShotId, reportJson, "Director did not explicitly disposition " + role + "'s concern.");
        }
        Object directorObj = root.get("director_review");
        if (!(directorObj instanceof Map)) return rejected(expectedShotId, reportJson, "Director's final visual decision is missing.");
        Map<?, ?> director = (Map<?, ?>) directorObj;
        if (!yes(director.get("visual_inspection")))
            return rejected(expectedShotId, reportJson, "Director did not inspect the shot image.");
        if (text(director.get("evidence")).length() < 8)
            return rejected(expectedShotId, reportJson, "Director supplied no image-grounded evidence.");
        if (!"director".equalsIgnoreCase(text(root.get("final_decision_by"))))
            return rejected(expectedShotId, reportJson, "Only the Director may issue final creative approval.");
        if (!"approve".equalsIgnoreCase(text(director.get("final_decision"))))
            return rejected(expectedShotId, reportJson, "Director did not approve this shot.");
        String reason = text(director.get("reason"));
        if (reason.length() < 8) return rejected(expectedShotId, reportJson, "Director's approval needs a reason.");
        return new Result(expectedShotId, true, reportJson, reason);
    }

    /**
     * Runs separate visual reviews against the same shot image. The Director then reviews the
     * image and all findings. Any failed or malformed Technical Director review blocks approval.
     */
    public static Result inspect(Cloud cloud, String shotId, String shotDescription,
                                 String previousShotDescription, String nextShotDescription,
                                 byte[] jpeg) throws IOException {
        if (cloud == null || jpeg == null || jpeg.length == 0)
            return rejected(shotId, "", "No cloud reviewer or shot image available.");
        Map<String, Object> roles = new LinkedHashMap<String, Object>();
        roles.put("reader", inspectRole(cloud, "STORY READER", "reader", shotId, shotDescription,
                "Check whether the visible action communicates the story beat and intended emotion; identify missing visible causal steps or props without inventing plot.", jpeg));
        roles.put("technical_director", inspectRole(cloud, "TECHNICAL DIRECTOR", "technical_director", shotId, shotDescription,
                "Check objective defects: identity/reference mismatch, broken anatomy or mesh, clipping, floating feet, missing contact/shadows, prop contact/state, impossible physics, unstable geometry, lighting discontinuity, legibility and safe framing. Any clear objective defect is revise/block.", jpeg));
        roles.put("editor", inspectRole(cloud, "FILM EDITOR", "editor", shotId, shotDescription,
                "Check framing, visual rhythm, reaction readability, redundancy and continuity using adjacent shot descriptions. Distinguish visible evidence from uncertainty.", jpeg));
        String directorPrompt = "You are the DIRECTOR and final creative authority. Independently inspect the attached actual shot image, then consider the three specialist reports. You may overrule Reader or Editor artistic preferences only with an explicit disposition and reason. You may NOT overrule a Technical Director failure. Return ONLY JSON: {visual_inspection:true, final_decision:approve|reject, evidence:string describing visible pixels, reason:string, director_dispositions:[{role:reader|editor, decision:accept|reject|adapt, reason:string}], repair_instructions:[string]}. Reject if the shot is not ready.\nSHOT ID: "
                + safe(shotId) + "\nSHOT INTENT: " + safe(shotDescription)
                + "\nPREVIOUS SHOT INTENT: " + safe(previousShotDescription)
                + "\nNEXT SHOT INTENT: " + safe(nextShotDescription)
                + "\nINDEPENDENT REVIEWS: " + Json.write(roles);
        Map<String, Object> director = parseReview(cloud.vision(directorPrompt, jpeg), "director");
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("contract_version", CONTRACT_VERSION);
        root.put("shot_id", shotId);
        root.put("role_reviews", roles);
        root.put("director_review", director);
        root.put("director_dispositions", director.get("director_dispositions"));
        root.put("final_decision_by", "director");
        root.put("shot_description", shotDescription == null ? "" : shotDescription);
        return validate(shotId, Json.write(root));
    }

    private static Map<String, Object> inspectRole(Cloud cloud, String roleName, String roleKey,
                                                    String id, String description, String responsibility,
                                                    byte[] jpeg) throws IOException {
        String prompt = "You are the independent " + roleName + " in a shot-by-shot animation review. Inspect the attached actual rendered keyframe; do not infer visual details that cannot be seen. " + responsibility
                + " Return ONLY JSON: {visual_inspection:true, decision:pass|revise|block, evidence:string describing observable pixels, findings:[string], required_fixes:[string]}. A pass requires specific image-grounded evidence and no material issue.\nSHOT ID: "
                + safe(id) + "\nSHOT INTENT: " + safe(description) + "\nROLE KEY: " + roleKey;
        return parseReview(cloud.vision(prompt, jpeg), roleName);
    }

    private static Map<String, Object> parseReview(String raw, String role) {
        Object parsed = Json.parseLoose(raw);
        if (!(parsed instanceof Map)) {
            Map<String, Object> invalid = new LinkedHashMap<String, Object>();
            invalid.put("visual_inspection", Boolean.FALSE);
            invalid.put("decision", "block");
            invalid.put("final_decision", "reject");
            invalid.put("evidence", "");
            invalid.put("reason", role + " response was not valid JSON.");
            return invalid;
        }
        Map<?, ?> src = (Map<?, ?>) parsed;
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        for (Map.Entry<?, ?> e : src.entrySet()) if (e.getKey() instanceof String) out.put((String) e.getKey(), e.getValue());
        return out;
    }

    private static boolean hasDisposition(Object raw, String role) {
        if (!(raw instanceof List)) return false;
        for (Object item : (List<?>) raw) if (item instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) item;
            String decision = text(m.get("decision")).toLowerCase(java.util.Locale.ROOT);
            if (role.equalsIgnoreCase(text(m.get("role")))
                    && ("accept".equals(decision) || "reject".equals(decision) || "adapt".equals(decision))
                    && text(m.get("reason")).length() >= 8) return true;
        }
        return false;
    }
    private static Result rejected(String id, String json, String reason) { return new Result(id, false, json, reason); }
    private static boolean yes(Object value) { return value instanceof Boolean && ((Boolean) value).booleanValue(); }
    private static double number(Object value) { return value instanceof Number ? ((Number) value).doubleValue() : -1; }
    private static String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private static String safe(String s) { return s == null ? "" : s.replace("\n", " ").replace("\r", " ").trim(); }
}

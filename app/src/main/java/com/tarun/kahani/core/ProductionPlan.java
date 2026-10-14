package com.tarun.kahani.core;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/** Validates a production conference and applies only explicitly approved, high-confidence scene inserts. */
public final class ProductionPlan {
    private ProductionPlan() {}
    public static final class Result {
        public int scenesAdded;
        public int assetsAdded;
        public boolean valid;
        public boolean readyToRender = true;
        public final List<String> warnings = new ArrayList<String>();
    }
    /** Human-readable blocker summary shown when the production chair says the film is not ready. */
    public static String blockingSummary(String report) {
        Object parsed = Json.parseLoose(report);
        if (!(parsed instanceof Map)) return "The production report is invalid. Re-run the reader/director review or inspect the saved report.";
        Map<?, ?> root = (Map<?, ?>) parsed;
        StringBuilder b = new StringBuilder();
        appendField(b, "Questions", root.get("human_questions"));
        appendField(b, "Technical blockers", root.get("technical_blockers"));
        appendField(b, "Asset requests", root.get("asset_requests"));
        appendField(b, "Role disagreements", root.get("role_disagreements"));
        String s = b.toString().trim();
        if (s.isEmpty()) s = "The production chair marked this plan not ready. Review the saved production report.";
        return s.length() > 1800 ? s.substring(0, 1800) + "…" : s;
    }
    private static void appendField(StringBuilder b, String label, Object value) {
        if (value == null) return;
        String s = value instanceof String ? ((String) value).trim() : Json.write(value);
        if (s.isEmpty() || "[]".equals(s) || "{}".equals(s)) return;
        if (b.length() > 0) b.append("\\n");
        b.append(label).append(": ").append(s);
    }

    public static Result apply(Story story, String report) {
        Result out = new Result();
        Object parsed = Json.parseLoose(report);
        if (!(parsed instanceof Map)) { out.warnings.add("Invalid production report"); return out; }
        out.valid = true;
        Map<?, ?> root = (Map<?, ?>) parsed;
        if (!"director".equalsIgnoreCase(text(root.get("final_decision_by")))) {
            out.readyToRender = false;
            out.warnings.add("Production plan lacks explicit final sign-off by the director.");
            return out;
        }
        Object contract = root.get("conference_contract_valid");
        Object contractVersion = root.get("conference_contract_version");
        if (contractVersion instanceof Number && ((Number) contractVersion).intValue() >= 1) {
            Object shots = root.get("final_shot_plan");
            Object decisions = root.get("director_decisions");
            Object finalDecision = root.get("director_final_decision");
            if (!(contract instanceof Boolean) || !((Boolean) contract).booleanValue()
                    || !(shots instanceof List) || ((List<?>) shots).isEmpty()
                    || !(decisions instanceof List) || !(finalDecision instanceof String)
                    || ((String) finalDecision).trim().isEmpty()) {
                out.readyToRender = false;
                out.warnings.add("Conference hand-off failed validation: director sign-off, decisions, and a non-empty final shot plan are required.");
                return out;
            }
        }
        Object ready = root.get("ready_to_render");
        if (ready instanceof Boolean) out.readyToRender = ((Boolean) ready).booleanValue();
        else { out.readyToRender = false; out.warnings.add("Production chair omitted the required ready_to_render decision."); }
        if (!out.readyToRender) return out;
        // Apply safe scene-indexed director cues to the Story actually consumed by Director.
        // Authored dialogue and scene order are never replaced by this overlay.
        Object shotPlan = root.get("final_shot_plan");
        if (shotPlan instanceof List) {
            int[] perScene = new int[story.scenes.size()];
            int applied = 0;
            for (Object raw : (List<?>) shotPlan) {
                if (applied >= 60 || !(raw instanceof Map)) continue;
                Map<?, ?> shot = (Map<?, ?>) raw;
                int sceneNo = (int) number(shot.get("scene"));
                if (sceneNo < 1 || sceneNo > story.scenes.size() || perScene[sceneNo - 1] >= 4) continue;
                String action = firstText(shot, "visual_action", "action", "description", "beat", "purpose");
                String framing = firstText(shot, "framing", "shot_type", "camera");
                String emotion = firstText(shot, "performance", "emotion", "reaction");
                String continuity = firstText(shot, "continuity", "continuity_lock");
                StringBuilder cue = new StringBuilder("Director-approved shot plan: ");
                if (!framing.isEmpty()) cue.append("framing=").append(framing).append("; ");
                if (!action.isEmpty()) cue.append("action=").append(action).append("; ");
                if (!emotion.isEmpty()) cue.append("performance=").append(emotion).append("; ");
                if (!continuity.isEmpty()) cue.append("continuity=").append(continuity).append("; ");
                String direction = cue.toString().trim();
                if (direction.endsWith(";")) direction = direction.substring(0, direction.length() - 1);
                if (direction.equals("Director-approved shot plan:")) continue;
                if (direction.length() > 700) direction = direction.substring(0, 700);
                story.scenes.get(sceneNo - 1).beats.add(Story.Beat.direction(direction));
                perScene[sceneNo - 1]++;
                applied++;
            }
            if (applied > 0) out.warnings.add("Applied " + applied + " director shot-plan cues to the actual scene data consumed by Director.");
            else if (contractVersion instanceof Number && ((Number) contractVersion).intValue() >= 1) {
                out.readyToRender = false;
                out.warnings.add("The director's shot plan contains no usable scene-indexed visual directions; render blocked rather than silently ignoring the final cut.");
                return out;
            }
        }
        Object assetsObj = root.get("approved_assets");
        if (assetsObj instanceof List) for (Object raw : (List<?>) assetsObj) {
            if (!(raw instanceof Map)) continue;
            Map<?, ?> asset = (Map<?, ?>) raw;
            String category = text(asset.get("category")).toLowerCase(java.util.Locale.ROOT);
            String name = text(asset.get("name"));
            String description = text(asset.get("description"));
            if (name.isEmpty() || number(asset.get("confidence")) < 0.85 || yes(asset.get("requires_approval"))) continue;
            if ("place".equals(category) || "background".equals(category)) {
                if (!hasPlace(story, name)) { Story.PlaceDef place = new Story.PlaceDef(); place.name = name; place.description = description; story.places.add(place); out.assetsAdded++; }
            } else if ("prop".equals(category)) {
                if (!hasThing(story, name)) { Story.PlaceDef prop = new Story.PlaceDef(); prop.name = name; prop.description = description; story.things.add(prop); out.assetsAdded++; }
            }
        }
        Object inserts = root.get("scene_insertions");
        if (!(inserts instanceof List)) return out;
        // Apply in original-scene order even when the model returns proposals out of order.
        final List<?> proposals = (List<?>) inserts;
        final int originalSceneCount = story.scenes.size();
        final java.util.Set<String> insertedKeys = new java.util.HashSet<String>();
        for (int after = 1; after <= originalSceneCount && out.scenesAdded < 8; after++) {
            for (Object raw : proposals) {
                if (out.scenesAdded >= 8) break;
                if (!(raw instanceof Map)) continue;
                Map<?, ?> item = (Map<?, ?>) raw;
                if ((int) number(item.get("after_scene")) != after
                        || !yes(item.get("auto_apply")) || number(item.get("confidence")) < 0.85) continue;
                String title = text(item.get("title"));
                String setting = text(item.get("setting"));
                String purpose = text(item.get("purpose"));
                if (title.isEmpty() || setting.isEmpty() || !knownPlace(story, setting)) continue;
                List<Story.CharacterDef> cast = new ArrayList<Story.CharacterDef>();
                Object names = item.get("cast");
                boolean ok = names instanceof List;
                if (names instanceof List) for (Object name : (List<?>) names) {
                    String characterName = text(name);
                    Story.CharacterDef found = character(story, characterName);
                    if (characterName.isEmpty() || found == null || found.voiceOnly) { ok = false; break; }
                    cast.add(found);
                }
                Object actions = item.get("actions");
                if (!ok || !(actions instanceof List) || ((List<?>) actions).isEmpty() || ((List<?>) actions).size() > 3) continue;
                StringBuilder keyBuilder = new StringBuilder(title.toLowerCase(java.util.Locale.ROOT))
                        .append('|').append(setting.toLowerCase(java.util.Locale.ROOT)).append('|')
                        .append(after).append('|').append(purpose.toLowerCase(java.util.Locale.ROOT));
                for (Object action : (List<?>) actions) keyBuilder.append('|').append(text(action).toLowerCase(java.util.Locale.ROOT));
                if (!insertedKeys.add(keyBuilder.toString())) continue;
                Story.Scene scene = new Story.Scene();
                scene.title = title; scene.setting = setting;
                if (!purpose.isEmpty()) scene.beats.add(Story.Beat.direction("Visual purpose: " + purpose));
                for (Object action : (List<?>) actions) {
                    String value = text(action);
                    if (value.length() > 500) value = value.substring(0, 500);
                    if (!value.isEmpty()) scene.beats.add(Story.Beat.direction(value));
                }
                if (!cast.isEmpty()) {
                    StringBuilder present = new StringBuilder("Present: ");
                    for (int ci = 0; ci < cast.size(); ci++) { if (ci > 0) present.append(", "); present.append(cast.get(ci).displayName); }
                    scene.beats.add(0, Story.Beat.direction(present.toString()));
                }
                if (scene.beats.isEmpty()) continue;
                story.scenes.add(after + out.scenesAdded, scene);
                out.scenesAdded++;
            }
        }
        for (int i = 0; i < story.scenes.size(); i++) {
            Story.Scene scene = story.scenes.get(i);
            scene.number = i + 1;
            scene.heading = (story.hindi ? "दृश्य " : "Scene ") + (i + 1);
        }
        return out;
    }
    private static boolean hasPlace(Story story, String name) {
        for (Story.PlaceDef p : story.places) if (p.name.equalsIgnoreCase(name)) return true;
        return false;
    }
    private static boolean hasThing(Story story, String name) {
        for (Story.PlaceDef p : story.things) if (p.name.equalsIgnoreCase(name)) return true;
        return false;
    }
    private static Story.CharacterDef character(Story s, String n) {
        for (Story.CharacterDef c : s.characters) {
            if (c.displayName.equalsIgnoreCase(n) || c.fullName.equalsIgnoreCase(n)) return c;
            for (String a : c.aliases) if (a.equalsIgnoreCase(n)) return c;
        }
        return null;
    }
    private static boolean knownPlace(Story s, String text) {
        for (Story.PlaceDef p : s.places) if (!p.name.isEmpty() && text.toLowerCase(java.util.Locale.ROOT).contains(p.name.toLowerCase(java.util.Locale.ROOT))) return true;
        return false;
    }
    private static String firstText(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            String value = text(map.get(key));
            if (!value.isEmpty()) return value;
        }
        return "";
    }
    private static boolean yes(Object o) { return o instanceof Boolean && ((Boolean) o).booleanValue(); }
    private static double number(Object o) { return o instanceof Number ? ((Number) o).doubleValue() : 0; }
    private static String text(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
}

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
    public static Result apply(Story story, String report) {
        Result out = new Result();
        Object parsed = Json.parseLoose(report);
        if (!(parsed instanceof Map)) { out.warnings.add("Invalid production report"); return out; }
        out.valid = true;
        Map<?, ?> root = (Map<?, ?>) parsed;
        Object ready = root.get("ready_to_render");
        if (ready instanceof Boolean) out.readyToRender = ((Boolean) ready).booleanValue();
        Object inserts = root.get("scene_insertions");
        if (!(inserts instanceof List)) return out;
        int total = 0;
        for (Object raw : (List<?>) inserts) {
            if (total >= 8 || !(raw instanceof Map)) break;
            Map<?, ?> item = (Map<?, ?>) raw;
            if (!yes(item.get("auto_apply")) || number(item.get("confidence")) < 0.85) continue;
            int after = (int) number(item.get("after_scene"));
            if (after < 1 || after > story.scenes.size()) continue;
            String title = text(item.get("title"));
            String setting = text(item.get("setting"));
            if (title.isEmpty() || setting.isEmpty() || !knownPlace(story, setting)) continue;
            List<Story.CharacterDef> cast = new ArrayList<Story.CharacterDef>();
            Object names = item.get("cast");
            boolean ok = true;
            if (names instanceof List) for (Object name : (List<?>) names) {
                Story.CharacterDef found = character(story, text(name));
                if (found == null || found.voiceOnly) { ok = false; break; }
                cast.add(found);
            }
            Object actions = item.get("actions");
            if (!ok || !(actions instanceof List) || ((List<?>) actions).isEmpty() || ((List<?>) actions).size() > 3) continue;
            Story.Scene scene = new Story.Scene();
            scene.title = title; scene.setting = setting;
            String purpose = text(item.get("purpose"));
            if (!purpose.isEmpty()) scene.beats.add(Story.Beat.direction("Visual purpose: " + purpose));
            for (Object action : (List<?>) actions) {
                String value = text(action);
                if (value.length() > 500) value = value.substring(0, 500);
                if (!value.isEmpty()) scene.beats.add(Story.Beat.direction(value));
            }
            if (scene.beats.isEmpty()) continue;
            story.scenes.add(after, scene);
            out.scenesAdded++; total++;
        }
        for (int i = 0; i < story.scenes.size(); i++) {
            Story.Scene scene = story.scenes.get(i);
            scene.number = i + 1;
            scene.heading = (story.hindi ? "दृश्य " : "Scene ") + (i + 1);
        }
        return out;
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
    private static boolean yes(Object o) { return o instanceof Boolean && ((Boolean) o).booleanValue(); }
    private static double number(Object o) { return o instanceof Number ? ((Number) o).doubleValue() : 0; }
    private static String text(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
}

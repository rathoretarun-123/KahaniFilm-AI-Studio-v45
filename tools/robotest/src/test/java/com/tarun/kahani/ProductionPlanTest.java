package com.tarun.kahani;

import static org.junit.Assert.*;
import com.tarun.kahani.core.ProductionPlan;
import com.tarun.kahani.core.Story;
import org.junit.Test;

/** Regression tests for the production-plan-to-story insertion boundary. */
public class ProductionPlanTest {
    private Story story() {
        Story s = new Story();
        s.hindi = false;
        Story.CharacterDef c = new Story.CharacterDef();
        c.id = "maya"; c.displayName = "Maya"; c.fullName = "Maya"; c.fromScript = true;
        s.characters.add(c);
        Story.PlaceDef p = new Story.PlaceDef(); p.name = "Garden"; p.description = "Existing garden";
        s.places.add(p);
        Story.Scene first = new Story.Scene(); first.number = 1; first.title = "Arrival"; first.setting = "Garden";
        Story.Beat dialogue = new Story.Beat(); dialogue.type = Story.Beat.DIALOGUE; dialogue.speaker = c;
        dialogue.speakerRaw = "Maya"; dialogue.text = "I am here."; first.beats.add(dialogue);
        Story.Scene second = new Story.Scene(); second.number = 2; second.title = "Discovery"; second.setting = "Garden";
        second.beats.add(Story.Beat.direction("Maya notices the gate."));
        s.scenes.add(first); s.scenes.add(second);
        return s;
    }

    private String insertion(int after, String title, String setting, String cast, String action) {
        return "{\"after_scene\":" + after + ",\"title\":\"" + title + "\",\"setting\":\"" + setting
                + "\",\"cast\":[" + cast + "],\"actions\":[\"" + action
                + "\"],\"purpose\":\"visual continuity\",\"confidence\":0.96,\"auto_apply\":true}";
    }

    @Test public void sortsInsertionsByOriginalSceneAndPreservesAuthoredContent() {
        Story s = story();
        Story.Scene originalFirst = s.scenes.get(0), originalSecond = s.scenes.get(1);
        String report = "{\"ready_to_render\":true,\"scene_insertions\":["
                + insertion(2, "Second bridge", "Garden", "\"Maya\"", "She pauses by the gate.") + ","
                + insertion(1, "First bridge", "Garden", "\"Maya\"", "She looks toward the gate.") + ","
                + insertion(1, "First bridge", "Garden", "\"Maya\"", "She looks toward the gate.") + "]}";
        ProductionPlan.Result r = ProductionPlan.apply(s, report);
        assertTrue(r.valid); assertTrue(r.readyToRender); assertEquals(2, r.scenesAdded);
        assertEquals(4, s.scenes.size());
        assertSame(originalFirst, s.scenes.get(0));
        assertEquals("First bridge", s.scenes.get(1).title);
        assertSame(originalSecond, s.scenes.get(2));
        assertEquals("Second bridge", s.scenes.get(3).title);
        assertEquals("I am here.", s.scenes.get(0).beats.get(0).text);
        for (int i = 0; i < s.scenes.size(); i++) assertEquals(i + 1, s.scenes.get(i).number);
    }

    @Test public void rejectsUnknownPlacesAndCharacters() {
        Story s = story();
        String report = "{\"ready_to_render\":true,\"scene_insertions\":["
                + insertion(1, "Unknown place", "Moon base", "\"Maya\"", "She looks around.") + ","
                + insertion(1, "Unknown cast", "Garden", "\"Ghost\"", "She looks around.") + "]}";
        ProductionPlan.Result r = ProductionPlan.apply(s, report);
        assertEquals(0, r.scenesAdded); assertEquals(2, s.scenes.size());
    }

    @Test public void missingReadinessDecisionBlocksRendering() {
        ProductionPlan.Result r = ProductionPlan.apply(story(), "{\"scene_insertions\":[]}");
        assertTrue(r.valid); assertFalse(r.readyToRender);
        assertFalse(r.warnings.isEmpty());
    }

    @Test public void missingDirectorSignoffBlocksRendering() {
        ProductionPlan.Result r = ProductionPlan.apply(story(), "{\\"ready_to_render\\":true,\\"scene_insertions\\":[]}");
        assertTrue(r.valid);
        assertFalse(r.readyToRender);
        assertTrue(r.warnings.toString().contains("director"));
    }

    @Test public void malformedReportIsInvalidAndDoesNotInsert() {
        Story s = story();
        ProductionPlan.Result r = ProductionPlan.apply(s, "not json");
        assertFalse(r.valid); assertEquals(2, s.scenes.size());
    }
}

package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.Pose;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

/**
 * v47: one seated when a part ends is still seated when the next part opens in the same place (बाबू on his bench
 * through all five scenes), "उनके पास बैठते हुए" in a line's manner seats the speaker beside them on the same
 * bench, and a seated speaker's close-up is framed on the seated face.
 */
public class SeatContinuityTest {
    static Film.Actor actor(Film.Seg s, String name) {
        for (Film.Actor a : s.actors) if (a.c.displayName.equals(name) || a.c.shown().equals(name)) return a;
        return null;
    }

    @Test public void babuStaysOnHisBenchThroughTheFilm() throws Exception {
        File assets = new File(System.getProperty("kahani.assets", "../../app/src/main/assets"));
        String source = new String(Files.readAllBytes(new File(assets, "babu_ke_liye_screenplay.txt").toPath()), StandardCharsets.UTF_8);
        Story story = ScriptParser.parse(source);
        Director director = new Director(story, new Director.Options());
        Art art = ReferenceArtLoader.load("babu_ke_liye", story, assets);
        director.prepare();
        Film film = director.direct(art);
        int sceneParts = 0, seatedParts = 0, girlSeated = 0;
        for (Film.Seg s : film.segs) {
            if (s.type != Film.S_SCENE) continue;
            sceneParts++;
            Film.Actor babu = actor(s, "बाबू");
            assertNotNull("बाबू is in every part (v47.8: in scene 3 his eyes rest on the letter — he is there)", babu);
            float mid = (s.t0 + s.t1) / 2;
            Film.Key k = babu.stateAt(mid);
            if (k.body == Pose.SIT && k.seat >= Film.SEAT_STOOL) seatedParts++;
            Film.Actor girl = actor(s, "बच्ची");
            if (s.scene >= 3 && girl != null && girl.stateAt(s.t1 - 0.5f).body == Pose.SIT) girlSeated++;
        }
        assertEquals(5, sceneParts);
        assertEquals("बाबू is seated on the bench in every part", 5, seatedParts);
        assertTrue("the girl sits beside him in scenes 4 and 5: " + girlSeated, girlSeated >= 2);
    }

    /** v47.8: someone's eyes or gaze on a thing puts them on the stage; remembered or pictured, it does not. */
    @Test public void eyesOnSomethingPutTheirOwnerOnStage() throws Exception {
        File assets = new File(System.getProperty("kahani.assets", "../../app/src/main/assets"));
        Story story = ScriptParser.parse(new String(Files.readAllBytes(new File(assets, "babu_ke_liye_screenplay.txt").toPath()), StandardCharsets.UTF_8));
        Story.CharacterDef babu = null;
        for (Story.CharacterDef c : story.characters) if (c.displayName.equals("बाबू")) babu = c;
        assertNotNull(babu);
        assertTrue(ScriptParser.present(story, "बाबू की आँखें पत्र पर टिक जाती हैं।").contains(babu));
        assertTrue(ScriptParser.present(story, "बाबू की नज़र बैग पर है।").contains(babu));
        assertFalse(ScriptParser.present(story, "उसे बाबू की आँखें याद आती हैं।").contains(babu));
        assertFalse("a thing of theirs alone does not", ScriptParser.present(story, "बाबू की छड़ी पेड़ से टिकी है।").contains(babu));
    }

    @Test public void aSeatedSpeakersCloseUpIsFramedOnTheSeatedFace() throws Exception {
        File script = new File("../../tools/testdata/aakhiri_jodi/aakhiri_jodi_screenplay.txt");
        assertTrue(script.isFile());
        Story story = ScriptParser.parse(new String(Files.readAllBytes(script.toPath()), StandardCharsets.UTF_8));
        Director director = new Director(story, new Director.Options());
        director.prepare();
        Film film = director.direct(new Art());
        Film.Seg scene = null;
        for (Film.Seg s : film.segs) if (s.type == Film.S_SCENE) { scene = s; break; }
        assertNotNull(scene);
        Film.Actor ismail = actor(scene, "इस्माइल"), nanhi = actor(scene, "नन्ही");
        assertNotNull(ismail); assertNotNull(nanhi);
        // "छुपा हुआ डिब्बा" hides nobody: इस्माइल sits on his newspaper from the first direction to the last
        int samples = 0, seated = 0;
        for (float t = scene.t0 + 1; t < scene.t1 - 1; t += 2) { samples++; if (ismail.stateAt(t).body == Pose.SIT) seated++; }
        assertEquals("इस्माइल is seated on the floor through the scene: " + seated + "/" + samples, samples, seated);
        // "नए लाल जूते पहनती है" is not a change of clothes
        for (Story.CharacterDef c : story.characters) assertTrue(c.displayName + " keeps her clothes: " + c.costumes.size(), c.costumes.isEmpty());
        // the first lip-sync close-up on नन्ही after she sits down is aimed at her seated face, not a standing head
        Film.Key sat = null;
        for (Film.Key k : nanhi.keys) if (k.body == Pose.SIT) { sat = k; break; }
        assertNotNull("नन्ही sits down on the floor", sat);
        float h = com.tarun.kahani.core.Renderer.actorHeight(nanhi.look, nanhi.c, new Art(), scene);
        boolean checked = false;
        for (Film.Shot sh : film.shots) {
            if (!sh.speech || !"नन्ही".equals(sh.subject) || sh.t < sat.t - 0.1f || sh.t > sat.t + 4f) continue;
            Film.Cam cam = null;
            for (Film.Cam c : scene.cams) if (c.t <= sh.t + 0.01f) cam = c;
            if (cam == null || cam.zoom < 2f) continue;
            float fh = 720f / cam.zoom, top = cam.cy - fh / 2;
            float standingHead = scene.ground - h, seatedHead = scene.ground - h * 0.65f;
            assertTrue("the close-up's top edge is below a standing head's top (" + top + " vs " + standingHead + ")", top > standingHead);
            assertTrue("and above the seated head (" + top + " vs " + seatedHead + ")", top < seatedHead);
            checked = true;
        }
        assertTrue("a seated close-up was checked", checked);
    }
}

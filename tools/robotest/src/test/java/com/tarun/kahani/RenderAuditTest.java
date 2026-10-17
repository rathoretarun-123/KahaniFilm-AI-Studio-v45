package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Casting;
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
 * v47.8 (the user: "debug everything … and all their collaboration"): what the frames of बाबू के लिए showed wrong and
 * was traced to casting and drawing —
 * <ul>
 * <li>a view line naming its view ("back", "side") instead of an angle was dropped without a word, so the over-the-
 * shoulder shot from behind the girl used a laughing panel of her sheet (tagged 180° for its shut eyes) as her back;</li>
 * <li>with the girl beside him on the bench, the old man's own photograph on his bench was put aside for the rig folded
 * onto a drawn bench, which read as standing.</li>
 * </ul>
 */
public class RenderAuditTest {

    @Test public void aViewNamedInWordsIsReadAsItsAngle() {
        assertEquals("180", Art.viewAngle("back"));
        assertEquals("180", Art.viewAngle(" Back "));
        assertEquals("-90", Art.viewAngle("side"));
        assertEquals("-45", Art.viewAngle("three-quarter"));
        assertEquals("0", Art.viewAngle("front"));
        assertEquals("90", Art.viewAngle("90"));
        assertEquals("-45", Art.viewAngle("-45"));
        assertEquals("sideways-ish", Art.viewAngle("sideways-ish"));     // (an unknown word stays as it is)
    }

    static Story babu(File assets) throws Exception {
        return ScriptParser.parse(new String(Files.readAllBytes(new File(assets, "babu_ke_liye_screenplay.txt").toPath()), StandardCharsets.UTF_8));
    }

    static Story.CharacterDef named(Story story, String name) {
        for (Story.CharacterDef c : story.characters) if (c.displayName.equals(name)) return c;
        return null;
    }

    @Test public void theReferencePacksBackViewsAreLoaded() throws Exception {
        File assets = new File(System.getProperty("kahani.assets", "../../app/src/main/assets"));
        Story story = babu(assets);
        Art art = ReferenceArtLoader.load("babu_ke_liye", story, assets);
        for (String name : new String[]{"बच्ची", "बाबू"}) {
            Story.CharacterDef c = named(story, name);
            assertNotNull(name, c);
            Art.Sprite sp = art.sprites.get(c.id);
            assertNotNull(name, sp);
            Art.Sprite back = sp.view(2);
            assertNotNull(name + "'s back view (the pack's view line says \"back\")", back);
            assertTrue(name + "'s back is a whole standing figure: " + back.w + "x" + back.h, back.h > back.w * 1.5f);
        }
    }

    @Test public void aSharedBenchKeepsTheSittersOwnBenchPhotograph() throws Exception {
        File assets = new File(System.getProperty("kahani.assets", "../../app/src/main/assets"));
        Story story = babu(assets);
        Director director = new Director(story, new Director.Options());
        Art art = ReferenceArtLoader.load("babu_ke_liye", story, assets);
        director.prepare();
        Film film = director.direct(art);
        Story.CharacterDef babu = named(story, "बाबू"), girl = named(story, "बच्ची");
        Art.Sprite sp = art.sprites.get(babu.id);
        int shared = 0, own = 0;
        for (Film.Shot sh : film.shots) {
            Film.Seg s = film.segAt(sh.t + 0.01f);
            if (s == null || s.type != Film.S_SCENE) continue;
            Film.Actor b = null, g = null;
            for (Film.Actor a : s.actors) { if (a.c == babu) b = a; if (a.c == girl) g = a; }
            if (b == null || g == null) continue;
            float mid = sh.t + Math.min(sh.dur * 0.5f, 1.2f);
            Film.Key kb = b.stateAt(mid), kg = g.stateAt(mid);
            if (!kb.visible || !kg.visible || kb.body != Pose.SIT || kg.body != Pose.SIT || kg.seat <= 0) continue;
            Integer idx = sh.pictures.get(babu.id);
            if (idx == null || idx < 0 || idx >= sp.poses.size() || sp.poses.get(idx).closeUp) continue;
            shared++;
            if (Casting.seatOfItsOwn(sp.poses.get(idx))) own++;
        }
        assertTrue("shots with both of them seated: " + shared, shared >= 3);
        assertEquals("the old man keeps his photograph on his own bench beside the girl", shared, own);
    }
}

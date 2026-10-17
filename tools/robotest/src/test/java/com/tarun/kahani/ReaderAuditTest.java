package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.Pose;
import com.tarun.kahani.core.ProductionDocument;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

/**
 * v47.8: the reader's hand-over to the director and the editor, checked over every story of the test library (2,100):
 * the place a script states above its lists, the people a sentence puts on the stage, the people a line only calls,
 * and the voices that are only heard.
 */
public class ReaderAuditTest {
    static final File ASSETS = new File(System.getProperty("kahani.assets", "../../app/src/main/assets"));
    static final File TESTDATA = new File("../../tools/testdata");

    static Story read(File f) throws Exception { return ScriptParser.parse(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8)); }

    static Film direct(Story st) { Director d = new Director(st, new Director.Options()); d.prepare(); return d.direct(new Art()); }

    static ProductionDocument document(Story st) {
        ProductionDocument pd = ProductionDocument.read(st, null);
        pd.bind(st); pd.attach(direct(st)); pd.validate();
        return pd;
    }

    static boolean onStage(Film film, int sceneIndex, String name) {
        for (Film.Seg s : film.segs) if (s.type == Film.S_SCENE && s.scene == sceneIndex) for (Film.Actor a : s.actors) if (a.c.displayName.equals(name)) return true;
        return false;
    }

    /**
     * "स्थान: उत्तर भारतीय कस्बे की पुरानी डाकघर वाली गली" above the lists is the story's place: it was taken for a list's
     * heading and lost, the continuity list's "4. स्थान: बड़ा पेड़…" became a place called "स्थान", scene 1 of बाबू के लिए
     * was set in a room and scenes 2–5 in a place of no kind (a time only) — so बाबू, seated on his bench in scene 1,
     * stood up in a different place in scene 2.
     */
    @Test public void theScriptsOwnPlaceIsEverySceneThatNamesNoOther() throws Exception {
        for (String name : new String[]{"babu_ke_liye_screenplay.txt", "aakhiri_chai_screenplay.txt"}) {
            Story st = read(new File(ASSETS, name));
            assertEquals(name, 1, st.places.size());
            Story.PlaceDef p = st.places.get(0);
            assertFalse(name + ": the place has a name of its own, not a label: " + p.name, ScriptParser.placeLabel(p.name));
            assertTrue(name + ": named by the script's own line: " + p.name, p.name.contains("गली"));
            for (Story.Scene sc : st.scenes) assertTrue(name + " scene " + sc.number + ": " + sc.setting, sc.setting.startsWith(p.name));
            Film film = direct(st);
            int set = -1;
            for (Film.Seg s : film.segs) if (s.type == Film.S_SCENE) { if (set < 0) set = s.set; assertEquals(name + ": one place, one set", set, s.set); }
        }
        // the old man stays on his bench through all five scenes with the studio's own drawings too
        Story babu = read(new File(ASSETS, "babu_ke_liye_screenplay.txt"));
        Film film = direct(babu);
        int parts = 0;
        for (Film.Seg s : film.segs) {
            if (s.type != Film.S_SCENE) continue;
            for (Film.Actor a : s.actors) if (a.c.displayName.equals("बाबू")) {
                parts++;
                Film.Key k = a.stateAt((s.t0 + s.t1) / 2);
                assertEquals("बाबू seated in scene " + babu.scenes.get(s.scene).number, Pose.SIT, k.body);
            }
        }
        assertEquals(5, parts);
        // a time line is still the hour, never the place
        assertEquals("सुनहरी शाम", babu.time);
    }

    /** "अयान दबे पाँव माँ के पीछे जाता है", "वनुशा के गालों पर आटा लगा है": the one followed, the one with the flour, are there. */
    @Test public void whereOneStandsByThemOrTheirFacePutsThemOnStage() throws Exception {
        Story a = read(new File(TESTDATA, "v44forge/story_00343.txt"));
        assertTrue("माँ (कविता) is followed in scene 2: she is there", onStage(direct(a), 1, "कविता"));
        Story b = read(new File(TESTDATA, "v42stories/story_15.txt"));
        assertTrue("वनुशा, flour on her cheeks, is in the kitchen in scene 1", onStage(direct(b), 0, "वनुशा"));
        // a thing of theirs alone, or a picture of them, does not
        Story.CharacterDef vanusha = null;
        for (Story.CharacterDef c : b.characters) if (c.displayName.equals("वनुशा")) vanusha = c;
        assertNotNull(vanusha);
        assertFalse(ScriptParser.present(b, "प्रिया के हाथ में वनुशा का रिबन है।").contains(vanusha));
        assertFalse(ScriptParser.present(b, "दीवार पर वनुशा के गालों वाली तस्वीर टँगी है।").contains(vanusha));
    }

    static int notes(ProductionDocument pd, String category, String word) {
        int n = 0;
        for (ProductionDocument.Issue i : pd.issues) if (category.equals(i.category) && i.requirement.contains("\"" + word + "\"")) n++;
        return n;
    }

    /** "डरो मत बेटा, …", "सब ठीक है, बेटा।" call the child on screen; "अपने बेटे की चिट्ठी" names one who is not there. */
    @Test public void aChildCalledIsNoUnseenPerson() throws Exception {
        assertEquals(0, notes(document(read(new File(TESTDATA, "v42stories/story_01.txt"))), "OFFSCREEN_PERSON", "बेटा"));
        assertEquals(0, notes(document(read(new File(TESTDATA, "v44stories/s01_family_car.txt"))), "OFFSCREEN_PERSON", "बेटा"));
        assertEquals(0, notes(document(read(new File(TESTDATA, "v44stories/s05_rajkumari.txt"))), "OFFSCREEN_PERSON", "बेटी"));
        assertEquals("\"Warm songs?\" names no son", 0, notes(document(read(new File(TESTDATA, "v42stories/story_03.txt"))), "OFFSCREEN_PERSON", "son"));
        ProductionDocument chitthi = document(read(new File(TESTDATA, "aakhiri_chitthi/aakhiri_chitthi_numbered.txt")));
        assertEquals("the son of the letter is still noted", 1, notes(chitthi, "OFFSCREEN_PERSON", "बेटे"));
    }

    /** A device's voice the reader knows is only heard ("मधुर AI आवाज़") is noted, never a blocker of the film. */
    @Test public void aVoiceOnlyHeardIsNoMissingSpeaker() throws Exception {
        ProductionDocument pd = document(read(new File(TESTDATA, "neo_mumbai.txt")));
        int blockers = 0, voices = 0;
        for (ProductionDocument.Issue i : pd.issues) {
            if (ProductionDocument.BLOCKER.equals(i.severity) && ProductionDocument.READER.equals(i.stage)) blockers++;
            if (ProductionDocument.WARNING.equals(i.severity) && i.requirement.startsWith("a voice only heard")) voices++;
        }
        assertEquals(0, blockers);
        assertTrue(voices >= 1);
    }
}

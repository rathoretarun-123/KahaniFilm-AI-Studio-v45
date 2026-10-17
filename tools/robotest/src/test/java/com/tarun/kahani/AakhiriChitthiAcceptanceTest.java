package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.ProductionDocument;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * v47.6: the acceptance fixture of the production protocol (Section 15): the filmmaker's आखिरी चिट्ठी, read exactly
 * as written — two characters, thirteen props, three places, three scenes, every line with its speaker in order, the
 * environment as causes and effects, the footwear, the stoop, the broken bench on its brick, where the places are,
 * परी's steps one by one, the letter's chain of hands, the closing pull-back — and the checks that keep a film honest:
 * a missing picture is said, a failed render is never reported as done.
 *
 * The pictures here are stand-ins made in code (the props only, no characters), so the director stages the things
 * exactly as it would with the user's own pictures, and the missing character pictures are the ones the checks must find.
 */
public class AakhiriChitthiAcceptanceTest {
    static final File SCRIPT = new File("../../tools/testdata/aakhiri_chitthi/aakhiri_chitthi_numbered.txt");

    static Art propsOnly(Story st) {
        Art art = new Art();
        for (Story.PlaceDef th : st.things) {
            Art.Shot s = new Art.Shot();
            s.scene = ""; s.keys = new String[]{th.name}; s.object = true;
            s.pic = new Art.Backdrop(); s.pic.img = new Object(); s.pic.w = 120; s.pic.h = 90; s.pic.object = true;
            art.shots.add(s);
        }
        return art;
    }

    static final class Made { Story st; Film film; ProductionDocument doc; }

    static Made make() throws Exception {
        Made m = new Made();
        m.st = ScriptParser.parse(new String(Files.readAllBytes(SCRIPT.toPath()), StandardCharsets.UTF_8));
        Art art = propsOnly(m.st);
        m.doc = ProductionDocument.read(m.st, null);
        Director dir = new Director(m.st, new Director.Options());
        dir.prepare();
        m.film = dir.direct(art);
        m.doc.bind(m.st);
        m.doc.attach(m.film);
        m.doc.assets(art);
        m.doc.validate();
        return m;
    }

    static ProductionDocument.Entity named(List<ProductionDocument.Entity> l, String name) {
        for (ProductionDocument.Entity e : l) if (e.name.equals(name)) return e;
        fail("no " + name);
        return null;
    }

    static ProductionDocument.Effect effect(ProductionDocument d, String scene, String kind) {
        for (ProductionDocument.Effect e : d.effects) if (e.sceneId.equals(scene) && e.kind.equals(kind)) return e;
        return null;
    }

    @Test public void theReaderTakesOnlyWhatIsWritten() throws Exception {
        ProductionDocument d = make().doc;
        assertEquals(2, d.characters.size());
        assertEquals("रघुवीर जी", d.characters.get(0).name);
        assertEquals("परी", d.characters.get(1).name);
        assertEquals(13, d.props.size());
        assertEquals(3, d.locations.size());
        assertEquals(3, d.scenes.size());
        for (ProductionDocument.Scene s : d.scenes) assertEquals("every scene is at the neem corner", named(d.locations, "नीम के पेड़ के नीचे का कोना").id, s.placeId);
        assertEquals("the post office and the dirt road are seen from the corner, no scene is set there", ProductionDocument.NOT_REQUIRED, named(d.locations, "कच्चा रास्ता").assetState);
    }

    @Test public void everyLineExactlyAsWrittenWithItsSpeakerInOrder() throws Exception {
        ProductionDocument d = make().doc;
        // the lines read from the file on their own (name: "line"), not through the parser
        List<String[]> want = new ArrayList<String[]>();
        boolean story = false;
        Matcher lm = Pattern.compile("^([^:\"()]{1,30}):\\s*\"([^\"]+)\"").matcher("");
        for (String l : new String(Files.readAllBytes(SCRIPT.toPath()), StandardCharsets.UTF_8).split("\n")) {
            if (l.trim().equals("कहानी")) story = true;
            if (story && lm.reset(l.trim()).find()) want.add(new String[]{lm.group(1).trim(), lm.group(2).trim()});
        }
        assertEquals(7, want.size());
        assertEquals(want.size(), d.lines.size());
        for (int i = 0; i < want.size(); i++) {
            ProductionDocument.Line got = d.lines.get(i);
            assertEquals("speaker of line " + (i + 1), want.get(i)[0], got.speakerName);
            assertEquals("line " + (i + 1), want.get(i)[1], got.text.trim());
            assertNotNull(got.speakerId);
            assertFalse("a delivery note is never part of the line", got.text.contains("("));
        }
        assertEquals("SCENE_01", d.lines.get(0).sceneId);
        assertEquals("SCENE_03", d.lines.get(6).sceneId);
    }

    @Test public void theEnvironmentIsCausesAndEffects() throws Exception {
        ProductionDocument d = make().doc;
        ProductionDocument.Effect time = effect(d, "SCENE_01", "TIME");
        assertNotNull(time);
        assertTrue(time.behaviour, time.behaviour.startsWith("2:00"));
        assertEquals(ProductionDocument.EXPLICIT, effect(d, "SCENE_01", "WIND").evidence);
        for (String kind : new String[]{"LEAF_SWAY", "DUST", "CLOTH"}) {
            ProductionDocument.Effect e = effect(d, "SCENE_01", kind);
            assertNotNull(kind, e);
            assertEquals(kind + " is the wind's", "WIND", e.cause);
            assertEquals(kind, ProductionDocument.EXPLICIT, e.evidence);
            assertTrue(kind + " is in the director's plan", e.scheduled);
        }
        assertEquals("the kurta that stirs is रघुवीर जी's", d.characters.get(0).id, effect(d, "SCENE_01", "CLOTH").affectedId);
        assertTrue("the bell and the rustle are heard", effect(d, "SCENE_01", "SOUND_BELL").scheduled && effect(d, "SCENE_01", "SOUND_RUSTLE").scheduled);
        // the leaves that slide along the ground: no renderer method — said, never claimed
        ProductionDocument.Effect slide = effect(d, "SCENE_02", "LEAVES_SLIDE");
        assertNotNull(slide);
        assertEquals(ProductionDocument.UNSUPPORTED, slide.method);
        assertFalse(slide.scheduled);
        boolean said = false;
        for (ProductionDocument.Issue i : d.issues) if (i.requirement.startsWith("LEAVES_SLIDE")) said = true;
        assertTrue("an unsupported requirement is in the issue register", said);
        // the bench rocks as परी sits on it (a procedural approximation, scheduled in the plan)
        ProductionDocument.Effect wob = effect(d, "SCENE_03", "BENCH_WOBBLE");
        assertNotNull(wob);
        assertEquals(ProductionDocument.PROCEDURAL, wob.method);
        assertTrue(wob.scheduled);
    }

    @Test public void thePeopleAndThingsAsDescribed() throws Exception {
        ProductionDocument d = make().doc;
        ProductionDocument.Entity raghu = d.characters.get(0), pari = d.characters.get(1);
        assertEquals("back bent 90°", raghu.fact("posture"));
        assertEquals("bare", raghu.fact("feet"));
        assertEquals("78", raghu.fact("age"));
        assertEquals("7", pari.fact("age"));
        ProductionDocument.Entity chappal = named(d.props, "परी की हवाई चप्पल");
        assertEquals("left foot", chappal.fact("wornOn"));
        assertEquals("right foot bare", chappal.fact("otherFoot"));
        assertNotNull(chappal.fact("wound"));
        assertEquals(pari.id, chappal.fact("owner"));
        ProductionDocument.Entity bench = named(d.props, "लकड़ी की टूटी बेंच"), brick = named(d.props, "बेंच के नीचे लगी ईंट");
        assertEquals("front-right leg", bench.fact("brokenPart"));
        assertTrue(brick.fact("supports").startsWith(bench.id));
        // "दादाजी" is what परी calls him: his stick is his, read from the dialogue and marked so
        ProductionDocument.Fact owner = named(d.props, "दादाजी की लकड़ी की छड़ी").factOf("owner");
        assertEquals(raghu.id, owner.value);
        assertEquals(ProductionDocument.INFERRED, owner.evidence);
        ProductionDocument.Entity corner = named(d.locations, "नीम के पेड़ के नीचे का कोना"), office = named(d.locations, "गांव का पुराना डाकघर - बाहरी हिस्सा");
        assertTrue(corner.fact("inFrontOf"), corner.fact("inFrontOf").startsWith(office.id + " at 10"));
        assertEquals(office.id, named(d.locations, "कच्चा रास्ता").fact("behind"));
    }

    @Test public void parisStepsOneByOne() throws Exception {
        ProductionDocument d = make().doc;
        String pari = d.characters.get(1).id, bag = named(d.props, "परी का गुलाबी स्कूल बैग").id, letter = named(d.props, "पीली पड़ी मुड़ी-तुड़ी चिट्ठी").id;
        List<String> steps = new ArrayList<String>();
        for (ProductionDocument.Action a : d.actions) if (a.sceneId.equals("SCENE_02") && a.actorId.equals(pari) && !a.verb.equals("MUST_NOT")) steps.add(a.verb + (a.targetId != null ? ":" + a.targetId : ""));
        String postbox = named(d.props, "पुराना लाल डाक-बक्सा").id, raghu = d.characters.get(0).id;
        String[] order = {"RUN", "GO_TO:" + postbox, "PLACE:" + bag, "KNEEL", "REACH_INTO:" + bag, "TAKE_OUT:" + letter, "STAND_UP", "STEP_TOWARD:" + raghu};
        int at = -1;
        for (String o : order) {
            int i = steps.indexOf(o);
            assertTrue(o + " in " + steps, i > at);
            at = i;
        }
        boolean silent = false;
        for (ProductionDocument.Action a : d.actions) if (a.verb.equals("MUST_NOT") && a.actorId.equals(pari) && a.span.contains("बिना आवाज")) silent = true;
        assertTrue("\"बिना आवाज किए\": no sound — a step that must not happen", silent);
        // scene 3: he takes the letter from her hand, she sits beside him on the bench
        boolean took = false, beside = false;
        for (ProductionDocument.Action a : d.actions) {
            if (a.verb.equals("TAKE_FROM") && a.actorId.equals(d.characters.get(0).id) && letter.equals(a.targetId)) took = "IN_PLAN".equals(a.status);
            if (a.verb.equals("SIT_BESIDE") && a.actorId.equals(pari)) beside = "IN_PLAN".equals(a.status);
        }
        assertTrue("रघुवीर जी takes the letter from परी's hand", took);
        assertTrue("परी sits beside him", beside);
    }

    @Test public void theLetterPassesFromHandToHandNeverInTwo() throws Exception {
        ProductionDocument d = make().doc;
        String raghu = d.characters.get(0).id, pari = d.characters.get(1).id, letter = named(d.props, "पीली पड़ी मुड़ी-तुड़ी चिट्ठी").id;
        List<ProductionDocument.StateChange> chain = new ArrayList<ProductionDocument.StateChange>();
        for (ProductionDocument.StateChange c : d.states) if (c.entityId.equals(letter)) chain.add(c);
        assertTrue(chain.size() >= 4);
        assertFalse("found on the ground behind the postbox is told, not shown", chain.get(0).onScreen);
        assertTrue(chain.get(0).state.startsWith("ON_GROUND"));
        assertEquals(pari, chain.get(0).holderId);
        String last = null;
        List<String> holders = new ArrayList<String>();
        for (ProductionDocument.StateChange c : chain) if (c.onScreen && c.holderId != null && !c.holderId.equals(last)) { holders.add(c.holderId); last = c.holderId; }
        assertEquals(java.util.Arrays.asList(pari, raghu), holders);
        for (ProductionDocument.Issue i : d.issues) assertFalse(i.requirement, i.severity.equals(ProductionDocument.BLOCKER));
    }

    @Test public void theFilmEndsOnTheScriptsSlowPullBack() throws Exception {
        Made m = make();
        Film.Seg last = null;
        for (Film.Seg sg : m.film.segs) if (sg.type == Film.S_SCENE) last = sg;
        Film.Cam end = last.cams.get(last.cams.size() - 1);
        assertTrue("the closing move is the script's own", end.scripted);
        assertTrue("it moves, slowly", end.ease >= 2f);
        assertTrue("it ends wide (zoom " + end.zoom + ")", end.zoom <= 1.2f);
        ProductionDocument.Shot shot = m.doc.shots.get(m.doc.shots.size() - 1);
        assertTrue(shot.scripted);
        assertEquals("SCENE_03", shot.sceneId);
    }

    @Test public void aMissingPictureIsSaidAndAFailedRenderIsNeverDone() throws Exception {
        ProductionDocument d = make().doc;
        for (ProductionDocument.Entity c : d.characters) {
            assertEquals(ProductionDocument.MISSING, c.assetState);
            boolean asked = false;
            for (ProductionDocument.Issue i : d.issues) if (i.entityIds.contains(c.id) && i.requirement.contains("picture")) asked = true;
            assertTrue("a picture of " + c.name + " is asked for", asked);
        }
        for (ProductionDocument.Entity p : d.props) assertEquals(p.name, ProductionDocument.MATCHED, p.assetState);
        // the director signs off; the render's file never came
        assertEquals("APPROVED", d.review(ProductionDocument.DIRECTOR, "APPROVED", "the plan follows the script").status);
        ProductionDocument.RenderRecord r = d.verifyRender(new File("../../build/no-such-film.mp4"), "test", 120);
        assertEquals("FAILED", r.status);
        assertFalse(d.complete());
        assertTrue(d.toJson().contains("\"FAILED\""));
    }
}

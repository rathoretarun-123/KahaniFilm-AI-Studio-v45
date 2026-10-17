package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.*;
import com.tarun.kahani.jvm.MakeFilm;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import org.junit.Test;

/**
 * v47.7 (the user: "even some animated objects like cars, toys may also have similar activities like speaking … like
 * humans"; "umbrella is rigid but can open, book is rigid but can open"): a thing that is a character is read as a
 * thing, drawn rigid and acts with its whole body, smoothly at 24 fps; a rigid thing opens at its hinge when the words
 * say so — a book's leaves, a box's lid where it lies, an umbrella in the sun — and the user's own picture of it open
 * is its open view, never the thing itself.
 */
public class ObjectCharactersTest {

    static final String TOYS = "पात्र:\n"
            + "1. चिंटू: एक छोटी लाल खिलौना कार, बड़ी-बड़ी आँखें, नटखट\n"
            + "2. गोलू (7 वर्ष): लड़का, नीली शर्ट\n"
            + "3. भोलू: एक भूरा टेडी बियर\n"
            + "चिंटू की सवारी\n"
            + "दृश्य 1: आँगन में\n"
            + "(स्थान: घर का आँगन। सुबह। चिंटू आँगन में खड़ी है। गोलू और भोलू पास में खड़े हैं।)\n"
            + "गोलू: \"चिंटू, क्या तुम मेरे साथ खेलोगी?\"\n"
            + "(चिंटू हाँ में सिर हिलाती है।)\n"
            + "चिंटू (खुश होकर): \"हाँ गोलू! चलो खेलते हैं!\"\n"
            + "भोलू: \"मैं भी आऊँगा!\"\n"
            + "(चिंटू ना में सिर हिलाती है।)\n"
            + "चिंटू: \"नहीं भोलू, तुम बहुत भारी हो!\"\n"
            + "(चिंटू बैठ जाती है।)\n"
            + "(भोलू हँसता है।)\n"
            + "(चिंटू खड़ी होती है।)\n"
            + "(चिंटू तेज़ी से आँगन में दौड़ती है।)\n"
            + "(भोलू दौड़ता है।)\n";

    static final String BOOK_BOX = "शीर्षक: मीना की किताब\n\n"
            + "पात्र\n1. मीना (8 साल) (छोटी लड़की, लाल फ्रॉक)\n2. राजू (10 साल) (लड़का, नीली कमीज)\n\n"
            + "स्थान और पृष्ठभूमि\n1. बगीचा (हरा बगीचा, धूप)\n\n"
            + "वस्तुएँ\n1. नीली किताब (मोटी नीली किताब)\n2. लकड़ी का बक्सा (पुराना भूरा बक्सा)\n3. पीला छाता (बड़ा पीला छाता)\n\n"
            + "कहानी शुरू\nदृश्य 1\n"
            + "(बगीचा, धूप खिली है। मीना नीली किताब हाथ में लेकर खड़ी है। पास में जमीन पर लकड़ी का बक्सा रखा है।)\n"
            + "मीना: \"आज मैं एक नई कहानी पढ़ूँगी।\" (खुश होकर)\n"
            + "(मीना नीली किताब खोलती है और पढ़ती है।)\n"
            + "मीना: \"वाह, कितनी सुंदर कहानी है!\" (मुस्कुराते हुए)\n"
            + "(मीना नीली किताब बंद करती है।)\n"
            + "(मीना लकड़ी का बक्सा खोलती है।)\n"
            + "मीना: \"इसमें तो मेरे पुराने खिलौने हैं!\" (हैरान होकर)\n"
            + "(राजू आता है। राजू पीला छाता खोलता है।)\n"
            + "राजू: \"अब धूप नहीं लगेगी।\"\n"
            + "समाप्त\n";

    static Film direct(Story st, Art art) {
        Director d = new Director(st, new Director.Options());
        d.prepare();
        return d.direct(art);
    }

    static Film.Actor actor(Film f, String name) {
        for (Film.Seg sg : f.segs) if (sg.type == Film.S_SCENE) for (Film.Actor a : sg.actors) if (a.c.displayName.equals(name)) return a;
        fail("no " + name);
        return null;
    }

    static Film.Seg sceneOf(Film f, Film.Actor a) {
        for (Film.Seg sg : f.segs) if (sg.actors.contains(a)) return sg;
        return null;
    }

    static Story.CharacterDef character(Story st, String name) {
        for (Story.CharacterDef c : st.characters) if (c.displayName.equals(name)) return c;
        fail("no " + name);
        return null;
    }

    @Test public void aToyCarAndATeddyAreThingsAndPeopleStayPeople() {
        Story st = ScriptParser.parse(TOYS);
        assertEquals(Look.OBJECT, character(st, "चिंटू").look.kind);
        assertEquals(Look.OB_CAR, character(st, "चिंटू").look.objectType);
        assertTrue("a car rolls on its wheels", character(st, "चिंटू").look.rolls());
        assertEquals(Look.OB_TEDDY, character(st, "भोलू").look.objectType);
        assertFalse("a teddy hops, it has no wheels", character(st, "भोलू").look.rolls());
        assertEquals(Look.BOY, character(st, "गोलू").look.kind);
        assertTrue("a toy is smaller than the boy", character(st, "चिंटू").look.height < character(st, "गोलू").look.height);
        // one who drives a car, a doll, a likeness: people (or humanoid toys), never things
        Story people = ScriptParser.parse("पात्र:\n1. राजू: ड्राइवर, कार चलाता है\n2. गुड्डी: एक कपड़े की गुड़िया\n3. मीना: कार जैसी तेज़ लड़की\nकहानी\nदृश्य 1: सड़क\n(राजू, गुड्डी और मीना सड़क पर हैं।)\n");
        for (Story.CharacterDef c : people.characters) assertNotEquals(c.displayName, Look.OBJECT, c.look.kind);
    }

    @Test public void theToyCarNodsShakesItsHeadSitsStandsAndRunsInFrontOfTheBoy() {
        Story st = ScriptParser.parse(TOYS);
        Film f = direct(st, new Art());
        Film.Actor car = actor(f, "चिंटू");
        boolean nod = false, shake = false;
        for (Film.Act x : car.acts) { if (x.type == Film.G_NOD) nod = true; if (x.type == Film.G_SHAKE_HEAD) shake = true; }
        assertTrue("it nods (हाँ में सिर हिलाती है)", nod);
        assertTrue("it shakes its head (ना में सिर हिलाती है)", shake);
        boolean sat = false, stoodAfter = false, ran = false;
        for (Film.Key k : car.keys) { if (k.body == Pose.SIT) sat = true; else if (sat && k.body == Pose.STAND) stoodAfter = true; if (k.moveDur > 0.2f && k.run) ran = true; }
        assertTrue("it sits down", sat);
        assertTrue("and stands again", stoodAfter);
        assertTrue("it runs across", ran);
        // its run goes past the boy standing there: in front of him, never through him
        assertFalse("its run is laid in front of the boy", car.track.isEmpty());
    }

    @Test public void itsWholeBodyMovesSmoothlyAtTwentyFourFramesASecond() {
        Story st = ScriptParser.parse(TOYS);
        Film f = direct(st, new Art());
        Renderer r = new Renderer(f, new Art());
        for (String name : new String[]{"चिंटू", "भोलू"}) {
            Film.Actor a = actor(f, name);
            Film.Seg sg = sceneOf(f, a);
            float h = Renderer.actorHeight(a.look, a.c, null, sg);
            float[] prev = null;
            float maxTilt = 0, minSy = 9, maxSpeakSy = 0, maxLift = 0;
            for (int i = (int) Math.ceil(sg.t0 * 24); i < sg.t1 * 24; i++) {
                float t = i / 24f;
                float[] m = r.objectMotion(a, a.look, t, h);
                if (prev != null) {
                    assertTrue(name + " lifts smoothly at " + t, Math.abs(m[1] - prev[1]) <= 0.02f * h);
                    assertTrue(name + " tilts smoothly at " + t + ": " + (m[2] - prev[2]), Math.abs(m[2] - prev[2]) <= 3f);
                    assertTrue(name + " squashes smoothly at " + t, Math.abs(m[3] - prev[3]) <= 0.025f && Math.abs(m[4] - prev[4]) <= 0.025f);
                }
                prev = m;
                maxTilt = Math.max(maxTilt, Math.abs(m[2]));
                minSy = Math.min(minSy, m[4]);
                maxLift = Math.max(maxLift, -m[1]);
                if (r.mouthAt(a, t) > 0.2f) maxSpeakSy = Math.max(maxSpeakSy, m[4]);
            }
            assertTrue(name + " talks with its body", maxSpeakSy > 1.01f);
            if (name.equals("चिंटू")) {
                assertTrue("the car tips for its nod and its head shake", maxTilt > 3);
                assertTrue("the car settles down to sit", minSy < 0.92f);
            } else assertTrue("the teddy hops as it runs", maxLift > 0.04f * h);
        }
    }

    static File pictures() throws Exception {
        File dir = Files.createTempDirectory("objects").toFile();
        // a toy car with no face; a closed blue book and the same book open; a wooden box; a yellow umbrella
        draw(new File(dir, "toycar.png"), 800, 420, new Color(30, 110, 210), true);
        draw(new File(dir, "book.png"), 300, 400, new Color(25, 80, 170), false);
        draw(new File(dir, "book_open.png"), 600, 400, new Color(250, 248, 235), false);
        draw(new File(dir, "box.png"), 500, 360, new Color(150, 95, 50), false);
        draw(new File(dir, "umbrella.png"), 500, 500, new Color(250, 200, 30), false);
        return dir;
    }

    static void draw(File f, int w, int h, Color c, boolean wheels) throws Exception {
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = im.createGraphics();
        g.setColor(c);
        g.fillRoundRect(w / 20, wheels ? h * 2 / 5 : h / 20, w - w / 10, wheels ? h * 2 / 5 : h - h / 10, 40, 40);
        if (wheels) { g.setColor(Color.DARK_GRAY); g.fillOval(w / 8, h * 3 / 5, w / 5, h * 2 / 5 - 4); g.fillOval(w * 5 / 8, h * 3 / 5, w / 5, h * 2 / 5 - 4); }
        g.dispose();
        ImageIO.write(im, "png", f);
    }

    @Test public void aPictureWithNoFaceGetsNoLipsOrEyesAndNeverBends() throws Exception {
        File dir = pictures();
        Story st = ScriptParser.parse(TOYS);
        Art art = Art.fromManifest("char|चिंटू|toycar.png\n", st, new MakeFilm.AwtLoader(dir.getPath()));
        Art.Sprite sp = art.sprites.get(character(st, "चिंटू").id);
        assertNotNull("the car's picture is its character", sp);
        assertFalse("no face in it", sp.faceKnown);
        assertNull("no rig: the picture is drawn as it is (no animal jaw, no person's legs, no drawn lips)", sp.rig);
    }

    @Test public void aBookOpensAndClosesABoxOpensWhereItLiesAndAnUmbrellaOpensInTheSun() throws Exception {
        File dir = pictures();
        Story st = ScriptParser.parse(BOOK_BOX);
        String cast = "shot||नीली किताब|book.png|object\nshot||खुली नीली किताब|book_open.png|object\nshot||लकड़ी का बक्सा|box.png|object\nshot||पीला छाता|umbrella.png|object\n";
        Art art = Art.fromManifest(cast, st, new MakeFilm.AwtLoader(dir.getPath()));
        Film f = direct(st, art);
        Film.Fx book = null, box = null;
        for (Film.Seg sg : f.segs) for (Film.Fx x : sg.fx) {
            if (x.type != Film.FX_HELD || x.what == null) continue;
            if (x.what.contains("किताब") && x.kind == Film.THING_HOLD) book = x;
            if (x.what.contains("बक्सा")) box = x;
        }
        assertNotNull("the book is in her hands", book);
        assertTrue("it opens", book.openFrom < 1e5f);
        assertTrue("and closes again", book.openUntil > book.openFrom && book.openUntil < 1e5f);
        assertTrue("her book is the closed picture (taller than wide); the open one is only its open view", book.pic.w < book.pic.h);
        assertNotNull("the box lies beside her", box);
        assertEquals("opened where it lies, it stays there", Film.THING_PLACE, box.kind);
        assertTrue("its lid opens", box.openFrom < 1e5f && box.t1 > 1e5f);
        Film.Actor raju = actor(f, "राजू");
        assertTrue("the umbrella is opened by the words", raju.umbrellaOpenFrom >= 0);
        for (Film.Weather w : f.weather) assertFalse("no rain: it opens in the sun", w.type == Film.W_RAIN || w.type == Film.W_STORM);
        // how each opens
        assertEquals(Rigidity.HINGE_LEAVES, Rigidity.hinge("नीली किताब"));
        assertEquals(Rigidity.HINGE_LID, Rigidity.hinge("लकड़ी का बक्सा"));
        assertEquals(Rigidity.HINGE_CANOPY, Rigidity.hinge("पीला छाता"));
        assertEquals(Rigidity.NO_HINGE, Rigidity.hinge("दादाजी की लकड़ी की छड़ी"));
        assertTrue(Rigidity.ledger(st).contains("opens at its lid"));
    }

    @Test public void theFilmDrawsThemWithoutErrors() throws Exception {
        // every frame of the toy story drawn once a second, with the car's picture and without
        File dir = pictures();
        Story st = ScriptParser.parse(TOYS);
        for (Art art : new Art[]{new Art(), Art.fromManifest("char|चिंटू|toycar.png\n", st, new MakeFilm.AwtLoader(dir.getPath()))}) {
            Film f = direct(st, art);
            Renderer r = new Renderer(f, art);
            BufferedImage im = new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB);
            com.tarun.kahani.jvm.AwtGfx g = new com.tarun.kahani.jvm.AwtGfx(im);
            int drawn = 0;
            for (float t = 0; t < f.duration; t += 1f) { r.render(g, t); drawn++; }
            assertTrue(drawn > 10);
        }
    }
}

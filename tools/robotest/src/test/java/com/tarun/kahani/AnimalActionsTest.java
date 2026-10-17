package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.Look;
import com.tarun.kahani.core.Pose;
import com.tarun.kahani.core.ProductionDocument;
import com.tarun.kahani.core.Renderer;
import com.tarun.kahani.core.Rig;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;
import com.tarun.kahani.core.Synth;

import org.junit.Test;

/**
 * v47.7: the animals of the cast act what the script says (the user: "जुगाली करना, घास खाना, भौंकना, दौड़ना, चलना, घोड़े
 * का दौड़ना, पूंछ उठाना, कान हिलाना, सर हिलाना, बैठना, खड़े होना … they may speak as well"): a cow chews the cud and
 * grazes, a dog barks (its jaw snapping with the barks it is heard to give), holds its tail up and wags it, a horse
 * gallops and twitches its ears, an animal speaks its line — and nothing it does jumps from one frame to the next at
 * 24 frames a second. What the script says it does not do is not done. Nothing here is special to these stories: they
 * are made up for the test.
 */
public class AnimalActionsTest {
    static final String FARM = "पात्र:\n"
            + "1. मोती (कुत्ता): भूरा देसी कुत्ता, गले में लाल पट्टा\n"
            + "2. गौरी (गाय): सफेद गाय, सींग छोटे\n"
            + "3. बादल (घोड़ा): काला घोड़ा, लंबी अयाल\n"
            + "खेत की सुबह\n"
            + "दृश्य 1: गाँव का खेत\n"
            + "(स्थान: गाँव का हरा खेत। सुबह। गौरी पेड़ के नीचे बैठी जुगाली कर रही है।)\n"
            + "(मोती दौड़ता हुआ आता है और ज़ोर से भौंकता है। वह पूंछ उठाकर हिलाता है।)\n"
            + "(गौरी खड़ी होती है और घास खाने लगती है।)\n"
            + "(बादल सरपट दौड़ता हुआ आता है और रुककर कान हिलाता है।)\n"
            + "मोती: \"गौरी दीदी, आज बादल बहुत खुश है!\"\n"
            + "(गौरी सिर हिलाती है। मोती नहीं भौंकता, चुपचाप बैठ जाता है।)\n";

    static final String QUIET = "पात्र:\n"
            + "1. शेरू (कुत्ता): काला कुत्ता\n"
            + "2. राजू (10 साल): लड़का, नीली कमीज़\n"
            + "रात का पहरा\n"
            + "दृश्य 1: घर का आँगन\n"
            + "(रात। राजू आँगन में बैठा है। शेरू उसके पास खड़ा है।)\n"
            + "(शेरू बिल्कुल नहीं भौंकता।)\n"
            + "राजू: \"शाबाश शेरू!\"\n";

    static final class Made { Story st; Film film; }

    static Made make(String script) {
        Made m = new Made();
        m.st = ScriptParser.parse(script);
        Director d = new Director(m.st, new Director.Options());
        d.prepare();
        m.film = d.direct(new Art());
        return m;
    }

    static Film.Actor actor(Film f, String name) {
        for (Film.Seg sg : f.segs) for (Film.Actor a : sg.actors) if (a.c.displayName.equals(name)) return a;
        fail("no " + name);
        return null;
    }

    static Film.Act act(Film.Actor a, int item) {
        for (Film.Act x : a.acts) if (x.type == Film.G_ANIMAL && x.item == item) return x;
        return null;
    }

    static boolean sfxNear(Film f, int type, float t, float within) {
        for (Film.Sfx s : f.sfx) if (s.type == type && Math.abs(s.t - t) <= within) return true;
        return false;
    }

    @Test public void theCastsAnimalsAreReadAsAnimals() {
        Made m = make(FARM);
        assertEquals(Look.SP_DOG, actor(m.film, "मोती").look.species);
        assertEquals(Look.SP_COW, actor(m.film, "गौरी").look.species);
        assertEquals(Look.SP_HORSE, actor(m.film, "बादल").look.species);
        for (String n : new String[]{"मोती", "गौरी", "बादल"}) assertFalse(n, actor(m.film, n).look.isHumanoid());
    }

    @Test public void theCowSitsChewingTheCudThenGetsUpAndGrazes() {
        Made m = make(FARM);
        Film.Actor cow = actor(m.film, "गौरी");
        Film.Seg sg = null;
        for (Film.Seg s : m.film.segs) if (s.actors.contains(cow)) sg = s;
        // the description sets how things are from the start: she is sitting, chewing the cud
        assertEquals("seated from the start", Pose.SIT, cow.stateAt(sg.t0 + 0.5f).body);
        Film.Act chew = act(cow, Film.AN_CHEW);
        assertNotNull("she chews the cud", chew);
        assertTrue("from the start of the part", chew.t0 < sg.t0 + 1);
        // she gets up, then grazes
        Film.Act graze = act(cow, Film.AN_GRAZE);
        assertNotNull("she grazes", graze);
        assertEquals("standing to graze", Pose.STAND, cow.stateAt(graze.t0 + 1).body);
        // her head shake
        boolean shake = false;
        for (Film.Act x : cow.acts) if (x.type == Film.G_SHAKE_HEAD) shake = true;
        assertTrue("she shakes her head", shake);
    }

    @Test public void theDogRunsInBarksWithItsOwnVoiceAndHoldsItsTailUpWagging() {
        Made m = make(FARM);
        Film.Actor dog = actor(m.film, "मोती");
        boolean ran = false;
        for (Film.Key k : dog.keys) if (k.moveDur > 0 && k.run) ran = true;
        assertTrue("it runs in", ran);
        Film.Act bark = act(dog, Film.AN_VOICE);
        assertNotNull("it barks", bark);
        assertTrue("it barks once it is there", dog.stateAt(bark.t0).visible);
        assertTrue("its barks are heard as its jaw snaps", sfxNear(m.film, Film.SFX_BARK, bark.t0, 0.01f));
        // only once: "मोती नहीं भौंकता" later is no bark
        int barks = 0, voices = 0;
        for (Film.Act x : dog.acts) if (x.type == Film.G_ANIMAL && x.item == Film.AN_VOICE) voices++;
        for (Film.Sfx s : m.film.sfx) if (s.type == Film.SFX_BARK) barks++;
        assertEquals("one bark act", 1, voices);
        assertEquals("one bark heard", 1, barks);
        assertNotNull("tail held up", act(dog, Film.AN_TAIL_UP));
        assertNotNull("and wagged", act(dog, Film.AN_WAG));
        // it sits down quietly at the end
        Film.Key last = dog.keys.get(dog.keys.size() - 1);
        assertEquals("it sits down quietly", Pose.SIT, last.body);
    }

    @Test public void theHorseGallopsInAndTwitchesItsEars() {
        Made m = make(FARM);
        Film.Actor horse = actor(m.film, "बादल");
        boolean gallop = false;
        for (Film.Key k : horse.keys) if (k.moveDur > 0 && k.run) gallop = true;
        assertTrue("a gallop is a run, and stays one", gallop);
        Film.Act ears = act(horse, Film.AN_EARS);
        assertNotNull("it twitches its ears", ears);
        float arrived = 0;
        for (Film.Key k : horse.keys) if (k.moveDur > 0) arrived = Math.max(arrived, k.t + k.moveDur);
        assertTrue("after it has stopped", ears.t0 >= arrived - 0.01f);
    }

    @Test public void aNegatedBarkIsNoBark() {
        Made m = make(QUIET);
        Film.Actor dog = actor(m.film, "शेरू");
        assertNull("\"शेरू बिल्कुल नहीं भौंकता\" — no bark", act(dog, Film.AN_VOICE));
        for (Film.Sfx s : m.film.sfx) assertNotEquals("no bark heard", Film.SFX_BARK, s.type);
    }

    @Test public void theJawSnapsWithEachBarkHeard() {
        // the jaw is open at each bark the synthesiser makes and shut between them
        float dur = 1.4f;
        Film.Actor dog = actor(make(FARM).film, "मोती");
        Film.Act bark = act(dog, Film.AN_VOICE);
        assertEquals(dur, bark.t1 - bark.t0, 1e-4f);
        for (float on : Synth.barkOnsets(dur)) {
            Pose p = new Pose();
            float t = bark.t0 + on + Synth.BARK_LEN * 0.4f;
            p.time = t;
            Renderer.animalActs(dog, p, t);
            assertTrue("open with the bark at " + on + " s: " + p.animalJaw, p.animalJaw > 0.8f);
        }
        Pose between = new Pose();
        float tb = bark.t0 + 0.55f;          // the pause between the two pairs of barks
        between.time = tb;
        Renderer.animalActs(dog, between, tb);
        assertTrue("shut between the barks: " + between.animalJaw, between.animalJaw < 0.05f);
    }

    /** Every animal of the farm story, frame by frame at 24 fps: no jump in the jaw, the tail, the head or the ears. */
    @Test public void nothingAnAnimalDoesJumpsBetweenFrames() {
        Made m = make(FARM);
        int frames = 0;
        for (Film.Seg sg : m.film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            for (Film.Actor a : sg.actors) {
                if (a.look.isHumanoid()) continue;
                Rig.State prev = null;
                for (int f = (int) Math.ceil(sg.t0 * 24); f < sg.t1 * 24; f++) {
                    float t = f / 24f;
                    Rig.State st = stateAt(a, t);
                    if (prev != null) {
                        boolean barking = false;
                        for (Film.Act x : a.acts) if (x.type == Film.G_ANIMAL && x.item == Film.AN_VOICE && t >= x.t0 - 0.1f && t <= x.t1 + 0.1f) barking = true;
                        String at = a.c.displayName + " at frame " + f + String.format(java.util.Locale.ROOT, " (%.2f s)", t);
                        assertTrue("jaw " + at + ": " + prev.jaw + " -> " + st.jaw, Math.abs(st.jaw - prev.jaw) <= (barking ? 0.6f : 0.12f));     // (a bark is a snap: open in three frames)
                        assertTrue("tail " + at + ": " + prev.tail + " -> " + st.tail, Math.abs(st.tail - prev.tail) <= 14f);     // (a glad wag swings up to about 11 degrees a frame)
                        assertTrue("head " + at + ": " + prev.nod + " -> " + st.nod, Math.abs(st.nod - prev.nod) <= 0.3f);
                        assertTrue("ears " + at + ": " + prev.ear + " -> " + st.ear, Math.abs(st.ear - prev.ear) <= 0.5f);
                    }
                    prev = st;
                    frames++;
                }
            }
        }
        assertTrue("frames checked: " + frames, frames > 24 * 30);
    }

    /** An animal that speaks: its jaw follows the words, a little behind, never flickering from frame to frame. */
    @Test public void aSpeakingAnimalsJawIsSmooth() {
        Made m = make(FARM);
        Film.Actor dog = actor(m.film, "मोती");
        assertFalse("it has a line", dog.speaks.isEmpty());
        Film.Speak sp = dog.speaks.get(0);
        Renderer r = new Renderer(m.film, new Art());
        float prev = -1, most = 0;
        for (int f = (int) Math.ceil(sp.t0 * 24); f < sp.t1 * 24; f++) {
            float j = r.jawAt(dog, f / 24f);
            if (prev >= 0) assertTrue("jaw at frame " + f + ": " + prev + " -> " + j, Math.abs(j - prev) <= 0.15f);
            most = Math.max(most, j);
            prev = j;
        }
        assertTrue("the jaw moves with the words: " + most, most > 0.15f);
    }

    /** The production document checks the animals' own actions against the plan, and a cast dog's bark is no far-off dog. */
    @Test public void theProductionDocumentFindsTheAnimalsActionsInThePlan() {
        Made m = make(FARM);
        ProductionDocument d = ProductionDocument.read(m.st, null);
        d.bind(m.st);
        d.attach(m.film);
        d.assets(new Art());
        d.validate();
        int animalActs = 0, mustNot = 0;
        for (ProductionDocument.Action a : d.actions) {
            if (!"ANIMAL_DOES".equals(a.verb)) continue;
            // ("मोती नहीं भौंकता" is read as a thing it must not do, and not looked for in the plan)
            if (a.negated) { mustNot++; continue; }
            animalActs++;
            assertEquals(a.span, "IN_PLAN", a.status);
        }
        assertTrue("the animals' doings are actions of the document: " + animalActs, animalActs >= 4);
        assertEquals("the bark it does not give", 1, mustNot);
        for (ProductionDocument.Effect e : d.effects) assertNotEquals("a cast dog barking is not \"a dog barks far off\"", "SOUND_DOG", e.kind);
    }

    /** The rig state of an animal at t as the renderer builds it (its acts, its feelings of the last moments). */
    static Rig.State stateAt(Film.Actor a, float t) {
        Pose p = new Pose();
        p.time = t;
        p.seed = 3;
        Film.Key k = a.stateAt(t);
        p.emotion = k.emotion;
        p.body = k.body;
        p.sit = Renderer.sitAmount(a, t);          // (sitting down and getting up move through it, as in the renderer)
        p.lie = Renderer.lieAmount(a, t);
        Renderer.animalActs(a, p, t);
        int[] recent = new int[8];
        for (int i = 0; i < recent.length; i++) recent[i] = a.stateAt(t - i * 0.05f).emotion;
        Rig.State st = new Rig.State();
        Renderer.animalState(st, p, recent);
        return st;
    }
}

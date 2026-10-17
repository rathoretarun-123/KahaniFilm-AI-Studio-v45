package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.Look;
import com.tarun.kahani.core.Pose;
import com.tarun.kahani.core.Renderer;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import org.junit.Test;

/**
 * v47.8 (the user: "daily activities should also be checked properly as per the gifs and pictures I have provided for
 * reference"): the first twenty everyday activities of docs/reference/daily_activities/gif_descriptions_40_hi.txt, each
 * in a one-line direction of a made-up two-person story, are staged as the reference shows them — folding clothes and
 * digging with a spade are their own tasks with the cloth and the spade in hand; one found asleep in bed lies there
 * from the first frame and then sits up and stands; two sitting side by side hold hands from their seats; two named
 * together with a plural verb both play; a dog walked goes along on its lead — and the task motions move smoothly at
 * 24 frames a second (slow cycles, eased in and out, no jump from one frame to the next).
 */
public class DailyActivitiesATest {
    static final String HEAD = "पात्र:\n1. मीना (30 साल): औरत, नीली साड़ी\n2. राजू (10 साल): लड़का, लाल कमीज\nकहानी\n"
            + "दृश्य 1: घर\n(स्थान: घर का कमरा। सुबह।)\n";

    static Film film(String... directions) {
        StringBuilder sb = new StringBuilder(HEAD);
        for (String d : directions) sb.append('(').append(d).append(")\n");
        Story st = ScriptParser.parse(sb.toString());
        Director d = new Director(st, new Director.Options());
        d.prepare();
        return d.direct(new Art());
    }

    static Film.Seg scene(Film f) {
        for (Film.Seg s : f.segs) if (s.type == Film.S_SCENE) return s;
        throw new AssertionError("no scene");
    }

    static Film.Actor actor(Film.Seg s, String name) {
        for (Film.Actor a : s.actors) if (a.c.displayName.equals(name)) return a;
        throw new AssertionError("no " + name);
    }

    static Film.Act act(Film.Actor a, int type, int item) {
        for (Film.Act x : a.acts) if (x.type == type && (item < 0 || x.item == item)) return x;
        return null;
    }

    static boolean moves(Film.Actor a) {
        for (Film.Key k : a.keys) if (k.moveDur > 0) return true;
        return false;
    }

    @Test
    public void foldingAndDiggingAreTheirOwnTasksWithTheirThingsInHand() {
        Film f = film("मीना टेबल पर कपड़े तह करती है।");
        Film.Act fold = act(actor(scene(f), "मीना"), Film.G_TASK, Film.T_FOLD);
        assertNotNull("folding clothes is staged as the fold task", fold);
        Film g = film("राजू बगीचे में फावड़े से मिट्टी खोदता है।");
        Film.Actor raju = actor(scene(g), "राजू");
        assertNotNull("digging with a spade is staged as the dig task", act(raju, Film.G_TASK, Film.T_DIG));
        assertNull("digging is no push", act(raju, Film.G_PUSH, -1));
        // nothing else is folded or dug
        Film h = film("मीना टेबल के पास खड़ी है।");
        assertNull(act(actor(scene(h), "मीना"), Film.G_TASK, Film.T_FOLD));
    }

    @Test
    public void theEverydayActivitiesAlreadyStagedStayStaged() {
        assertNotNull("make-up at the mirror", act(actor(scene(film("मीना दर्पण के सामने बैठकर मेकअप लगाती है और होंठों पर लिपस्टिक लगाती है।")), "मीना"), Film.G_MAKEUP, -1));
        assertNotNull("combing", act(actor(scene(film("मीना अपने लंबे बालों में कंघी करती है।")), "मीना"), Film.G_TASK, Film.T_COMB));
        assertNotNull("brushing the teeth", act(actor(scene(film("राजू सिंक के सामने टूथब्रश से दांत साफ करता है।")), "राजू"), Film.G_TASK, Film.T_BRUSH));
        assertNotNull("cooking", act(actor(scene(film("मीना स्टोव पर पैन में खाना पकाती है और चम्मच से चलाती है।")), "मीना"), Film.G_TASK, Film.T_COOK));
        assertNotNull("tea", act(actor(scene(film("मीना कुर्सी पर बैठकर कप से चाय पीती है।")), "मीना"), Film.G_DRINK, -1));
        assertNotNull("breakfast", act(actor(scene(film("राजू टेबल पर बैठकर कटोरे से चम्मच से नाश्ता करता है।")), "राजू"), Film.G_EAT, -1));
        assertNotNull("push-ups", act(actor(scene(film("राजू फर्श पर पुशअप्स करता है।")), "राजू"), Film.G_EXERCISE, -1));
        Film limp = film("राजू एक पैर में चोट के कारण लंगड़ाकर चलता है।");
        Film.Actor raju = actor(scene(limp), "राजू");
        assertTrue("the limp is a walk", moves(raju));
        assertTrue("with the limping gait", (raju.look.condition & Look.C_LIMP) != 0);
    }

    @Test
    public void oneFoundAsleepLiesInBedFromTheFirstFrameThenSitsUpAndStands() {
        Film f = film("मीना बिस्तर पर सो रही है।", "मीना उठकर बिस्तर पर बैठती है और फिर खड़ी हो जाती है।");
        Film.Seg s = scene(f);
        Film.Actor m = actor(s, "मीना");
        Film.Key first = m.stateAt(s.t0 + 0.05f);
        assertEquals("lying from the scene's first frame", Pose.LIE, first.body);
        assertTrue("asleep from the first frame", first.eyesShut);
        float sat = -1, stood = -1;
        for (float t = s.t0; t < s.t1; t += 0.1f) {
            int b = m.stateAt(t).body;
            if (b == Pose.SIT && sat < 0) sat = t;
            if (b == Pose.STAND && sat >= 0 && stood < 0) stood = t;
        }
        assertTrue("she sits up", sat > s.t0 + 1);
        assertTrue("and then stands", stood > sat);
        // lying down before us is still a movement: "सो जाती है" sits down first, then lies back
        Film g = film("मीना थककर बिस्तर पर सो जाती है।");
        Film.Seg s2 = scene(g);
        assertNotEquals(Pose.LIE, actor(s2, "मीना").stateAt(s2.t0 + 0.05f).body);
    }

    @Test
    public void twoSittingSideBySideHoldHandsFromTheirSeats() {
        Film f = film("मीना और राजू साथ बैठकर एक दूसरे का हाथ पकड़े हुए हैं।");
        Film.Seg s = scene(f);
        Film.Actor m = actor(s, "मीना"), r = actor(s, "राजू");
        Film.Act hold = null;
        for (Film.Actor a : s.actors) for (Film.Act x : a.acts) if (x.type == Film.G_HOLD_HAND) hold = x;
        assertNotNull("they hold hands", hold);
        for (float t = hold.t0; t < Math.min(hold.t1, s.t1); t += 0.1f) {
            assertEquals("मीना stays seated holding hands at " + t, Pose.SIT, m.stateAt(t).body);
            assertEquals("राजू sits beside her at " + t, Pose.SIT, r.stateAt(t).body);
        }
    }

    @Test
    public void twoNamedTogetherWithAPluralVerbBothPlay() {
        for (String d : new String[]{"राजू और मीना पार्क में खेलते हैं।", "राजू और मीना पार्क में दौड़ते और खेलते हैं।"}) {
            Film.Seg s = scene(film(d));
            assertNotNull("राजू plays: " + d, act(actor(s, "राजू"), Film.G_PLAY, -1));
            assertNotNull("मीना plays too: " + d, act(actor(s, "मीना"), Film.G_PLAY, -1));
        }
        // one who plays with the other ("के साथ", a singular verb) is the one named first only
        Film.Seg s = scene(film("राजू मीना के साथ खेलता है।"));
        assertNotNull(act(actor(s, "राजू"), Film.G_PLAY, -1));
        assertNull(act(actor(s, "मीना"), Film.G_PLAY, -1));
    }

    @Test
    public void aDogWalkedGoesAlongOnItsLead() {
        Film.Seg s = scene(film("राजू पार्क में अपने कुत्ते को पट्टे से घुमाता है।"));
        Film.Actor raju = actor(s, "राजू");
        assertTrue("he walks", moves(raju));
        Film.Fx dog = null;
        for (Film.Fx x : s.fx) if (x.type == Film.FX_ANIMAL && x.kind == Look.SP_DOG) dog = x;
        assertNotNull("the dog is there", dog);
        assertEquals("on the lead: it goes along with him", 3f, dog.y, 0f);
        assertSame(raju, dog.a);
    }

    /** The fold and the dig at 24 fps: slow cycles (under 6 a second), eased in and out, never a jump between frames. */
    @Test
    public void foldAndDigMoveSmoothlyFrameByFrame() throws Exception {
        java.lang.reflect.Method apply = Renderer.class.getDeclaredMethod("applyActs", Film.Actor.class, Pose.class, float.class);
        apply.setAccessible(true);
        Object[][] cases = {{"मीना टेबल पर कपड़े तह करती है।", "मीना", Film.T_FOLD}, {"राजू बगीचे में फावड़े से मिट्टी खोदता है।", "राजू", Film.T_DIG}};
        for (Object[] c : cases) {
            Film f = film((String) c[0]);
            Film.Actor a = actor(scene(f), (String) c[1]);
            Film.Act task = act(a, Film.G_TASK, (Integer) c[2]);
            assertNotNull(task);
            Renderer r = new Renderer(f, new Art());
            Pose prev = null;
            int frames = 0, turns = 0;
            float lastD = 0;
            for (int i = (int) ((task.t0 - 0.5f) * 24); i <= (int) ((task.t1 + 0.5f) * 24); i++) {
                float t = i / 24f;
                Pose p = new Pose();
                apply.invoke(r, a, p, t);
                if (prev != null) {
                    String at = c[1] + " frame " + i;
                    assertTrue("right arm " + at + ": " + prev.armR + " -> " + p.armR, Math.abs(p.armR - prev.armR) <= 6f);
                    assertTrue("left arm " + at, Math.abs(p.armL - prev.armL) <= 6f);
                    assertTrue("elbows " + at + ": " + prev.elbowR + "/" + prev.elbowL + " -> " + p.elbowR + "/" + p.elbowL,
                            Math.abs(p.elbowR - prev.elbowR) <= 9f && Math.abs(p.elbowL - prev.elbowL) <= 9f);
                    assertTrue("lean " + at, Math.abs(p.tilt - prev.tilt) <= 2.5f);
                    assertTrue("head " + at, Math.abs(p.nod - prev.nod) <= 0.06f);
                    float dd = p.armR - prev.armR;
                    if (dd * lastD < 0) turns++;
                    if (dd != 0) lastD = dd;
                }
                if (t >= task.t0 && t < task.t1) assertTrue("the thing is in the hand", p.holdR == ((Integer) c[2] == Film.T_FOLD ? Pose.I_CLOTH : Pose.I_SPADE));
                prev = p;
                frames++;
            }
            float secs = task.t1 - task.t0;
            assertTrue(c[1] + ": " + frames + " frames", frames > 24 * 3);
            assertTrue(c[1] + ": the arm turns back " + turns + " times in " + secs + " s (under 6 cycles a second)", turns / 2f / secs < 6);
        }
    }
}

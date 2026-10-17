package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.Pose;
import com.tarun.kahani.core.Renderer;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import org.junit.Test;

/**
 * v47.8 (the user: "daily activities should also be checked properly as per the gifs and pictures I have provided for
 * reference"): the second half of the reference activities (docs/reference/daily_activities/gif_descriptions_40_hi.txt,
 * 21–40) are staged as the reference shows them, also where another rule took the sentence first (a sit, a pick-up):
 * the bed is there to be made, the one meditating sits cross-legged on the floor with the eyes shut, two on a sofa
 * talk in turn, the rubbish goes out through the room's door, a turn on the spot is a whole eased turn, three
 * watching television sit and laugh one after another, the shoelaces are tied down on one knee and up again, the
 * plants are watered (not washed). What the words deny is not done. The stories are made up for the test.
 */
public class DailyActivitiesBTest {
    static final String CAST = "पात्र:\n1. मीना (8 वर्ष): लड़की, गुलाबी फ्रॉक, दो चोटियाँ\n2. राजू (10 वर्ष): लड़का, नीली कमीज\n"
            + "3. दादाजी (70 वर्ष): बूढ़े आदमी, सफेद कुर्ता\n4. माँ (35 वर्ष): औरत, हरी साड़ी\n";

    static Film make(String place, String what) {
        String script = CAST + "छोटी कहानी\nदृश्य 1: " + place + "\n(स्थान: " + place + "। दोपहर। " + what + ")\nमीना: \"अच्छा।\"\n";
        Story st = ScriptParser.parse(script);
        Director d = new Director(st, new Director.Options());
        d.prepare();
        return d.direct(new Art());
    }

    static Film.Seg scene(Film f) {
        for (Film.Seg s : f.segs) if (s.type == Film.S_SCENE) return s;
        fail("no scene part");
        return null;
    }

    static Film.Actor who(Film.Seg s, String name) {
        for (Film.Actor a : s.actors) if (name.equals(a.c.displayName)) return a;
        fail(name + " is not in the part");
        return null;
    }

    static java.util.List<Film.Act> acts(Film.Actor a, int type) {
        java.util.List<Film.Act> out = new java.util.ArrayList<Film.Act>();
        for (Film.Act x : a.acts) if (x.type == type) out.add(x);
        return out;
    }

    @Test
    public void theBedIsThereToBeMade() {
        Film f = make("घर का कमरा", "माँ बिस्तर पर चादर बिछाती है और तकिए ठीक करती है।");
        Film.Seg s = scene(f);
        Film.Fx bed = null;
        for (Film.Fx x : s.fx) if (x.type == Film.FX_SEAT && x.kind == Film.SEAT_BED) bed = x;
        assertNotNull("a bed stands where the bed is made", bed);
        assertTrue("the bed is there from the start of the part (it does not appear)", bed.t0 <= s.t0 + 0.01f);
        assertFalse("she reaches over it", acts(who(s, "माँ"), Film.G_REACH).isEmpty());
    }

    @Test
    public void meditationIsCrossLeggedOnTheFloorWithTheEyesShut() {
        Film f = make("घर का कमरा", "दादाजी पालथी मारकर बैठते हैं और आँखें बंद करके ध्यान करते हैं।");
        Film.Seg s = scene(f);
        Film.Actor a = who(s, "दादाजी");
        float shut = 0;
        boolean floor = false, chair = false;
        for (float t = s.t0; t < s.t1; t += 0.1f) {
            Film.Key k = a.stateAt(t);
            if (k.body == Pose.SIT && k.seat == Film.SEAT_FLOOR) floor = true;
            if (k.seat == Film.SEAT_CHAIR) chair = true;
            if (k.eyesShut) shut += 0.1f;
        }
        assertTrue("sat on the floor (पालथी)", floor);
        assertFalse("not on a chair", chair);
        assertTrue("the eyes stay shut a while (" + shut + " s)", shut >= 3.5f);
    }

    @Test
    public void twoOnASofaTalkInTurn() {
        Film f = make("घर का कमरा", "माँ और दादाजी सोफे पर बैठकर बातें करते हैं।");
        Film.Seg s = scene(f);
        Film.Actor m = who(s, "माँ"), g = who(s, "दादाजी");
        java.util.List<Film.Act> tm = acts(m, Film.G_TALK), tg = acts(g, Film.G_TALK);
        assertTrue("both talk", tm.size() >= 2 && tg.size() >= 2);
        for (Film.Act x : tm) for (Film.Act y : tg)
            assertTrue("one talks while the other listens", x.t1 <= y.t0 + 0.01f || y.t1 <= x.t0 + 0.01f);
        for (Film.Act x : tm) assertEquals("seated while she talks", Pose.SIT, m.stateAt((x.t0 + x.t1) / 2).body);
        for (Film.Act y : tg) assertEquals("seated while he talks", Pose.SIT, g.stateAt((y.t0 + y.t1) / 2).body);
    }

    @Test
    public void theRubbishGoesOutThroughTheDoor() {
        Film f = make("घर का दरवाज़ा", "माँ कचरे की थैली उठाकर बाहर ले जाती है।");
        Film.Seg s = scene(f);
        boolean door = false;
        for (Film.Fx x : s.fx) if (x.type == Film.FX_DOOR) door = true;
        assertTrue("the room's door is used", door);
        assertFalse("she has gone out by the end of the part", who(s, "माँ").stateAt(s.t1 - 0.1f).visible);
    }

    @Test
    public void aTurnOnTheSpotIsOneWholeEasedTurn() {
        Film f = make("घर का आँगन", "मीना अपनी जगह पर पूरा एक चक्कर घूमती है।");
        Film.Seg s = scene(f);
        Film.Actor a = who(s, "मीना");
        java.util.List<Film.Act> sp = acts(a, Film.G_SPIN);
        assertEquals("one turn", 1, sp.size());
        Film.Act x = sp.get(0);
        float x0 = a.stateAt(x.t0).x;
        assertEquals("on the spot", x0, a.stateAt(x.t1 - 0.05f).x, 1f);
        float last = Renderer.spinAt(a, x.t0), narrow = 1;
        boolean other = false;
        assertEquals("starts facing as before", 1f, Math.abs(last), 0.02f);
        for (int i = 1; i <= Math.round((x.t1 - x.t0) * 24); i++) {
            float t = x.t0 + i / 24f, w = Renderer.spinAt(a, Math.min(t, x.t1 - 1e-4f));
            assertTrue("no jump in width between two frames at " + t, Math.abs(Math.abs(w) - Math.abs(last)) <= 0.2f);
            narrow = Math.min(narrow, Math.abs(w));
            if (w < 0) other = true;
            last = w;
        }
        assertTrue("edge-on at its narrowest", narrow <= 0.42f);
        assertTrue("the other side shows halfway round", other);
        assertEquals("back as before at the end", 1f, Math.abs(Renderer.spinAt(a, x.t1 - 1e-4f)), 0.02f);
        assertEquals("not spinning after it", 1f, Renderer.spinAt(a, x.t1 + 0.1f), 0f);
    }

    @Test
    public void threeWatchingTelevisionSitAndLaughInTurn() {
        Film f = make("घर का कमरा", "माँ, राजू और मीना सोफे पर बैठकर साथ में टीवी देखते हैं।");
        Film.Seg s = scene(f);
        java.util.List<Float> starts = new java.util.ArrayList<Float>();
        for (String n : new String[]{"माँ", "राजू", "मीना"}) {
            Film.Actor a = who(s, n);
            java.util.List<Film.Act> l = acts(a, Film.G_LAUGH);
            assertFalse(n + " laughs at the show", l.isEmpty());
            assertEquals(n + " is seated on the sofa", Film.SEAT_SOFA, a.stateAt(l.get(0).t0).seat);
            starts.add(l.get(0).t0);
        }
        for (int i = 0; i < starts.size(); i++) for (int j = i + 1; j < starts.size(); j++)
            assertTrue("not all on one frame", Math.abs(starts.get(i) - starts.get(j)) >= 0.2f);
    }

    @Test
    public void shoelacesAreTiedOnOneKneeAndUpAgain() {
        Film f = make("घर का दरवाज़ा", "राजू झुककर अपने जूतों के फीते बांधता है।");
        Film.Seg s = scene(f);
        Film.Actor a = who(s, "राजू");
        Film.Act reach = null;
        for (Film.Act x : acts(a, Film.G_REACH)) if (x.item == 1) reach = x;
        assertNotNull("the hands go down to the shoes", reach);
        assertEquals("on one knee for it", Pose.KNEEL, a.stateAt((reach.t0 + reach.t1) / 2).body);
        assertEquals("up again once they are tied", Pose.STAND, a.stateAt(s.t1 - 0.1f).body);
    }

    @Test
    public void thePlantsAreWateredNotWashed() {
        Film f = make("बगीचा", "दादाजी बगीचे में पौधों को पानी देते हैं।");
        Film.Seg s = scene(f);
        int water = 0, other = 0;
        for (Film.Act x : acts(who(s, "दादाजी"), Film.G_TASK)) if (x.item == Film.T_WATER) water++; else other++;
        assertEquals("watered once", 1, water);
        assertEquals("no washing or another task over it", 0, other);
    }

    @Test
    public void wipingTheSweatIsNoCleaning() {
        Film f = make("घर का आँगन", "राजू तेज़ी से दौड़ता है और माथे से पसीना पोंछता है।");
        for (Film.Act x : acts(who(scene(f), "राजू"), Film.G_TASK))
            assertNotEquals("no window wiped or floor swept for the sweat", Film.T_SWEEP, x.item);
    }

    @Test
    public void whatTheWordsDenyIsNotDone() {
        Film f = make("घर का आँगन", "मीना अपनी जगह पर चक्कर नहीं घूमती।");
        assertTrue("no turn", acts(who(scene(f), "मीना"), Film.G_SPIN).isEmpty());
        Film g = make("घर का कमरा", "माँ और दादाजी सोफे पर बैठते हैं पर बातें नहीं करते।");
        assertTrue("no talk staged for the one who does not talk", acts(who(scene(g), "दादाजी"), Film.G_TALK).isEmpty());
    }
}

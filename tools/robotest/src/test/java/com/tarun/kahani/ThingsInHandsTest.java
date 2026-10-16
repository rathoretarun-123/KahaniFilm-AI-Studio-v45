package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * v47.4 (the user: "any object just can't enter the frame floating from anywhere — it must be in the person's bag and he
 * must have taken it out"): a thing's picture is never an insert over the frame; the sentence stages it — taken out of a
 * bag or a box in a hand, held, held out and taken by the other, lying in its place from the start of the part, put down
 * at the feet — and it carries from part to part.
 */
public class ThingsInHandsTest {
    static Art.Shot thing(Art art, String key) {
        Art.Shot s = new Art.Shot();
        s.scene = ""; s.keys = new String[]{key}; s.object = true;
        s.pic = new Art.Backdrop(); s.pic.img = new Object(); s.pic.w = 120; s.pic.h = 90; s.pic.object = true;
        art.shots.add(s);
        return s;
    }
    static List<Film.Fx> held(Film film, Art.Backdrop pic) {
        List<Film.Fx> out = new ArrayList<Film.Fx>();
        for (Film.Seg s : film.segs) for (Film.Fx f : s.fx) if (f.type == Film.FX_HELD && f.pic == pic) out.add(f);
        return out;
    }
    static String who(Film.Fx f) { return f.a == null ? "-" : f.a.c.shown(); }

    @Test public void aakhiriJodiThingsAreTakenOutHeldAndPlacedNeverInserted() throws Exception {
        File script = new File("../../tools/testdata/aakhiri_jodi/aakhiri_jodi_screenplay.txt");
        assertTrue(script.isFile());
        Story story = ScriptParser.parse(new String(Files.readAllBytes(script.toPath()), StandardCharsets.UTF_8));
        Art art = new Art();
        Art.Shot shoe = thing(art, "टूटा हुआ काला जूता"), pair = thing(art, "लाल जूतों की जोड़ी"), glass = thing(art, "स्टील का गिलास"),
                box = thing(art, "लाल जूतों का डिब्बा"), paper = thing(art, "पुराना अखबार"), tools = thing(art, "औजारों वाला बक्सा"), hut = thing(art, "लकड़ी का खोखा");
        Director d = new Director(story, new Director.Options());
        d.prepare();
        Film film = d.direct(art);
        for (Film.Seg s : film.segs) for (Film.Fx f : s.fx) assertFalse("no insert of a thing over the frame", f.type == Film.FX_SHOT && f.pic != null && f.pic.object);
        Film.Seg scene = null;
        for (Film.Seg s : film.segs) if (s.type == Film.S_SCENE) { scene = s; break; }
        assertNotNull(scene);
        // lying in their places from the start of the part: the newspaper under him, the glass and the toolbox beside him
        for (Art.Shot t : new Art.Shot[]{glass, paper, tools}) {
            List<Film.Fx> h = held(film, t.pic);
            assertTrue(t.keys[0] + " lies placed", !h.isEmpty() && h.get(0).kind == Film.THING_PLACE);
            assertEquals(t.keys[0] + " is there from the start of the part", scene.t0, h.get(0).t0, 0.01f);
            assertEquals(t.keys[0] + " lies beside इस्माइल", "इस्माइल", who(h.get(0)));
        }
        assertTrue("the hut is the place, never a picture of its own", held(film, hut.pic).isEmpty());
        // the broken shoe: in नन्ही's hand as she runs in, put down (it lies at her feet), picked up by इस्माइल
        List<Film.Fx> sh = held(film, shoe.pic);
        assertTrue("the shoe is staged: " + sh.size(), sh.size() >= 3);
        assertTrue("first in नन्ही's hand", sh.get(0).kind == Film.THING_HOLD && who(sh.get(0)).equals("नन्ही"));
        boolean down = false, picked = false;
        for (Film.Fx f : sh) { if (f.kind == Film.THING_PLACE && f.t0 > sh.get(0).t0) down = true; if (f.kind == Film.THING_HOLD && who(f).equals("इस्माइल")) picked = true; }
        assertTrue("put down at her feet", down);
        assertTrue("picked up by इस्माइल", picked);
        assertTrue("her hold ended when she threw it down", sh.get(0).t1 < 1e5f);
        // the box: taken out from behind the toolbox lying beside him (its picture is the box it comes from)
        List<Film.Fx> bx = held(film, box.pic);
        assertTrue("the box is taken out by इस्माइल", !bx.isEmpty() && bx.get(0).kind == Film.THING_TAKE && who(bx.get(0)).equals("इस्माइल"));
        assertTrue("the taking out lasts 1.4 s", bx.get(0).t2 > bx.get(0).t0 + 1.3f);
        assertEquals("from behind the toolbox picture beside him", 3, bx.get(0).a.bagKind);
        // the red pair: taken out of the box he holds out (not out of a bag of her own), gone when she puts them on
        List<Film.Fx> rp = held(film, pair.pic);
        assertTrue("the red shoes are taken out by नन्ही", !rp.isEmpty() && rp.get(0).kind == Film.THING_TAKE && who(rp.get(0)).equals("नन्ही"));
        assertEquals("out of the box held out to her", 4, rp.get(0).a.bagKind);
        assertTrue("worn: the hold ends", rp.get(0).t1 < 1e5f);
        assertTrue("the box leaves his hands when the shoes come out of it", bx.get(0).t1 <= rp.get(0).t0 + 0.5f);
    }

    @Test public void theLetterComesOutOfTheBagChangesHandsAndCarriesIntoTheNextPart() throws Exception {
        File assets = new File(System.getProperty("kahani.assets", "../../app/src/main/assets"));
        String source = new String(Files.readAllBytes(new File(assets, "babu_ke_liye_screenplay.txt").toPath()), StandardCharsets.UTF_8);
        Story story = ScriptParser.parse(source);
        Art art = new Art();
        Art.Shot letter = thing(art, "पुराना पीला ख़त"), bench = thing(art, "पुरानी लकड़ी की बेंच"), stick = thing(art, "बाबू की लकड़ी की छड़ी");
        Director d = new Director(story, new Director.Options());
        d.prepare();
        Film film = d.direct(art);
        assertTrue("the bench and the stick are the place's and the studio's, never pictures of their own", held(film, bench.pic).isEmpty() && held(film, stick.pic).isEmpty());
        List<Film.Fx> lt = held(film, letter.pic);
        assertTrue("the letter is staged: " + lt.size(), lt.size() >= 3);
        assertTrue("taken out of her bag by the girl (\"पत्र निकालती है\" names it by another word)", lt.get(0).kind == Film.THING_TAKE && who(lt.get(0)).equals("बच्ची") && lt.get(0).a.bagKind == 1);
        assertTrue("her bag is there from the start of her part", lt.get(0).a.bagFrom <= lt.get(0).t0 - 1);
        boolean given = false, taken = false;
        for (Film.Fx f : lt) { if (f.kind == Film.THING_GIVE && who(f).equals("बच्ची")) given = true; if (f.kind == Film.THING_HOLD && who(f).equals("बाबू")) taken = true; }
        assertTrue("held out by the girl", given);
        assertTrue("in बाबू's hands", taken);
        // the part after: still in his hands from its first moment (nothing comes and goes between parts)
        Film.Fx last = lt.get(lt.size() - 1);
        Film.Seg seg = film.segAt(last.t0 + 0.1f);
        assertEquals("carried into the next part from its start", seg.t0, last.t0, 0.01f);
        assertEquals("in बाबू's hands there too", "बाबू", who(last));
        for (Film.Seg s : film.segs) for (Film.Fx f : s.fx) assertFalse(f.type == Film.FX_SHOT && f.pic != null && f.pic.object);
    }
}

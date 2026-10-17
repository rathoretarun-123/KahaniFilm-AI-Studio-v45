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
import java.util.List;
import org.junit.Test;

/**
 * v47.9 (the sofa story, with the user's pictures of its things): the script calls "भूरा टेडी बियर" just "टेडी"; the
 * book is "गोद में खुली" (open in her lap) and closed in a line's manner ("किताब धीरे से बंद करके गोद में रखते हुए"); the
 * fan's wind stirring its pages opens nothing; the sofa she sits on is never in her hand; and a thing the room's
 * picture already shows on its sofa (the red cushion) is not pasted over it again.
 */
public class SofaThingsTest {
    static Film film(Art art) throws Exception {
        File script = new File("../../tools/testdata/sofe_wali_jadu/sofe_wali_jadu_screenplay.txt");
        assertTrue(script.isFile());
        Story story = ScriptParser.parse(new String(Files.readAllBytes(script.toPath()), StandardCharsets.UTF_8));
        Director d = new Director(story, new Director.Options());
        d.prepare();
        return d.direct(art);
    }

    @Test public void theTeddyTheBookAndTheSofaAreReadAsAPersonWould() throws Exception {
        Art art = new Art();
        Art.Shot teddy = ThingsInHandsTest.thing(art, "भूरा टेडी बियर"), book = ThingsInHandsTest.thing(art, "चित्रों वाली कहानी की किताब"),
                sofa = ThingsInHandsTest.thing(art, "तीन सीटर वाला सोफा");
        Film film = film(art);
        List<Film.Fx> td = ThingsInHandsTest.held(film, teddy.pic);
        assertFalse("the teddy is staged (\"बीच में टेडी रखा है\")", td.isEmpty());
        assertEquals("first it lies between them", Film.THING_PLACE, td.get(0).kind);
        boolean inHer = false;
        for (Film.Fx f : td) if (f.kind == Film.THING_HOLD && "वनुषा".equals(ThingsInHandsTest.who(f))) inHer = true;
        assertTrue("then वनुषा holds it as she speaks (\"टेडी को गोद में कस कर पकड़े हुए\")", inHer);
        List<Film.Fx> bk = ThingsInHandsTest.held(film, book.pic);
        assertFalse("the book is staged", bk.isEmpty());
        Film.Fx b0 = bk.get(0);
        assertEquals("in her lap: held", Film.THING_HOLD, b0.kind);
        assertEquals("by वृंदा", "वृंदा", ThingsInHandsTest.who(b0));
        assertTrue("open from the first: " + b0.openFrom + " vs " + b0.t0, b0.openFrom <= b0.t0 + 0.01f);
        assertTrue("closed by the line's manner", b0.openUntil < 1e5f);
        for (Film.Fx f : bk) assertFalse("the wind in its pages opens it no second time", f != b0 && f.openFrom < 1e5f);
        assertTrue("the sofa is never in a hand", ThingsInHandsTest.held(film, sofa.pic).isEmpty()
                || ThingsInHandsTest.held(film, sofa.pic).get(0).kind == Film.THING_PLACE);
    }

    /** A grey sofa 160×90 with a red cushion on its left arm; a red thing is already there, a brown one is not. */
    @Test public void aThingThePictureShowsOnItsSofaIsKnown() {
        int w = 160, h = 90;
        int[] room = new int[w * h];
        for (int i = 0; i < room.length; i++) room[i] = 0xFFF2E6D2;                      // a cream wall
        for (int y = 45; y < 80; y++) for (int x = 30; x < 125; x++) room[y * w + x] = 0xFF8C8C8C;       // the sofa
        for (int y = 48; y < 64; y++) for (int x = 30; x < 46; x++) room[y * w + x] = 0xFFB4281E;       // a warm red cushion
        Art.Backdrop b = new Art.Backdrop();
        b.small = room; b.sw = w; b.sh = h;
        b.seatU0 = 30 / 160f; b.seatU1 = 125 / 160f; b.seatV = 0.72f; b.floorV = 0.9f;
        assertTrue("the crimson cushion's picture: already on the sofa", Art.shownOnSofa(b, thing(0xFFC81C28)));
        assertFalse("a brown teddy: not in the picture", Art.shownOnSofa(b, thing(0xFF73503A)));
        assertFalse("a grey thing on a grey sofa is never taken for it", Art.shownOnSofa(b, thing(0xFF8A8A8A)));
        b.seatU0 = -1;
        assertFalse("no sofa found: nothing is in it", Art.shownOnSofa(b, thing(0xFFC81C28)));
    }

    static Art.Backdrop thing(int colour) {
        Art.Backdrop t = new Art.Backdrop();
        t.sw = 40; t.sh = 40; t.small = new int[40 * 40];
        java.util.Arrays.fill(t.small, colour);
        return t;
    }
}

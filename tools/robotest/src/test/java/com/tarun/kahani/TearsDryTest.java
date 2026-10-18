package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import org.junit.Test;

/**
 * v47.11 (the user: "facial expressions"): tears come with a sad line and dry with a line of another feeling — they ran
 * on down her cheeks through her surprise.
 */
public class TearsDryTest {
    static Film.Actor vrinda(String secondManner) throws Exception {
        String script = "शीर्षक: आँसू\n\nपात्र\n\n1. वृंदा (11 साल)\n(पतली लड़की)\n\nस्थान\n\n1. घर का कमरा\n(क्रीम दीवारें)\n\n"
                + "कहानी\n\nदृश्य 1\n(वृंदा कमरे में खड़ी है)\n\nवृंदा: \"मेरी गुड़िया टूट गई।\" (उदास होकर, रोते हुए)\n\n"
                + "वृंदा: \"अरे! ये तो यहीं पड़ी है!\" (" + secondManner + ")\n\nसमाप्त\n";
        Story story = ScriptParser.parse(script);
        Director d = new Director(story, new Director.Options());
        d.prepare();
        Film film = d.direct(new Art());
        for (Film.Seg seg : film.segs) for (Film.Actor a : seg.actors) if ("वृंदा".equals(a.c.shown()) && !film.lines.isEmpty()) return a;
        fail("वृंदा is on the stage");
        return null;
    }

    @Test public void tearsDryWithASurprise() throws Exception {
        Film.Actor a = vrinda("हैरानी से आँखें बड़ी करके");
        boolean cried = false, dryAtEnd;
        float last = 0;
        for (Film.Key k : a.keys) { if (k.tears) cried = true; last = Math.max(last, k.t); }
        dryAtEnd = !a.stateAt(last + 0.5f).tears;
        assertTrue("the sad line brings tears", cried);
        assertTrue("the surprised line has dried them", dryAtEnd);
    }

    @Test public void tearsStayThroughAnotherSadLine() throws Exception {
        Film.Actor a = vrinda("उदास होकर");
        float last = 0;
        for (Film.Key k : a.keys) last = Math.max(last, k.t);
        assertTrue("still crying", a.stateAt(last + 0.5f).tears);
    }
}

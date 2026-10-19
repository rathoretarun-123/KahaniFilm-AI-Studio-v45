package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.Pose;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import org.junit.Test;

/**
 * v47.12 (a new village story made end to end): "मोहन फिसलकर गिर जाता है" … "दादी … मोहन के पास जाती हैं और उसे उठाती
 * हैं" — he stays down until she has come to him (he used to stand up by himself, and she lifted no one); with no one
 * coming for him he gets up by himself, as before.
 */
public class FallHelpTest {
    static Film film(String afterFall) {
        Story st = ScriptParser.parse("शीर्षक: खेत\n\nपात्र\n\n1. मोहन (10 साल)\n(दुबला लड़का)\n\n2. दादी (65 साल)\n(सफ़ेद साड़ी)\n\n"
                + "स्थान\n\n1. गाँव का खेत\n(हरे खेत)\n\nकहानी\n\nदृश्य 1\n(दादी पेड़ के नीचे बैठी हैं। मोहन खेत में खड़ा है।)\n\n"
                + "(मोहन फिसलकर गिर जाता है।)\n\nमोहन: \"आह! मेरा घुटना!\" (दर्द से)\n\n" + afterFall + "\n\nसमाप्त\n");
        Director d = new Director(st, new Director.Options());
        d.prepare();
        return d.direct(new Art());
    }

    static Film.Actor actor(Film f, String name) {
        for (Film.Seg seg : f.segs) for (Film.Actor a : seg.actors) if (name.equals(a.c.shown())) return a;
        fail(name);
        return null;
    }

    @Test public void heStaysDownUntilSheComes() {
        Film f = film("(दादी उठकर मोहन के पास जाती हैं और उसे उठाती हैं)");
        Film.Actor mohan = actor(f, "मोहन"), dadi = actor(f, "दादी");
        float fell = -1, walkEnd = -1;
        for (Film.Key k : mohan.keys) if (k.body == Pose.LIE && fell < 0) fell = k.t;
        assertTrue("he falls", fell > 0);
        for (Film.Key k : dadi.keys) if (k.moveDur > 0 && k.t > fell) walkEnd = Math.max(walkEnd, k.t + k.moveDur);
        assertTrue("she walks to him", walkEnd > fell);
        assertEquals("still down while she comes", Pose.LIE, mohan.stateAt(walkEnd - 0.3f).body);
        assertEquals("up once she is there", Pose.STAND, mohan.stateAt(walkEnd + 1.5f).body);
    }

    @Test public void withNoOneComingHeGetsUpHimself() {
        Film f = film("दादी: \"धीरे चल बेटा।\" (चिंतित होकर)");
        Film.Actor mohan = actor(f, "मोहन");
        float fell = -1;
        for (Film.Key k : mohan.keys) if (k.body == Pose.LIE && fell < 0) fell = k.t;
        assertTrue("he falls", fell > 0);
        assertEquals("and gets up", Pose.STAND, mohan.stateAt(fell + 3f).body);
    }
}

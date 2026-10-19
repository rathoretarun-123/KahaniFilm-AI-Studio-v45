package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import org.junit.Test;

/**
 * v47.13 (new stories made end to end): "Bruno … jumps into Maya's arms" — the puppy landed beside her. One small
 * enough taken into someone's arms is held in front of their chest from then on; a ball picked up near the puppy
 * leaves the puppy on the ground.
 */
public class InArmsTest {
    static Film.Key heldAtEnd(String script, String name) {
        Story st = ScriptParser.parse(script);
        Director d = new Director(st, new Director.Options());
        d.prepare();
        Film film = d.direct(new Art());
        for (Film.Seg seg : film.segs) for (Film.Actor a : seg.actors) if (name.equals(a.c.shown())) return a.stateAt(seg.t1 - 0.5f);
        fail(name + " is on the stage");
        return null;
    }

    static final String RAIN = "Title: Rain\n\nCharacters\n\n1. Maya (9 years)\n(a girl)\n\n2. Papa (40 years)\n(a tall man)\n\n"
            + "3. Bruno (dog)\n(a small brown puppy)\n\nPlaces\n\n1. Living room\n(a sofa, a window, a door)\n\nStory\n\nScene 1\n"
            + "(Maya sits on the sofa. Papa stands by the window. Bruno sleeps by the door.)\n\nMaya: \"I am so bored.\" (sadly)\n\n";

    @Test public void thePuppyJumpsIntoHerArms() {
        Film.Key k = heldAtEnd(RAIN + "(Maya stands up. Bruno runs and jumps into Maya's arms)\n\nMaya: \"Bruno!\" (happily)\n\nTHE END\n", "Bruno");
        assertEquals("Bruno is held", Film.A_IN_ARMS, k.anchor);
        assertEquals("in Maya's arms", "Maya", k.anchorActor.c.shown());
    }

    @Test public void aBallPickedUpIsNoPuppyPickedUp() {
        Film.Key k = heldAtEnd(RAIN + "(Papa picks up the ball and throws it to Bruno)\n\nMaya: \"Catch it!\" (happily)\n\nTHE END\n", "Bruno");
        assertEquals("Bruno stays on the ground", Film.A_GROUND, k.anchor);
    }

    @Test public void माँMunniKoGodMeinUthatiHai() {
        Film.Key k = heldAtEnd("शीर्षक: गोद\n\nपात्र\n\n1. माँ (30 साल)\n(हरी साड़ी)\n\n2. मुन्नी (2 साल)\n(छोटी बच्ची, गुलाबी फ्रॉक)\n\n"
                + "स्थान\n\n1. घर का कमरा\n(क्रीम दीवारें)\n\nकहानी\n\nदृश्य 1\n(माँ कमरे में खड़ी हैं। मुन्नी फ़र्श पर खेल रही है।)\n\n"
                + "(माँ मुन्नी को गोद में उठा लेती हैं)\n\nमाँ: \"मेरी गुड़िया!\" (प्यार से)\n\nसमाप्त\n", "मुन्नी");
        assertEquals("मुन्नी is held", Film.A_IN_ARMS, k.anchor);
        assertEquals("in माँ's arms", "माँ", k.anchorActor.c.shown());
    }

    @Test public void putDownAgain() {
        Film.Key k = heldAtEnd(RAIN + "(Maya stands up. Bruno jumps into Maya's arms)\n\nMaya: \"Bruno!\" (happily)\n\n"
                + "(Maya puts Bruno down)\n\nPapa: \"Good dog.\" (smiling)\n\nTHE END\n", "Bruno");
        assertEquals("Bruno is on the ground again", Film.A_GROUND, k.anchor);
    }
}

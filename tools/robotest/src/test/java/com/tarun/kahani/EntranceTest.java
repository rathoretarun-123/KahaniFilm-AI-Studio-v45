package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import org.junit.Test;

/**
 * v47.12 (new stories made end to end): one who arrives is not there from the start. The puppy first named after the
 * door is opened comes in through it; the boy whose first line is said "(दौड़ते हुए आता है)" runs in with it.
 */
public class EntranceTest {
    static Film.Actor actor(String script, String name) {
        Story st = ScriptParser.parse(script);
        Director d = new Director(st, new Director.Options());
        d.prepare();
        Film film = d.direct(new Art());
        for (Film.Seg seg : film.segs) for (Film.Actor a : seg.actors) if (name.equals(a.c.shown())) return a;
        fail(name + " is on the stage");
        return null;
    }

    static void comesInLater(Film.Actor a) {
        assertFalse(a.c.shown() + " is not there from the start", a.keys.get(0).visible);
        boolean seen = false;
        for (Film.Key k : a.keys) if (k.visible) seen = true;
        assertTrue(a.c.shown() + " comes in", seen);
    }

    @Test public void throughTheOpenedDoor() {
        comesInLater(actor("Title: Rain\n\nCharacters\n\n1. Maya (9 years)\n(a girl)\n\n2. Bruno (dog)\n(a small brown puppy)\n\n"
                + "Places\n\n1. Living room\n(a sofa, a window, a door)\n\nStory\n\nScene 1\n(Maya sits on the sofa.)\n\n"
                + "Maya: \"I am so bored.\" (sadly)\n\n(Someone scratches at the door. Maya runs to the door and opens it.)\n\n"
                + "Maya: \"Bruno! You are all wet!\" (in surprise)\n\n(Bruno shakes the water off.)\n\nTHE END\n", "Bruno"));
    }

    @Test public void withTheMannerOfTheFirstLine() {
        comesInLater(actor("शीर्षक: खेत\n\nपात्र\n\n1. मोहन (10 साल)\n(दुबला लड़का)\n\n2. दादी (65 साल)\n(सफ़ेद साड़ी)\n\n"
                + "स्थान\n\n1. गाँव का खेत\n(हरे खेत)\n\nकहानी\n\nदृश्य 1\n(दादी पेड़ के नीचे बैठी हैं।)\n\n"
                + "मोहन: \"दादी! बारिश होने वाली है!\" (दौड़ते हुए आता है)\n\nदादी: \"धीरे चल बेटा।\" (चिंतित होकर)\n\nसमाप्त\n", "मोहन"));
    }
}

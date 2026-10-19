package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import org.junit.Test;

/**
 * v47.12 (a new English story made end to end): "Bruno (dog)" with "(a small brown puppy)" under it was read as a role
 * and an empty description — the puppy was drawn as a man in white. A character named as an animal is that animal, and
 * keeps its own name; a robot dog keeps its role.
 */
public class DogCharacterTest {
    static Story parse(String cast) {
        return ScriptParser.parse("Title: T\n\nCharacters\n\n" + cast + "\n\nPlaces\n\n1. Living room\n\nStory\n\nScene 1\n(They are in the room)\n\n"
                + "Maya: \"Hello.\"\n\nTHE END\n");
    }

    static Story.CharacterDef named(Story st, String name) {
        for (Story.CharacterDef c : st.characters) if (c.shown().equals(name)) return c;
        fail("no character " + name + " in " + st.characters.size());
        return null;
    }

    @Test public void aDogIsADogCalledByItsName() {
        Story st = parse("1. Maya (9 years)\n(a girl in a yellow raincoat)\n\n2. Bruno (dog)\n(a small brown puppy)\n\n3. Tommy - a little dog");
        assertFalse("Bruno is no person", named(st, "Bruno").look.isHumanoid());
        assertFalse("Tommy is no person", named(st, "Tommy").look.isHumanoid());
        assertTrue("Maya is a girl", named(st, "Maya").look.isHumanoid());
    }

    @Test public void aRobotDogKeepsItsRole() {
        Story st = parse("1. Maya (9 years)\n(a girl)\n\n2. Bolt (Robo-Dog)\n(a silver robot dog)");
        assertEquals("Robo-Dog", named(st, "Bolt").role);
    }
}

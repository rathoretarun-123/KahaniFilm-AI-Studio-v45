package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

/**
 * v46: the Reader never invents a character from a prop or place entry of the cast list, and the writer's notes
 * after the last scene are instructions to the studio, not the last scene's action.
 */
public class ScreenplayNotesTest {
    static Story bundled(String name) throws Exception {
        File assets = new File(System.getProperty("kahani.assets", "../../app/src/main/assets"));
        return ScriptParser.parse(new String(Files.readAllBytes(new File(assets, name).toPath()), StandardCharsets.UTF_8));
    }

    @Test public void babuKeLiyeHasTwoCharactersALetterAndNoSlide() throws Exception {
        Story st = bundled("babu_ke_liye_screenplay.txt");
        assertEquals(st.characters.toString(), 2, st.characters.size());
        for (Story.CharacterDef c : st.characters) assertFalse(c.displayName, c.displayName.contains("वस्तु") || c.displayName.contains("स्थान"));
        boolean letter = false;
        for (Story.PlaceDef t : st.things) if (t.name.contains("ख़त") || t.name.contains("पत्र")) letter = true;
        assertTrue("the letter is the story's prop: " + st.things, letter);
        assertTrue("the place came from the cast list: " + st.places, !st.places.isEmpty());
        assertEquals(5, st.scenes.size());
        Story.Scene last = st.scenes.get(4);
        for (Story.Beat b : last.beats) {
            assertFalse("a note is not an action: " + b.text, b.text.contains("स्लाइडशो") || b.text.contains("स्वीकृति"));
        }
        assertTrue("the notes are kept: " + st.productionNotes.size(), st.productionNotes.size() >= 7);
        assertTrue(st.productionNotes.get(0).startsWith("निर्देशन"));
    }

    @Test public void aakhiriChaiHasThreeCharactersAndItsQcRulesApart() throws Exception {
        Story st = bundled("aakhiri_chai_screenplay.txt");
        assertEquals(st.characters.toString(), 3, st.characters.size());
        for (Story.CharacterDef c : st.characters) assertTrue(c.displayName, c.fromScript && !c.displayName.startsWith("-"));
        assertEquals(4, st.scenes.size());
        for (Story.Beat b : st.scenes.get(3).beats) assertFalse(b.text, b.text.contains("QC") || b.text.contains("Continuity"));
        assertTrue(st.productionNotes.size() >= 3);
    }

    @Test public void aNoteHeadingIsRecognisedAndADialogueLineIsNot() {
        assertTrue(ScriptParser.productionNotesHeading("निर्देशन और तकनीकी स्वीकृति:"));
        assertTrue(ScriptParser.productionNotesHeading("निर्देशन और QC नियम:"));
        assertTrue(ScriptParser.productionNotesHeading("Director's notes:"));
        assertFalse(ScriptParser.productionNotesHeading("मीरा (शांत): \"चलो\""));
        assertFalse(ScriptParser.productionNotesHeading("(रघु केतली का ढक्कन उठाते हैं।)"));
        assertFalse(ScriptParser.productionNotesHeading("दृश्य 3: चाय की गर्मी"));
        assertTrue(ScriptParser.propLabel("मुख्य वस्तु"));
        assertTrue(ScriptParser.placeLabel("स्थान"));
        assertFalse(ScriptParser.propLabel("मीरा"));
    }
}

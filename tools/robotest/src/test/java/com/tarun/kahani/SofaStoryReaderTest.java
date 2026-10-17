package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Look;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

/**
 * v47.9 (the user: "no voice is appearing for any character … are u not able to read dialogues in """): the reader
 * on the user's सोफे वाली जादू की बातें — the girl of 11 with a ponytail, called दीदी, is a girl (she was read as a
 * boy, from "पतला-दुबला शरीर", "रंग गोरा" and "मुस्कुराते हुए"); a line with a direction between two quotes is two
 * lines, and no spoken text holds a quote mark or a direction.
 */
public class SofaStoryReaderTest {
    static Story story() throws Exception {
        return ScriptParser.parse(new String(Files.readAllBytes(new File("../../tools/testdata/sofe_wali_jadu/sofe_wali_jadu_screenplay.txt").toPath()), StandardCharsets.UTF_8));
    }

    @Test public void bothSistersAreGirls() throws Exception {
        Story st = story();
        assertEquals(2, st.characters.size());
        for (Story.CharacterDef c : st.characters) {
            assertEquals(c.displayName + " is a girl", Look.GIRL, c.look.kind);
            assertTrue(c.displayName, c.look.female);
        }
    }

    @Test public void everyLineIsOnlyItsWords() throws Exception {
        Story st = story();
        assertEquals("9 lines (one of them said in two parts)", 9, st.dialogueCount());
        int n = 0;
        for (Story.Scene sc : st.scenes) for (Story.Beat b : sc.beats) {
            if (b.type != Story.Beat.DIALOGUE) continue;
            n++;
            assertNotNull("a known speaker: " + b.speakerRaw, b.speaker);
            assertFalse("no quote mark or direction in the words: " + b.text, b.text.matches(".*[\"“”()].*"));
            if (n == 4) { assertTrue(b.text, b.text.startsWith("नहीं पगली")); assertTrue(b.manner, b.manner.contains("हंसी")); }
            if (n == 5) { assertTrue(b.text, b.text.startsWith("सूरज दिन में")); assertEquals(st.scenes.get(1).beats.get(1).speaker, b.speaker); }
        }
        assertEquals(2, st.places.size());
        assertEquals(4, st.things.size());
    }
}

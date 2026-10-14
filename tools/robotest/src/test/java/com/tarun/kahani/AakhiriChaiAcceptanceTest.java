package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Acceptance gate for the user's ultimate benchmark film: Aakhiri Chai. */
public class AakhiriChaiAcceptanceTest {
    private String screenplay() throws Exception {
        File file = new File(new File(System.getProperty("kahani.assets")), "aakhiri_chai_screenplay.txt");
        assertTrue("the complete Aakhiri Chai screenplay must ship in app assets", file.isFile());
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test public void completeScreenplayParsesAndCoreDialogueSurvives() throws Exception {
        String source = screenplay();
        Story story = ScriptParser.parse(source);
        assertNotNull("the parser must return a Story", story);
        assertEquals("Aakhiri Chai must remain four authored scenes", 4, story.scenes.size());

        StringBuilder dialogue = new StringBuilder();
        for (Story.Scene scene : story.scenes) {
            assertTrue("scene must include staging or dialogue: " + scene.title, !scene.beats.isEmpty());
            for (Story.Beat beat : scene.beats) {
                if (beat.type == Story.Beat.DIALOGUE && beat.text != null) {
                    dialogue.append(beat.speakerRaw).append(": ").append(beat.text).append('\n');
                }
            }
        }
        String spoken = dialogue.toString();
        assertTrue("Raghu's thematic line must survive parsing: " + spoken, spoken.contains("कहानी कागज पर नहीं"));
        assertTrue("Meera's signature line must survive parsing: " + spoken, spoken.contains("बारिश का स्वाद"));
        assertTrue("Aarav's resolution must survive parsing: " + spoken, spoken.contains("आज बस याद रखूंगा"));
        assertTrue("the final line must survive parsing: " + spoken, spoken.contains("कल फिर आना"));
    }

    @Test public void continuityAndNoSlideshowRequirementsStayDocumented() throws Exception {
        String source = screenplay();
        assertTrue(source.contains("नीली बाल्टी"));
        assertTrue(source.contains("कढ़ाई वाला रूमाल"));
        assertTrue(source.contains("दृश्य 3 में दो कुल्हड़ दिए जाते हैं"));
        assertTrue(source.contains("स्थिर चित्रों की स्लाइडशो-शैली"));
        assertTrue(source.contains("अंतिम pull-back"));
    }
}

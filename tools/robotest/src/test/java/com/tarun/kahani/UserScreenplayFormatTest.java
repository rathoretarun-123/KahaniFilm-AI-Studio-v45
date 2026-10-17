package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.ScriptAI;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;
import org.junit.Test;

/** Guards the user's bilingual, plain-text screenplay format from import through the reader and director. */
public final class UserScreenplayFormatTest {
    private static final String HINDI = "शीर्षक: आखिरी चिट्ठी\n\n"
            + "पात्र\nरघुवीर जी (78 साल) (झुका हुआ शरीर, सफेद मूंछें)\nपरी (7 साल) (नीली स्कूल यूनिफॉर्म)\n\n"
            + "वस्तुएं\nलकड़ी की छड़ी (पुरानी, गहरे भूरे रंग की)\n\n"
            + "स्थान और पृष्ठभूमि\nपुराना डाकघर (गांव का पीला डाकघर)\n\n"
            + "कहानी\n\nदृश्य 1\n(दोपहर में रघुवीर जी डाकघर के बाहर बेंच पर बैठे हैं।)\n"
            + "रघुवीर जी: \"आज भी नहीं आई।\" (धीमी, थकी आवाज में)\n"
            + "परी: \"मैं परी हूँ।\" (मुस्कुराकर)\n\nसमाप्त\n";

    private static final String ENGLISH = "Title: The Last Letter\n\n"
            + "Characters\nRaghu (78 years) (stooped posture, white moustache)\n\n"
            + "Objects\nWalking stick (old, dark wood)\n\n"
            + "Places and Backgrounds\nOld post office (small yellow village post office)\n\n"
            + "Story\n\nScene 1\n(Raghu waits outside the post office in the afternoon.)\n"
            + "Raghu: \"It still hasn't come.\" (quietly, tired voice)\n\nEND\n";

    @Test public void hindiSampleFormatKeepsInventoryScenesDialogueAndDeliverySeparate() {
        Story story = ScriptParser.parse(HINDI);
        assertEquals("आखिरी चिट्ठी", story.title);
        assertEquals(2, story.characters.size());
        assertEquals(78, story.characters.get(0).age);
        assertEquals(1, story.things.size());
        assertEquals("लकड़ी की छड़ी", story.things.get(0).name);
        assertEquals(1, story.places.size());
        assertEquals("पुराना डाकघर", story.places.get(0).name);
        assertEquals(1, story.scenes.size());
        assertEquals(2, story.dialogueCount());
        assertEquals("आज भी नहीं आई।", line(story, 0).text);
        assertTrue("delivery remains a performance note", line(story, 0).manner.contains("धीमी"));
        assertFalse("the scene description is not narrated", story.hasNarrator);
        assertFalse("no parser warning for the canonical format: " + story.warnings, story.warnings.toString().contains("No 'Scene 1'"));

        Director.Options options = new Director.Options();
        options.narrator = true;
        Film film = new Director(story, options).prepare();
        assertEquals("only the two authored lines become speech", 2, film.lines.size());
    }

    @Test public void englishSampleFormatParsesAndHindiAndEnglishTemplatesTeachTheSameLayout() {
        Story story = ScriptParser.parse(ENGLISH);
        assertEquals("The Last Letter", story.title);
        assertEquals(1, story.characters.size());
        assertEquals(1, story.things.size());
        assertEquals(1, story.places.size());
        assertEquals(1, story.scenes.size());
        assertEquals(1, story.dialogueCount());
        assertTrue(ScriptAI.system(true).contains("स्थान और पृष्ठभूमि"));
        assertTrue(ScriptAI.system(true).contains("समाप्त"));
        assertTrue(ScriptAI.system(false).contains("Places and Backgrounds"));
        assertTrue(ScriptAI.system(false).contains("END"));
        assertFalse("templates avoid markdown list markers", ScriptAI.system(true).contains(" * "));
    }

    @Test public void completeUserStoryIsParsedWithoutPromotingDescriptionsToSpeech() throws Exception {
        java.io.File screenplay = new java.io.File("../../tools/testdata/aakhiri_chitthi/aakhiri_chitthi_screenplay.txt");
        assertTrue("the user's full sample screenplay is committed", screenplay.isFile());
        String text = new String(java.nio.file.Files.readAllBytes(screenplay.toPath()), "UTF-8");
        Story story = ScriptParser.parse(text);
        assertEquals("आखिरी चिट्ठी", story.title);
        assertEquals(2, story.characters.size());
        assertEquals(13, story.things.size());
        assertEquals(4, story.places.size());
        assertEquals(3, story.scenes.size());
        assertEquals(7, story.dialogueCount());
        assertFalse("parenthesized scene prose is never narration", story.hasNarrator);
        for (Story.Scene scene : story.scenes) for (Story.Beat beat : scene.beats)
            if (beat.type == Story.Beat.DIALOGUE) assertTrue("dialogue remains a quoted authored line", beat.text.length() > 0);
        Director.Options options = new Director.Options();
        options.narrator = true;
        Film film = new Director(story, options).prepare();
        assertEquals("only the seven authored lines become speech", 7, film.lines.size());
    }

    private static Story.Beat line(Story story, int at) {
        int seen = 0;
        for (Story.Scene scene : story.scenes) for (Story.Beat beat : scene.beats)
            if (beat.type == Story.Beat.DIALOGUE && seen++ == at) return beat;
        fail("missing dialogue line " + at);
        return null;
    }
}

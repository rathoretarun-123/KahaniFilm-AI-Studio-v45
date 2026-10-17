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

    /**
     * The user's numbered format (17 October): "1. रघुवीर जी (78 साल)" with the description on the next line, the
     * same for objects and places, then कहानी, दृश्य N with its (पर्यावरण - …) direction, dialogue and समाप्त. The reader
     * takes only what the script says: every listed name as written, every scene at the place it happens, no extra people.
     */
    @Test public void numberedUserStoryIsReadExactlyAsWritten() throws Exception {
        java.io.File screenplay = new java.io.File("../../tools/testdata/aakhiri_chitthi/aakhiri_chitthi_numbered.txt");
        assertTrue("the user's numbered sample screenplay is committed", screenplay.isFile());
        Story story = ScriptParser.parse(new String(java.nio.file.Files.readAllBytes(screenplay.toPath()), "UTF-8"));
        assertEquals("आखिरी चिट्ठी", story.title);
        assertEquals(2, story.characters.size());
        Story.CharacterDef raghu = story.characters.get(0), pari = story.characters.get(1);
        assertEquals("रघुवीर जी", raghu.displayName);
        assertEquals(78, raghu.age);
        assertEquals("परी", pari.displayName);
        assertEquals(7, pari.age);
        assertTrue("the description on the line under the name is the character's", raghu.description.contains("दुबला-पतला"));
        assertTrue("the child's description is hers", pari.description.contains("लाल रिबन"));
        assertFalse("an honorific alone is never a name for him", raghu.aliases.contains("जी"));
        String[] things = {"दादाजी की लकड़ी की छड़ी", "दादाजी का मोटे फ्रेम वाला चश्मा", "परी का गुलाबी स्कूल बैग", "परी के लाल रिबन",
                "परी की हवाई चप्पल", "पुराना लाल डाक-बक्सा", "डाक-बक्से का छोटा ताला", "पीली पड़ी मुड़ी-तुड़ी चिट्ठी", "नीम का पेड़",
                "लकड़ी की टूटी बेंच", "बेंच के नीचे लगी ईंट", "बंद पड़ा चाय का ठेला", "जमीन पर बिखरे सूखे पत्ते और धूल"};
        assertEquals(things.length, story.things.size());
        for (int i = 0; i < things.length; i++) {
            assertEquals("object " + (i + 1) + " as written", things[i], story.things.get(i).name);
            assertTrue("object " + (i + 1) + " keeps its description", story.things.get(i).description.length() > 20);
            assertFalse("a listed object is tangible: " + things[i], com.tarun.kahani.core.Bible.intangible(things[i]));
        }
        String[] places = {"गांव का पुराना डाकघर - बाहरी हिस्सा", "नीम के पेड़ के नीचे का कोना", "कच्चा रास्ता"};
        assertEquals(places.length, story.places.size());
        for (int i = 0; i < places.length; i++) assertEquals("place " + (i + 1) + " as written", places[i], story.places.get(i).name);
        assertEquals("every listed place is its own, once", 3, com.tarun.kahani.core.Bible.listedPlaces(story).size());
        assertEquals(3, story.scenes.size());
        // scene 2 shows the path only behind ("पीछे कच्चा रास्ता …") and Pari comes from it ("कच्चे रास्ते से दौड़ती हुई
        // आती है"); Raghuveer is still on the bench under the neem: all three scenes are at the main location
        for (Story.Scene sc : story.scenes)
            assertTrue("scene " + sc.number + " is at the neem corner, not " + sc.setting, sc.setting.startsWith("नीम के पेड़ के नीचे का कोना"));
        assertEquals(7, story.dialogueCount());
        assertEquals("आज भी नहीं आई... पंद्रह साल हो गए।", line(story, 0).text);
        assertTrue("the delivery note stays a note", line(story, 0).manner.startsWith("धीरे से"));
        assertFalse("the delivery note is never spoken", line(story, 0).text.contains("आवाज"));
        assertEquals("तुम सच में परी हो।", line(story, 6).text);
        for (Story.Scene sc : story.scenes) for (Story.Beat b : sc.beats) {
            assertFalse("समाप्त ends the film; it is no action", b.text.trim().equals("समाप्त"));
            if (b.type == Story.Beat.DIALOGUE) assertTrue("every line has a listed speaker", b.speaker == raghu || b.speaker == pari);
        }
        assertFalse("descriptions are never narrated", story.hasNarrator);
        Director.Options options = new Director.Options();
        options.narrator = true;
        Film film = new Director(story, options).prepare();
        assertEquals("only the seven authored lines become speech", 7, film.lines.size());
    }

    /** One-line numbered entries and a "कहानी शुरू" heading: clean names, descriptions apart, one neem place. */
    @Test public void numberedOneLineEntriesAndStoryStartHeading() {
        Story story = ScriptParser.parse("शीर्षक: आखिरी चिट्ठी\n\nपात्र\n1. रघुवीर जी (78 साल) (बूढ़े दादाजी)\n\n"
                + "स्थान और पृष्ठभूमि\n1. नीम का पेड़ (विशाल पेड़)\n2. नीम के पेड़ के नीचे का कोना (पेड़ की छाया)\n3. नीम की छाया (ठंडी छाया)\n4. पुराना डाकघर (पीली इमारत)\n\n"
                + "वस्तुएँ\n1. पुराना लाल डाक-बक्सा (जंग लगा लोहे का बक्सा)\n2. पीली चिट्ठी (मुड़ा हुआ कागज)\n\n"
                + "कहानी शुरू\nदृश्य 1\n(नीम के पेड़ के नीचे का कोना, दादाजी चिट्ठी हाथ में रखते हैं और छड़ी पकड़ते हैं।)\n"
                + "रघुवीर जी: \"यह चिट्ठी है।\" (धीमे से)\nदृश्य 2\n(नीम की छाया में, दादाजी आसमान देखते हैं।)\nरघुवीर जी: \"इंतजार है।\" (उदास होकर)\nसमाप्त");
        assertEquals("आखिरी चिट्ठी", story.title);
        assertEquals(1, story.characters.size());
        assertEquals(78, story.characters.get(0).age);
        assertEquals(2, story.things.size());
        assertEquals("पुराना लाल डाक-बक्सा", story.things.get(0).name);
        assertEquals("जंग लगा लोहे का बक्सा", story.things.get(0).description);
        assertEquals(4, story.places.size());
        assertEquals("the neem tree, corner and shade are one place", 2, com.tarun.kahani.core.Bible.listedPlaces(story).size());
        assertEquals(2, story.scenes.size());
        assertTrue(story.scenes.get(0).setting.startsWith("नीम के पेड़ के नीचे का कोना"));
        assertEquals(2, story.dialogueCount());
        for (Story.Scene sc : story.scenes) for (Story.Beat b : sc.beats) assertFalse(b.text.trim().equals("समाप्त"));
    }

    private static Story.Beat line(Story story, int at) {
        int seen = 0;
        for (Story.Scene scene : story.scenes) for (Story.Beat beat : scene.beats)
            if (beat.type == Story.Beat.DIALOGUE && seen++ == at) return beat;
        fail("missing dialogue line " + at);
        return null;
    }
}

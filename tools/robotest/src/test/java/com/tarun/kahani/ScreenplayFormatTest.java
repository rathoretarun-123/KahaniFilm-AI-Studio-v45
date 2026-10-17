package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.ScreenplayFormat;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.Test;

/**
 * v47.6: the standard screenplay format — a plain title, scene headings and descriptions in parentheses, every spoken
 * line in double quotation marks with how it is said after it in parentheses of its own, a plain ending — written from
 * the reader's reading (never by replacing characters), checked line by line, idempotent, and never changing a word.
 */
public class ScreenplayFormatTest {
    static String hi(String scene) {
        return "शीर्षक: आँगन की गेंद\n\nपात्र\n\n1. मीरा (10 साल)\nएक छोटी लड़की, लाल फ्रॉक\n\n2. राजू (12 साल)\nमीरा का भाई\n\nवस्तुएं\n\n1. लाल गेंद\nरबर की गेंद\n\n"
                + "स्थान\n\n1. घर का आँगन\nईंटों का आँगन\n\nकहानी\n\n" + scene + "\nसमाप्त\n";
    }
    static final String EN = "Title: The Kite\n\nCharacters\n\n1. Mira (9 years)\nA small girl in a red frock\n\nObjects\n\n1. Red kite\nA paper kite\n\n"
            + "Places\n\n1. Rooftop\nA flat roof\n\nStory\n\nScene 1\n(Mira stands on the rooftop holding the red kite.)\n\nMira: \"Look, it flies!\" (happily)\n\nTHE END\n";
    static final String HINGLISH = "Title: Chai Time\n\nCharacters\n\n1. Raju (30 years)\nChaiwala, white kurta\n\n2. Meena (25 years)\nTeacher, yellow suit\n\n"
            + "Places\n\n1. Tapri\nA small tea stall\n\nStory\n\nScene 1\n(Raju chai bana raha hai, Meena aati hai.)\n\nRaju: \"Chai pi lo, madam!\" (muskurate hue)\n\nMeena: \"Haan, ek cutting dena.\" (thakan se)\n\nTHE END\n";

    static String read(String path) throws Exception { return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8); }

    static void roundTrip(String raw) {
        Story a = ScriptParser.parse(raw);
        String f = ScreenplayFormat.format(a);
        Story b = ScriptParser.parse(f);
        assertTrue("same scenes, speakers and words after formatting:\n" + f, ScreenplayFormat.sameStory(a, b));
        assertEquals("formatting a formatted script changes nothing", f, ScreenplayFormat.format(b));
        assertEquals("the formatted script keeps every rule: " + ScreenplayFormat.validateFormattedText(f), 0, ScreenplayFormat.validateFormattedText(f).size());
        assertNotNull(ScreenplayFormat.formatScript(raw));
    }

    @Test public void theFormatsRulesOnTheFormattedScript() {
        String raw = hi("दृश्य 1\n(मीरा आँगन में लाल गेंद उठाती है)\n\nमीरा: (खुश होकर) \"चलो खेलें!\"\n\nराजू: \"हाँ, चलो!\" (हँसते हुए)\n");
        String f = ScreenplayFormat.format(ScriptParser.parse(raw));
        String[] ls = f.split("\n");
        assertEquals("शीर्षक: आँगन की गेंद", ls[0]);
        assertEquals("समाप्त", ls[ls.length - 1]);
        assertTrue("a scene heading in parentheses: " + f, f.contains("\n(दृश्य 1)\n") || f.contains("\n(दृश्य 1 — घर का आँगन)\n"));
        assertTrue(f, f.contains("\n(मीरा आँगन में लाल गेंद उठाती है)\n"));
        // the note before the words moves after them, in parentheses of its own; the words stay exactly
        assertTrue(f, f.contains("\nमीरा: \"चलो खेलें!\" (खुश होकर)\n"));
        assertTrue(f, f.contains("\nराजू: \"हाँ, चलो!\" (हँसते हुए)\n"));
        roundTrip(raw);
    }

    @Test public void theRulesFindWhatIsOffFormat() {
        String bad = "शीर्षक: \"आँगन की गेंद\"\n\nकहानी\n\nदृश्य 1\nमीरा आँगन में खेलती है\n\nमीरा: चलो खेलें!\n\n[कैमरा पीछे जाता है]\n\nराजू: \"हाँ\" खुश होकर\n\n(समाप्त)\n";
        List<String> v = ScreenplayFormat.validateFormattedText(bad);
        String all = v.toString();
        assertTrue(all, all.contains("title is plain"));
        assertTrue(all, all.contains("scene heading goes in parentheses"));
        assertTrue(all, all.contains("Line 6: a description goes in parentheses"));
        assertTrue(all, all.contains("Line 8: the spoken words go in double quotation marks"));
        assertTrue(all, all.contains("not square brackets"));
        assertTrue(all, all.contains("what follows the spoken words goes in parentheses"));
        assertTrue(all, all.contains("ending is plain"));
        assertTrue(ScreenplayFormat.validateFormattedText("").contains("The script is empty."));
        assertTrue(ScreenplayFormat.validateFormattedText("शीर्षक: क\n\nकहानी\n\n(दृश्य 1)\n(कुछ होता है)\n").toString().contains("does not end with समाप्त"));
        assertTrue(ScreenplayFormat.validateFormattedText("शीर्षक: क\n\nकहानी\n\n(दृश्य 1)\n(कुछ (होता है)\n\nसमाप्त\n").toString().contains("parentheses do not pair"));
    }

    @Test public void englishAndHinglishToo() {
        roundTrip(EN);
        String f = ScreenplayFormat.format(ScriptParser.parse(EN));
        assertTrue(f, f.startsWith("Title: The Kite\n"));
        assertTrue(f, f.endsWith("THE END\n"));
        assertTrue(f, f.contains("\nMira: \"Look, it flies!\" (happily)\n"));
        assertTrue(f, f.contains("1. Mira (9 years)\n"));
        roundTrip(HINGLISH);
        assertTrue(ScreenplayFormat.format(ScriptParser.parse(HINGLISH)).contains("Raju: \"Chai pi lo, madam!\" (muskurate hue)"));
    }

    @Test public void quotationsInsideALineAndBracketsInsideADescription() {
        String raw = hi("दृश्य 1\n(मीरा गेंद उठाती है (धीरे से) और मुस्कुराती है)\n\nमीरा: \"माँ ने कहा था “कल आना” और चली गई।\" (उदास)\n\n(राजू आता है (बिना बोले\n");
        String f = ScreenplayFormat.format(ScriptParser.parse(raw));
        assertTrue(f, f.contains("(मीरा गेंद उठाती है (धीरे से) और मुस्कुराती है)"));
        assertTrue("the quoted words inside the line are kept: " + f, f.contains("कल आना"));
        for (String m : ScreenplayFormat.validateFormattedText(f)) fail(m + "\n" + f);
        roundTrip(raw);
    }

    @Test public void theStoryItselfIsChecked() {
        // a speaker who is not in the list, an empty scene, no title
        Story s = ScriptParser.parse(hi("दृश्य 1\n(मीरा खेलती है)\n\nश्यामू: \"मैं भी खेलूँगा!\"\n\nदृश्य 2\n"));
        String v = ScreenplayFormat.validate(s).toString();
        assertTrue(v, v.contains("श्यामू") && v.contains("not in the list of characters"));
        Story none = ScriptParser.parse("कहानी\n\nदृश्य 1\n(कुछ होता है)\n\nसमाप्त\n");
        none.title = "";
        assertTrue(ScreenplayFormat.validate(none).toString().contains("no title"));
        Story empty = new Story();
        empty.title = "x";
        assertTrue(ScreenplayFormat.validate(empty).toString().contains("no scenes"));
    }

    @Test public void descriptionsAreNeverMadeIntoSpeech() {
        // a description with a colon in it ("LOW ANGLE: …") stays a description
        String raw = hi("दृश्य 1\n(LOW ANGLE: मीरा आँगन में दौड़ती है)\n\nमीरा: \"चलो!\"\n");
        Story a = ScriptParser.parse(raw);
        String f = ScreenplayFormat.format(a);
        assertTrue(f, f.contains("(LOW ANGLE: मीरा आँगन में दौड़ती है)"));
        int lines = 0;
        for (Story.Scene sc : ScriptParser.parse(f).scenes) for (Story.Beat b : sc.beats) if (b.type == Story.Beat.DIALOGUE) lines++;
        assertEquals(1, lines);
    }

    @Test public void theUsersScriptsKeepEveryLineAndFormatOnce() throws Exception {
        for (String p : new String[]{"../../tools/testdata/aakhiri_chitthi/aakhiri_chitthi_numbered.txt", "../../tools/testdata/aakhiri_jodi/aakhiri_jodi_numbered.txt",
                "../../tools/testdata/aakhiri_chai/aakhiri_chai_numbered.txt", "../../app/src/main/assets/aakhiri_chai_screenplay.txt", "../../app/src/main/assets/babu_ke_liye_screenplay.txt"})
            roundTrip(read(p));
        // the user's own scripts as written: what the standard format would change is said, line by line
        String chai = ScreenplayFormat.validateFormattedText(read("../../tools/testdata/aakhiri_chai/aakhiri_chai_numbered.txt")).toString();
        assertTrue(chai, chai.contains("a scene heading goes in parentheses"));
        assertTrue(chai, chai.contains("not square brackets"));
        String jodi = ScreenplayFormat.validateFormattedText(read("../../tools/testdata/aakhiri_jodi/aakhiri_jodi_numbered.txt")).toString();
        assertTrue(jodi, jodi.contains("the ending is the word alone"));
    }
}

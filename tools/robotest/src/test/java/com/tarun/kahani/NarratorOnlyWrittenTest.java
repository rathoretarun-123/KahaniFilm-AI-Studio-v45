package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;
import org.junit.Test;

/** v47.13 (the user: "make sure narrator only speaks if written in script"): the narrator says only its own lines. */
public final class NarratorOnlyWrittenTest {
    private static Film film(String script) {
        Story st = ScriptParser.parse(script);
        return new Director(st, new Director.Options()).prepare();
    }

    private static int narrated(Film f) {
        int n = 0;
        for (Film.Line l : f.lines) if (l.who == null) n++;
        return n;
    }

    @Test public void directionsAndDescriptionsAreNeverSpoken() {
        Film f = film("शीर्षक: बेंच\n\nपात्र\nरघुवीर जी (78 साल)\nपरी (7 साल)\n\nकहानी\n\nदृश्य 1\n"
                + "(सुबह। रघुवीर जी नीम के पेड़ के नीचे बेंच पर बैठे हैं। परी दौड़ती हुई आती है।)\n"
                + "परी: \"नमस्ते दादाजी!\"\nरघुवीर जी: \"आओ बेटा।\"\n\nसमाप्त\n");
        assertEquals("only the two written lines are spoken", 2, f.lines.size());
        assertEquals("no narrator line", 0, narrated(f));
    }

    @Test public void aStoryWithoutLinesIsActedNotNarrated() {
        Story st = ScriptParser.parse("Title: Rain\n\nScene 1\n(A girl runs through the rain to the school gate. A puppy follows her.)\n\nEND\n");
        assertEquals(0, st.dialogueCount());
        assertFalse("no narrator is made up", st.hasNarrator);
        Director d = new Director(st, new Director.Options());
        Film f = d.prepare();
        assertEquals("nothing is spoken", 0, f.lines.size());
        f = d.direct(null);
        assertTrue("the film is still made", f.segs.size() > 0 && f.duration > 3);
    }

    @Test public void theNarratorsOwnLinesAreSpoken() {
        Film f = film("शीर्षक: चिट्ठी\n\nपात्र\nपरी (7 साल)\n\nकहानी\n\nदृश्य 1\n(डाकघर के बाहर।)\n"
                + "कथावाचक: \"उस दिन भी चिट्ठी नहीं आई।\"\nपरी: \"दादाजी!\"\n\nसमाप्त\n");
        assertEquals(2, f.lines.size());
        assertEquals("the narrator speaks exactly its one written line", 1, narrated(f));
    }

    @Test public void onlyTheQuotedWordsWrittenAgainstTheNarrator() throws Exception {
        Film f = film("शीर्षक: बेंच\n\nपात्र\nपरी (7 साल)\n\nकहानी\n\nदृश्य 1\n(सुबह।)\n"
                + "नैरेटर: \"गाँव में सुबह हो चुकी थी।\"\n"
                + "कथावाचक: उस दिन कोई नहीं आया।\n"
                + "कथावाचक (धीरे से): \"पर परी आई।\" और फिर वह मुस्कुराई।\n"
                + "परी: \"नमस्ते!\"\n\nसमाप्त\n");
        java.util.List<String> said = new java.util.ArrayList<String>();
        for (Film.Line l : f.lines) if (l.who == null) said.add(l.shown == null ? l.text : l.shown);
        assertEquals("the narrator speaks its two quoted lines only: " + said, 2, said.size());
        assertTrue(said.get(0), said.get(0).contains("गाँव में सुबह हो चुकी थी"));
        assertTrue("nothing outside the quotes: " + said.get(1), said.get(1).contains("पर परी आई") && !said.get(1).contains("मुस्कुराई"));
        for (String t : said) assertFalse("unquoted words are never spoken: " + t, t.contains("कोई नहीं आया"));
        assertEquals("the girl's line", 1, f.lines.size() - said.size());
    }

    @Test public void aGroupWordWithNobodyIsNotTheNarrator() throws Exception {
        Story st = ScriptParser.parse("Title: Fair\n\nScene 1\n(The fair.)\nEveryone: \"Hurray!\"\n\nEND\n");
        assertFalse("no narrator is made up", st.hasNarrator);
    }
}

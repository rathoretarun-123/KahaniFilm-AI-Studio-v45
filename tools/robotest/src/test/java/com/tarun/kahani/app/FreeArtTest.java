package com.tarun.kahani.app;

import static org.junit.Assert.*;

import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;
import java.util.List;
import org.junit.Test;

/**
 * The optional emoji stand-ins (FreeArt) take only the objects the script lists, and only when the emoji is that thing:
 * the user's चिट्ठी got "Magic wand" for a walking stick, "Scroll" for a letter, "Backpack" for a school bag and "Open
 * book" for a book only mentioned in a direction.
 */
public final class FreeArtTest {
    @Test public void onlyListedObjectsWithTheSameThingAreWanted() throws Exception {
        java.io.File f = new java.io.File("../../tools/testdata/aakhiri_chitthi/aakhiri_chitthi_numbered.txt");
        Story st = ScriptParser.parse(new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8"));
        List<String[]> w = FreeArt.wanted(st);
        StringBuilder names = new StringBuilder();
        for (String[] o : w) names.append(o[1]).append(" <- ").append(o[2]).append("; ");
        String got = names.toString();
        for (String wrong : new String[]{"Magic wand", "Scroll", "Backpack", "Open book", "Diya lamp", "Feather", "Alarm clock", "Top hat", "Money bag"})
            assertFalse(wrong + " is not a thing of this script: " + got, got.contains(wrong + " <-"));
        for (String[] o : w) {
            boolean listed = false;
            for (Story.PlaceDef th : st.things) if (th.name.trim().equals(o[2])) listed = true;
            assertTrue("only listed objects: " + o[2], listed);
        }
    }

    @Test public void wordsInDirectionsNeverBecomeObjects() {
        Story st = ScriptParser.parse("शीर्षक: दिया\n\nपात्र\nमीना (9 साल) (लाल फ्रॉक)\n\nस्थान\nआंगन (मिट्टी का आंगन)\n\nकहानी\n\nदृश्य 1\n"
                + "(मीना ने किताब उठाई और घड़ी भर पंखा देखा, फिर छड़ी से चिट्ठी उठाई।)\nमीना: \"दिया जलाओ।\" (धीरे से)\n\nसमाप्त\n");
        assertTrue("a story with no object list gets no emoji: " + FreeArt.wanted(st).size(), FreeArt.wanted(st).isEmpty());
    }

    @Test public void headNounAndWholeNameMatchAsWholeWords() {
        assertNotNull(FreeArt.match("पुराना लोहे का ताला"));
        assertEquals("Locked", FreeArt.match("पुराना लोहे का ताला")[1]);
        assertNull("a fan is not a feather", FreeArt.match("छत का पंखा"));
        assertNull("a walking stick is not a magic wand", FreeArt.match("दादाजी की लकड़ी की छड़ी"));
        assertEquals("Magic wand", FreeArt.match("परी की जादू की छड़ी")[1]);
        assertNull("a letter is not a scroll", FreeArt.match("पीली पड़ी मुड़ी-तुड़ी चिट्ठी"));
        assertNull("a postman's bag is not a backpack", FreeArt.match("डाकिये का बैग"));
    }
}

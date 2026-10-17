package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Look;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;
import org.junit.Test;

/**
 * v47.6: "काली" is black as often as it is the goddess. परी's "गहरी काली आँखें" had given her ten arms and a tiger to ride
 * (she was then never moved through her rig); a goddess named as one keeps what her name implies.
 */
public final class LookReadingTest {
    private static Story read(String chars) {
        Story st = ScriptParser.parse("शीर्षक: जाँच\n\nपात्र\n\n" + chars + "\nस्थान\n\n1. आंगन\nमिट्टी का आंगन\n\nकहानी\n\nदृश्य 1\n(सब आंगन में हैं।)\n\nपरी: \"चलो।\"\n\nसमाप्त\n");
        new Director(st, new Director.Options()).prepare();
        return st;
    }

    @Test public void blackEyesAreNotTheGoddess() {
        Story st = read("1. परी (7 साल)\nबहुत बड़ी गोल चमकदार गहरी काली आँखें, नीली स्कूल यूनिफॉर्म\n\n2. आरव (10 साल)\nकाले बाल, काली पैंट, असुरक्षित सा चेहरा\n");
        for (Story.CharacterDef c : st.characters) {
            assertEquals(c.displayName + " has two arms", 2, c.look.arms);
            assertEquals(c.displayName + " rides nothing", -1, c.look.mount);
            assertNotEquals(c.displayName + " has no horns", Look.HW_HORNS, c.look.headwear);
        }
    }

    @Test public void aGoddessNamedAsOneKeepsHerTraits() {
        Story st = read("1. माँ काली (देवी)\nकाली साड़ी, लाल आँखें\n\n2. परी (7 साल)\nकाली आँखें\n");
        Story.CharacterDef kali = st.characters.get(0);
        assertTrue("Kali has her ten arms", kali.look.arms >= 10);
        assertEquals(2, st.characters.get(1).look.arms);
    }
}

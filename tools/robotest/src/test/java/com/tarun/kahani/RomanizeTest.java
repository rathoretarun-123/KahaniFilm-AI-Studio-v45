package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Txt;
import org.junit.Test;

/** v47.9: a phone with no Hindi voice reads a Hindi line in Latin letters rather than leaving it silent. */
public class RomanizeTest {
    @Test public void hindiInLatinLetters() {
        assertEquals("deedee, ye chaand din men kahaan chalaa jaataa hai?", Txt.romanize("दीदी, ये चाँद दिन में कहाँ चला जाता है?"));
        assertEquals("kamal", Txt.romanize("कमल"));
        assertEquals("zaroor", Txt.romanize("ज़रूर"));
        assertEquals("dost hain.", Txt.romanize("दोस्त हैं।"));
        assertEquals("Hello 12", Txt.romanize("Hello १२"));
        assertTrue(Txt.isDevanagariText("abc चाँद"));
        assertFalse(Txt.isDevanagariText("abc"));
    }
}

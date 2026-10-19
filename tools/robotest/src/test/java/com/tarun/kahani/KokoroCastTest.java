package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Json;
import com.tarun.kahani.core.KokoroCast;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.Test;

/**
 * v47.13 (the user: "include full size kokoro in apk … and voices generated in apk library"): the Kokoro voices of
 * the library are each a real recorded sample, and a voice chosen there is the one the film speaks with.
 */
public class KokoroCastTest {
    static final File ASSETS = new File(System.getProperty("kahani.assets", "../../app/src/main/assets"));

    @Test public void theLibrarysVoicesAreRealSamples() throws Exception {
        Object o = Json.parseLoose(new String(Files.readAllBytes(new File(ASSETS, "voices/kokoro/index.json").toPath()), StandardCharsets.UTF_8));
        assertTrue(o instanceof List);
        List<?> l = (List<?>) o;
        assertEquals("two Hindi women, two Hindi men, two British women, two British men", 8, l.size());
        for (Object x : l) {
            File f = new File(ASSETS, Json.str(x, "file", ""));
            assertTrue(f + " is there", f.isFile() && f.length() > 4000);
            int sid = (int) Json.num(x, "speaker", -1);
            assertTrue("a Kokoro speaker: " + sid, sid >= 0 && sid < 53);
            assertEquals("hi".equals(Json.str(x, "language", "")), sid >= 31 && sid <= 34);
        }
    }

    @Test public void aChosenVoiceIsSpoken() {
        KokoroCast k = new KokoroCast();
        float[] before = k.voice(null).clone();
        k.choose(null, 32);
        assertEquals("the Hindi lines take the chosen Hindi voice", 32f, k.voice(null)[0], 0f);
        assertEquals("the English lines keep theirs", before[1], k.voice(null)[1], 0f);
        k.choose(null, 21);
        assertEquals("an English voice is for the English lines", 21f, k.voice(null)[1], 0f);
        assertEquals(32f, k.voice(null)[0], 0f);
        k.choose(null, -1);
        assertEquals("no choice changes nothing", 21f, k.voice(null)[1], 0f);
    }
}

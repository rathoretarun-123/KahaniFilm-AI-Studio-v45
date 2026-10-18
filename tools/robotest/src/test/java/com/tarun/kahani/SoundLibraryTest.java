package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Json;
import com.tarun.kahani.core.SoundLib;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.Test;

/**
 * v47.10 (the user: "save openly available sounds and voices in library"): the app's own sound library carries the open
 * recordings of Moodist (CC0 or the Pixabay Content License), each file present, each entry with its licence, found by
 * the script's own words — the sofa story's fan ("पंखा धीरे चल रहा है और हल्की घर-घर की आवाज") — while a word the old
 * library already answered keeps its old recording.
 */
public class SoundLibraryTest {
    static final File DIR = new File("../../app/src/main/assets/sounds");

    static String index() throws Exception { return new String(Files.readAllBytes(new File(DIR, "index.json").toPath()), StandardCharsets.UTF_8); }

    @Test public void everyEntryHasItsFileAndEveryOpenRecordingItsLicence() throws Exception {
        Object o = Json.parseLoose(index());
        assertTrue(o instanceof List);
        int open = 0;
        for (Object x : (List<?>) o) {
            String f = Json.str(x, "file", "");
            assertTrue("the file of " + f + " is in the library", new File(DIR, f).length() > 1000);
            if (f.startsWith("moodist_")) {
                open++;
                assertTrue(f + " says its licence", Json.str(x, "licence", "").contains("CC0"));
                assertTrue(f + " has Hindi words", Json.str(x, "words", "").matches(".*[\\u0900-\\u097F].*"));
            }
        }
        assertTrue("the open recordings are in: " + open, open >= 80);
        assertTrue("their credits are kept", new File(DIR, "CREDITS-moodist.txt").isFile());
    }

    @Test public void theScriptsWordsFindThem() throws Exception {
        SoundLib lib = new SoundLib(null);
        lib.addIndex(index(), "");
        assertEquals("moodist_things_ceiling_fan.ogg", lib.best("पंखा धीरे चल रहा है और हल्की घर-घर की आवाज", "amb", null).path);
        assertEquals("moodist_things_boiling_water.ogg", lib.best("केतली में चाय का पानी उबल रहा है", "sfx", null).path);
        assertEquals("a word the old library answered keeps its recording", "rain.ogg", lib.best("बारिश हो रही है", "amb", null).path);
    }
}

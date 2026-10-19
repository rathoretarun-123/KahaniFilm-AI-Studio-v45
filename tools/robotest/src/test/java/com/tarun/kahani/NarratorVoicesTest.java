package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.EdgeVoice;
import com.tarun.kahani.core.NarratorVoices;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * v47.13 (the user: "narrator voices and include them in apk library with proper metadata"): the studio's narrator
 * voices shipped in the APK (assets/voices/narrators.json) are all there and each carries its whole metadata — names,
 * language, gender, age, tone, pace, what it suits, a sample line, its pitch and pace on the phone and its neural voice.
 */
public class NarratorVoicesTest {
    static List<NarratorVoices.Voice> voices() throws Exception {
        File f = new File(System.getProperty("kahani.assets", "../../app/src/main/assets"), NarratorVoices.ASSET);
        assertTrue("the narrator library is in the app's assets: " + f, f.isFile());
        return NarratorVoices.parse(new String(Files.readAllBytes(f.toPath()), "UTF-8"));
    }

    @Test public void everyVoiceHasItsMetadata() throws Exception {
        List<NarratorVoices.Voice> all = voices();
        assertTrue("narrator voices: " + all.size(), all.size() >= 12);
        Set<String> ids = new HashSet<String>(), known = new HashSet<String>();
        for (String[] l : new String[][]{EdgeVoice.HINDI_FEMALE, EdgeVoice.HINDI_MALE, EdgeVoice.ENGLISH_FEMALE, EdgeVoice.ENGLISH_MALE, EdgeVoice.ENGLISH_CHILD}) known.addAll(Arrays.asList(l));
        for (NarratorVoices.Voice v : all) {
            assertTrue("unique id " + v.id, ids.add(v.id));
            for (String[] f : new String[][]{{"name", v.name}, {"name_en", v.nameEn}, {"gender", v.gender}, {"age", v.age}, {"tone", v.tone}, {"pace", v.pace}, {"best_for", v.bestFor}, {"sample", v.sample}})
                assertTrue(v.id + ": " + f[0], f[1] != null && f[1].trim().length() > 0);
            assertTrue(v.id + ": language", v.language.equals("hi") || v.language.equals("en"));
            assertTrue(v.id + ": a neural voice the studio has: " + v.naturalVoice, known.contains(v.naturalVoice));
            assertEquals(v.id + ": its neural voice speaks its language", v.hindi(), v.naturalVoice.startsWith("hi-"));
            assertTrue(v.id + ": phone pitch " + v.phonePitch, v.phonePitch >= 0.6f && v.phonePitch <= 1.4f);
            assertTrue(v.id + ": phone pace " + v.phoneRate, v.phoneRate >= 0.6f && v.phoneRate <= 1.3f);
            assertTrue(v.id + ": a sample line in its language", v.hindi() == com.tarun.kahani.core.Txt.mostlyHindi(v.sample));
        }
    }

    @Test public void bothLanguagesHaveNarratorsAndOneDefault() throws Exception {
        List<NarratorVoices.Voice> all = voices();
        for (boolean hi : new boolean[]{true, false}) {
            int n = 0, def = 0;
            for (NarratorVoices.Voice v : all) if (v.hindi() == hi) { n++; if (v.isDefault) def++; }
            assertTrue((hi ? "Hindi" : "English") + " narrators: " + n, n >= 5);
            assertEquals((hi ? "Hindi" : "English") + ": one default narrator", 1, def);
            assertEquals("the story's language first", hi, NarratorVoices.forStory(all, hi).get(0).hindi());
        }
    }
}

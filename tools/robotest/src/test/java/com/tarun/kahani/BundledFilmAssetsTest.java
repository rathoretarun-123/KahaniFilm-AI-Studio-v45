package com.tarun.kahani;

import static org.junit.Assert.*;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;
import org.junit.Test;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Ensures both stories and their bundled reference media are included in the offline APK. */
public final class BundledFilmAssetsTest {
    private final File assets = new File(System.getProperty("kahani.assets"));
    private File required(String path) {
        File f = new File(assets, path);
        assertTrue("Missing bundled file: " + f, f.isFile() && f.length() > 0);
        return f;
    }
    private void mp4(String path) throws Exception {
        File f = required(path);
        assertTrue("Reference video is too small: " + f, f.length() > 1000000L);
        byte[] h = new byte[12];
        try (FileInputStream in = new FileInputStream(f)) { assertEquals(12, in.read(h)); }
        assertEquals("ftyp", new String(h, 4, 4, StandardCharsets.US_ASCII));
    }
    private int images(File dir) {
        File[] fs = dir.listFiles(); if (fs == null) return 0;
        int n=0; for (File f:fs) if (f.isFile() && f.getName().matches("(?i).*\\.(webp|jpg|jpeg|png)$")) n++;
        return n;
    }
    @Test public void bothScreenplaysParse() throws Exception {
        Story a=ScriptParser.parse(new String(Files.readAllBytes(required("aakhiri_chai_screenplay.txt").toPath()),StandardCharsets.UTF_8));
        Story b=ScriptParser.parse(new String(Files.readAllBytes(required("babu_ke_liye_screenplay.txt").toPath()),StandardCharsets.UTF_8));
        assertNotNull(a); assertNotNull(b); assertFalse(a.scenes.isEmpty()); assertFalse(b.scenes.isEmpty());
    }
    @Test public void bothUploadedReferencesAreValidMp4() throws Exception {
        mp4("film_library/aakhiri_chai/videos/user_uploaded_reference.mp4");
        mp4("film_library/babu_ke_liye/videos/user_uploaded_reference.mp4");
    }
    @Test public void stillsAndManifestsAreBundled() throws Exception {
        File a=new File(assets,"film_library/aakhiri_chai/stills");
        File b=new File(assets,"film_library/babu_ke_liye/stills");
        assertTrue(a.isDirectory()); assertTrue(b.isDirectory());
        assertTrue("Aakhiri still count: "+images(a),images(a)>=60);
        assertTrue("Babu still count: "+images(b),images(b)>=40);
        required("film_library/aakhiri_chai/manifest.json");
        required("film_library/babu_ke_liye/manifest.json");
    }

    @Test public void aakhiriChaiManifestHasConservativeSemanticReferenceTags() throws Exception {
        String manifest = new String(Files.readAllBytes(required("film_library/aakhiri_chai/manifest.json").toPath()), StandardCharsets.UTF_8);
        assertTrue("Raghu references must be labelled", manifest.contains("\"entity_id\": \"raghu_kaka\""));
        assertTrue("Aarav references must be labelled", manifest.contains("\"entity_id\": \"aarav\""));
        assertTrue("Meera references must be labelled", manifest.contains("\"entity_id\": \"meera\""));
        assertTrue("Tea-stall references must be labelled", manifest.contains("\"entity_id\": \"tea_stall\""));
        assertTrue("Ambiguous assets must remain review-required", manifest.contains("\"review_required\": true"));
        assertTrue("Only high-confidence assets are auto-assigned", manifest.contains("\"auto_assign\": true"));
    }
}

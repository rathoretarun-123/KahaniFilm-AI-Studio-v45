package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

/** Narration contract: stage directions are visual-only, even if a legacy narrator toggle is enabled. */
public final class NarratorContractTest {
    @Test public void stageDirectionsNeverBecomeNarratorDialogue() throws Exception {
        File assets = new File(System.getProperty("kahani.assets", "../../app/src/main/assets"));
        File file = new File(assets, "aakhiri_chai_screenplay.txt");
        assertTrue(file.isFile());
        Story story = ScriptParser.parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        assertNotNull(story);
        Director.Options options = new Director.Options();
        options.narrator = true; // Legacy toggle must not turn scene descriptions into spoken narration.
        Film film = new Director(story, options).prepare();
        assertNotNull(film);
        assertEquals("Only authored dialogue beats may become spoken lines", story.dialogueCount(), film.lines.size());
        assertFalse("title narration remains opt-in", options.narrateTitle);
        assertFalse("subtitles remain off by default", options.subtitles);
    }
}

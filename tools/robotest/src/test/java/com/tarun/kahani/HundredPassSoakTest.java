package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import org.junit.Test;

/**
 * Deterministic repeated regression soak. This tests the story-to-timeline planning boundary,
 * not external provider availability or perceptual Pixar-level quality.
 */
public class HundredPassSoakTest {
    @Test
    public void oneHundredScriptToTimelinePasses() {
        String[] corpus = SoakStories.all();
        assertEquals("curated multilingual corpus size", 20, corpus.length);
        int dialogueLines = 0;
        int timelineSegments = 0;
        for (int pass = 0; pass < 100; pass++) {
            String source = corpus[pass % corpus.length];
            Story story = ScriptParser.parse(source);
            assertNotNull("parsed story on pass " + pass, story);
            assertNotNull("title on pass " + pass, story.title);
            assertFalse("scenes on pass " + pass + " (" + story.title + ")", story.scenes.isEmpty());
            Director director = new Director(story, new Director.Options());
            Film film = director.prepare();
            assertNotNull("prepared film on pass " + pass, film);
            assertFalse("timeline on pass " + pass + " (" + story.title + ")", film.segs.isEmpty());
            assertTrue("positive duration on pass " + pass, film.duration > 0f);
            dialogueLines += story.dialogueCount();
            timelineSegments += film.segs.size();
        }
        System.out.println("100-pass script-to-timeline soak passed; corpus=20, dialogueLines="
                + dialogueLines + ", timelineSegments=" + timelineSegments);
        assertTrue("dialogue corpus exercised", dialogueLines > 100);
        assertTrue("timeline corpus exercised", timelineSegments > 100);
    }
}

package com.tarun.kahani;

import static org.junit.Assert.*;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.tarun.kahani.app.AndroidGfx;
import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.Renderer;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;

/** 1,000 deterministic generated-script planning/render regression cases plus visual audit sheet. */
@RunWith(RobolectricTestRunner.class)
public class HundredPassSoakTest {
    private static final String[] ACTIONS = {
        "A soft breeze stirs the leaves near the doorway",
        "A character pauses and looks toward a distant sound",
        "A loose cloth edge flutters briefly in the wind",
        "Dust drifts through a beam of morning light",
        "A small object rolls a short distance and settles",
        "A character shifts their weight before taking a step",
        "Cloud shadows pass slowly across the ground",
        "A character turns their head before moving their body",
        "A few petals cross the foreground and fall",
        "A character glances at the other character, then looks away",
        "A distant bird crosses the open sky",
        "A character reaches carefully toward a nearby object",
        "A quiet pause lets the previous line settle",
        "A character takes one measured step into the room",
        "A faint ripple moves across a puddle",
        "A branch bends gently and returns toward rest",
        "A character's expression changes from concern to relief",
        "A moving shadow reveals the shape of the doorway",
        "A character looks down to check where their foot lands",
        "A warm shaft of light reveals floating dust"
    };

    @Test
    public void oneThousandGeneratedScriptsRenderAndProduceVisualAuditSheet() throws Exception {
        String[] corpus = SoakStories.all();
        assertEquals("curated multilingual seed corpus", 20, corpus.length);
        final int width = 320, height = 180;
        Bitmap frame = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        AndroidGfx gfx = new AndroidGfx(frame, 2);
        Bitmap sheet = Bitmap.createBitmap(480, 270, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(sheet);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
        label.setColor(0xFFFFFFFF);
        label.setTextSize(8f);
        Set<Long> hashes = new HashSet<Long>();
        int rendered = 0, scenes = 0, shots = 0, dialogue = 0, blankFrames = 0;

        for (int pass = 0; pass < 1000; pass++) {
            String base = corpus[pass % corpus.length];
            String action1 = ACTIONS[(pass * 7 + pass / 20) % ACTIONS.length];
            String action2 = ACTIONS[(pass * 11 + pass / 7 + 3) % ACTIONS.length];
            String generated = base + "\n(" + action1 + ".)\n(" + action2 + ".)\n";
            Story story = ScriptParser.parse(generated);
            assertNotNull("parse pass " + pass, story);
            assertNotNull("title pass " + pass, story.title);
            assertFalse("scenes pass " + pass + " title=" + story.title, story.scenes.isEmpty());

            Director director = new Director(story, new Director.Options());
            Film film = director.prepare();
            assertNotNull("prepared film pass " + pass, film);
            Art art = new Art();
            film = director.direct(art);
            assertNotNull("directed film pass " + pass, film);
            assertFalse("shots pass " + pass + " title=" + story.title, film.shots.isEmpty());
            assertFalse("timeline pass " + pass + " title=" + story.title, film.segs.isEmpty());
            assertTrue("duration pass " + pass, film.duration > 0f);

            Film.Seg sceneSegment = null;
            for (Film.Seg segment : film.segs) {
                if (segment.type == Film.S_SCENE) { sceneSegment = segment; break; }
            }
            assertNotNull("no scene segment pass " + pass + " title=" + story.title, sceneSegment);
            Film.Shot shot = null;
            for (Film.Shot candidate : film.shots) {
                if (candidate.t >= sceneSegment.t0 && candidate.t < sceneSegment.t1) { shot = candidate; break; }
            }
            assertNotNull("no scene shot pass " + pass + " title=" + story.title, shot);
            float time = shot.t + Math.min(0.25f, Math.max(0.05f, shot.dur * 0.25f));
            frame.eraseColor(0xFF202020);
            new Renderer(film, art).render(gfx, time);
            int varied = 0;
            long hash = 1469598103934665603L;
            for (int y = 0; y < height; y += 6) for (int x = 0; x < width; x += 6) {
                int rgb = frame.getPixel(x, y) & 0x00FFFFFF;
                if (rgb != 0x00202020 && rgb != 0x00000000) varied++;
                hash ^= rgb;
                hash *= 1099511628211L;
            }
            if (varied < 8) blankFrames++;
            // Use a step relatively prime to the 20-template corpus so the sheet samples different seeds.
            if (pass % 37 == 0 && pass / 37 < 25) {
                hashes.add(hash);
                int sample = pass / 37;
                int col = sample % 5, row = sample / 5;
                Rect dest = new Rect(col * 96, row * 54, col * 96 + 96, row * 54 + 54);
                canvas.drawBitmap(frame, null, dest, paint);
                Paint shade = new Paint();
                shade.setColor(0xAA000000);
                canvas.drawRect(col * 96, row * 54, col * 96 + 96, row * 54 + 10, shade);
                canvas.drawText("case " + (pass + 1), col * 96 + 3, row * 54 + 8, label);
            }
            rendered++;
            scenes += story.scenes.size();
            shots += film.shots.size();
            dialogue += story.dialogueCount();
            if (pass % 100 == 99) System.out.println("SOAK_PROGRESS " + (pass + 1) + "/1000");
        }

        assertEquals(1000, rendered);
        assertTrue("dialogue coverage " + dialogue, dialogue > 1000);
        assertTrue("shot coverage " + shots, shots > 5000);
        assertTrue("near-blank rendered frames " + blankFrames, blankFrames < 10);
        assertTrue("visual diversity in sampled frames " + hashes.size(), hashes.size() >= 10);

        File out = new File(System.getProperty("kahani.out", "build/frames"));
        assertTrue("create visual audit directory", out.isDirectory() || out.mkdirs());
        File sheetFile = new File(out, "generated_1000_contact_sheet.jpg");
        try (FileOutputStream stream = new FileOutputStream(sheetFile)) {
            assertTrue("contact sheet encoded", sheet.compress(Bitmap.CompressFormat.JPEG, 80, stream));
        }
        System.out.println("SOAK_SUMMARY cases=" + rendered + " scenes=" + scenes + " shots=" + shots
                + " dialogueLines=" + dialogue + " nearBlankFrames=" + blankFrames
                + " sampledVisualHashes=" + hashes.size() + " contactSheet=" + sheetFile.getAbsolutePath());
        frame.recycle();
        sheet.recycle();
    }
}

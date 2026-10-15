package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.PixarLead;
import com.tarun.kahani.core.Renderer;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;
import com.tarun.kahani.jvm.AwtGfx;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Random;
import org.junit.Test;

/**
 * v47.2: the three test stories (बाबू के लिए, आखिरी चाय, आखिरी जोड़ी) directed 1,000 times each through the
 * production Director with the options varied every pass — the five delivery formats, pace, subtitles, scene cards,
 * animation on twos, the narrator, three output heights — and a frame of the production renderer drawn every 25th
 * pass. Every pass must direct without an exception, keep every camera finite, every key in time order, every shot
 * inside the film, and the drawn frame must not be blank. -Dkahani.story.passes lowers the count for a quick run.
 */
public class ThreeStoriesSoakTest {
    // (v47.3: 1,000 per story locally and on demand; 25 on CI so the build fits its time)
    static final int PASSES = Integer.getInteger("kahani.story.passes", System.getenv("CI") != null ? 25 : 1000);
    static final String[] ASPECTS = {"16:9", "9:16", "1:1", "4:5", "2.39:1"};

    @Test public void babuKeLiyeDirectsAThousandTimes() throws Exception { soak("babu_ke_liye", "babu_ke_liye_screenplay.txt", true); }
    @Test public void aakhiriChaiDirectsAThousandTimes() throws Exception { soak("aakhiri_chai", "aakhiri_chai_screenplay.txt", true); }
    @Test public void aakhiriJodiDirectsAThousandTimes() throws Exception { soak("aakhiri_jodi", "tools/testdata/aakhiri_jodi/aakhiri_jodi_screenplay.txt", false); }

    static void soak(String id, String screenplay, boolean pictures) throws Exception {
        File assets = new File(System.getProperty("kahani.assets", "../../app/src/main/assets")).getAbsoluteFile();
        // the repository root is four levels above app/src/main/assets; the third story lives in tools/testdata
        File repo = assets.getParentFile().getParentFile().getParentFile().getParentFile();
        File file = screenplay.startsWith("tools/") ? new File(repo, screenplay) : new File(assets, screenplay);
        assertTrue("screenplay exists: " + file, file.isFile());
        Story story = ScriptParser.parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        assertFalse(id + " has scenes", story.scenes.isEmpty());
        Art art = pictures ? ReferenceArtLoader.load(id, story, assets) : new Art();
        if (pictures) assertTrue(id + " reference sprites loaded", art.sprites.size() >= 2);
        Random rnd = new Random(4711);
        long t0 = System.currentTimeMillis();
        int frames = 0;
        final int w = 320;
        for (int p = 0; p < PASSES; p++) {
            Director.Options o = new Director.Options();
            o.pace = 0.8f + 0.5f * rnd.nextFloat();
            o.aspect = ASPECTS[p % ASPECTS.length];
            o.subtitles = (p & 1) == 1;
            o.sceneCards = p % 5 == 0;
            o.onTwos = p % 7 == 0;
            o.narrator = p % 11 == 0;
            o.outHeight = new int[]{360, 720, 1080}[p % 3];
            String tag = id + " pass " + p + " (" + o.aspect + ", pace " + o.pace + ", subs " + o.subtitles + ", cards " + o.sceneCards + ", twos " + o.onTwos + ", h " + o.outHeight + ")";
            Film f;
            try {
                Director d = new Director(story, o);
                d.prepare();
                f = d.direct(art);
            } catch (Throwable e) {
                throw new AssertionError(tag + ": " + e, e);
            }
            assertTrue(tag + " has a duration", f.duration > 1);
            assertFalse(tag + " has shots", f.shots.isEmpty());
            for (Film.Shot sh : f.shots) assertTrue(tag + " shot inside the film: " + sh.t, sh.t >= -0.01f && sh.t <= f.duration + 0.01f);
            for (Film.Line l : f.lines) assertTrue(tag + " line has length", l.dur > 0);
            for (Film.Seg sg : f.segs) {
                assertTrue(tag + " segment in order", sg.t1 >= sg.t0);
                for (Film.Cam c : sg.cams) {
                    assertFalse(tag + " camera finite at " + c.t, Float.isNaN(c.cx) || Float.isNaN(c.cy) || Float.isNaN(c.zoom) || Float.isInfinite(c.cx) || Float.isInfinite(c.cy));
                    assertTrue(tag + " zoom positive at " + c.t, c.zoom > 0);
                }
                for (Film.Actor ac : sg.actors) for (int i = 1; i < ac.keys.size(); i++)
                    assertTrue(tag + " keys of " + ac.c.displayName + " in time order", ac.keys.get(i).t >= ac.keys.get(i - 1).t - 1e-3f);
            }
            if (p % 25 == 0) {
                int h = Math.round(w / PixarLead.spec(o.aspect).ratio) & ~1;
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                AwtGfx g = new AwtGfx(img);
                float t = Math.min(f.duration - 0.2f, f.duration * (0.25f + 0.5f * rnd.nextFloat()));
                new Renderer(f, art).render(g, t);
                long varied = 0;
                for (int y = 0; y < h; y += 4) for (int x = 0; x < w; x += 4) if ((img.getRGB(x, y) & 0xFFFFFF) != 0) varied++;
                assertTrue(tag + " frame at " + t + " is not blank: " + varied, varied > 100);
                frames++;
            }
            if ((p + 1) % 200 == 0) System.out.println("STORY_SOAK " + id + " " + (p + 1) + "/" + PASSES + " " + (System.currentTimeMillis() - t0) / 1000 + "s");
        }
        System.out.println("STORY_SOAK " + id + " done: " + PASSES + " passes, " + frames + " frames, " + (System.currentTimeMillis() - t0) / 1000 + "s");
    }
}

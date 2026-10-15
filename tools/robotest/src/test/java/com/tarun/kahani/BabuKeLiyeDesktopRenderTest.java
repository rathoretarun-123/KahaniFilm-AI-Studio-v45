package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.Renderer;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;
import com.tarun.kahani.jvm.AwtGfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.junit.Test;

/** Deterministic off-device render acceptance for the second bundled story. */
public class BabuKeLiyeDesktopRenderTest {
    @Test public void screenplayProducesReviewableFrames() throws Exception {
        File assets = new File(System.getProperty("kahani.assets", "../../app/src/main/assets"));
        File screenplay = new File(assets, "babu_ke_liye_screenplay.txt");
        assertTrue("Babu Ke Liye screenplay exists: " + screenplay, screenplay.isFile());
        String source = new String(java.nio.file.Files.readAllBytes(screenplay.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        Story story = ScriptParser.parse(source);
        assertNotNull(story);
        assertFalse("screenplay has scenes", story.scenes.isEmpty());
        Director director = new Director(story, new Director.Options());
        Art art = ReferenceArtLoader.load("babu_ke_liye", story, assets);
        assertTrue("real character reference sprites loaded: " + art.sprites.size(), art.sprites.size() >= 2);
        assertTrue("real location backgrounds loaded: " + art.scenes.size(), art.scenes.size() >= Math.min(3, story.scenes.size()));
        assertTrue("missing isolated mailbox is surfaced for review",
                art.notes.stream().anyMatch(n -> n.startsWith("MISSING_REFERENCE:") && n.contains("लाल डाक पेटी")));
        File out = new File(System.getProperty("kahani.out", "build/frames"));
        assertTrue(out.isDirectory() || out.mkdirs());
        java.nio.file.Files.write(new File(out, "babu_ke_liye_reference_notes.txt").toPath(),
                String.join("\n", art.notes).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Film prepared = director.prepare();
        assertNotNull("director prepared a film", prepared);
        Film film = director.direct(art);
        assertSame("direct() must complete the prepared film", prepared, film);
        assertNotNull(film);
        assertFalse("director produced shots", film.shots.isEmpty());

        int width = 640, height = 360;
        BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        AwtGfx gfx = new AwtGfx(frame);
        int samples = 0, nonBlank = 0;
        for (float time = 0.1f; time < film.duration; time += Math.max(0.5f, film.duration / 12f)) {
            Graphics2D clear = frame.createGraphics();
            clear.setColor(new Color(0xFF202020, true));
            clear.fillRect(0, 0, width, height);
            clear.dispose();
            new Renderer(film, art).render(gfx, time);
            long varied = 0;
            for (int y = 0; y < height; y += 4) for (int x = 0; x < width; x += 4) {
                int rgb = frame.getRGB(x, y) & 0x00FFFFFF;
                if (rgb != 0x00202020 && rgb != 0) varied++;
            }
            if (varied > 200) nonBlank++;
            if (samples < 12) {
                File png = new File(out, String.format(java.util.Locale.ROOT, "babu_ke_liye_%02d.png", samples));
                assertTrue("write frame " + png.getName(), ImageIO.write(frame, "png", png));
            }
            samples++;
        }
        assertTrue("sampled frames: " + samples, samples >= 8);
        assertTrue("nonblank frames: " + nonBlank + "/" + samples, nonBlank >= samples * 0.8);
    }
}

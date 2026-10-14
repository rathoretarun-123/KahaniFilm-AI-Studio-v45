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

/** Deterministic off-device render check for the same core renderer used by the Android app. */
public class AakhiriChaiDesktopRenderTest {
    @Test public void screenplayProducesNonBlankReviewableFrames() throws Exception {
        File assets = new File(System.getProperty("kahani.assets", "../../app/src/main/assets"));
        File screenplay = new File(assets, "aakhiri_chai_screenplay.txt");
        assertTrue("Aakhiri Chai screenplay exists: " + screenplay, screenplay.isFile());
        String source = new String(java.nio.file.Files.readAllBytes(screenplay.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        Story story = ScriptParser.parse(source);
        assertNotNull(story);
        assertFalse("screenplay has scenes", story.scenes.isEmpty());
        Director director = new Director(story, new Director.Options());
        Film film = director.direct(new Art());
        assertNotNull(film);
        assertFalse("director produced shots", film.shots.isEmpty());

        int width = 640, height = 360;
        BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        AwtGfx gfx = new AwtGfx(frame);
        File out = new File(System.getProperty("kahani.out", "build/frames"));
        assertTrue(out.isDirectory() || out.mkdirs());
        int samples = 0, nonBlank = 0;
        for (float time = 0.1f; time < film.duration; time += 0.5f) {
            Graphics2D clear = frame.createGraphics();
            clear.setColor(new Color(0xFF202020, true));
            clear.fillRect(0, 0, width, height);
            clear.dispose();
            new Renderer(film, new Art()).render(gfx, time);
            long varied = 0;
            for (int y = 0; y < height; y += 4) for (int x = 0; x < width; x += 4) {
                int rgb = frame.getRGB(x, y) & 0x00FFFFFF;
                if (rgb != 0x00202020 && rgb != 0) varied++;
            }
            if (varied > 200) nonBlank++;
            if (samples < 12) {
                File png = new File(out, String.format(java.util.Locale.ROOT, "aakhiri_chai_%02d.png", samples));
                assertTrue("write frame " + png.getName(), ImageIO.write(frame, "png", png));
            }
            samples++;
        }
        assertTrue("sampled frames: " + samples, samples >= 10);
        assertTrue("nonblank frames: " + nonBlank + "/" + samples, nonBlank >= samples * 0.8);
    }
}

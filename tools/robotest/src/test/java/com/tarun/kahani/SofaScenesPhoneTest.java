package com.tarun.kahani;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import android.graphics.Bitmap;

import com.tarun.kahani.app.AndroidGfx;
import com.tarun.kahani.app.Project;
import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.FilmLook;
import com.tarun.kahani.core.Pose;
import com.tarun.kahani.core.Renderer;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/**
 * v48 (the user's film from the phone, सोफे वाली जादू की बातें: in scene 2 the girls stood before the window, the
 * sofa gone): the user's own pictures as the phone's Studio keeps them — both scenes at the living room (the sofa
 * corner is part of it), the sofa corner's pictures stored under scene 1 only — made the phone's way. Both girls stay
 * seated through scene 2 and its close shots stand against the sofa corner's pictures. The user's pictures are not in
 * the public repository: the test needs -Dkahani.sofaAssets=<folder with cast.txt> and is skipped without it.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, manifest = Config.NONE)
public class SofaScenesPhoneTest {
    @Test public void bothGirlsStaySeatedInSceneTwo() throws Exception {
        File assets = new File(System.getProperty("kahani.sofaAssets", "/nonexistent"));
        assumeTrue("the user's sofa pictures (-Dkahani.sofaAssets)", new File(assets, "cast.txt").isFile());
        android.content.Context ctx = RuntimeEnvironment.getApplication();
        Project p = Project.create(ctx);
        StringBuilder cast = new StringBuilder();
        for (String l : new String(Files.readAllBytes(new File(assets, "cast.txt").toPath()), StandardCharsets.UTF_8).split("\n")) {
            String[] f = l.split("\\|");
            if (l.startsWith("scene|2v")) continue;                       // (as the phone keeps them: under scene 1 only)
            for (String part : f) if (part.matches(".+\\.(png|jpg)")) { File src = new File(assets, part); if (src.isFile() && !new File(p.dir, part).exists()) Files.copy(src.toPath(), new File(p.dir, part).toPath()); }
            cast.append(l).append('\n');
        }
        Story story = ScriptParser.parse(new String(Files.readAllBytes(new File("../testdata/sofe_wali_jadu/sofe_wali_jadu_screenplay.txt").toPath()), StandardCharsets.UTF_8));
        Art.spriteSide = 640; Art.backdropSide = 640;
        Art art = Art.fromManifest(cast.toString(), story, p.loader());
        Director d = new Director(story, new Director.Options());
        d.prepare();
        Film film = d.direct(art);
        // the second scene of the story, whatever its number in the film
        java.util.List<Integer> scenes = new java.util.ArrayList<Integer>();
        for (Film.Seg s : film.segs) if (s.scene >= 0 && !scenes.contains(s.scene)) scenes.add(s.scene);
        assertTrue("two scenes in the film: " + scenes, scenes.size() >= 2);
        int second = scenes.get(1);
        int w = 424, h = 240;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        AndroidGfx g = new AndroidGfx(bmp, 4);
        Renderer r = new Renderer(film, art);
        File out = new File("build/sofa_frames");
        out.mkdirs();
        StringBuilder rep = new StringBuilder("SOFA SCENE 2 (film scene " + second + ")\n");
        int standing = 0, seen = 0;
        for (Film.Seg s : film.segs) {
            if (s.scene != second) continue;
            for (float t = s.t0 + 0.4f; t < s.t1; t += 2f) {
                for (Film.Actor a : s.actors) {
                    Film.Key k = a.stateAt(t);
                    if (!k.visible) continue;
                    seen++;
                    if (k.body != Pose.SIT) standing++;
                    rep.append(String.format(java.util.Locale.US, "  %.1f s %s body=%d seat=%d%n", t, a.c.displayName, k.body, k.seat));
                }
                r.render(g, t);
                try (FileOutputStream fo = new FileOutputStream(new File(out, String.format(java.util.Locale.US, "s2_%05.1f.png", t)))) { bmp.compress(Bitmap.CompressFormat.PNG, 100, fo); }
            }
        }
        g.release();
        System.out.println(rep);
        assertTrue("someone is in scene 2", seen > 0);
        assertEquals("both girls stay seated through scene 2:\n" + rep, 0, standing);
    }
}

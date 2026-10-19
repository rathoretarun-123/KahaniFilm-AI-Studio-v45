package com.tarun.kahani;

import static org.junit.Assert.*;

import android.graphics.Bitmap;

import com.tarun.kahani.app.AndroidGfx;
import com.tarun.kahani.app.Project;
import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.FilmLook;
import com.tarun.kahani.core.FrameAudit;
import com.tarun.kahani.core.Renderer;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/**
 * v47.12 (the user: "check everything is working fine … from script"): a new story, never seen by the engine, made the
 * way the phone makes it — the user's pictures read by the phone's own loader (Project.loader: BitmapFactory, the
 * cut-out), the Director, and every frame drawn by the phone's AndroidGfx with the film look — not the desktop
 * renderer. Every frame is audited (no jolt, jump or pop); the faces live, the speakers' lips move, the picture moves
 * from frame to frame; one frame a second is kept under build/phone_frames to be looked at.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, manifest = Config.NONE)
public class PhoneFilmTest {
    static final String SCRIPT = "शीर्षक: नीम के नीचे\n\n"
            + "पात्र\n\n1. रघुवीर जी (72 साल)\n(बुज़ुर्ग, सफ़ेद कुर्ता, चश्मा)\n\n2. परी (7 साल)\n(छोटी लड़की, लाल रिबन, स्कूल बैग)\n\n"
            + "स्थान\n\n1. पार्क\n(नीम का पेड़, लकड़ी की बेंच)\n\n"
            + "कहानी\n\nदृश्य 1\n(रघुवीर जी बेंच पर बैठे हैं)\n\n"
            + "(परी दौड़ती हुई आती है)\n\n"
            + "परी: \"दादाजी! आज मैंने स्कूल में पहला इनाम जीता!\" (खुशी से उछलते हुए)\n\n"
            + "रघुवीर जी: \"अरे वाह! मेरी परी तो सबसे होशियार है।\" (मुस्कुराते हुए)\n\n"
            + "(परी बेंच पर दादाजी के पास बैठ जाती है)\n\n"
            + "परी: \"पर मेरी सहेली रो रही थी, उसे कुछ नहीं मिला।\" (उदास होकर)\n\n"
            + "रघुवीर जी: \"तो कल तुम उसे अपना इनाम दिखाकर हँसाना।\" (प्यार से समझाते हुए)\n\n"
            + "समाप्त\n";

    @Test public void aNewStoryIsMadeOnThePhoneFromTheUsersPictures() throws Exception {
        android.content.Context ctx = RuntimeEnvironment.getApplication();
        Project p = Project.create(ctx);
        File stills = new File("../../tools/testdata/aakhiri_chitthi/stills");
        Files.copy(new File(stills, "grandpa_standing_front.jpg").toPath(), new File(p.dir, "grandpa.jpg").toPath());
        Files.copy(new File(stills, "pari_standing_front.jpg").toPath(), new File(p.dir, "pari.jpg").toPath());
        String cast = "char|रघुवीर जी|grandpa.jpg\nchar|परी|pari.jpg\n";
        Story story = ScriptParser.parse(SCRIPT);
        assertEquals("both characters read", 2, story.characters.size());
        Art.spriteSide = 640; Art.backdropSide = 640;
        Art art = Art.fromManifest(cast, story, p.loader());
        Director.Options opt = new Director.Options();
        Director d = new Director(story, opt);
        d.prepare();
        Film film = d.direct(art);
        assertTrue("every line in the film: " + film.lines.size(), film.lines.size() == 4);

        int w = 480, h = 270, fps = 24, frames = (int) (film.duration * fps);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        AndroidGfx g = new AndroidGfx(bmp, 4);
        Renderer r = new Renderer(film, art);
        FrameAudit audit = new FrameAudit(fps);
        r.audit = audit;
        FilmLook look = new FilmLook(w, h);
        FilmLook.Params lp = new FilmLook.Params();
        for (java.util.concurrent.atomic.AtomicLong s : Renderer.STAT) s.set(0);
        File out = new File("build/phone_frames");
        out.mkdirs();
        int[] px = new int[w * h], prev = null;
        int still = 0, blank = 0;
        for (int f = 0; f < frames; f++) {
            r.render(g, f / (float) fps);
            bmp.getPixels(px, 0, w, 0, 0, w, h);
            look.apply(px, FilmLook.at(film, f / (float) fps, lp));
            int lo = 255, hi = 0;
            for (int i = 0; i < px.length; i += 97) { int v = (px[i] >> 8) & 255; lo = Math.min(lo, v); hi = Math.max(hi, v); }
            if (hi - lo < 12) blank++;
            if (prev != null) {
                long diff = 0;
                for (int i = 0; i < px.length; i += 31) diff += Math.abs(((px[i] >> 8) & 255) - ((prev[i] >> 8) & 255));
                if (diff == 0) still++;
            }
            prev = px.clone();
            if (f % fps == 0) {
                Bitmap o = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888);
                try (FileOutputStream fo = new FileOutputStream(new File(out, String.format(java.util.Locale.US, "f%04d.png", f)))) { o.compress(Bitmap.CompressFormat.PNG, 100, fo); }
            }
        }
        g.release();
        FrameAudit.Result res = audit.read(film);
        String stats = Renderer.stats();
        System.out.println("PHONE FILM " + frames + " frames: " + stats + "\n" + res.text());
        assertTrue("no blank frames (" + blank + " of " + frames + ")", blank <= fps * 2);       // (the title fades in from black)
        assertTrue("the picture moves: only " + still + " of " + frames + " frames identical to the one before", still < frames / 4);
        assertTrue("the frame audit is clean:\n" + res.text(), res.clean());
        long drawn = Renderer.STAT[0].get(), alive = Renderer.STAT[1].get(), speaking = Renderer.STAT[2].get(), lips = Renderer.STAT[3].get();
        assertTrue("the user's pictures are drawn: " + stats, drawn > frames / 2);
        assertTrue("their faces live: " + stats, alive > drawn * 0.9);
        assertTrue("the speakers' lips move: " + stats, speaking > 0 && lips > speaking * 0.8);
    }
}

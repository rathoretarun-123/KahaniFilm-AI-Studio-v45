package com.tarun.kahani;

import static org.junit.Assert.*;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

import com.tarun.kahani.app.FilmJob;
import com.tarun.kahani.app.MainActivity;
import com.tarun.kahani.app.Project;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/**
 * v47.10 (the user: "recheck your studio page and film making page ... very cluttered ... have bugs, no continue button"):
 * the pages as a phone shows them — every screen drawn whole (its full scroll) into a picture under build/ui_shots, and
 * the film page in each of its states: making, waiting for pictures, stopped (Continue must be there), and after the
 * app was closed while a film was being made.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, manifest = Config.NONE, qualifiers = "w393dp-h852dp-xhdpi")
public class UiShotsTest {
    static final File SHOTS = new File("build/ui_shots");

    /** The page's whole content drawn (its full scroll height, not only the screen). */
    static void shot(MainActivity a, String name) throws Exception {
        AppTest.idle();
        View root = ((ViewGroup) a.findViewById(android.R.id.content)).getChildAt(0);
        int w = a.findViewById(android.R.id.content).getWidth();
        if (w <= 0) w = 786;
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int h = Math.max(1, Math.min(root.getMeasuredHeight(), 12000));
        View content = root;
        android.widget.ScrollView sv = findScroll(root);
        if (sv != null && sv.getChildCount() > 0) content = sv.getChildAt(0);     // a scrolling page: all of it
        content.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        h = Math.max(1, Math.min(Math.max(h, content.getMeasuredHeight()), 12000));
        content.layout(0, 0, w, h);
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        c.drawColor(0xFFFFFFFF);
        content.draw(c);
        SHOTS.mkdirs();
        try (FileOutputStream fo = new FileOutputStream(new File(SHOTS, name + ".png"))) { bm.compress(Bitmap.CompressFormat.PNG, 100, fo); }
        StringBuilder sb = new StringBuilder();
        for (String t : AppTest.texts(root, new java.util.ArrayList<String>())) if (t.trim().length() > 0) sb.append(t.replace('\n', ' ')).append('\n');
        Files.write(new File(SHOTS, name + ".txt").toPath(), sb.toString().getBytes("UTF-8"));
    }


    static android.widget.ScrollView findScroll(View v) {
        if (v instanceof android.widget.ScrollView) return (android.widget.ScrollView) v;
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) { android.widget.ScrollView s = findScroll(((ViewGroup) v).getChildAt(i)); if (s != null) return s; }
        return null;
    }

    static Project sofaProject() throws Exception {
        Project p = Project.create(RuntimeEnvironment.getApplication());
        Files.copy(new File("../../tools/testdata/sofe_wali_jadu/sofe_wali_jadu_screenplay.txt").toPath(), new File(p.dir, "script.txt").toPath());
        return p;
    }

    @Test public void everyPageAsThePhoneShowsIt() throws Exception {
        android.content.Context ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("kahani", 0).edit().putString("online", "0").putString("skipLogin", "1").commit();
        Project p = sofaProject();
        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        AppTest.set(a, "project", p);
        AppTest.call(a, "showHome", new Class<?>[0]); shot(a, "1_home");
        AppTest.set(a, "project", p);             // (the home page closes the story)
        AppTest.call(a, "showStudio", new Class<?>[0]); shot(a, "2_studio");
        java.util.List<String> studio = AppTest.texts(a.findViewById(android.R.id.content), new java.util.ArrayList<String>());
        int make = -1;
        for (int i = 0; i < studio.size(); i++) if (studio.get(i).contains("Make film")) { make = i; break; }
        assertTrue("Make film is near the top of the studio (at " + make + " of " + studio.size() + ")", make >= 0 && make < 12);
        View front = AppTest.byText(a.findViewById(android.R.id.content), "+ Front", new int[]{0});
        assertNotNull("a character's front picture place", front);
        assertTrue("it can be seen and tapped (width " + front.getWidth() + ")", front.getWidth() > 40);
        int words = 0;
        for (String t : studio) if (t.trim().length() > 2) words++;          // (not the 📷 of an empty picture place)
        assertTrue("the studio is a few screens, not fifteen: " + words + " texts", words < 80);
        AppTest.call(a, "showStory", new Class<?>[0]); shot(a, "3_story");

        FilmJob j = new FilmJob(ctx, p);
        FilmJob.current = j;
        j.stage = "🎬 The director stages scene 1"; j.progress = 0.35f;
        AppTest.call(a, "showProgress", new Class<?>[0]); shot(a, "4_making");
        j.picturesWaiting = true; j.picturesNeeded = "वृंदा, वनुषा";
        AppTest.call(a, "showProgress", new Class<?>[0]); shot(a, "5_waiting");
        j.picturesWaiting = false; j.failed = true; j.error = "Example: the production review could not be read";
        AppTest.call(a, "showProgress", new Class<?>[0]); shot(a, "6_stopped");
        String stopped = new String(Files.readAllBytes(new File(SHOTS, "6_stopped.txt").toPath()), "UTF-8");
        assertTrue("a stopped film offers Continue:\n" + stopped, stopped.contains("Continue"));
        String making = new String(Files.readAllBytes(new File(SHOTS, "4_making.txt").toPath()), "UTF-8");
        assertFalse("no list of every character while the film is made:\n" + making, making.contains("no picture yet: add angles"));
        // the app closed while the film was being made: Continue on the film page and on the home page
        FilmJob.current = null;
        p.setSetting(FilmJob.MAKING, "1");
        AppTest.call(a, "showProgress", new Class<?>[0]); shot(a, "7_after_restart");
        String after = new String(Files.readAllBytes(new File(SHOTS, "7_after_restart.txt").toPath()), "UTF-8");
        assertTrue("an interrupted film offers Continue:\n" + after, after.contains("Continue"));
        AppTest.call(a, "showHome", new Class<?>[0]); shot(a, "8_home_after_restart");
        String home = new String(Files.readAllBytes(new File(SHOTS, "8_home_after_restart.txt").toPath()), "UTF-8");
        assertTrue("the home page offers to continue it:\n" + home, home.contains("Continue making your film"));
        p.setSetting(FilmJob.MAKING, "");
    }
}

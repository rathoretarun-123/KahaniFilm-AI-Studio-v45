package com.tarun.kahani;

import static org.junit.Assert.*;

import android.view.View;

import com.tarun.kahani.app.MainActivity;
import com.tarun.kahani.app.Project;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.android.controller.ActivityController;

import java.util.ArrayList;
import java.util.List;

/**
 * v47.2: the dashboard and the films screen, rebuilt 100,000 times on the real Android framework with the state
 * changing under them (films added and deleted, the account signed in and out, back taps in between). Every pass
 * must build without an exception and show the four tiles; the films tile must always say the true number.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, manifest = Config.NONE)
public class DashboardSoakTest {
    // (v47.3: the full 100,000 locally and on demand; on CI 3,000 so the build fits its time — the 45-minute job was cancelled)
    static final int PASSES = Integer.getInteger("kahani.dashboard.passes", System.getenv("CI") != null ? 3000 : 100000);

    @Test
    public void dashboardSurvivesOneHundredThousandRebuilds() throws Exception {
        android.content.Context ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("kahani", 0).edit().putString("online", "0").putString("skipLogin", "1").commit();
        java.lang.reflect.Field shared = com.tarun.kahani.app.Library.class.getDeclaredField("shared");
        shared.setAccessible(true);
        shared.set(null, null);
        for (Project p : Project.all(ctx)) MainActivityAccess.deleteDir(p.dir);

        ActivityController<MainActivity> ac = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = ac.get();
        View root = a.getWindow().getDecorView();
        List<Project> mine = new ArrayList<Project>();
        int homes = 0, films = 0, backs = 0;
        long t0 = System.currentTimeMillis();
        for (int pass = 0; pass < PASSES; pass++) {
            // the state moves under the screens: a film every 7th pass, one deleted every 11th, the account every 13th
            if (pass % 7 == 3) mine.add(Project.create(ctx));
            if (pass % 11 == 5 && !mine.isEmpty()) MainActivityAccess.deleteDir(mine.remove(0).dir);
            if (pass % 13 == 2) ctx.getSharedPreferences("kahani", 0).edit().putString("account", pass % 26 == 2 ? "tester@example.com" : "").commit();
            boolean toFilms = pass % 3 == 1;
            try {
                AppTest.call(a, toFilms ? "showFilms" : "showHome", new Class<?>[0]);
                AppTest.idle();
            } catch (Throwable e) {
                Throwable c = e instanceof java.lang.reflect.InvocationTargetException && e.getCause() != null ? e.getCause() : e;
                fail("pass " + pass + " (" + (toFilms ? "films" : "home") + ", " + mine.size() + " films): " + c);
            }
            String shown = AppTest.texts(root, new ArrayList<String>()).toString();
            int n = Project.all(ctx).size();
            if (toFilms) {
                films++;
                assertTrue("pass " + pass + " films screen: " + shown, shown.contains("My films") && shown.contains("Example"));
                if (n == 0) assertTrue("pass " + pass + " empty films screen: " + shown, shown.contains("No films yet"));
                else assertTrue("pass " + pass + " films listed: " + shown, shown.contains("Open") && shown.contains("Delete"));
                if (pass % 2 == 0) { a.onBackPressed(); AppTest.idle(); backs++; shown = AppTest.texts(root, new ArrayList<String>()).toString(); assertTrue("back from films lands on home: " + shown, shown.contains("New film")); }
            } else {
                homes++;
                assertTrue("pass " + pass + " home tiles: " + shown, shown.contains("New film") && shown.contains("My films") && shown.contains("Library") && shown.contains("Settings"));
                String count = n == 0 ? "None yet" : n == 1 ? "1 film" : n + " films";
                assertTrue("pass " + pass + " films tile says " + count + ": " + shown, shown.contains(count));
                assertFalse("pass " + pass + " nothing of the old dashboard: " + shown, shown.contains("Add pictures, voices or sounds"));
                assertTrue("pass " + pass + " version in the footer: " + shown, shown.contains("v47") || shown.contains("v4"));
                assertNotNull("pass " + pass + " the new-film tile answers a tap", AppTest.find(root, "New film"));
            }
            if (pass % 10000 == 9999) System.out.println("DASHBOARD_SOAK " + (pass + 1) + "/" + PASSES + " in " + (System.currentTimeMillis() - t0) / 1000 + "s");
        }
        System.out.println("DASHBOARD_SOAK done: " + homes + " home + " + films + " films screens, " + backs + " back taps, " + (System.currentTimeMillis() - t0) / 1000 + "s");
        assertEquals(PASSES, homes + films);
        for (Project p : mine) MainActivityAccess.deleteDir(p.dir);
    }

    /** The app's own recursive delete, reached through its package (the test must not keep a copy that could drift). */
    static final class MainActivityAccess {
        static void deleteDir(java.io.File d) throws Exception {
            java.lang.reflect.Method m = MainActivity.class.getDeclaredMethod("deleteDir", java.io.File.class);
            m.setAccessible(true);
            m.invoke(null, d);
        }
    }
}

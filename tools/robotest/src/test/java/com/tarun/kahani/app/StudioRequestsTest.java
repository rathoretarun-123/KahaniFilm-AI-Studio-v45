package com.tarun.kahani.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tarun.kahani.core.Story;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The user: "debug the studio page properly that is asking for only what is mention in script — 1) characters upto 18
 * pics each, no splitting beyond 3x2, so characters with objects remain intact 2) objects, upto 18 pics each 3) places
 * and background upto 18 images each 4) additional reference pictures upto 100 … no beyond 3x2 splitting 5) starting
 * page 6) ending page 7) reference images and videos upto 18 — also make sure film making doesn't stop any where, if
 * anything to ask, pause for 30 second to allow user to upload, otherwise continue" and "make sure app doesn't ask for
 * narrator photos at studio or any other place".
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, manifest = Config.NONE)
public class StudioRequestsTest {

    /** Two characters, a listed सूत्रधार and नैरेटर (narrators), a कथावाचक, a Narrator and a "सब" line, two places, two objects. */
    static final String HINDI = "शीर्षक: जादुई पतंग\n\n"
            + "पात्र परिचय\n1. राहुल - 10 साल, बेटा\n2. माँ - 38 साल, राहुल की माँ\n3. सूत्रधार - कहानी सुनाने वाला\n4. नैरेटर\n\n"
            + "जगह का परिचय\n1. घर की छत - खुली छत\n2. बाज़ार - भीड़ भरा बाज़ार\n\n"
            + "वस्तुओं का वर्णन\n1. पतंग - लाल रंग की बड़ी पतंग\n2. चरखी - लकड़ी की चरखी\n\n"
            + "स्क्रिप्ट शुरू\n\nदृश्य 1: घर की छत - सुबह\n\nकथावाचक: \"एक दिन की बात है।\"\n\n[राहुल छत पर पतंग उड़ाता है।]\n\n"
            + "राहुल: \"वाह!\"\n\nमाँ: \"नीचे आओ!\"\n\nसब: \"हाँ!\"\n\nसूत्रधार: \"और फिर…\"\n\n"
            + "दृश्य 2: बाज़ार - शाम\n\nनैरेटर: \"शाम हो गई।\"\n\nNarrator: \"And so it went.\"\n\n[राहुल छड़ी उठाता है।]\n\nराहुल: \"माँ, चरखी लो।\"\n\nसमाप्त\n";

    /** The same in English: a Narrator and a Voice-over listed as characters, "Everyone:" speaking. */
    static final String ENGLISH = "Title: The Lost Kite\n\nCharacters\n1. Rahul - 10, a boy\n2. Mother - 38, his mother\n3. Narrator - tells the story\n4. Voice-over\n\n"
            + "Places\n1. The roof of the house\n2. The market\n\nObjects\n1. Kite - a big red kite\n\nScript begins\n\n"
            + "Scene 1: The roof of the house - morning\n\nNarrator: \"One fine morning...\"\n\n[Rahul flies the kite on the roof. He picks up a stick.]\n\n"
            + "Rahul: \"Look!\"\n\nMother: \"Come down!\"\n\nEveryone: \"Yes!\"\n\nScene 2: The market - evening\n\nVoice-over: \"Evening came.\"\n\nRahul: \"Mother, the kite!\"\n\nThe End\n";

    @Before
    public void fresh() throws Exception {
        Field f = Library.class.getDeclaredField("shared");
        f.setAccessible(true);
        f.set(null, null);
        RuntimeEnvironment.getApplication().getSharedPreferences("kahani", 0).edit().putString("online", "0").putString("skipLogin", "1").commit();
        FilmJob.waitSeconds = FilmJob.WAIT_SECONDS;
    }

    @After
    public void restore() { FilmJob.waitSeconds = FilmJob.WAIT_SECONDS; FilmJob.current = null; }

    static Project project(String script) throws Exception {
        Project p = Project.create(RuntimeEnvironment.getApplication());
        p.write("script.txt", script);
        return p;
    }

    static Set<String> requests(Story st) {
        Set<String> out = new TreeSet<String>();
        for (StudioPlan.Request r : StudioPlan.requests(st)) out.add(r.kind + ":" + r.shown);
        return out;
    }

    static List<String> texts(View v, List<String> out) {
        if (v instanceof TextView) out.add(((TextView) v).getText().toString());
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) texts(((ViewGroup) v).getChildAt(i), out);
        return out;
    }

    static View byText(View v, String text) {
        if (v instanceof TextView && ((TextView) v).getText().toString().equals(text)) return v;
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) { View r = byText(((ViewGroup) v).getChildAt(i), text); if (r != null) return r; }
        return null;
    }

    static Object call(Object o, String m, Class<?>[] types, Object... args) throws Exception {
        Method mm = o.getClass().getDeclaredMethod(m, types);
        mm.setAccessible(true);
        return mm.invoke(o, args);
    }

    static void idle() { shadowOf(Looper.getMainLooper()).idle(); }

    // ------------------------------------------------------------------ 1-3: exactly what the script names

    @Test
    public void theRequestListIsExactlyTheScriptsCharactersObjectsAndPlaces() throws Exception {
        Project p = project(HINDI);
        Story st = FilmJob.storyOf(p);
        Set<String> want = new TreeSet<String>();
        want.add("char:राहुल"); want.add("char:माँ");
        want.add("obj:पतंग"); want.add("obj:चरखी");
        want.add("scene:घर की छत"); want.add("scene:बाज़ार");
        assertEquals("the Studio asks for exactly the script's characters, objects and places", want, requests(st));
        // the walking stick the action mentions is no request; the narrators and "सब" are never characters to picture
        for (Story.CharacterDef c : st.characters) if (StudioPlan.narratorLike(c)) assertTrue(c.displayName + " is a voice only", c.voiceOnly);
        for (Story.CharacterDef c : st.cast()) assertFalse("the narrator is not in the cast: " + c.displayName, StudioPlan.narratorLike(c) || StudioPlan.groupWord(c.displayName));

        Project e = project(ENGLISH);
        Story es = FilmJob.storyOf(e);
        Set<String> wantEn = new TreeSet<String>();
        wantEn.add("char:Rahul"); wantEn.add("char:Mother"); wantEn.add("obj:Kite");
        for (StudioPlan.Place pl : StudioPlan.places(es)) wantEn.add("scene:" + pl.shown);
        assertEquals("two places in the English script: " + wantEn, 5, wantEn.size());
        assertEquals(wantEn, requests(es));
        for (StudioPlan.Request r : StudioPlan.requests(es)) assertFalse("no stick, no narrator, no voice-over, no everyone: " + r, r.shown.equalsIgnoreCase("stick") || r.shown.contains("Narrator") || r.shown.contains("Voice") || r.shown.contains("Everyone"));

        // the Studio page shows one picture card per request and none for the narrator
        ActivityController<MainActivity> ac = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = ac.get();
        Field pf = MainActivity.class.getDeclaredField("project");
        pf.setAccessible(true);
        pf.set(a, p);
        call(a, "showStudio", new Class<?>[0]);
        idle();
        List<String> tx = texts(a.findViewById(android.R.id.content), new ArrayList<String>());
        int charButtons = 0, objButtons = 0, placeButtons = 0;
        for (String t : tx) {
            if (t.equals("📷 Up to " + StudioPlan.MAX_CHARACTER_PICTURES)) charButtons++;
            if (t.equals("📷 Angles") || t.equals("📷 More angles")) objButtons++;
            if (t.equals("📷 Pictures") || t.startsWith("📷 Pictures of ")) placeButtons++;
            assertFalse("no thing outside the script: " + t, t.startsWith("➕"));
        }
        assertEquals("a picture button per character: " + tx, 2, charButtons);
        assertEquals("a picture button per object: " + tx, 2, objButtons);
        assertEquals("a picture button per place: " + tx, 2, placeButtons);
        assertNull("the narrator is never offered a picture", byText(a.findViewById(android.R.id.content), "📷 Picture it anyway"));
        boolean narratorRow = false;
        for (String t : tx) if (t.contains("सूत्रधार") && t.contains("never pictured")) narratorRow = true;
        assertTrue("the listed सूत्रधार shows as a voice only: " + tx, narratorRow);
        // the make-film popup's "Pictures first" lists the same and nothing else
        call(a, "askBeforeMaking", new Class<?>[]{Runnable.class}, new Runnable() { public void run() { } });
        idle();
        android.app.AlertDialog mk = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        mk.getButton(android.content.DialogInterface.BUTTON_NEUTRAL).performClick();
        idle();
        android.app.AlertDialog list = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        List<String> items = new ArrayList<String>();
        for (int i = 0; i < list.getListView().getCount(); i++) items.add(String.valueOf(list.getListView().getItemAtPosition(i)));
        assertEquals("the popup lists the six requests: " + items, 6, items.size());
        for (String it : items) assertFalse("no narrator in the popup: " + it, it.contains("सूत्रधार") || it.contains("नैरेटर") || it.contains("कथावाचक"));
        list.dismiss();
        mk.dismiss();
        ac.pause().stop().destroy();
    }

    // ------------------------------------------------------------------ never a narrator's picture

    @Test
    public void theNarratorIsNeverAskedForAPictureAnywhere() throws Exception {
        Project p = project(HINDI);
        // even when "Picture it anyway" was chosen for it before
        p.setSetting("pictured.सूत्रधार", "1");
        p.setSetting("pictured.नैरेटर", "1");
        Story st = FilmJob.storyOf(p);
        String[] narrators = {"सूत्रधार", "नैरेटर", "कथावाचक", "Narrator"};
        // the Studio's list, the missing pictures (the film's wait), the film job's own question
        for (StudioPlan.Request r : StudioPlan.requests(st)) for (String n : narrators) assertFalse("Studio: " + r, r.shown.contains(n));
        for (String[] t : AutoLibrary.missingTargets(p, st)) for (String n : narrators) assertFalse("missing pictures: " + t[0], t[0].contains(n) || t[1].contains(n));
        for (String[] t : AutoLibrary.missingForUser(p, st)) for (String n : narrators) assertFalse("asked of the user: " + t[0], t[0].contains(n));
        FilmJob j = new FilmJob(RuntimeEnvironment.getApplication(), p);
        for (String t : j.neededPictures(st)) for (String n : narrators) assertFalse("the film's question: " + t, t.contains(n));
        // the production file for other apps asks no picture of a voice only
        String bible = com.tarun.kahani.core.Bible.write(st, null, new java.util.HashSet<String>(), new java.util.HashSet<String>());
        int at = bible.indexOf("सूत्रधार");
        assertTrue("the narrator is described as a voice", at >= 0);
        String entry = bible.substring(at, Math.min(bible.length(), bible.indexOf("\n\n", at) < 0 ? bible.length() : bible.indexOf("\n\n", at)));
        assertFalse("no picture asked of the narrator in the production file:\n" + entry, entry.contains("Picture:") || entry.contains("चित्र:") || entry.contains("Prompt:"));
        // the library's suggestions (pictures placed by the director) have no narrator target
        ActivityController<MainActivity> ac = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = ac.get();
        Field pf = MainActivity.class.getDeclaredField("project");
        pf.setAccessible(true);
        pf.set(a, p);
        Field cf = MainActivity.class.getDeclaredField("castStory");
        cf.setAccessible(true);
        cf.set(a, st);
        @SuppressWarnings("unchecked") List<String[]> targets = (List<String[]>) call(a, "pictureTargets", new Class<?>[]{Story.class}, st);
        for (String[] t : targets) for (String n : narrators) assertFalse("library suggestion target: " + t[0], t[0].contains(n));
        // the progress screen's picture card
        android.widget.LinearLayout card = new android.widget.LinearLayout(a);
        call(a, "picturesCard", new Class<?>[]{android.widget.LinearLayout.class, Story.class}, card, st);
        for (String t : texts(card, new ArrayList<String>())) for (String n : narrators) assertFalse("progress card: " + t, t.contains(n));
        ac.pause().stop().destroy();
        // the names a narrator goes by
        for (String n : new String[]{"कथावाचक", "सूत्रधार", "नैरेटर", "Narrator", "narrator", "Voice-over", "voice over", "VoiceOver"}) assertTrue(n, StudioPlan.narratorLike(n));
        for (String n : new String[]{"राहुल", "माँ", "Rahul", "Mother", "रेडियो की आवाज़"}) assertFalse(n, StudioPlan.narratorLike(n));
        for (String g : new String[]{"सब", "सभी", "दोनों", "Everyone", "all"}) assertTrue(g, StudioPlan.groupWord(g));
        assertFalse(StudioPlan.groupWord("सबरीना"));
    }

    // ------------------------------------------------------------------ the limits

    @Test
    public void theLimitsAre18PerCharacterObjectAndPlace100AdditionalAnd18References() throws Exception {
        assertEquals(18, StudioPlan.limitFor("char"));
        assertEquals(18, StudioPlan.limitFor("obj"));
        assertEquals(18, StudioPlan.limitFor("scene"));
        assertEquals(18, MainActivity.maxPictures("angles:char:राहुल:राहुल", null));
        assertEquals(18, MainActivity.maxPictures("angles:obj:पतंग:पतंग", null));
        assertEquals(18, MainActivity.maxPictures("angles:scene:1:घर की छत", null));
        assertEquals("one picture for one named view", 1, MainActivity.maxPictures("angles:char:राहुल:राहुल", "front"));
        assertEquals(100, Project.MAX_BOARD);
        assertEquals(100, StudioPlan.MAX_ADDITIONAL_REFERENCES);
        assertEquals(18, Project.MAX_REFERENCES);
        assertEquals(18, StudioPlan.MAX_REFERENCE_MEDIA);

        // 20 pictures given for one character / object / place: 18 are taken
        Project p = project(HINDI);
        Story st = FilmJob.storyOf(p);
        Library lib = Library.get(RuntimeEnvironment.getApplication());
        for (String tgt : new String[]{"angles:char:राहुल:राहुल", "angles:obj:पतंग:पतंग", "angles:scene:1:घर की छत"}) {
            List<byte[]> twenty = new ArrayList<byte[]>();
            for (int i = 0; i < 20; i++) twenty.add(photo(320, 240, 0xFF000000 | (i * 12) << 16 | 0x4080));
            String said = String.valueOf(SheetSaver.save(p, lib, st, tgt, twenty, null, null));
            assertTrue(tgt + ": " + said, said.contains("2 more left out: up to 18"));
        }
        assertTrue("a place keeps at most 18 of one upload: " + SheetSaver.placePictures(p, "1"), SheetSaver.placePictures(p, "1") <= 18);
        assertTrue("a thing keeps at most 18 of one upload: " + SheetSaver.thingPictures(p, "पतंग"), SheetSaver.thingPictures(p, "पतंग") <= 18);

        // reference images and videos: 18 in all, whichever mix
        File refs = p.refsDir();
        for (int i = 1; i <= 10; i++) new FileOutputStream(new File(refs, "frame_" + i + ".jpg")).close();
        for (int i = 1; i <= 8; i++) new FileOutputStream(new File(refs, "video_" + i + ".mp4")).close();
        assertEquals(18, p.references());
        assertNull("no 19th reference image", p.saveRefFrame(RuntimeEnvironment.getApplication(), android.net.Uri.parse("content://none/1")));
        assertNull("no 19th reference video", p.saveRefVideo(RuntimeEnvironment.getApplication(), android.net.Uri.parse("content://none/2")));
        // additional reference pictures: 100
        for (int i = 1; i <= 100; i++) new FileOutputStream(new File(p.boardDir(), String.format(java.util.Locale.US, "board_%03d.png", i))).close();
        assertEquals(100, p.boardPictures().size());
        assertNull("no 101st additional reference", p.saveBoardPicture(RuntimeEnvironment.getApplication(), android.net.Uri.parse("content://none/3")));
    }

    // ------------------------------------------------------------------ 3×2 at most

    @Test
    public void aSheetIsNeverSplitBeyond3x2() throws Exception {
        assertEquals(6, StudioPlan.MAX_SHEET_PANELS);
        List<Integer> n = new ArrayList<Integer>();
        for (int i = 0; i < 12; i++) n.add(i);
        assertEquals(0, StudioPlan.withinSheet(n.subList(0, 1)).size());
        assertEquals(2, StudioPlan.withinSheet(n.subList(0, 2)).size());
        assertEquals(6, StudioPlan.withinSheet(n.subList(0, 6)).size());
        assertEquals("seven pieces: kept whole", 0, StudioPlan.withinSheet(n.subList(0, 7)).size());
        assertEquals("twelve pieces: kept whole", 0, StudioPlan.withinSheet(n).size());
        // a drawn sheet of 3×2 figures splits; one of 4×2 (more than 3×2) stays one picture
        byte[] six = sheet(3, 2), eight = sheet(4, 2);
        assertTrue("a 3×2 sheet is a sheet", SheetSaver.isSheet(six, false));
        assertFalse("a 4×2 picture is not split", SheetSaver.isSheet(eight, false));
        Library lib = Library.get(RuntimeEnvironment.getApplication());
        List<Library.Item> six6 = SheetSaver.saveToLibrary(lib, "छह", "person", six, "test");
        assertTrue("the 3×2 sheet gives at most 6 pictures: " + six6.size(), six6.size() >= 2 && six6.size() <= 6);
        assertEquals("the 4×2 picture is not split in the library", 0, SheetSaver.saveToLibrary(lib, "आठ", "person", eight, "test").size());
        // through the upload: the 4×2 picture of a thing is kept as one picture
        Project p = project(HINDI);
        Story st = FilmJob.storyOf(p);
        List<byte[]> one = new ArrayList<byte[]>();
        one.add(eight);
        SheetSaver.save(p, lib, st, "angles:obj:पतंग:पतंग", one, null, null);
        assertEquals("kept whole: one picture of the thing", 1, SheetSaver.thingPictures(p, "पतंग"));
        List<byte[]> sheet6 = new ArrayList<byte[]>();
        sheet6.add(six);
        SheetSaver.save(p, lib, st, "angles:obj:चरखी:चरखी", sheet6, null, null);
        int got = SheetSaver.thingPictures(p, "चरखी");
        assertTrue("a 3×2 sheet of a thing gives at most six pictures: " + got, got >= 2 && got <= 6);
    }

    @Test
    public void additionalReferencesAreEachAtMost3x2MomentsAndNeverThePages() throws Exception {
        Project p = project(HINDI);
        Story st = FilmJob.storyOf(p);
        for (int i = 1; i <= 3; i++) {
            FileOutputStream o = new FileOutputStream(new File(p.boardDir(), String.format(java.util.Locale.US, "board_%03d.png", i)));
            o.write(sheet(3, 2));
            o.close();
        }
        com.tarun.kahani.core.Art art = new com.tarun.kahani.core.Art();
        com.tarun.kahani.core.StoryBoard b = FilmJob.readBoardSheets(art, p.loader(), p.boardNames(), st);
        assertNotNull(b);
        assertSame(b, art.board);
        assertNull("the first is not taken for the starting page", b.titleFile);
        assertNull("the last is not taken for the ending page", b.endFile);
        assertEquals("three pictures, six moments each", 18, b.moments.size());
        int[] perSheet = new int[4];
        for (com.tarun.kahani.core.StoryBoard.Moment m : b.moments) { perSheet[m.sheet]++; assertTrue("panel " + m.panel, m.panel >= 1 && m.panel <= 6); }
        for (int s = 1; s <= 3; s++) assertEquals("sheet " + s, 6, perSheet[s]);
        for (int i = 0; i < b.moments.size(); i++) assertEquals("story order", i, b.moments.get(i).order);
    }

    // ------------------------------------------------------------------ never stops: 30 seconds, then on

    @Test
    public void theFilmWaitsAtMostThirtySecondsForPicturesThenGoesOnByItself() throws Exception {
        assertEquals(30, FilmJob.WAIT_SECONDS);
        assertEquals(30, FilmJob.waitSeconds());
        FilmJob.waitSeconds = 2;            // the same wait, shortened for the test
        Project p = project(HINDI);
        final FilmJob job = new FilmJob(RuntimeEnvironment.getApplication(), p);
        job.askForPictures = true;
        Thread th = new Thread(job);
        th.start();
        try {
            long until = System.currentTimeMillis() + 120000;
            while (!job.picturesWaiting && th.isAlive() && System.currentTimeMillis() < until) Thread.sleep(50);
            assertTrue("the film asks for the pictures: " + job.stage + " " + job.error, job.picturesWaiting);
            assertTrue("it says what it waits for: " + job.picturesNeeded, job.picturesNeeded.contains("राहुल") && !job.picturesNeeded.contains("सूत्रधार"));
            int left = job.secondsLeft();
            assertTrue("a countdown of at most the wait: " + left, left >= 0 && left <= 2);
            // "I need more time": one more wait
            job.moreTime();
            int more = job.secondsLeft();
            assertTrue("more time given: " + left + " -> " + more, more > left && more <= 4);
            long t0 = System.currentTimeMillis();
            while (job.picturesWaiting && th.isAlive() && System.currentTimeMillis() - t0 < 20000) Thread.sleep(50);
            long waited = System.currentTimeMillis() - t0;
            assertFalse("no answer: the film went on by itself", job.picturesWaiting);
            assertTrue("it went on after the wait (" + waited + " ms)", waited <= 8000);
            assertEquals("the studio draws what has no picture", 3, job.pictureChoice);
            // it carries on making the film
            long t1 = System.currentTimeMillis();
            while (th.isAlive() && job.stage.startsWith("Waiting for you") && System.currentTimeMillis() - t1 < 20000) Thread.sleep(50);
            assertFalse("past the question: " + job.stage, job.stage.startsWith("Waiting for you"));
            assertTrue("it says so: " + job.info, job.info.contains("went on by itself"));
            assertFalse("the film did not stop: " + job.error, job.failed);
        } finally {
            job.cancel();
            th.join(120000);
        }
    }

    @Test
    public void theMakeFilmQuestionStartsTheFilmByItselfAfterThirtySeconds() throws Exception {
        Project p = project(HINDI);
        ActivityController<MainActivity> ac = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = ac.get();
        Field pf = MainActivity.class.getDeclaredField("project");
        pf.setAccessible(true);
        pf.set(a, p);
        final int[] started = {0};
        Runnable go = new Runnable() { public void run() { started[0]++; } };
        // 1. no answer: started by itself after 30 seconds, not before
        call(a, "askBeforeMaking", new Class<?>[]{Runnable.class}, go);
        idle();
        android.app.AlertDialog d1 = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        assertTrue(d1.isShowing());
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(28));
        assertEquals("not before 30 seconds", 0, started[0]);
        String shown = texts(d1.getWindow().getDecorView(), new ArrayList<String>()).toString();
        assertTrue("a visible countdown: " + shown, shown.contains("started by itself in"));
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3));
        assertEquals("started by itself", 1, started[0]);
        assertFalse(d1.isShowing());
        // 2. "I need more time": 30 seconds more
        call(a, "askBeforeMaking", new Class<?>[]{Runnable.class}, go);
        idle();
        android.app.AlertDialog d2 = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        View more = byText(d2.getWindow().getDecorView(), "⏳ I need more time (+30 s)");
        assertNotNull("a need-more-time button", more);
        more.performClick();
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(45));
        assertEquals("still waiting after 45 s", 1, started[0]);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(16));
        assertEquals("then started by itself", 2, started[0]);
        // 3. the progress screen's question offers more time too
        FilmJob j = new FilmJob(RuntimeEnvironment.getApplication(), p);
        FilmJob.current = j;
        j.picturesWaiting = true;
        j.picturesNeeded = "राहुल";
        j.autoContinueAt = System.currentTimeMillis() + 30000;
        call(a, "showProgress", new Class<?>[0]);
        idle();
        View more2 = byText(a.findViewById(android.R.id.content), "⏳ I need more time (+30 s)");
        assertNotNull("the film's own question offers more time", more2);
        int before = j.secondsLeft();
        more2.performClick();
        assertTrue("more time: " + before + " -> " + j.secondsLeft(), j.secondsLeft() >= before + 29);
        j.picturesWaiting = false;
        j.cancel();
        FilmJob.current = null;
        ac.pause().stop().destroy();
    }

    // ------------------------------------------------------------------ pictures for the tests

    static byte[] photo(int w, int h, int colour) {
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) px[y * w + x] = colour ^ ((x * 255 / w) << 8) ^ (y * 200 / h);
        b.setPixels(px, 0, w, 0, 0, w, h);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.JPEG, 90, out);
        return out.toByteArray();
    }

    /** A sheet of cols × rows standing figures (a head and a body), apart from each other, on white. */
    static byte[] sheet(int cols, int rows) {
        int cw = 220, ch = 330, w = cols * cw, h = rows * ch;
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.drawColor(0xFFFFFFFF);
        Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        int[] colours = {0xFFC0392B, 0xFF2471A3, 0xFF229954, 0xFF7D3C98, 0xFFD68910, 0xFF17A589, 0xFF884EA0, 0xFF2E4053};
        for (int r = 0; r < rows; r++) for (int k = 0; k < cols; k++) {
            float cx = k * cw + cw / 2f, top = r * ch + 30;
            pt.setColor(0xFFE0AC69);
            c.drawCircle(cx, top + 30, 28, pt);
            pt.setColor(colours[(r * cols + k) % colours.length]);
            c.drawRoundRect(new RectF(cx - 40, top + 58, cx + 40, top + 270), 18, 18, pt);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, out);
        return out.toByteArray();
    }

    static void assertSame(Object a, Object b) { org.junit.Assert.assertSame(a, b); }
}

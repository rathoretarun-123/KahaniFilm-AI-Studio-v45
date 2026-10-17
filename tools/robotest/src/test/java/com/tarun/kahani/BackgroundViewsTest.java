package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Bible;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;
import com.tarun.kahani.core.Txt;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * v47.8 (the user: "let him ask as many pictures as possible for background in the studio page as described in
 * script"): each place the script sets scenes in asks for its wide view, its reverse angle, its close views and each
 * thing of the story seen there — from the script's own words.
 */
public class BackgroundViewsTest {
    @Test public void aPlaceAsksForEveryViewItsScriptDescribes() throws Exception {
        Story st = ScriptParser.parse(new String(Files.readAllBytes(new File("../../tools/testdata/aakhiri_chai/aakhiri_chai_numbered.txt").toPath()), StandardCharsets.UTF_8));
        int places = 0;
        for (String[] pl : Bible.listedPlaces(st)) {
            List<Story.Scene> here = new ArrayList<Story.Scene>();
            for (Story.Scene sc : st.scenes) if (Txt.norm(sc.setting + " " + sc.title).contains(Txt.norm(Bible.firstClauseOf(pl[0]))) || Bible.similar(pl[0], sc.setting)) here.add(sc);
            if (here.isEmpty()) continue;
            places++;
            List<String[]> v = Bible.backgroundViews(st, pl[0], pl[1] == null ? "" : pl[1], here);
            assertTrue(v.get(0)[1].startsWith("The wide view"));
            assertTrue(v.get(1)[1].startsWith("The reverse angle"));
            assertTrue(v.get(2)[1].startsWith("The close views"));
            int things = 0;
            for (String[] x : v) if ("thing".equals(x[0])) { things++; assertTrue(x[2].length() > 0); }
            assertTrue("the things of the story seen in " + pl[0], things > 0);
        }
        assertTrue("a place with scenes", places > 0);
    }

    @Test public void aThingIsSeenByItsNameItsBracketsOrItsStem() {
        assertTrue(Bible.seenIn("छत का पंखा (Ceiling Fan)", Txt.norm("ऊपर पंखे की हवा चलती है").toLowerCase()));
        assertTrue(Bible.seenIn("गोल दीवार घड़ी (Round Wall Clock)", Txt.norm("the round wall clock ticks").toLowerCase()));
        assertFalse(Bible.seenIn("छत का पंखा (Ceiling Fan)", Txt.norm("सोफ़े पर तकिया").toLowerCase()));
    }
}

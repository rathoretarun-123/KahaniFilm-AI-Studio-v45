package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;
import com.tarun.kahani.core.StoryBoard;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * v47.8 (the user: up to 100 story-board pictures, "3x2 grid as per the story … more than one character at once with
 * objects as well, without background … starting title and end image should be only one"): the first and the last
 * pictures are the title and the end (never split), each sheet between is read as six moments, each moment's characters
 * are known by their colours left to right, and the director places the moments at their shots in story order and
 * reports a moment staged the other way round.
 */
public class StoryBoardTest {
    static final int RED = 0xFFD03030, BLUE = 0xFF3050D0, GREEN = 0xFF30B040;

    /** A clear picture {w, h, px…} with filled boxes {x0, y0, x1, y1, colour}. */
    static int[] picture(int w, int h, int[]... boxes) {
        int[] d = new int[2 + w * h];
        d[0] = w; d[1] = h;
        for (int[] b : boxes) for (int y = b[1]; y < b[3]; y++) for (int x = b[0]; x < b[2]; x++) d[2 + y * w + x] = b[4];
        return d;
    }

    static Art.Loader loader(final Map<String, int[]> pics) {
        return new Art.Loader() {
            public int[] decode(String name, int maxSide) { return pics.get(name); }
            public Object create(int[] argb, int w, int h) { return new Object(); }
            public Object load(String name, int maxSide) { return pics.containsKey(name) ? new Object() : null; }
            public int width(Object img) { return 10; }
            public int height(Object img) { return 10; }
        };
    }

    @Test public void theBoardIsReadPlacedAndChecked() throws Exception {
        Story story = ScriptParser.parse(new String(Files.readAllBytes(new File("../../tools/testdata/aakhiri_jodi/aakhiri_jodi_screenplay.txt").toPath()), StandardCharsets.UTF_8));
        assertTrue(story.characters.size() >= 2);
        Story.CharacterDef a = story.characters.get(0), b = story.characters.get(1);
        Art art = new Art();
        Art.Sprite sa = new Art.Sprite(), sb = new Art.Sprite();
        int[] red = picture(40, 80, new int[]{0, 0, 40, 80, RED}), blue = picture(40, 80, new int[]{0, 0, 40, 80, BLUE});
        sa.colours = StoryBoard.signature(red, 2, 40, 0, 0, 40, 80);
        sb.colours = StoryBoard.signature(blue, 2, 40, 0, 0, 40, 80);
        art.sprites.put(a.id, sa); art.sprites.put(b.id, sb);
        // a sheet 600×400: moment 1 red left of blue; moment 2 blue left of red; moment 3 only a green thing; 4–6 empty
        int[] sheet = picture(600, 400,
                new int[]{20, 60, 70, 190, RED}, new int[]{120, 60, 170, 190, BLUE},
                new int[]{220, 60, 270, 190, BLUE}, new int[]{320, 60, 370, 190, RED},
                new int[]{450, 120, 500, 160, GREEN});
        Map<String, int[]> pics = new HashMap<String, int[]>();
        pics.put("title", picture(60, 40, new int[]{0, 0, 60, 40, RED}));
        pics.put("sheet", sheet);
        pics.put("end", picture(60, 40, new int[]{0, 0, 60, 40, BLUE}));
        StoryBoard board = StoryBoard.read(loader(pics), Arrays.asList("title", "sheet", "end"), art, story);
        assertEquals("title", board.titleFile);
        assertEquals("end", board.endFile);
        assertEquals("one sheet, six moments", 6, board.moments.size());
        StoryBoard.Moment m1 = board.moments.get(0), m2 = board.moments.get(1), m3 = board.moments.get(2);
        assertEquals(Arrays.asList(a.id, b.id), m1.who);
        assertEquals(Arrays.asList(b.id, a.id), m2.who);
        assertTrue(m3.who.isEmpty());
        assertEquals("the green box is a thing", 1, m3.things);
        for (int i = 3; i < 6; i++) assertTrue("moment " + (i + 1) + " is empty", board.moments.get(i).empty);
        // a single picture is a sheet; two pictures are the title and the end
        assertArrayEquals(new int[]{0}, StoryBoard.roles(1));
        assertArrayEquals(new int[]{1, 2}, StoryBoard.roles(2));
        assertArrayEquals(new int[]{1, 0, 0, 2}, StoryBoard.roles(4));

        // the director places them at their shots, in story order, and reports the one staged the other way round
        Director d = new Director(story, new Director.Options());
        d.prepare();
        Film film = d.direct(new Art());
        board.place(film, story);
        assertTrue("moment 1 is placed", m1.shot >= 0);
        assertTrue("moment 2 is placed at or after moment 1", m2.shot >= m1.shot);
        boolean otherWay = false;
        for (String f : board.findings) if (f.contains("the other way round")) otherWay = true;
        assertTrue("one of two opposite moments differs from the stage: " + board.findings, otherWay);
        assertTrue("the shot list names the board", film.shotList.contains("STORY BOARD"));
        // the production check reports it
        com.tarun.kahani.core.ProductionDocument doc = com.tarun.kahani.core.ProductionDocument.read(story, null);
        doc.bind(story); doc.attach(film); doc.validate();
        boolean reported = false;
        for (com.tarun.kahani.core.ProductionDocument.Issue i : doc.issues) if ("STORY_BOARD".equals(i.category)) reported = true;
        assertTrue("the production document carries the board's findings", reported);
    }

    @Test public void aBoardOfNothingChangesNothing() {
        StoryBoard b = StoryBoard.read(loader(new HashMap<String, int[]>()), java.util.Collections.<String>emptyList(), new Art(), null);
        assertTrue(b.moments.isEmpty());
        assertNull(b.titleFile);
    }
}

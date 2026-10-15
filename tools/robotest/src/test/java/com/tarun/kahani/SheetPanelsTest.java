package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Angles;
import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.Test;

/**
 * v47: a contact sheet of place pictures is cut at its gutters into its real panels (not an even 3x2 grid that put a
 * third of the next panel into each crop), a panel's gutter and rounded corners are kept out of the frame, and a
 * markdown script reads the same as a plain one.
 */
public class SheetPanelsTest {
    /** A 3x2 sheet of panels on a light paper with 16 px gutters, every panel its own flat colour. */
    static int[] sheet(int w, int h, int cols, int rows, int gutter, int paper) {
        int[] px = new int[w * h];
        java.util.Arrays.fill(px, paper);
        int pw = (w - gutter * (cols + 1)) / cols, ph = (h - gutter * (rows + 1)) / rows;
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            int x0 = gutter + c * (pw + gutter), y0 = gutter + r * (ph + gutter), colour = 0xFF000000 | (40 + 30 * c) << 16 | (90 + 20 * r) << 8 | 60;
            for (int y = y0; y < y0 + ph; y++) for (int x = x0; x < x0 + pw; x++) px[y * w + x] = colour;
        }
        return px;
    }

    @Test public void aSheetIsCutAtItsGuttersInReadingOrder() {
        int w = 980, h = 420;
        int[] px = sheet(w, h, 3, 2, 16, 0xFFFAF0EE);
        List<int[]> panels = Angles.sheetPanels(px, w, h);
        assertNotNull(panels);
        assertEquals(6, panels.size());
        int[] first = panels.get(0), second = panels.get(1), fourth = panels.get(3);
        assertEquals(16, first[0]); assertEquals(16, first[1]);
        assertTrue("second panel is to the right of the first", second[0] > first[2]);
        assertTrue("fourth panel starts the second row", fourth[1] > first[3] && fourth[0] == first[0]);
        for (int[] b : panels) { assertTrue(b[2] - b[0] > 280); assertTrue(b[3] - b[1] > 160); }
    }

    @Test public void aPictureWithoutGuttersIsNotCut() {
        int w = 640, h = 360;
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) px[y * w + x] = 0xFF000000 | ((x * 255 / w) << 16) | ((y * 255 / h) << 8) | 90;
        assertNull(Angles.sheetPanels(px, w, h));
        assertNull(Art.panelInset(px, w, h));
    }

    @Test public void aPanelsGutterAndRoundedCornersAreKeptOutOfTheCrop() {
        int w = 640, h = 400, g = 24, r = 30, paper = 0xFFFAF0EE;
        int[] px = new int[w * h];
        java.util.Arrays.fill(px, paper);
        for (int y = g; y < h - g; y++) for (int x = g; x < w - g; x++) {
            // the picture inside the gutter, its corners rounded with radius r
            int cx = x < g + r ? g + r : x >= w - g - r ? w - g - r - 1 : x, cy = y < g + r ? g + r : y >= h - g - r ? h - g - r - 1 : y;
            if ((x - cx) * (x - cx) + (y - cy) * (y - cy) > r * r) continue;
            px[y * w + x] = 0xFF000000 | ((x * 200 / w) << 16) | (120 << 8) | (y * 200 / h);
        }
        float[] in = Art.panelInset(px, w, h);
        assertNotNull("the frame is measured", in);
        assertTrue("left inset covers the gutter and the corner: " + in[0] * w, in[0] * w >= g + 6);
        assertTrue("top inset covers the gutter and the corner: " + in[1] * h, in[1] * h >= g + 6);
        assertTrue("never more than a fifth: " + in[0], in[0] < 0.2f && in[1] < 0.2f);
        Art.Backdrop b = new Art.Backdrop();
        b.w = w; b.h = h;
        Art.fitCrop(b, in[0], in[1], in[2], in[3]);
        assertTrue(b.x0 >= in[0] - 1e-4f && b.x1 <= 1 - in[2] + 1e-4f);
        assertTrue(b.y0 >= in[1] - 1e-4f && b.y1 <= 1 - in[3] + 1e-4f);
        float ar = (b.x1 - b.x0) * w / ((b.y1 - b.y0) * h);
        assertEquals(16f / 9f, ar, 0.02f);
    }

    @Test public void aMarkdownScriptReadsLikeAPlainOne() throws Exception {
        File script = new File("../../tools/testdata/aakhiri_jodi/aakhiri_jodi_screenplay.txt");
        assertTrue(script.isFile());
        Story st = ScriptParser.parse(new String(Files.readAllBytes(script.toPath()), StandardCharsets.UTF_8));
        assertEquals("आखिरी जोड़ी", st.title);
        assertEquals(st.characters.toString(), 2, st.characters.size());
        for (Story.CharacterDef c : st.characters) {
            assertFalse(c.fullName, c.fullName.contains("*"));
            for (String a : c.aliases) assertFalse(a, a.contains("*") || a.matches("\\d+\\."));
        }
        assertEquals("इस्माइल चाचा", st.characters.get(0).fullName);
        assertEquals(1, st.scenes.size());
        assertFalse(st.scenes.get(0).title, st.scenes.get(0).title.contains("*"));
        int lines = 0;
        for (Story.Beat b : st.scenes.get(0).beats) if (b.type == Story.Beat.DIALOGUE) lines++;
        assertEquals(13, lines);
    }
}
